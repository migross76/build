package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import data.Appearances;
import data.HOF;
import data.Master;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;

public class HallUsage {
  private static Master.Table _mt = null;
  private static War.ByIDAlone _wBy = null;
  private static WeightedApp.ByID _waBy = null;
  private static Teams.Table _tt = null;
  private static HOF.Table _ht = null;
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    War.Table wt = new War.Table(db, type); 
    _wBy.addAll(wt);
    _waBy.addAll(new WeightedApp.Tally(wt, AT));
  }
  
  public static class Franch implements Comparable<Franch> {
    public Franch(String id) { _id = id; }
    public final String _id;
    public double _score = 0;

    @Override public int compareTo(Franch arg0) {
      return -Double.compare(_score, arg0._score); 
    }
  }
  
  public static void main(String[] args) throws Exception {
    _wBy = new War.ByIDAlone();
    _waBy = new WeightedApp.ByID();
    try (MyDatabase db = new MyDatabase()) {
      Appearances.Table at = new Appearances.Table(db);
      _mt = new Master.Table(db, Sort.UNSORTED);
      assemble(db, at, Type.BAT);
      assemble(db, at, Type.PITCH);
      _tt = new Teams.Table(db);
      _ht = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED);
    }
    TreeSet<String> players = new TreeSet<>();
    for (List<HOF> player : _ht.byID().values()) {
      players.add(player.get(0).hofID());
    }
    for (String id : players) {
      ByPlayer<WeightedApp> wapps = _waBy.get(id);
      ByPlayer<War> wars = _wBy.get(id);
      Master m = _mt.byID(id);
      if (wapps == null || wars == null || m == null) { continue; }
      
      War warTot = wars.total();
      System.out.format("%s\t%s %s", m.playerID(), m.nameFirst(), m.nameLast());
      for (WeightedApp.Use use : wapps.total()) {
        double tot = use.games() / warTot.war() * 100;
        if (tot < 20) { continue; }
        System.out.format("\t%s\t%d", use.pos().getName(), (int)tot);
      }
      System.out.println();
    }
    System.out.println();
    
    TreeSet<String> active = new TreeSet<>();
    for (Teams team : _tt.year(1940)) {
      active.add(team.franchID());
    }
    System.out.print("ID\tName");
    for (String fr : active) { System.out.format("\t%s", fr); }
    System.out.println();
    for (String id : players) {
      ByPlayer<War> wars = _wBy.get(id);
      Master m = _mt.byID(id);
      
      double total = 0;
      TreeMap<String, Franch> byFranch = new TreeMap<>();
      if (wars != null) {
        for (War w : wars) {
          String franch = _tt.getFranchiseID(w.teamID());
          if (!active.contains(franch)) { continue; }
          Franch val = byFranch.get(franch);
          if (val == null) { byFranch.put(franch, val = new Franch(franch)); }
          val._score += w.war();
          total += w.war();
        }
      }
      System.out.format("%s\t%s %s", m.playerID(), m.nameFirst(), m.nameLast());
      if (total == 0) { System.out.println(); continue; }
      for (String fr : active) {
        Franch vals = byFranch.get(fr);
        double val = vals == null ? 0 : vals._score;
        //val = val / total * 100;
        if (val < 0.5) { System.out.print("\t"); }
        else { System.out.format("\t%d", (int)val); }
      }
/*      
      ArrayList<Franch> franchs = new ArrayList<>(byFranch.values());
      Collections.sort(franchs);
      for (Franch franch : franchs) {
        double tot = franch._score / warTot.war() * 100;
        if (tot < 0.5) { continue; }
        if (!active.contains(franch._id)) { continue; } // only active franchises
        System.out.format("\t%s\t%d", franch._id, (int)tot);
      }
*/
      System.out.println();
    }
    
  }

}
