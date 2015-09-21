package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.Master;
import data.Position;
import data.Sort;
import data.Type;
import data.War;


// inspired by http://seamheads.com/baseballgauge/year.php?yearID=2015&tab=est_level
public class Established {
  private WeightedApp.ByYear byWA = new WeightedApp.ByYear();
  private WeightedWar.ByYear byWW = new WeightedWar.ByYear();
  private Master.Table MT = null;
  
  private static final int START_YEAR = 1901;
  
  private static class Player implements Comparable<Player> {
    @Override
    public int compareTo(Player o) { 
      double diff = _score - o._score;
      if (diff != 0) { return diff > 0 ? -1 : 1; }
      return playerID().compareTo(o.playerID());
    }
    
    public String playerID() { return _first.playerID(); }

    public Player(WeightedWar ww) { _first = ww; }
    
    public WeightedWar _first = null;
    
    public void add(WeightedWar ww) {
      _seasons += ww.seasontime();
      _career += ww.wwar();
      if (_peak < ww.wwar()) { _peak = ww.wwar(); }
      _current[0] = ww.wwar();
      computeScore();
    }
    
    public void setApp(WeightedApp wa) { _app[0] = wa; computePosition(); }
    
    public void push() {
      for (int i = 1; i != 2; ++i) {
        _current[i+1] = _current[i];
        _app[i+1] = _app[i];
      }
      _current[0] -= 20;
      _app[0] = null;
      computeScore();
    }
    
    public double _seasons = 0; // Bat=150G, C=135G, SP=30G, RP=60G
    public double _career = 0;
    public double _peak = 0;
    public double[] _current = new double[3];
    
    public WeightedApp[] _app = new WeightedApp[3]; 
    
    public double _score = 0;
    public Position _pos = null;

    public double career ( ) { return _career / 10; }
    public double seasonal ( ) { return _career / (_seasons > 1 ? _seasons : 1); }
    public double peak ( ) { return _peak / 2; }
    public double history ( ) { return (_current[2] + _current[1]) / 4; }
    public double current ( ) { return _current[0] / 2; }
    
    // Career WAR, WAR/Season, Peak, Two Previous Seasons, Current Season
    public void computeScore() {
      _score = career() + seasonal() + peak() + history() + current();
    }
    
    public void computePosition() {
      WeightedApp wa = new WeightedApp(_first.playerID(), _first.yearID());
      for (WeightedApp yr : _app) { if (yr != null) { wa = yr/*.add(yr)*/; } }
      _pos = wa.primary() == null ? null : wa.primary().pos();
    }
    
  }

  private static class Slot {
    public Slot(Position pos) { _name = pos.getName(); _pos = EnumSet.of(pos); }
    public Slot(String name, Position... poses) { _name = name; _pos = EnumSet.copyOf(Arrays.asList(poses)); }
    
    public boolean add(Player p) {
      if (!_pos.contains(p._pos)) { return false; }
      _player = p; return true;
    }
    
    public void reset() { _player = null; }
    
    public boolean isEmpty() { return _player == null; }
    
    public String _name = null;
    public EnumSet<Position> _pos = null;
    public Player _player = null;
  }

