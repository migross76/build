package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import util.ByPlayer;
import util.MyDatabase;
import data.Master;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;

/* Inspired by http://www.highheatstats.com/2012/05/baseball-mount-rushmores/?utm_source=rss&utm_medium=rss&utm_campaign=baseball-mount-rushmores
 * 
 * Pick the top N players for each active team/franchise.
 * - Treats WAR for their team separate from WAR for other teams
 * - Overall WAR = 1.0 team, 1.0 others
 * - Team WAR = 1.0 team, 0.0 others
 * - Good Rushmore = 1.0 team, -0.5 others
 * - They Played Where? = -2.0 team, 1.0 others
 */
public class Rushmore {
  private static final double TEAM_FACTOR = 1.0;
  private static final double OTHER_FACTOR = -0.5;
  private static final int FACES = 8;
  private static final int START_YEAR = 1801;
  
  public static class Player implements Comparable<Player> {
    public Player(Master M, double value) { _master = M; _value = value; }
    
    public Master _master = null;
    public double _value = 0;
    @Override public int compareTo(Player arg0) {
      if (_value != arg0._value) { return _value > arg0._value ? -1 : 1; } 
      return _master.playerID().compareTo(arg0._master.playerID());
    }
    
  }
  
  private static void add(TreeMap<String, ArrayList<Player>> map, String id, Master M, double year, double total) {
    if (id == null) { System.err.println("Unknown id"); return; }
    ArrayList<Player> list = map.get(id);
    if (list == null) { map.put(id, list = new ArrayList<>()); }
    Player P = list.isEmpty() ? null : list.get(list.size() - 1);
    if (P == null || P._master != M) { list.add(P = new Player(M, total * OTHER_FACTOR)); }
    P._value += year * (TEAM_FACTOR-OTHER_FACTOR);
  }
  
  private static void print(TreeMap<String, ArrayList<Player>> map, String title) {
    System.out.println(title);
    for (Map.Entry<String, ArrayList<Player>> E : map.entrySet()) {
      Collections.sort(E.getValue());
      int end = FACES < E.getValue().size() ? FACES : E.getValue().size();
      System.out.print(E.getKey());
      for (int i = 0; i != end; ++i) {
        Player P = E.getValue().get(i);
        System.out.format("\t%s %s\t%.1f", P._master.nameFirst(), P._master.nameLast(), P._value);
      }
      System.out.println();
    }
    System.out.println();
  }
  
  public static void main(String[] args) throws Exception {
    Master.Table MT = null;
    Teams.Table TT = null;
//    HOF.Table HT = null;
    War.ByID wBy = new War.ByID();
    wBy.addFilter(War.filterPositive);
    try (MyDatabase db = new MyDatabase()) {
  //    Appearances.Table AT = new Appearances.Table();
      MT = new Master.Table(db, Sort.UNSORTED);
  //    assemble(AT, Type.BAT);
  //    assemble(AT, Type.PITCH);
      TT = new Teams.Table(db);
//      HT = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED);
      wBy.addAll(new War.Table(db, Type.BAT));
      wBy.addAll(new War.Table(db, Type.PITCH));
    }
    
    TreeMap<String, ArrayList<Player>> franchises = new TreeMap<>();
    TreeMap<String, ArrayList<Player>> teams = new TreeMap<>();
    
    for (Teams T : TT.year(TT.yearLast())) {
      franchises.put(T.franchID()+"\t", new ArrayList<Player>());
    }
    
    for (ByPlayer<War> wwBy : wBy) {
      if (wwBy.first().yearID() < START_YEAR) { continue; }
      Master M = MT.byID(wwBy.total().playerID());
//      if (HT.idFirst(M.hofID()) == null) { continue; }
      for (War W : wwBy) {
        Teams T = TT.team(W.yearID(), W.teamID());
        if (T != null && franchises.containsKey(T.franchID()+"\t")) {
          add(teams, T.franchID()+"\t"+T.teamID(), M, W.war(), wwBy.total().war());
          add(franchises, T.franchID()+"\t", M, W.war(), wwBy.total().war());
        }
      }
    }
    print(teams, "Teams");
    print(franchises, "Franchises");
  }
}
