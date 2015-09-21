package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import data.Batting;
import data.Groupable;
import data.HOF;
import data.HOF.Selection;
import data.Master;
import data.Pitching;
import data.Sort;
import data.Teams;

/** Calculate the 7 seasons chosen by Strat to be the ones used in their cards, and print out year and team for each season */

/*
 * Ruth(SP) = 1915-1918 divide by 4
 * Koufax = 1960-1966
 * TODO missing the final year for Koufax (or rather, the final 7 years) - off by 1 error?
 * TODO compute WAR and WAA for 7 seasons
 * TODO print totals for players < 7 years
 * TODO am I doing something wrong with split seasons? kellejo01, henderi01, hermabi01 all have good split seasons and have non-zero scores
 */

public class StratYears {
  public static final Path BASE_DIR = Paths.get("C:/build/strat");
  private static final Path BAT_DIR = BASE_DIR.resolve("bat");
  private static final Path PITCH_DIR = BASE_DIR.resolve("pit");

  private static final boolean PRINT_STATS = false;
  private static final int YRS = 7;

  private Master.Table _mt = null;
  private Teams.Table _tt = null;
  private HOF.Table _ht = null;
  private Batting.ByID _byBT = new Batting.ByID();
  private Pitching.ByID _byPT = new Pitching.ByID();
  
  // seasons = 7 (0-6), years = 20 (0-19); stop when all entries are 13 (years - seasons - 1)
  private static class Seasons {
    public Seasons(int total) { _max = total - _working.length - 1; }

    public boolean submit(int diff) {
      if (_best_diff > diff) { _best_diff = diff; _best = Arrays.copyOf(_working, YRS); return true; }
      return false;
    }
    
    public boolean next() {
      if (_best_diff == 0) { return false; }
      int adj = YRS - 1;
      for (; adj != -1; --adj) { if (_working[adj] <= _max) { break; } }
      if (adj == -1) { return false; }
      int new_val = _working[adj] + 1;
      for (; adj != YRS; ++adj) { _working[adj] = new_val; }
      return true;
    }

    public final int _max;
    public int[] _working = new int[YRS];
    public int[] _best = null;
    public int _best_diff = Integer.MAX_VALUE;
  }
  
  private static String getName(Path cardFile) {
    return cardFile.getFileName().toString().split("\\.")[0];
  }

  private static int AVG(int val) { return (int)Math.round(val/(double)YRS); } 
  
  private static int diff(int strat, int db) {
    return Math.abs(strat - AVG(db));
  }
  
  private static class Bat {
    public Bat(String line) {
      String[] tokens = line.split(" ");
      _avg = Double.parseDouble(tokens[1]);
      _ab  = Integer.parseInt(tokens[2]);
      _d   = Integer.parseInt(tokens[3]);
      _t   = Integer.parseInt(tokens[4]);
      _hr  = Integer.parseInt(tokens[5]);
      _rbi = Integer.parseInt(tokens[6]);
      _bb  = Integer.parseInt(tokens[7]);
      _so  = Integer.parseInt(tokens[8]);
      _sb  = Integer.parseInt(tokens[9]);
      _cs  = Integer.parseInt(tokens[10]);
      _slg = Double.parseDouble(tokens[11]);
      _oba = Double.parseDouble(tokens[12]);
    }
    