  public void execute() {

    ArrayList<Slot> slots = new ArrayList<>();
    slots.add(new Slot(Position.CATCH));
    slots.add(new Slot("1B/DH", Position.FIRST));
    slots.add(new Slot(Position.SECOND));
    slots.add(new Slot(Position.THIRD));
    slots.add(new Slot(Position.SHORT));
    slots.add(new Slot(Position.LEFT));
    slots.add(new Slot(Position.CENTER));
    slots.add(new Slot(Position.RIGHT));
    slots.add(new Slot(Position.DESIG));
    slots.add(new Slot("IF", Position.CATCH, Position.FIRST, Position.SECOND, Position.THIRD, Position.SHORT, Position.DESIG));
    slots.add(new Slot("OF", Position.LEFT, Position.CENTER, Position.RIGHT));
    slots.add(new Slot("Bat", Position.CATCH, Position.FIRST, Position.SECOND, Position.THIRD, Position.SHORT, Position.LEFT, Position.CENTER, Position.RIGHT, Position.DESIG));
    slots.add(new Slot("Bat", Position.CATCH, Position.FIRST, Position.SECOND, Position.THIRD, Position.SHORT, Position.LEFT, Position.CENTER, Position.RIGHT, Position.DESIG));
    slots.add(new Slot("Bat", Position.CATCH, Position.FIRST, Position.SECOND, Position.THIRD, Position.SHORT, Position.LEFT, Position.CENTER, Position.RIGHT, Position.DESIG));
    slots.add(new Slot(Position.STARTER));
    slots.add(new Slot(Position.STARTER));
    slots.add(new Slot(Position.STARTER));
    slots.add(new Slot(Position.STARTER));
    slots.add(new Slot(Position.STARTER));
    slots.add(new Slot("RP", Position.MIDDLE, Position.CLOSER));
    slots.add(new Slot("RP", Position.MIDDLE, Position.CLOSER));
    slots.add(new Slot("Pit", Position.STARTER, Position.MIDDLE, Position.CLOSER));
    slots.add(new Slot("Pit", Position.STARTER, Position.MIDDLE, Position.CLOSER));
    
    System.out.print("Year");
    for (Slot S : slots) { System.out.format("\t%s\t%s", S._name, S._name); }
    System.out.println();

    HashMap<String, Player> players = new HashMap<>();  
    for (ByPlayer<WeightedWar> pWW : byWW) {
      int year = pWW.first().yearID();
      for (Player p : players.values()) { p.push(); }
      for (WeightedWar ww : pWW) {
        Player p = players.get(ww.playerID());
        if (p == null) { players.put(ww.playerID(), p = new Player(ww)); }
        p.add(ww);
      }
      ByPlayer<WeightedApp> bpWA = byWA.get(year);
      if (bpWA != null) {
        for (WeightedApp wa : byWA.get(year)) {
          Player p = players.get(wa.playerID());
          if (p != null) { p.setApp(wa); }
        }
      }
      if (year < START_YEAR) { continue; }
      ArrayList<Player> list = new ArrayList<>(players.values());
      Collections.sort(list);
      
      for (Slot S : slots) { S.reset(); }
      for (Player p : list) {
        boolean alldone = true;
        for (Slot s : slots) {
          if (s.isEmpty()) { alldone = false; } else { continue; }
          if (s.add(p)) { break; }
        }
        if (alldone) { break; }
      }
      System.out.format("%d", year);
/*
      for (int i = 0; i != 10; ++i) {
        Player p = list.get(i);
        Master M = MT.byID(p.playerID());
        System.out.format("\t%s %s\t%.2f", M.nameFirst(), M.nameLast(), p._score);
//        System.out.format("\t%s %s\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", M.nameFirst(), M.nameLast(), p._score, p._seasons, p.career(), p.seasonal(), p.peak(), p.history(), p.current());
      }
      System.out.println();
    }
*/
    for (Slot S : slots) {
      if (S.isEmpty()) { System.out.print("\t\t"); continue; }
      Player P = S._player;
      Master M = MT.byID(P.playerID());
      System.out.format("\t%s %s\t%.1f", M.nameFirst(), M.nameLast(), P._score);
    }
    System.out.println();
/*      
      for (int i_yr = 0; i_yr != YEARS; ++i_yr) {
        for (WeightedApp WA : byWA.get(s_yr + i_yr)) {
          for (Slot S : slots) { S.addIf(WA); }
        }
      }
      System.out.format("%d\t%d", s_yr, s_yr + YEARS - 1);
*/
    }
  }
  
  private void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    War.Table WT = new War.Table(db, type);
    WeightedWar.Tally WWT = new WeightedWar.Tally(WT); 
    WWT.adjustByPosition(AT);
    byWW.addAll(WWT);
    WeightedApp.Tally WA = new WeightedApp.Tally(WT, AT);
    byWA.addAll(WA);
  }
  
  public Established() throws SQLException {
    try (MyDatabase db = new MyDatabase()) {
      Appearances.Table AT = new Appearances.Table(db);
      MT = new Master.Table(db, Sort.UNSORTED);
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }
  }
  
  public static void main(String[] args) throws SQLException {
    new Established().execute();
  }

}
