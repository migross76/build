package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeSet;
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

/*
 * Elect Hall-of-Famers based on a percentage of their peers (1% of all players, or 5% of 10-year players).
 * Create a slot for each of the peer slots, and fill them in with the best players available.
 * Give a penalty for each slot the player drifts from their original slot.
 * This provides a balance between better players and players from each era.
 */
public class HallSlots {
  // 20 | 10 for best HOF-eligible draft
  // 100 | 1 for best players
  // 80 | 1 for Poz standards
  // 50 | 1 for roughly HOF standards
  // 123 | 1 | 1983-12-31 for 100 players
  // 260 | 1 | 5.0 for 50 players (131 for 100)

//  private static final int PER_BLOCK = 75;
//  private static final double SLOT_SHIFT = 0.1; //1.0 / PER_BLOCK;

  private static final int PER_BLOCK = 25; // 1000, 500, 250, 160, 120
  private static final double SLOT_SHIFT = 0.01;

  private static final int MIN_YEARS = 1;
  private static final int PLAY_AFTER = 0;
  private static final double MIN_WWAR = 0;
  private static final boolean DEBUG = false;
  private static final String LAST_DATE = "1980-12-31";
  private static final Position[] POSITIONS = { };
  
  private static Master.Table _mt = null;
  private static WeightedWar.ByID _wwBy = null;
  private static WeightedApp.ByID _waBy = null;
  
  private static class Player implements Comparable<Player> {
    public final Master _master;
    public final WeightedWar _wwar;
    public final WeightedApp _app;
    public int _pos = -1;
    public double _score = 0;

    @Override public int compareTo(Player arg0) {
      if (arg0 == null) { return -1; }
      int cmp = -Double.compare(_wwar.wwar(), arg0._wwar.wwar());
      if (cmp != 0) { return cmp; }
      cmp = -Double.compare(_wwar.waa_pos(), arg0._wwar.waa_pos());
      if (cmp != 0) { return cmp; }
      return _master.playerID().compareTo(arg0._master.playerID());
    }
    
    public Player(Master M, ByPlayer<WeightedWar> WW, WeightedApp WA) {
      _master = M;
      _wwar = WW.total();
      _app = WA;
    }
  }
  
