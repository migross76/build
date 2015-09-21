package app;

import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import util.ByPlayer;
import util.MyDatabase;
import data.HOF;
import data.Master;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;

/*
 * determine how many HOFs were active in each season
 */
public class ActiveHOF {
  public static class Season {
    public int _year;
    public int _teams;
    public HashSet<HOF> _players = new HashSet<>();
  }
  
  public static void main(String[] args) throws Exception {
    Master.Table mt = null;
    HOF.Table ht = null;
    Teams.Table tt = null;
    War.ByID wt = new War.ByID();
    try (MyDatabase db = new MyDatabase()) {
      mt = new Master.Table(db, Sort.UNSORTED);
      ht = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED);
      tt = new Teams.Table(db);
      wt.addAll(new War.Table(db, Type.BAT));
      wt.addAll(new War.Table(db, Type.PITCH));
    }
    TreeMap<Integer, Season> seasons = new TreeMap<>();
    for (int i_yr = tt.yearFirst(), e_yr = tt.yearLast() + 1; i_yr != e_yr; ++i_yr) {
      Season s = new Season();
      s._year = i_yr;
      s._teams = tt.year(i_yr).size();
      seasons.put(i_yr, s);
    }
    for (List<HOF> player : ht.byID().values()) {
      HOF h = player.get(0);
      Master m = mt.byID(h.hofID());
      if (m == null) { continue; }
      ByPlayer<War> bpw = wt.get(m.playerID());
      if (bpw == null) { continue; }
      for (War w : bpw) {
        Season s = seasons.get(w.yearID());
        s._players.add(h);
      }
    }
    
    System.out.print("Year");
    for (int yr = 10; yr != 55; yr += 5) {
      System.out.format("\t%d-%d", yr-4, yr);
    }
    System.out.println("\t51+");
    for (Season s : seasons.values()) {
      System.out.format("%d", s._year);
      int i_yr = s._year;
      for (int e_yr = s._year + 10; i_yr < s._year + 50; i_yr = e_yr + 1, e_yr += 5) {
        int count = 0;
        for (HOF h : s._players) { if (h.yearID() >= i_yr && h.yearID() <= e_yr) { ++count; } }
        System.out.format("\t%d", count);
      }
      int count = 0;
      for (HOF h : s._players) { if (h.yearID() >= i_yr) { ++count; } }
      System.out.format("\t%d", count);
      System.out.println();
    }
  }
}
