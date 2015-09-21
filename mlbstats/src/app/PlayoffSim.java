package app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.TreeMap;
import util.MyDatabase;
import data.Teams;

/*
 * Simulate a huge number of 6 division, 30 team leagues from random historical teams
 * Use runs scored/allowed as their true win%, plus home field bonus
 * Tally the playoff seed and success of teams based on number of simulated wins in 162 season
 * Also print the "season" with the best world series matchup
 */
public class PlayoffSim {
  private static final double PYTHAG_EXP = 1.83; // TODO: consider exponent of 1.83 (c/o http://www.baseball-reference.com/about/faq.shtml#pyth)
  private static final int NUM_SIMS = 100000;
  private static final int START_YEAR = 1961;
  private static final String[] LEAGUES = { "AL", "NL" };
  private static final double HOME_FIELD = 0.040;
//=(S3/(1-S3))/(S3/(1-S3)+T10/(1-T10))
  private static class SimTeam implements Comparable<SimTeam> {
    
    public SimTeam(Teams T) {
      _team = T;
      double rspow = Math.pow(T.runs(), PYTHAG_EXP);
      _wp = rspow / (rspow + Math.pow(T.runsAllowed(), PYTHAG_EXP));
    }
    
    public Teams _team = null;
    public int _win = 0;
    public int _loss = 0;
    public double _wp = 0; // Runs scored [squared] / (Runs scored [squared] + runs allowed [squared])
    
    public void print() {
      System.out.format("%d %s(%.3f)\t%d\t%d\t%.3f\n", _team.yearID(), _team.teamID(), _wp, _win, _loss, _win / (double)(_win + _loss));
    }

    @Override
    public int compareTo(SimTeam arg0) {
      if (_win != arg0._win) { return arg0._win - _win; }
      return _wp > arg0._wp ? -1 : 1; 
    }
  }
  
  private static double wp(SimTeam home, SimTeam vis) {
    double homewp = home._wp + HOME_FIELD;
    double viswp = vis._wp - HOME_FIELD;
    return (homewp/(1 - homewp))/(homewp/(1-homewp) + viswp/(1-viswp));
  }
  
  private static void play(SimTeam home, SimTeam vis, int g) {
    double wp = wp(home, vis);
    for (int i = 0; i != g; ++i) {
      if (wp >= Math.random()) { ++home._win; ++vis._loss; } else { ++home._loss; ++vis._win; }
    }
  }
  
  private static boolean firstTo(SimTeam fav, SimTeam und, int g) {
    double homewp = wp(fav, und);
    double viswp = 1 - wp(und, fav);
    
    int hw = 0, vw = 0;
    int hgStart = (int)Math.ceil(g / 2);
    for (int i = 0; i != hgStart; ++i) {
      if (homewp >= Math.random()) { if (++hw == g) { return true; } } else { if (++vw == g) { return false; } }
    }
    for (int i = 0; i != g - 1; ++i) {
      if (viswp >= Math.random()) { if (++hw == g) { return true; } } else { if (++vw == g) { return false; } }
    }
    while (true) {
      if (homewp >= Math.random()) { if (++hw == g) { return true; } } else { if (++vw == g) { return false; } }
    }
  }

  private class SimDiv {
    public int _div = 0; 
    public SimTeam[] _teams = new SimTeam[5];
    
    public SimDiv(int div) {
      _div = div;
      for (int i = 0; i != _teams.length; ++i) {
        _teams[i] = new SimTeam(_pool.get((int)(Math.random() * _pool.size())));
      }
    }
    
    public void print() {
      Arrays.sort(_teams);
      System.out.println("Team\tW\tL\tPct");
      for (SimTeam T : _teams) { T.print(); }
      System.out.println();
    }
  }

  private class SimLeague {
    public int _lg = 0;
    public SimDiv[] _divs = new SimDiv[3];
    
    public SimLeague(int lg) {
      _lg = lg;
      for (int i = 0; i != _divs.length; ++i) { _divs[i] = new SimDiv(i); }
    }
    
    public void print() {
      for (SimDiv D : _divs) { D.print(); }
      System.out.println();
    }
  }

  
  private static class WinRatio {
    public WinRatio(int wins) { _wins = wins; for (int i = 0; i != _playoff.length; ++i) { _playoff[i] = 0; } }
    public int _wins = 0;
    public int[] _playoff = new int[5];
    public int _out = 0;
    public int _total = 0;
    public int _champ = 0;
    
    public void add(int slot) {
      if (slot < 5) { ++_playoff[slot]; } else { ++_out; }
      ++_total;
    }
  }
  private static class RatioMap extends TreeMap<Integer, WinRatio> {
    private static final long serialVersionUID = -5151294250908586139L;

