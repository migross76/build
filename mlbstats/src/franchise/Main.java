package franchise;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import util.ByPlayer;
import util.MyDatabase;
import data.Appearances;
import data.Master;
import data.Position;
import data.Sort;
import data.Type;
import data.War;

class Count {
  public double _war_proj;
  public double _pt_proj = 0.001;
  public double _war_act;
  public double _pt_act = 0.001;
  
  public double pt_frac() { return _pt_act / _pt_proj; }
  public double war_frac() { return (_war_act / _pt_act) / (_war_proj / _pt_proj); } 
  
  public void add(Player p) {
    _war_proj += p._wProj;
    _pt_proj += p._pProj;
    _war_act += p._wNow;
    _pt_act += p._pNow;
  }
  
  public void add(Player p, double frac) {
    _war_proj += frac * p._wProj;
    _pt_proj += frac * p._pProj;
    _war_act += frac * p._wNow;
    _pt_act += frac * p._pNow;
  }
}

public class Main {
  private static final int YEAR_FIRST = 1945;
  private static final int SEASON_BAT = 700;
  private static final int SEASON_PITCH = 750;
  
  public static ArrayList<Player> pickCandidates(Iterable<Player> history, int year) {
    ArrayList<Player> candidates = new ArrayList<>();
    for (Player p : history) {
      if (p._allwar.first().yearID() > year || p._allwar.last().yearID() < year) { continue; }
      Projection.compute(p, year);
      candidates.add(p);
    }
    
    Collections.sort(candidates, new Comparator<Player>() {
      @Override public int compare(Player arg0, Player arg1) {
        return -Double.compare(arg0.nowScore(), arg1.nowScore());
      }
    });
    return candidates;
  }
  
  private static final int PT_DIVISION = 50;
  
  public static void gatherStats(Iterable<Player> history, int yearStart, int yearEnd, int range) {
    TreeMap<Integer, Count> byAge = new TreeMap<>();
    TreeMap<Integer, Count> byPA = new TreeMap<>();
    TreeMap<Integer, Count> byIP = new TreeMap<>();
    EnumMap<Position, Count> byPos = new EnumMap<>(Position.class);
    for (int year = yearStart; year < yearEnd; year += range) {
    ArrayList<Player> candidates = pickCandidates(history, year);
    for (int i = 0; i != 400; ++i) {
      Player p = candidates.get(i);
      int age = p._master.age(year);
      Count agect = byAge.get(age); if (agect == null) { byAge.put(age, agect = new Count()); }
      agect.add(p);
      Appearances.Use use = p._allapp.total().primary();
      if (use == null) { System.err.println("No primary? : " + year + " : " + p._master.nameFirst() + " " + p._master.nameLast()); }
      else {
        Count posct = byPos.get(use.pos()); if (posct == null) { byPos.put(use.pos(), posct = new Count()); }
        posct.add(p);
      }
      TreeMap<Integer, Count> byPT = p._season == SEASON_BAT ? byPA : byIP;
      double pt = p._pProj * p._season;
      int min_pt = (int)Math.floor(pt / PT_DIVISION) * PT_DIVISION;
      double perc = (pt - min_pt) / PT_DIVISION;
      Count ptct = byPT.get(min_pt); if (ptct == null) { byPT.put(min_pt, ptct = new Count()); }
      ptct.add(p, 1 - perc);
      ptct = byPT.get(min_pt + PT_DIVISION); if (ptct == null) { byPT.put(min_pt + PT_DIVISION, ptct = new Count()); }
      ptct.add(p, perc);
    }
    }
    System.out.println("Age\tpWAR\tpPT\taWAR\taPT\tPT%\tsWAR%");
    for (Map.Entry<Integer, Count> e : byAge.entrySet()) {
      Count ct = e.getValue();
      System.out.format("%d\t%.1f\t%.1f\t%.1f\t%.1f\t%.3f\t%.3f\n", e.getKey(), ct._war_proj, ct._pt_proj, ct._war_act, ct._pt_act, ct.pt_frac(), ct.war_frac());
    }
    System.out.println("\nPos\tpWAR\tpPT\taWAR\taPT\tPT%\tsWAR%");
    for (Map.Entry<Position, Count> e : byPos.entrySet()) {
      Count ct = e.getValue();
      System.out.format("%s\t%.1f\t%.1f\t%.1f\t%.1f\t%.3f\t%.3f\n", e.getKey().getName(), ct._war_proj, ct._pt_proj, ct._war_act, ct._pt_act, ct.pt_frac(), ct.war_frac());
    }
    System.out.println("\nPA\tpWAR\tpPT\taWAR\taPT\tPT%\tsWAR%");
    for (Map.Entry<Integer, Count> e : byPA.entrySet()) {
      Count ct = e.getValue();
      System.out.format("%d\t%.1f\t%.1f\t%.1f\t%.1f\t%.3f\t%.3f\n", e.getKey(), ct._war_proj, ct._pt_proj, ct._war_act, ct._pt_act, ct.pt_frac(), ct.war_frac());
    }
    System.out.println("\nIP3\tpWAR\tpPT\taWAR\taPT\tPT%\tsWAR%");
    for (Map.Entry<Integer, Count> e : byIP.entrySet()) {
      Count ct = e.getValue();
      System.out.format("%d\t%.1f\t%.1f\t%.1f\t%.1f\t%.3f\t%.3f\n", e.getKey(), ct._war_proj, ct._pt_proj, ct._war_act, ct._pt_act, ct.pt_frac(), ct.war_frac());
    }
  }
  
