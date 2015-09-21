package strat.server;

import java.util.Map;
import java.util.TreeMap;
import strat.client.model.Batter;
import strat.client.model.GameLog;
import strat.client.model.LineupLog;
import strat.client.model.Pitcher;
import strat.client.model.Play;
import strat.client.model.PlayLog;
import strat.client.model.Player;
import strat.client.model.Position;
import strat.client.model.RunLog;
import strat.shared.BaseState;

public class SeasonStats {
  private BatterStats findBatter(String id) {
    BatterStats bs = _batStats.get(id);
    if (bs == null) { _batStats.put(id, bs = new BatterStats(id)); }
    return bs;
  }
  
  private PitcherStats findPitcher(String id) {
    PitcherStats ps = _pitStats.get(id);
    if (ps == null) { _pitStats.put(id, ps = new PitcherStats(id)); }
    return ps;
  }
  
  private void resetSlots() {
    for (RunExpStats re : _reStats) { re._inning_ct = 0; }
  }
  
  private RunExpStats getSlot(BaseState bases) {
    int ct = bases._outs * 8;
    if (bases._onbase[0] != null) { ct += 1; }
    if (bases._onbase[1] != null) { ct += 2; }
    if (bases._onbase[2] != null) { ct += 4; }
    return _reStats[ct];
  }
  
  public void addLog(GameLog log) {
    ++_games;
    for (LineupLog ll : log._lineup) {
      BatterStats bs = findBatter(ll._batter._id);
      if (ll._num != 0) {
        if (ll._facing == 'R') { ++bs._gs_r; } else { ++bs._gs_l; }
        if (ll._asDH) {
          if (ll._facing == 'R') { ++bs._dh_r; } else { ++bs._dh_l; } 
        } else {
          bs._pos = ll._batter._onfield._pos;
        }
      }
    }
    for (Player p : log._starters) {
      if (p instanceof Pitcher) {
        PitcherStats ps = findPitcher(p._id);
        ++ps._g; ++ps._gs;
      }
    }
    for (Player p : log._subs) {
      if (p instanceof Pitcher) {
        PitcherStats ps = findPitcher(p._id);
        ++ps._g;
      }
    }
    for (PlayLog pl : log._plays) {
      BatterStats bs = findBatter(pl._batter._id);
      PitcherStats ps = findPitcher(pl._pitcher._id);
      BasicStats fs = null;
      if (pl._fielder != null) { if (pl._fielder == pl._pitcher) { fs = ps; } else { fs = findBatter(pl._fielder._id); } }
      
      if (pl._main_play == null) {
        if (pl._special != null) { // FIXME why would this be null?
          switch (pl._special) {
            case WP: ++ps._wp; break;
            case BK: ++ps._bk; break;
            case PB: ++findBatter(pl._fielder._id)._pb; break;
            default: break;
          }
        }
      } else {
        if (pl._pitcher._pitches == 'R') { ++bs._pa_r; } else { ++bs._pa_l; }
        char bats = pl._batter.selectHanded(pl._pitcher._pitches);
        if (bats == 'R') { ++ps._bf_rt; } else { ++ps._bf_lt; }
        Play.Type pt = pl._main_play._type;
        if (pt == Play.Type.FIELD) { pt = pl._range_play._play._type; }
        switch (pt) {
          case SINGLE: ++bs._h; ++bs._si; ++ps._h; break;
          case DOUBLE: ++bs._h; ++bs._do; ++ps._h; break;
          case TRIPLE: ++bs._h; ++bs._tr; ++ps._h; break;
          case HOMERUN: ++bs._h; ++bs._hr; ++ps._h; ++ps._hr; break;
          case HBP: ++bs._hb; ++ps._hb; break;
          case WALK: ++bs._bb; ++ps._bb; break;
          case STRIKEOUT: ++bs._so; ++ps._so; break;
          default: break;
        }
        bs._bi += pl._scored.size();        
      }
      if (pl._special != null) {
        switch (pl._special) {
          case DP: ++bs._dp; break;
          case FC: ++bs._fc; break;
          case SF: ++bs._sf; break;
          default: break;
        }
      }
      ps._ip3 += pl._e_bases._outs - pl._s_bases._outs;
      ps._r += pl._scored.size();
      for (Batter run : pl._scored) { ++findBatter(run._id)._r; }
      // run expectancy
      RunExpStats reBat = getSlot(pl._s_bases);
      ++reBat._total_ct; ++reBat._inning_ct;
      if (!pl._scored.isEmpty()) {
        for (RunExpStats re : _reStats) {
          re._total_runs += re._inning_ct * pl._scored.size();
        }
      }
      if (pl._e_bases._outs == 3) { resetSlots(); }
      double re_plus = pl._e_bases._runexp - pl._s_bases._runexp + pl._scored.size();
      if (pl._pitcher._pitches == 'R') { bs._re_rt += re_plus; } else { bs._re_lt += re_plus; }
      char bats = pl._batter.selectHanded(pl._pitcher._pitches);
      if (bats == 'R') { ps._re_rt -= re_plus; } else { ps._re_lt -= re_plus; }
      if (pl._range_play != null) {
        if (fs != null) {
          fs._re_fld -= re_plus;
          if (pl._error_play > 0) { ++fs._e; ++bs._onE; }
        }
        double[] val = _fldRE.get(pl._fielder._onfield._pos);
        if (val == null) { _fldRE.put(pl._fielder._onfield._pos, val = new double[1]); }
        val[0] -= re_plus;
      }
      if (pl._main_play != null) {
        String playKey = pl._main_play.createShortLine();
        String genericKey = playKey.replaceAll("\\(.*?\\)", "(#)");
        double[] val = _playRE.get(playKey);
        if (val == null) { _playRE.put(playKey, val = new double[4]); }
        double[] genericVal = null;
        if (!playKey.equals(genericKey)) {
          genericVal = _playRE.get(genericKey);
          if (genericVal == null) { _playRE.put(genericKey, genericVal = new double[4]); }
        }
        ++val[0]; val[1] += re_plus;
        if (genericVal != null) { ++genericVal[0]; genericVal[1] += re_plus; }
        if (pl._runner != null) {
          RunLog rl = pl._runner;
          double rerun_plus = rl._bases._runexp - pl._e_bases._runexp + (rl._scored ? 1 : 0);
          ++val[2]; val[3] += rerun_plus;
          if (genericVal != null) { ++genericVal[2]; genericVal[3] += rerun_plus; }
          double[] rval = _runRE.get(rl._runner._run._advance);
          if (rval == null) { _runRE.put(rl._runner._run._advance, rval = new double[2]); }
          ++rval[0]; rval[1] += rerun_plus;
        }
      }
      if (pl._runner != null) {
        RunLog rl = pl._runner;
        double re_run = rl._bases._runexp - pl._e_bases._runexp + (rl._scored ? 1 : 0);
        BatterStats rbs = findBatter(rl._runner._id);
        if (rl._cause == RunLog.Cause.STEAL) { ps._re_run -= re_run; }
        else { bs._re_run += re_run; } // batter gets some credit for giving the opportunity for the runner to advance
        rbs._re_run += re_run;
        if (fs != null) {
          fs._re_fld -= re_run;
          if (re_run < 0) { ++fs._assist; }
          double[] val = _fldRE.get(pl._fielder._onfield._pos);
          if (val == null) { _fldRE.put(pl._fielder._onfield._pos, val = new double[1]); }
          val[0] -= re_run;
        }
        if (pl._special != null) {
          switch (pl._special) { 
            case SB: ++rbs._sb; break;
            case CS: ++rbs._cs; break;
            default: break;
          }
        }
      }
    }
    resetSlots();
    ++findPitcher(log._winner._id)._w;
    ++findPitcher(log._loser._id)._l;
  }
  
