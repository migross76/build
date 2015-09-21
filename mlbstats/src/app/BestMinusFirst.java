package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import util.ByPlayer;
import util.WeightedApp;
import data.Appearances;
import data.Master;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;
import util.MyDatabase;

// Calculates the best players in career WAR, minus the team they earned the most WAR for
// Therefore, one-team players (e.g., Gehrig, Ripken, Williams) won't qualify for this
// A-Rod is pretty strong in this, though he won't be earning any more, since NYY is his top team
public class BestMinusFirst {
  public static final double MIN_SECONDARY = 10; // Minimal score for secondary team, (double this for single team contributions)
  
  public static class Player implements Comparable<Player> {
    public Player ( War total, FValue one, FValue two, double score ) { _total = total; _one = one; _two = two; _score = score; }
    
    public War _total = null;
    public FValue _one = null;
    public FValue _two = null;
    
    public double _score = 0;
    
    @Override
    public int compareTo(Player arg0) {
      if (_score != arg0._score) { return _score > arg0._score ? -1 : 1; }
      return _total.playerID().compareTo(arg0._total.playerID());
    }
  }
  
  public static class FValue implements Comparable<FValue> {
    public FValue(String franchID) { _franchID = franchID; }
    
    public String _franchID = null;
    public double _value = 0;
    @Override public int compareTo(FValue arg0) {
      if (_value != arg0._value) { return _value > arg0._value ? -1 : 1; }
      return _franchID.compareTo(arg0._franchID);
    }
  }
  
  public static void main(String[] args) throws Exception {
    Master.Table MT = null;
    Teams.Table TT = null;
    Appearances.Table AT = null;
    War.ByID wBy = new War.ByID();
    wBy.addFilter(War.filterPositive);
    WeightedApp.ByID WAby = new WeightedApp.ByID();
    try (MyDatabase db = new MyDatabase()) {
      MT = new Master.Table(db, Sort.UNSORTED);
      TT = new Teams.Table(db);
      AT = new Appearances.Table(db);
      wBy.addAll(new War.Table(db, Type.BAT));
      wBy.addAll(new War.Table(db, Type.PITCH));
      WeightedApp.assemble(db, WAby, AT, Type.BAT);
      WeightedApp.assemble(db, WAby, AT, Type.PITCH);
    }
    
    ArrayList<Player> next = new ArrayList<>();
    for (ByPlayer<War> WW : wBy) {
      War pos = WW.filter(War.filterPositive);
      if (pos == null || pos.war() < MIN_SECONDARY * 2) { continue; }
      HashMap<String, FValue> map = new HashMap<>();
      for (War W : WW) {
        String franchID = TT.getFranchiseID(W.teamID());
        if (franchID == null) { continue; }
        FValue FV = map.get(franchID);
        if (FV == null) { map.put(franchID, FV = new FValue(franchID)); }
        FV._value += W.war();
      }
      ArrayList<FValue> vals = new ArrayList<>(map.values());
      Collections.sort(vals);
      double score = WW.total().war() - vals.get(0)._value;
      if (vals.size() > 1 && score >= MIN_SECONDARY) {
        next.add(new Player(WW.total(), vals.get(0), vals.get(1), score));
      }
    }
    
    Collections.sort(next);
    
    for (Player P : next) {
      Master M = MT.byID(P._total.playerID());
      WeightedApp WA = WAby.get(P._total.playerID()).total();
      System.out.format("%.1f\t%s\t%s %s\t%.1f\t%s\t%.1f\t%s\t%.1f\n", P._score, WA.primary().pos().getName(), M.nameFirst(), M.nameLast(), P._total.war(), P._one._franchID, P._one._value, P._two._franchID, P._two._value);
    }
  }
}