  public static void printCandidates(Iterable<Player> history, int year) {
    ArrayList<Player> candidates = pickCandidates(history, year);
    System.out.println("#\tName\tPos\tAge\tWAR\tsAge\tPT\tpAge\tNow\tAcc\tAct");
    for (int i = 0; i != 300; ++i) {
      Player p = candidates.get(i);
      System.out.format("%d\t%s %s\t%s\t%d\t%.1f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.1f\n",
                        i+1, p._master.nameFirst(), p._master.nameLast(), p._allapp.total().primary().pos().getName(), p._master.age(year),
                        p._wProj, p._wAgeRate, p._pProj, p._pAge, p.nowScore(), p.accum(), p._wNow);
    }
  }
  
  public static void printFull(Iterable<Player> history, int sYear, int eYear) {
    ArrayList<Player> candidates = pickCandidates(history, sYear);
    System.out.print("#\tName\tPos\tAge");
    for (int y = sYear; y != eYear; ++y) {
      System.out.print("\t" + y);
    }
    System.out.println();
    for (int i = 0; i != 300; ++i) {
      Player p = candidates.get(i);
      System.out.format("%d\t%s %s\t%s\t%d", i+1, p._master.nameFirst(), p._master.nameLast(), p._allapp.total().primary().pos().getName(), p._master.age(eYear));
      for (int y = sYear; y != eYear; ++y) {
        p.reset();
        Projection.compute(p, y);
        System.out.format("\t%.2f", p.nowScore());
      }
      System.out.println();
    }
  }
  
  public static void main(String[] args) throws Exception {
    Master.Table MT = null;
    War.ByID batWar = new War.ByID();
    War.ByID pitWar = new War.ByID();
    Appearances.ByID bApp = new Appearances.ByID();
    try (MyDatabase db = new MyDatabase()) {
      MT = new Master.Table(db, Sort.UNSORTED);
      batWar.addAll(new War.Table(db, Type.BAT));
      pitWar.addAll(new War.Table(db, Type.PITCH));
      bApp.addAll(new Appearances.Table(db));
    }
    
    ArrayList<Player> history = new ArrayList<>();
    for (ByPlayer<War> bat : batWar) {
      if (bat.last().yearID() < YEAR_FIRST) { continue; } // skip anyone whose career ended before the first year we care about
      String id = bat.total().playerID();
      ByPlayer<War> pit = pitWar.get(id);
      boolean isBat = pit == null || pit.total().playtime() < bat.total().playtime();
      
      history.add(new Player(MT.byID(id), isBat ? bat : pit, bApp.get(id), isBat ? SEASON_BAT : SEASON_PITCH));
    }
    for (ByPlayer<War> pit : pitWar) {
      if (pit.last().yearID() < YEAR_FIRST) { continue; } // skip anyone whose career ended before the first year we care about
      String id = pit.total().playerID();
      ByPlayer<War> bat = batWar.get(id);
      if (bat == null) { history.add(new Player(MT.byID(id), pit, bApp.get(id), SEASON_PITCH)); }
    }
    //gatherStats(history, 1946, 1988, 1);
    //printCandidates(history, 1986);
    printFull(history, 1986, 1990);
  }
}