  public void printBatters(int seasons) {
    BatterStats.printHeader();
    for (BatterStats bs : _batStats.values()) {
      double posREperG = 0;
      if (bs._pos != null) {
        double[] posRE = _fldRE.get(bs._pos);
        posREperG = posRE == null ? 0 : posRE[0] / (_games * 2); // games * 2, for home & visitor
      }
      bs.print(seasons, posREperG);
    }
  }
  
  public void printPitchers(int seasons) {
    PitcherStats.printHeader();
    double posREperG = _fldRE.get(Position.PITCH)[0] / (_games * 2); // games * 2, for home & visitor
    for (PitcherStats ps : _pitStats.values()) {
      ps.print(seasons, posREperG);
    }
  }
  
  public void printFieldRunExp() {
    System.out.println("Pos\tRE+");
    for (Map.Entry<Position, double[]> entry : _fldRE.entrySet()) {
      System.out.format("%s\t%.3f\n", entry.getKey().code().toUpperCase(), entry.getValue()[0]);
    }
  }
 
  public void printPlayRunExp() {
    System.out.println("Play\t#\tTot\tRE+\tr#\trTot\trRE+");
    for (Map.Entry<String, double[]> entry : _playRE.entrySet()) {
      System.out.format("%s\t%.0f\t%.1f\t%.3f", entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[1] / entry.getValue()[0]);
      if (entry.getValue()[2] != 0) {
        System.out.format("\t%.0f\t%.1f\t%.3f", entry.getValue()[2], entry.getValue()[3], entry.getValue()[3] / entry.getValue()[2]);
      }
      System.out.println();
    }
    System.out.println();
    System.out.println("Run Rate\t#\tTot\tRE+");
    for (Map.Entry<Integer, double[]> entry : _runRE.entrySet()) {
      System.out.format("%d\t%.0f\t%.1f\t%.3f\n", entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[1] / entry.getValue()[0]);
    }
  }
 
  public void printRunExp() {
    System.out.println("Outs\tBases\tRuns\tCount\tAvg");
    for (int i = 0; i != _reStats.length; ++i) {
      int outs = i / 8;
      int bases = i % 8;
      boolean first = bases % 2 == 1;
      boolean second = bases / 2 % 2 == 1;
      boolean third = bases / 4 == 1;
      System.out.format("%d\t%c%c%c\t%d\t%d\t%.3f\n", outs, first ? '1' : '-', second ? '2' : '-', third ? '3' : '-',
        _reStats[i]._total_runs, _reStats[i]._total_ct, _reStats[i]._total_runs / (double)_reStats[i]._total_ct);
    }
  }
  
  public SeasonStats() {
    for (int i = 0; i != _reStats.length; ++i) { _reStats[i] = new RunExpStats(); }
  }
  
  private TreeMap<String, BatterStats> _batStats = new TreeMap<>();
  private TreeMap<String, PitcherStats> _pitStats = new TreeMap<>();
  private TreeMap<Position, double[]> _fldRE = new TreeMap<>();
  
  private TreeMap<String, double[]> _playRE = new TreeMap<>();
  private TreeMap<Integer, double[]> _runRE = new TreeMap<>(); // running-based stats (not stealing)

  private int _games = 0;
  private RunExpStats[] _reStats = new RunExpStats[24];
}
