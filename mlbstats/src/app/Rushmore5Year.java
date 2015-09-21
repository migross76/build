package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.Master;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;

/* Inspired by http://www.highheatstats.com/2012/05/baseball-mount-rushmores/?utm_source=rss&utm_medium=rss&utm_campaign=baseball-mount-rushmores
 * 
 * Pick the top N players for each active team/franchise.
 * - Only counts WAA from their team/franchise
 * - Only picks the top N seasons
 */
public class Rushmore5Year {
  private static final int SEASONS = 5;
  private static final int FACES = 8;
  private static final int START_YEAR = 1801;
  
  public static class Player implements Comparable<Player> {
    public Player(Master M) { _master = M; }
    
    public Master _master = null;
    
    
    public void add(double val) {
      if (_values[0] < val) { _total += val - _values[0]; _values[0] = val; Arrays.sort(_values); }
    }
    
    public double[] _values = new double[SEASONS];
    public double _total = 0;

    @Override public int compareTo(Player arg0) {
      if (_total != arg0._total) { return _total > arg0._total ? -1 : 1; } 
      return _master.playerID().compareTo(arg0._master.playerID());
    }
    
  }
  
  private static void add(TreeMap<String, ArrayList<Player>> map, String id, Master M, double year) {
    if (id == null) { System.err.println("Unknown id"); return; }
    ArrayList<Player> list = map.get(id);
    if (list == null) { map.put(id, list = new ArrayList<>()); }
    Player P = list.isEmpty() ? null : list.get(list.size() - 1);
    if (P == null || P._master != M) { list.add(P = new Player(M)); }
    P.add(year);
  }
  
  private static void print(TreeMap<String, ArrayList<Player>> map, String title) {
    System.out.println(title);
    for (Map.Entry<String, ArrayList<Player>> E : map.entrySet()) {
      Collections.sort(E.getValue());
      int end = FACES < E.getValue().size() ? FACES : E.getValue().size();
      for (int i = 0; i != end; ++i) {
        Player P = E.getValue().get(i);
        System.out.format("%s\t%s %s\t%.1f\n", E.getKey(), P._master.nameFirst(), P._master.nameLast(), P._total);
      }
    }
    System.out.println();
  }
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
    WWT.adjustByPosition(AT);
    byWW.addAll(WWT);
    WeightedApp.Tally WA = new WeightedApp.Tally(WWT, AT);
    byWA.addAll(WA);
  }
  
  private static WeightedApp.ByID byWA = new WeightedApp.ByID();
  private static WeightedWar.ByID byWW = new WeightedWar.ByID();

  public static void main(String[] args) throws Exception {
    Master.Table MT = null;
    Teams.Table TT = null;
    WeightedWar.ByID wBy = new WeightedWar.ByID();
    wBy.addFilter(WeightedWar.filterPositive);
    try (MyDatabase db = new MyDatabase()) {
      TT = new Teams.Table(db);
      Appearances.Table AT = new Appearances.Table(db);
      MT = new Master.Table(db, Sort.UNSORTED);
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }
    
    TreeMap<String, ArrayList<Player>> franchises = new TreeMap<>();
    TreeMap<String, ArrayList<Player>> teams = new TreeMap<>();
    
    for (Teams T : TT.year(TT.yearLast())) {
      franchises.put(T.franchID()+"\t", new ArrayList<Player>());
    }
    
    for (ByPlayer<WeightedWar> wwBy : byWW) {
      if (wwBy.first().yearID() < START_YEAR) { continue; }
      Master M = MT.byID(wwBy.total().playerID());
//      if (HT.idFirst(M.hofID()) == null) { continue; }
      for (WeightedWar W : wwBy) {
        Teams T = TT.team(W.yearID(), W.teamID());
        if (T != null && franchises.containsKey(T.franchID()+"\t")) {
          add(teams, T.franchID()+"\t"+T.teamID(), M, W.wwar());
          add(franchises, T.franchID()+"\t", M, W.wwar());
        }
      }
    }
    print(teams, "Teams");
    print(franchises, "Franchises");
  }
}
