package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import data.Appearances;
import data.Batting;
import data.HOF;
import data.HOF.Selection;
import data.Master;
import data.Pitching;
import data.Position;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;

/*
 * Pick the players for each franchise with the best N peak seasons
 */

public class PlayerPeak {
  private static final int YRS = 7;
  private static final int PER_POS = 0;
  private static final int PER_SP = 0;
  private static final int YEAR_START = 1800;

  private WeightedApp.ByID _byWA = new WeightedApp.ByID();
  private War.ByID _byWar = new War.ByID();
  private Master.Table _mt = null;
  private Teams.Table _tt = null;
  private HOF.Table _ht = null;
  private Batting.ByID _byBT = new Batting.ByID();
  private Pitching.ByID _byPT = new Pitching.ByID();
  
  private static class FValue implements Comparable<FValue> {
    public FValue(Master player, HOF hof) { _player = player; _hof = hof; }
    
    public final Master _player;
    public final HOF _hof;
    public List<War> _seasons = new ArrayList<>();
    
    public double _score = 0;
    
    public WeightedApp _fielding = null;
    public Batting     _batting = null;
    public Pitching    _pitching = null;

    @Override public int compareTo(FValue arg0) {
      if (_score != arg0._score) { return _score > arg0._score ? -1 : 1; }
      return _player.playerID().compareTo(arg0._player.playerID());
    }
  }
  
  private static Comparator<War> BY_YEAR = new Comparator<War>() {
    @Override public int compare(War o1, War o2) {
      return Integer.compare(o1.yearID(), o2.yearID());
    }
  };

  private static Comparator<War> BY_WAR = new Comparator<War>() {
    @Override public int compare(War o1, War o2) {
      return -Double.compare(o1.war(), o2.war());
    }
  };

  private static Comparator<War> BY_WAA = new Comparator<War>() {
    @Override public int compare(War o1, War o2) {
      return -Double.compare(o1.waa(), o2.waa());
    }
  };

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
  
  private static int AVG(int val) { return (int)Math.round(val/(double)YRS); } 
  
  private void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    War.Table WT = new War.Table(db, type);
    _byWA.addAll(new WeightedApp.Tally(WT, AT));
    _byWar.addAll(WT);
  }
  
  public PlayerPeak() throws SQLException {
    try (MyDatabase db = new MyDatabase()) {
      _tt = new Teams.Table(db);
      _byBT.addAll(new Batting.Table(db));
      _byPT.addAll(new Pitching.Table(db));
      _ht = new HOF.Table(db, Sort.UNSORTED, Selection.ELECTED);
      Appearances.Table at = new Appearances.Table(db);
      _mt = new Master.Table(db, Sort.UNSORTED);
      assemble(db, at, Type.BAT);
      assemble(db, at, Type.PITCH);
    }
  }
  
  public static void main(String[] args) throws SQLException {
    PlayerPeak tp = new PlayerPeak();
    tp.execute();
  }
}