  private static Comparator<Player> BY_BIRTH = new Comparator<Player>() {
    @Override public int compare(Player arg0, Player arg1) {
      Master m0 = arg0._master, m1 = arg1._master;
      int cmp = m0.yearBirth() - m1.yearBirth();
      if (cmp != 0) { return cmp; }
      cmp = m0.monthBirth() - m1.monthBirth();
      if (cmp != 0) { return cmp; }
      cmp = m0.dayBirth() - m1.dayBirth();
      if (cmp != 0) { return cmp; }
      return m0.playerID().compareTo(m1.playerID());
    }
  };
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type, int playtime) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type), playtime); 
    WWT.adjustByPosition(AT);
    _wwBy.addAll(WWT);
    _waBy.addAll(new WeightedApp.Tally(WWT, AT));
  }
  
  public static void main(String[] args) throws Exception {
    // table initialization
    _wwBy = new WeightedWar.ByID();
    _waBy = new WeightedApp.ByID();
    try (MyDatabase db = new MyDatabase()) {
      _mt = new Master.Table(db, Sort.SORTED);
      Appearances.Table AT = new Appearances.Table(db);
      assemble(db, AT, Type.BAT, 650);
      assemble(db, AT, Type.PITCH, 800);
    }
    
    HashSet<Position> positions = new HashSet<>(Arrays.asList(POSITIONS));
    
    ArrayList<Player> players = new ArrayList<>();
    for (ByPlayer<WeightedWar> wwBy : _wwBy) {
      int years = 0, lastYear = 0;
      for (WeightedWar ww : wwBy) {
        if (   lastYear != ww.yearID()
            && !ww.leagueID().equals("FL")
            && ww.yearID() >= PLAY_AFTER
            && ww.wwar() >= MIN_WWAR) { lastYear = ww.yearID(); ++years; } 
      }
      if (years < MIN_YEARS) { continue; }
      Master M = _mt.byID(wwBy.total().playerID());
      if (LAST_DATE.compareTo(M.birthday()) < 0) { continue; }
      ByPlayer<WeightedApp> waBy = _waBy.get(M.playerID());
      WeightedApp WA = waBy == null ? null : waBy.total();
      if (!positions.isEmpty() && WA != null && WA.primary() != null && !positions.contains(WA.primary().pos())) { continue; }
      players.add(new Player(M, wwBy, WA));
    }
    Collections.sort(players, BY_BIRTH);
    for (int i = 0; i != players.size(); ++i) { players.get(i)._pos = i; }
    Collections.sort(players);
    
    Player[] slots = new Player[players.size() / PER_BLOCK];
    TreeSet<Player> extra = new TreeSet<>();
    int min = slots.length;
    double min_score = 10000;
    // TODO consider creating a cutoff point; what does this mean for Pujols, etc.?
    for (Player p : players) {
      while (p != null) {
        p._score = 0;
        int best_slot = -1;
        int id = p._pos;
        int slot = id / PER_BLOCK;
        double score = p._wwar.wwar();
        if (min <= 0 && score < min_score) { p = null; break; }
        // exact slot
        if (slot < slots.length && (slots[slot] == null || score > slots[slot]._score)) { p._score = score; best_slot = slot; }
        // slot before
        int b_slot = slot - 1;
        double b_score = score - (id - slot * PER_BLOCK - 1) * SLOT_SHIFT;
        while (b_score > p._score && b_slot >= 0) {
          if (b_slot < slots.length && (slots[b_slot] == null || b_score > slots[b_slot]._score)) { p._score = b_score; best_slot = b_slot; break; }
          --b_slot; b_score -= SLOT_SHIFT * PER_BLOCK;
        }
        // slot after
        int e_slot = slot + 1;
        double e_score = score - ((slot+1) * PER_BLOCK - id + 1) * SLOT_SHIFT;
        while (e_score > p._score && e_slot < slots.length) {
          if (slots[e_slot] == null || e_score > slots[e_slot]._score) { p._score = e_score; best_slot = e_slot; break; }
          ++e_slot; e_score -= SLOT_SHIFT * PER_BLOCK;
        }
        if (best_slot != -1) {
          Player old = slots[best_slot];
if (DEBUG) {
  System.err.format("%d\t(%d)\t%s %s\t%.1f\t%.1f", best_slot, min, p._master.nameFirst(), p._master.nameLast(), p._wwar.wwar(), p._score);
  if (old != null) { System.err.format("\t%s %s\t%.1f\t%.1f", old._master.nameFirst(), old._master.nameLast(), old._wwar.wwar(), old._score); }
  System.err.println();
}
          slots[best_slot] = p;
          p = old;
          if (p == null) { --min; } // filled a new slot
        } else { extra.add(p); if (DEBUG) { System.err.format("\t\t%s %s\t%.1f\n", p._master.nameFirst(), p._master.nameLast(), p._wwar.wwar()); } p = null; }
      }
      if (min <= 0) {
        min_score = 10000;
        for (Player p2 : slots) {
          if (p2 == null) { min_score = 0; break; }
          if (min_score > p2._score) { min_score = p2._score; }
        }
      }
    }
    
    Collections.sort(players, BY_BIRTH);
    System.out.print("Start\tEnd\tName\tDOB\tPos\tWAR\tFactor\twWAR\tScore\n");
    for (int i = 0; i != slots.length; ++i) {
      Player p = slots[i];
      if (p == null) { System.out.println("[NULL???]"); continue; }
      System.out.format("%s\t%s\t%s %s\t%s\t%s\t%.1f\t%.2f\t%.1f\t%.1f\n", players.get(i*PER_BLOCK)._master.birthday(), players.get((i+1)*PER_BLOCK)._master.birthday(), p._master.nameFirst(), p._master.nameLast(), p._master.birthday(),
          p._app.primary().pos().getName(), p._wwar.war(), p._wwar.factor(), p._wwar.wwar(), p._score);
    }
/*    
    int i = 0;
    System.out.println();
    for (Player p : extra) {
      System.out.format("\t\t%s %s\t%s\t%s\t%.1f\t%.2f\t%.1f\n", p._master.nameFirst(), p._master.nameLast(), p._master.birthday(),
          p._app.primary().pos().getName(), p._wwar.war(), p._wwar.factor(), p._wwar.wwar());
      if (++i == 50) { break; }
    }
*/
  }
}