    public int compare(Batting bt) {
      int diff = 0;
      diff += (int)(Math.abs(_avg - bt.avg()) * 1000);
      diff += diff(_ab, bt.ab());
      diff += diff(_d, bt.d());
      diff += diff(_t, bt.t());
      diff += diff(_hr, bt.hr());
      diff += diff(_rbi, bt.bi());
      diff += diff(_bb, bt.bb());
//      diff += diff(_so, bt.so());
//      diff += diff(_sb, bt.sb());
//      diff += diff(_cs, bt.cs());
      diff += (int)(Math.abs(_slg - bt.slg()) * 1000);
      diff += (int)(Math.abs(_oba - bt.obp()) * 1000);
/*
      if (diff == 4) {
        System.out.format("avg %d %d\n", (int)(_avg * 1000), (int)(bt.avg() * 1000));
        System.out.format("ab %d %d\n", _ab, AVG(bt.ab()));
        System.out.format("d %d %d\n", _d, AVG(bt.d()));
        System.out.format("t %d %d\n", _t, AVG(bt.t()));
        System.out.format("hr %d %d\n", _hr, AVG(bt.hr()));
        System.out.format("rbi %d %d\n", _rbi, AVG(bt.bi()));
        System.out.format("bb %d %d\n", _bb, AVG(bt.bb()));
        System.out.format("slg %d %d\n", (int)(_slg * 1000), (int)(bt.slg() * 1000));
        System.out.format("oba %d %d\n", (int)(_oba * 1000), (int)(bt.obp() * 1000));
        System.out.println();
      }
*/
      return diff;
    }

    public double _avg;
    public int _ab;
    public int _d;
    public int _t;
    public int _hr;
    public int _rbi;
    public int _bb;
    public int _so;
    public int _sb;
    public int _cs;
    public double _slg;
    public double _oba;
  }

  private static class Pitch {
    public Pitch(String line) {
      String[] tokens = line.split(" ");
      _w   = Integer.parseInt(tokens[1]);
      _l   = Integer.parseInt(tokens[2]);
      _era = Double.parseDouble(tokens[3]);
      _gs  = Integer.parseInt(tokens[4]);
      _sv  = Integer.parseInt(tokens[5]);
      _ip  = Integer.parseInt(tokens[6]);
      _h   = Integer.parseInt(tokens[7]);
      _bb  = Integer.parseInt(tokens[8]);
      _so  = Integer.parseInt(tokens[9]);
      _hr  = Integer.parseInt(tokens[10]);
    }
    
    public int compare(Pitching pt) {
      int diff = 0;
      diff += diff(_w, pt.w());
      diff += diff(_l, pt.l());
      diff += (int)(Math.abs(_era - pt.era()) * 100);
      diff += diff(_gs, pt.gs());
      diff += diff(_sv, pt.sv());
      diff += diff(_ip, pt.ip3() / 3);
      diff += diff(_h, pt.h());
      diff += diff(_hr, pt.hr());
      diff += diff(_bb, pt.bb());
      diff += diff(_so, pt.so());
      return diff;
    }

    public int _w;
    public int _l;
    public double _era;
    public int _gs;
    public int _sv;
    public int _ip;
    public int _h;
    public int _bb;
    public int _so;
    public int _hr;
  }

  public static class Result<T extends Groupable<T>> {
    Result(String id, int best_fit, ByPlayer<T> byYear) { _id = id; _best_fit = best_fit; _byYear = byYear; }

    final String _id;
    final int _best_fit;
    final ByPlayer<T> _byYear;
    
    T _total;
    ArrayList<T> _seasons = new ArrayList<>();
  }
  
  
  public Result<Batting> findBatter(String id, Bat stats) {
    ByPlayer<Batting> byYear = _byBT.get(id);
    if (byYear == null) { return null; }
    if (byYear.size() < YRS) { return new Result<>(id, -byYear.size(), byYear); }
    Seasons ssn = new Seasons(byYear.size());
    do {
      Batting pit = byYear.first().create();
      for (int i = 0; i != ssn._working.length; ++i) {
        Batting bat = byYear.get(i + ssn._working[i]);
        pit.add(bat);
      }
      ssn.submit(stats.compare(pit));
    } while (ssn.next());
    Result<Batting> result = new Result<>(id, ssn._best_diff, byYear);
    for (int i = 0; i != ssn._best.length; ++i) {
      int offset = i + ssn._best[i];
      result._seasons.add(byYear.get(offset));
    }
    return result;
  }

  
  public Result<Pitching> findPitcher(String id, Pitch stats) {
    ByPlayer<Pitching> byYear = _byPT.get(id);
    if (byYear == null) { return null; }
    if (byYear.size() < YRS) { return new Result<>(id, -byYear.size(), byYear); }
    Seasons ssn = new Seasons(byYear.size());
    do {
      Pitching pit = byYear.first().create();
      for (int i = 0; i != ssn._working.length; ++i) {
        pit.add(byYear.get(i + ssn._working[i]));
      }
      ssn.submit(stats.compare(pit));
    } while (ssn.next());
    Result<Pitching> result = new Result<>(id, ssn._best_diff, byYear);
    for (int i = 0; i != ssn._best.length; ++i) {
      int offset = i + ssn._best[i];
      result._seasons.add(byYear.get(offset));
    }
    return result;
  }

