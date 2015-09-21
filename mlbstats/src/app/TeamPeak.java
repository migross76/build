package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.HOF;
import data.HOF.Selection;
import data.Master;
import data.Position;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;

/*
 * Pick the players for each franchise with the best N peak seasons
 */

public class TeamPeak {
  
  private static final int YRS = 5;
  private static final int PER_POS = 2;
  private static final int PER_BAT = 2;
  private static final int PER_SP = 5;
  private static final int PER_RP = 3;
  private static final int PER_PIT = 1;
  private static final int YEAR_ACTIVE = 2013;
  private static final double MIN_HOF = 0.35;
  private static final double MIN_ACTIVE = 0.6;
  private static final double MIN_ANY = 0.85;
  private static final int YEAR_START = 1800;
  private static final double FIELD_PLAY = 0.1;

  private static class Franchise {
    public Franchise(String name) { _name = name; }
    
    public EnumMap<Position, ArrayList<FValue>> _players = new EnumMap<>(Position.class);
    
    public final String _name;
  }
  
  private static class Roster {
    public EnumMap<Position, ArrayList<FValue>> _team = new EnumMap<>(Position.class);
    public ArrayList<FValue> _minors = new ArrayList<>();
    
    public void collectMinors(ArrayList<FValue> fvs) {
      double baseline = fvs.get(0)._score;
      for (FValue fv : fvs) {
        if (fv._score < baseline * MIN_HOF) { break; }
        if (fv._hof != null ||
            (fv._last_year >= YEAR_ACTIVE && fv._score > baseline * MIN_ACTIVE) ||
            fv._score > baseline * MIN_ANY) {
          _minors.add(fv);
        }
      }
    }
    
    public Roster() {
      for (Position pos : EnumSet.allOf(Position.class)) { _team.put(pos, new ArrayList<FValue>()); }
    }
  }
  
  private WeightedApp.ByID _byWA = new WeightedApp.ByID();
  private WeightedWar.ByIDAlone _byWar = new WeightedWar.ByIDAlone();
  private Master.Table _mt = null;
  private Teams.Table _tt = null;
  private HOF.Table _ht = null;
  
  private static class FValue implements Comparable<FValue> {
    @Override public String toString() { return String.format("{%s:%.2f}", _player.playerID(), _score); }
    
    public FValue(String franchise, Master player, HOF hof) { _franchise = franchise; _player = player; _hof = hof; }
    
    public final String _franchise;
    public final Master _player;
    public final HOF _hof;
    public TreeSet<WeightedWar> _seasons = new TreeSet<>();
    public int _total_games = 0;
    public int _last_year = 0;
    
    public double _score = 0;
    
    public WeightedApp _fielding = null;
    
    public Position primary() { return _fielding == null || _fielding.primary() == null ? null : _fielding.primary().pos(); }

    @Override public int compareTo(FValue arg0) {
      if (_score != arg0._score) { return _score > arg0._score ? -1 : 1; }
      return _player.playerID().compareTo(arg0._player.playerID());
    }
  }
  
  private static void print(String franchise, FValue fv) {
    System.out.format("%s", franchise);
    Position primary = fv.primary();
    if (primary == null) {
      System.out.print("\t--\t");
    } else {
      for (WeightedApp.Use u : fv._fielding) {
        if (u.pos() == primary) { System.out.format("\t%s\t", u.pos().getName()); }
        else if (u.games() > fv._total_games * FIELD_PLAY && u.pos() != Position.DESIG) { System.out.format("%s ", u.pos().getName()); }
      }
    }
    System.out.format("\t%s %s\t%.2f\t%.2f", fv._player.nameFirst(), fv._player.nameLast(), fv._score, fv._seasons.first().war());
    int b_yr = 5000, e_yr = 0;
    for (WeightedWar w : fv._seasons) {
      if (b_yr > w.yearID()) { b_yr = w.yearID(); }
      if (e_yr < w.yearID()) { e_yr = w.yearID(); }
    }
    System.out.format("\t%d\t%d", b_yr, e_yr);
    if (fv._hof != null) { System.out.format("\t%d", fv._hof.yearID()); }
    System.out.println();
  }
  
