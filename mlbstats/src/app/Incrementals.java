package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;
import util.ByPlayer;
import util.MyDatabase;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

// Draft a different kind of HOF
// - First, select top 20 players of all-time (by wWAR)
// - Next, select next 19 players based on top wWAR of 19-year span
// - Continue until last 1 player selected based on top single season
public class Incrementals {
  private static final int STARTING_NUMBER = 20;
  private static final int LAST_YEAR = 1910;
  
  public static Comparator<ByPlayer<War>> _comp_positive = new Comparator<ByPlayer<War>>() {
    @Override
    public int compare(ByPlayer<War> o1, ByPlayer<War> o2) {
      int cmp = o1.filter(War.filterPositive).compareTo(o2.filter(War.filterPositive));
      if (cmp != 0) { return cmp; }
      return o1.total().compareTo(o2.total());
    }
  };
  
  public static class Player implements Comparable<Player> {
    public Master _master = null;
    public ByPlayer<War> _bp = null;
    public int _yearSpan = 0;
    public int _yearStart = 0;
    public int _yearEnd = 0;
    public double _war = 0;
    @Override
    public int compareTo(Player arg0) {
      if (_yearSpan != arg0._yearSpan) { return _yearSpan > arg0._yearSpan ? -1 : 1; } 
      if (_war != arg0._war) { return _war > arg0._war ? -1 : 1; }
      return _master.playerID().compareTo(arg0._master.playerID());
    }
  }
  
  public static void main(String[] args) throws Exception {
    Master.Table MT = null;
    War.ByID WT = new War.ByID();
    WT.addFilter(War.filterPositive);
    try (MyDatabase db = new MyDatabase()) {
      MT = new Master.Table(db, Sort.UNSORTED);
      WT.addAll(new War.Table(db, Type.BAT));
      WT.addAll(new War.Table(db, Type.PITCH));
    }
    ArrayList<ByPlayer<War>> all = new ArrayList<>();
    for (ByPlayer<War> BP : WT) {
      if (BP.filter(War.filterPositive) == null) { continue; }
      if (BP.last().yearID() < LAST_YEAR) { continue; }
      all.add(BP);
    }
    Collections.sort(all, _comp_positive);
    ArrayList<Player> hof = new ArrayList<>();
    TreeSet<Player> list = new TreeSet<>();
    for (ByPlayer<War> BP : all) {
      if (list.size() >= STARTING_NUMBER && list.last()._war > BP.filter(War.filterPositive).war()) { break; }
      if (list.size() < STARTING_NUMBER || list.last()._war < BP.total().war()) {
        Player P = new Player();
        P._bp = BP;
        P._war = BP.total().war();
        P._master = MT.byID(BP.total().playerID());
        P._yearSpan = STARTING_NUMBER;
        P._yearStart = BP.first().yearID();
        P._yearEnd = BP.last().yearID();
        list.add(P);
        if (list.size() > STARTING_NUMBER) { list.pollLast(); } // removes the last entry
      }
    }
    hof.addAll(list);
    for (Player P : list) { all.remove(P._bp); }
    for (int ct = STARTING_NUMBER - 1; ct != 0; --ct) {
      list.clear();
      for (ByPlayer<War> BP : all) {
        if (list.size() >= ct && list.last()._war > BP.filter(War.filterPositive).war()) { break; }
        
        double bestWar = 0;
        int startYear = 0, endYear = 0;
        if (BP.first().yearID() + ct >= BP.last().yearID() + 2) { // +1 for inclusive years, +1 for one past the end
          bestWar = BP.total().war();
          startYear = BP.first().yearID();
          endYear = BP.last().yearID();
        } else {
          for (int i = 0, e = BP.size(); i != e; ++i) {
            int i_yr = BP.get(i).yearID();
            int e_yr = i_yr + ct - 1;
            double war = 0;
            int j = i;
            for (; j != e; ++j) {
              War W = BP.get(j);
              if (W.yearID() > e_yr) { break; }
              war += W.war();
            }
            if (bestWar < war) {
              bestWar = war;
              startYear = i_yr;
              endYear = e_yr;
            }
            if (j == e) { break; } // reached the end of the career
          }
        }
        if (list.size() < ct || list.last()._war < bestWar) {
          Player P = new Player();
          P._bp = BP;
          P._war = bestWar;
          P._master = MT.byID(BP.total().playerID());
          P._yearSpan = ct;
          P._yearStart = startYear;
          P._yearEnd = endYear;
          list.add(P);
          if (list.size() > ct) { list.pollLast(); } // removes the last entry
        }
      }
      hof.addAll(list);
      for (Player P : list) { all.remove(P._bp); }
    }
    for (Player P : hof) {
      System.out.format("%d\t%s %s\t%d\t%d\t%.1f\t%.1f\n",
                        P._yearSpan, P._master.nameFirst(), P._master.nameLast(),
                        P._yearStart, P._yearEnd, P._war, P._bp.total().war());
    }
  }
}