  public ArrayList<Result<Batting>> batting() throws IOException {
    ArrayList<Result<Batting>> results = new ArrayList<>();
    for (Path cardFile : Files.newDirectoryStream(BAT_DIR, "*.txt")) {
      String id = getName(cardFile);
//      if (!id.startsWith("henderi")) { continue; }
      Bat stats = null;
      for (String line : Files.readAllLines(cardFile, StandardCharsets.UTF_8)) {
        if (line.startsWith("S ")) { stats = new Bat(line); break; }
      }
      if (stats == null) { System.err.println(id + " : no stats found"); continue; }
      
      Result<Batting> result = findBatter(id, stats);
      if (result != null) { results.add(result); }
    }
    return results;
  }

  public ArrayList<Result<Pitching>> pitching() throws IOException {
    ArrayList<Result<Pitching>> results = new ArrayList<>();
    for (Path cardFile : Files.newDirectoryStream(PITCH_DIR, "*.txt")) {
      String id = getName(cardFile);
      Pitch stats = null;
      for (String line : Files.readAllLines(cardFile, StandardCharsets.UTF_8)) {
        if (line.startsWith("S ")) { stats = new Pitch(line); break; }
      }
      if (stats == null) { System.err.println(id + " : no stats found"); continue; }
      Result<Pitching> result = findPitcher(id, stats);
      if (result != null) { results.add(result); }
    }
    return results;
  }

/*
  private void execute() {
    HashSet<String> active_franch = new HashSet<>();
    for (Teams t : _tt.year(2013)) { active_franch.add(t.franchID()); }
    EnumSet<Position> pitchPos = Position.find("P");
    EnumMap<Position, TreeSet<FValue>> byPos = new EnumMap<>(Position.class);
    for (ByPlayer<War> WW : _byWar) {
      Master m = _mt.byID(WW.total().playerID());
      FValue fv = new FValue(m, _ht.idFirst(m.hofID()));
      for (War W : WW) {
        if (W.yearID() < YEAR_START) { continue; }
        fv._seasons.add(W);
      }
      HashSet<Integer> seasons = new HashSet<>();
      Collections.sort(fv._seasons, BY_WAR);
      if (fv._seasons.size() > YRS) { fv._seasons = fv._seasons.subList(0, YRS); }
      Collections.sort(fv._seasons, BY_YEAR);
      for (War w : fv._seasons) {
        fv._score += w.waa();
        seasons.add(w.yearID());
      }
      ByPlayer<WeightedApp> WA = _byWA.get(m.playerID());
      if (WA != null) {
        for (WeightedApp wa : WA) {
          if (!seasons.contains(wa.yearID())) { continue; }
          if (fv._fielding == null) { fv._fielding = wa.create(); }
          fv._fielding.add(wa);
        }
      }
      if (fv._fielding != null && fv._fielding.primary() != null) {
        Position primary = fv._fielding.primary().pos();
        TreeSet<FValue> players = byPos.get(primary);
        if (players == null) { byPos.put(primary, players = new TreeSet<>()); }
        players.add(fv);
        if (pitchPos.contains(primary)) {
          ByPlayer<Pitching> bp = _byPT.get(m.playerID());
          if (bp != null) {
            for (Pitching p : bp) {
              if (!seasons.contains(p.yearID())) { continue; }
              if (fv._pitching == null) { fv._pitching = p.create(); }
              fv._pitching.add(p);
            }
          }
        } else {
          ByPlayer<Batting> bb = _byBT.get(m.playerID());
          if (bb != null) {
            for (Batting b : bb) {
              if (!seasons.contains(b.yearID())) { continue; }
              if (fv._batting == null) { fv._batting = b.create(); }
              fv._batting.add(b);
            }
          }
        }
      }
    }
    for (Map.Entry<Position, TreeSet<FValue>> entry : byPos.entrySet()) {
      int ct = (entry.getKey() == Position.STARTER) ? PER_SP : PER_POS;
      for (FValue fv : entry.getValue()) {
        if (--ct < 0 && fv._hof == null) { continue; }
        System.out.format("%s\t%s\t%s %s\t%c\t%.2f", entry.getKey().getName(), fv._player.playerID(), fv._player.nameFirst(), fv._player.nameLast(), fv._hof == null ? '-' : 'Y', fv._score);
        for (War w : fv._seasons) { System.out.format("\t%.2f", w.waa()); }
        for (int num = fv._seasons.size(); num != YRS; ++num) { System.out.print("\t"); }
        for (War w : fv._seasons) { System.out.format("\t%d", w.yearID()); }
        for (int num = fv._seasons.size(); num != YRS; ++num) { System.out.print("\t"); }
        if (fv._batting != null) {
          Batting b = fv._batting;
          // AVG    AB  2B  3B  HR  RBI BB  SO  SB  CS  SLG OBP
          System.out.format("\t%.3f\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%.3f\t%.3f", b.avg(), AVG(b.ab()), AVG(b.d()), AVG(b.t()), AVG(b.hr()), AVG(b.bi()), AVG(b.bb()), AVG(b.so()), AVG(b.sb()), AVG(b.cs()), b.slg(), b.obp());
        } else if (fv._pitching != null) {
          Pitching p = fv._pitching;
          // W L ERA GS SV IP H BB SO HR
          System.out.format("\t%d\t%d\t%.2f\t%d\t%d\t%d\t%d\t%d\t%d\t%d", AVG(p.w()), AVG(p.l()), p.era(), AVG(p.gs()), AVG(p.sv()), AVG(p.ip3() / 3), AVG(p.h()), AVG(p.bb()), AVG(p.so()), AVG(p.hr()));
        }
        System.out.println();
      }
    }
  }
*/
  public void printBatter7Year(Result<Batting> result) {
    if (result._best_fit < 0) { System.out.format("%s\t%d\n", result._id, -result._best_fit); return; }
    System.out.format("%s\t%d", result._id, result._best_fit);
    for (Batting p : result._seasons) {
      System.out.format("\t%d", p.yearID());
    }
    for (Batting p : result._seasons) {
      System.out.format("\t%s", _tt.getFranchiseID(p.teamID()));
    }
    //if (PRINT_STATS) {
//      System.out.format("\t%.3f\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%.3f\t%.3f", stats._avg, stats._ab, stats._d, stats._t, stats._hr, stats._rbi, stats._bb, stats._so, stats._sb, stats._cs, stats._slg, stats._oba);
//    }
    System.out.println();
  }
  