  private void execute() {
    HashSet<String> active_franch = new HashSet<>();
    for (Teams t : _tt.year(2013)) { active_franch.add(t.franchID()); }
    
    TreeMap<String, Franchise> franchises = new TreeMap<>();
    for (ByPlayer<WeightedWar> WW : _byWar) {
      HashMap<String, FValue> map = new HashMap<>();
      Master m = _mt.byID(WW.total().playerID());
      for (WeightedWar W : WW) {
        if (W.yearID() < YEAR_START) { continue; }
        if (W.wwar() <= 0) { continue; }
        String franchID = _tt.getFranchiseID(W.teamID());
        if (franchID == null || !active_franch.contains(franchID)) { franchID = "---"; }
        FValue FV = map.get(franchID);
        if (FV == null) { map.put(franchID, FV = new FValue(franchID, m, _ht.idFirst(m.hofID()))); }
        FV._seasons.add(W);
        if (FV._last_year < W.yearID()) { FV._last_year = W.yearID(); }
      }
      ByPlayer<WeightedApp> WA = _byWA.get(m.playerID());
      for (FValue fv : map.values()) {
        HashSet<Integer> seasons = new HashSet<>();
        int max = Math.min(fv._seasons.size(), YRS);
        for (WeightedWar w : fv._seasons) {
          if (--max < 0) { break; }
          fv._score += w.wwar();
          seasons.add(w.yearID());
        }
        if (WA != null) {
          for (WeightedApp wa : WA) {
            if (!seasons.contains(wa.yearID())) { continue; }
            if (fv._fielding == null) { fv._fielding = wa.create(); }
            fv._fielding.add(wa);
          }
          if (fv._fielding != null) {
            for (WeightedApp.Use u : fv._fielding) { fv._total_games += u.games(); }
          }
        }
        Franchise f = franchises.get(fv._franchise);
        if (f == null) { franchises.put(fv._franchise, f = new Franchise(fv._franchise)); }
        if (fv._fielding != null && fv._fielding.primary() != null) {
          Position primary = fv._fielding.primary().pos();
          ArrayList<FValue> players = f._players.get(primary);
          if (players == null) { f._players.put(primary, players = new ArrayList<>()); }
          players.add(fv);
        }
      }
    }
    for (Franchise f : franchises.values()) {
      // Team output : 2C, 2IF (minus worst), 2OF (minus worst), 2Bat, 6SP, 3RP
      // Top 6 remaining players(?) - [Top 3 Bat + Top 3 Pit] - or any players w/in replacement level (60% of average)? - average batter vs. average pitcher
      // 40% average for active, 20% average for HOF [what about ensuring every HOF is covered?]
      Roster r = new Roster();
      ArrayList<FValue> xbats = new ArrayList<>();
      for (ArrayList<FValue> fvs : f._players.values()) { Collections.sort(fvs); }
      for (Position pos : EnumSet.range(Position.CATCH, Position.RIGHT)) {
        ArrayList<FValue> fvs = f._players.get(pos);
        for (int i = 0; i != PER_POS; ++i) { r._team.get(pos).add(fvs.remove(0)); }
        xbats.addAll(fvs);
      }
      FValue worst = null; Position worstPos = null;
      for (Position pos : EnumSet.range(Position.FIRST, Position.SHORT)) {
        ArrayList<FValue> fvs = r._team.get(pos);
        if (worst == null || worst._score > fvs.get(PER_POS-1)._score) {
          worst = fvs.get(PER_POS-1); worstPos = pos;
        }
      }
      xbats.add(r._team.get(worstPos).remove(PER_POS-1));
      worst = null; worstPos = null;
      for (Position pos : EnumSet.range(Position.LEFT, Position.RIGHT)) {
        ArrayList<FValue> fvs = r._team.get(pos);
        if (worst == null || worst._score > fvs.get(PER_POS-1)._score) {
          worst = fvs.get(PER_POS-1); worstPos = pos;
        }
      }
      xbats.add(r._team.get(worstPos).remove(PER_POS-1));
      xbats.addAll(r._team.get(Position.DESIG));
      Collections.sort(xbats);
      for (int i = 0; i != PER_BAT; ++i) {
        FValue fv = xbats.remove(0);
        r._team.get(fv.primary()).add(fv);
      }
      Collections.sort(xbats);
      r.collectMinors(xbats);

      ArrayList<FValue> xpits = new ArrayList<>();
      for (Position pos : EnumSet.of(Position.MIDDLE, Position.CLOSER)) { xpits.addAll(f._players.get(pos)); }
      Collections.sort(xpits);
      for (int i = 0; i != PER_RP; ++i) {
        FValue fv = xpits.remove(0);
        r._team.get(fv.primary()).add(fv);
      }
      {
        ArrayList<FValue> fvs = f._players.get(Position.STARTER);
        for (int i = 0; i != PER_SP; ++i) { r._team.get(Position.STARTER).add(fvs.remove(0)); }
        xpits.addAll(fvs);
      }
      Collections.sort(xpits);
      for (int i = 0; i != PER_PIT; ++i) {
        FValue fv = xpits.remove(0);
        r._team.get(fv.primary()).add(fv);
      }
      r.collectMinors(xpits);
      
      for (ArrayList<FValue> fvs : r._team.values()) {
        for (FValue fv : fvs) { print(f._name, fv); }
      }
      System.out.println();
      for (FValue fv : r._minors) { print(f._name, fv); }
      System.out.println("\n");
    }
  }
  
  private void assemble(MyDatabase db, Appearances.Table AT, Type type, int playtime) throws SQLException {
    War.Table WT = new War.Table(db, type);
    WeightedWar.Tally WWT = new WeightedWar.Tally(WT, playtime); 
    WWT.adjustByPosition(AT);
    _byWar.addAll(WWT);
    _byWA.addAll(new WeightedApp.Tally(WT, AT));
  }

  public TeamPeak() throws SQLException {
    WeightedWar.WEIGHT_WAR = false;
    try (MyDatabase db = new MyDatabase()) {
      _tt = new Teams.Table(db);
      Appearances.Table at = new Appearances.Table(db);
      _mt = new Master.Table(db, Sort.UNSORTED);
      _ht = new HOF.Table(db, Sort.UNSORTED, Selection.ELECTED);
      assemble(db, at, Type.BAT, 650);
      assemble(db, at, Type.PITCH, 800);
    }
  }
  
  public static void main(String[] args) throws SQLException {
    TeamPeak tp = new TeamPeak();
    tp.execute();
  }
}