    public WinRatio build(int wins) {
      WinRatio WR = get(wins);
      if (WR == null) { put(wins, WR = new WinRatio(wins)); }
      return WR;
    }
    
    public void print() {
      System.out.println("W\t#\t1st\t2nd\t3rd\t4th\t5th\tIn\tOut\tChamp\t%1st\t%2nd\t%3rd\t%4th\t%5th\t%In\t%Out\t%Champ");
      for (WinRatio WR : values()) {
        System.out.format("%d\t%d", WR._wins, WR._total);
        int in = 0;
        for (int perc : WR._playoff) { System.out.format("\t%d", perc); in += perc; }
        System.out.format("\t%d\t%d\t%d", in, WR._out, WR._champ);
        for (int perc : WR._playoff) { System.out.format("\t%.1f", perc * 100.0 / WR._total); }
        System.out.format("\t%.1f\t%.1f\t%.1f\n", in * 100.0 / WR._total, WR._out * 100.0 / WR._total, WR._champ * 100.0 / WR._total);
      }
    }
  }

  private void execute() {
    RatioMap ratios = new RatioMap();
    
    SimLeague[] lgs = new SimLeague[2];
    SimLeague[] best = new SimLeague[2]; int best_w = 0;
    for (int sim = 0; sim != NUM_SIMS; ++sim) {
      for (int lg = 0; lg != lgs.length; ++lg) { lgs[lg] = new SimLeague(lg); }
      
      for (SimLeague myL : lgs) {
        for (SimDiv myD : myL._divs) {
          for (SimTeam me : myD._teams) {
            // PLAY ALL MY HOME GAMES (visitor games will be played by other's home games)
            // within division
            for (SimTeam T : myD._teams) {
              if (T == me) { continue; } // don't play myself
              play(me, T, 9);
            }
            // within league
            for (SimDiv D : myL._divs) {
              if (D == myD) { continue; } // already played my division
              for (SimTeam T : D._teams) {
                play(me, T, 3);
              }
            }
            // interleague
            for (SimLeague L : lgs) {
              if (L == myL) { continue; } // already played my league
              SimDiv D1 = L._divs[myD._div];
              play(me, D1._teams[0], 3);
              play(me, D1._teams[2], 3);
              play(me, D1._teams[4], 3);
              SimDiv D2 = L._divs[(myD._div + 1) % 3];
              play(me, D2._teams[1], 3);
              play(me, D2._teams[3], 3);
            }
          }
        }
      }
      
      SimTeam[] lgWin = new SimTeam[2];
      for (SimLeague L : lgs) {
        ArrayList<SimTeam> div = new ArrayList<>();
        ArrayList<SimTeam> wc = new ArrayList<>();
        for (SimDiv D : L._divs) {
          Arrays.sort(D._teams);
          div.add(D._teams[0]);
          for (int i = 1; i != D._teams.length; ++i) {
            wc.add(D._teams[i]);
          }
        }
        Collections.sort(div);
        Collections.sort(wc);
        div.addAll(wc); // combine all into an ordered list
        for (int i = 0; i != div.size(); ++i) {
          ratios.build(div.get(i)._win).add(i);
        }
        // wildcard
        if (!firstTo(div.get(3), div.get(4), 1)) { div.set(3, div.get(4)); }
        // division
        if (!firstTo(div.get(1), div.get(2), 5)) { div.set(1, div.get(2)); }
        if (!firstTo(div.get(0), div.get(3), 5)) { div.set(0, div.get(1)); div.set(1, div.get(3)); }
        // league
        if (!firstTo(div.get(0), div.get(1), 7)) { div.set(0, div.get(1)); }
        lgWin[L._lg] = div.get(0);
//        L.print();
      }
      int ws_w = lgWin[0]._win + lgWin[1]._win;
      if (best_w < ws_w) { best[0] = lgs[0]; best[1] = lgs[1]; best_w = ws_w; }
      ++ratios.build(lgWin[firstTo(lgWin[0], lgWin[1], 7) ? 0 : 1]._win)._champ;
    }
    ratios.print();
    best[0].print();
    best[1].print();
  }

  private PlayoffSim() throws Exception {
    Teams.Table TT = null;
    try (MyDatabase db = new MyDatabase()) { TT = new Teams.Table(db); }
    HashSet<String> lgs = new HashSet<>();
    lgs.addAll(Arrays.asList(LEAGUES));
    for (Teams T : TT) {
      if (T.yearID() >= START_YEAR && lgs.contains(T.lgID())) { _pool.add(T); }
    }
  }
  
  private ArrayList<Teams> _pool = new ArrayList<>();
  
  public static void main(String[] args) throws Exception {
    new PlayoffSim().execute();
  }

}