  public void printPitcher7Year(Result<Pitching> result) {
    if (result._best_fit < 0) { System.out.format("%s\t%d\n", result._id, -result._best_fit); return; }
    System.out.format("%s\t%d", result._id, result._best_fit);
    for (Pitching p : result._seasons) {
      System.out.format("\t%d", p.yearID());
    }
    for (Pitching p : result._seasons) {
      System.out.format("\t%s", _tt.getFranchiseID(p.teamID()));
    }
//    if (PRINT_STATS) {
//      System.out.format("\t%d\t%d\t%.2f\t%d\t%d\t%d\t%d\t%d\t%d\t%d", stats._w, stats._l, stats._era, stats._gs, stats._sv, stats._ip, stats._h, stats._bb, stats._so, stats._hr);
//    }
    System.out.println();
  }
  
  enum Franchise { ATL, BAL, BOS, CHC, CHW, CIN, CLE, DET,  LAD, MIN, NYY, OAK, PHI, PIT, SFG, STL, X }

  public void printBatterPerFranchise(Result<Batting> result) {
    int[] counts = new int[Franchise.values().length];
    for (Batting b : result._byYear) {
      String fr = _tt.getFranchiseID(b.teamID());
      Franchise f = Franchise.X;
      try { f = Franchise.valueOf(fr); } catch (IllegalArgumentException e) { /* not found, so carry on */ }
      ++counts[f.ordinal()];
    }
    if (result._best_fit >= 0) { 
      for (Batting b : result._seasons) {
        String fr = _tt.getFranchiseID(b.teamID());
        Franchise f = Franchise.X;
        try { f = Franchise.valueOf(fr); } catch (IllegalArgumentException e) { /* not found, so carry on */ }
        counts[f.ordinal()] += 9;
      }
    }
    Franchise f_best = Franchise.X; int best_ct = 0, second_ct = 0;
    for (Franchise f : EnumSet.range(Franchise.ATL, Franchise.STL)) {
      int ct = counts[f.ordinal()];
      if (ct > best_ct) { second_ct = best_ct; best_ct = ct; f_best = f; }
      else if (ct > second_ct) { second_ct = ct; }
    }
    double tot = best_ct * 2 / (double)(best_ct + second_ct) - 1;
    if (f_best == Franchise.X) { tot = 0; } 
    
    System.out.format("%s", result._id);
    for (int ct : counts) { System.out.format("\t%d", ct); }
    System.out.format("\t%s\t%.3f\t%.3f\n", f_best, tot, tot * best_ct);
  }
  
  public void printPitcherPerFranchise(Result<Pitching> result) {
    int[] counts = new int[Franchise.values().length];
    for (Pitching p : result._byYear) {
      String fr = _tt.getFranchiseID(p.teamID());
      Franchise f = Franchise.X;
      try { f = Franchise.valueOf(fr); } catch (IllegalArgumentException e) { /* not found, so carry on */ }
      ++counts[f.ordinal()];
    }
    if (result._best_fit >= 0) { 
      for (Pitching p : result._seasons) {
        String fr = _tt.getFranchiseID(p.teamID());
        Franchise f = Franchise.X;
        try { f = Franchise.valueOf(fr); } catch (IllegalArgumentException e) { /* not found, so carry on */ }
        counts[f.ordinal()] += 9;
      }
    }
    Franchise f_best = Franchise.X; int best_ct = 0, second_ct = 0;
    for (Franchise f : EnumSet.range(Franchise.ATL, Franchise.STL)) {
      int ct = counts[f.ordinal()];
      if (ct > best_ct) { second_ct = best_ct; best_ct = ct; f_best = f; }
      else if (ct > second_ct) { second_ct = ct; }
    }
    double tot = best_ct * 2 / (double)(best_ct + second_ct) - 1;
    if (f_best == Franchise.X) { tot = 0; } 
    
    System.out.format("%s", result._id);
    for (int ct : counts) { System.out.format("\t%d", ct); }
    System.out.format("\t%s\t%.3f\t%.1f\n", f_best, tot, tot * best_ct);
  }


  public StratYears() throws SQLException {
    try (MyDatabase db = new MyDatabase()) {
      _tt = new Teams.Table(db);
      _byBT.addAll(new Batting.Table(db));
      _byPT.addAll(new Pitching.Table(db));
      _ht = new HOF.Table(db, Sort.UNSORTED, Selection.ELECTED);
      _mt = new Master.Table(db, Sort.UNSORTED);
    }
  }
  
  public static void main(String[] args) throws SQLException, IOException {
    StratYears tp = new StratYears();
    System.out.format("Name");
    for (Franchise f : Franchise.values()) { System.out.format("\t%s", f.toString()); }
    System.out.println("\tBest\tSc\tTot");
    for (Result<Batting> result : tp.batting()) { tp.printBatterPerFranchise(result); }
    for (Result<Pitching> result : tp.pitching()) { tp.printPitcherPerFranchise(result); }
/*
    for (Result<Batting> result : tp.batting()) { tp.printBatter7Year(result); }
    System.out.println();
    for (Result<Pitching> result : tp.pitching()) { tp.printPitcher7Year(result); }
*/
  }
}
