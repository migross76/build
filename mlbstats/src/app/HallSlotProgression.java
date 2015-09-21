package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

/*
 * Elect Hall-of-Famers based on a percentage of their peers (1% of all players, or 5% of 10-year players).
 * Create a slot for each of the peer slots, and fill them in with the best players available.
 * Give a penalty for each slot the player drifts from their original slot.
 * This provides a balance between better players and players from each era.
 */
public class HallSlotProgression {
  // 20 | 10 for best HOF-eligible draft
  // 100 | 1 for best players
  // 80 | 1 for Poz standards
  private static final int PER_BLOCK = 80;
  private static final int MIN_YEARS = 1;
  private static final double SLOT_SHIFT = 1.0 / PER_BLOCK;
  private static final boolean DEBUG = false;
  private static final int START_YEAR = 1885;
  private static final int END_YEAR = 1970;
  private static final String DAY = "-12-31";
  
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
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
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
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }
    
    ArrayList<Player> full = new ArrayList<>();
    for (ByPlayer<WeightedWar> wwBy : _wwBy) {
      int years = 0, lastYear = 0;
      for (WeightedWar ww : wwBy) { if (lastYear != ww.yearID() && !ww.leagueID().equals("FL")) { lastYear = ww.yearID(); ++years; } }
      if (years < MIN_YEARS) { continue; }
      Master M = _mt.byID(wwBy.total().playerID());
      ByPlayer<WeightedApp> waBy = _waBy.get(M.playerID());
      WeightedApp WA = waBy == null ? null : waBy.total();
      full.add(new Player(M, wwBy, WA));
    }
    Collections.sort(full, BY_BIRTH);
    for (int i = 0; i != full.size(); ++i) { full.get(i)._pos = i; }
    
    ArrayList<Player> players = new ArrayList<>();

    System.out.print("Year\tStart\tEnd\tName\tDOB\tPos\tWAR\tFactor\twWAR\tScore\n");
    TreeSet<Player> last_slots = new TreeSet<>();
    for (int year = START_YEAR; year != END_YEAR; ++year) {
      String date = year + DAY;
//System.err.println(date);
      while (!full.isEmpty() && full.get(0)._master.birthday().compareTo(date) <= 0) { players.add(full.remove(0)); }
      Collections.sort(players);
    
      Player[] slots = new Player[players.size() / PER_BLOCK];
      int min = slots.length;
      double min_score = 10000;
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
          } else { p = null; }
        }
        if (min <= 0) {
          min_score = 10000;
          for (Player p2 : slots) {
            if (p2 == null) { min_score = 0; break; }
            if (min_score > p2._score) { min_score = p2._score; }
          }
          if (DEBUG) { System.err.format("min : %.1f\n", min_score); }
        }
      }
      Collections.sort(players, BY_BIRTH);
      for (int i = 0; i != slots.length; ++i) {
        Player p = slots[i];
        boolean deleted = last_slots.remove(p);
        if (!deleted) {
          if (p == null) { System.out.println("[NULL???]"); continue; }
          System.out.format("%d\t%s\t%s\t%s %s\t%s\t%s\t%.1f\t%.2f\t%.1f\t%.1f\n", year, players.get(i*PER_BLOCK)._master.birthday(), players.get((i+1)*PER_BLOCK-1)._master.birthday(), p._master.nameFirst(), p._master.nameLast(), p._master.birthday(),
              p._app.primary().pos().getName(), p._wwar.war(), p._wwar.factor(), p._wwar.wwar(), p._score);
        }
      }
      for (Player p : last_slots) {
        System.out.format("%d\t****-**-**\t****-**-**\t%s %s\t%s\t%s\t%.1f\t%.2f\t%.1f\t%.1f\n", year, p._master.nameFirst(), p._master.nameLast(), p._master.birthday(),
            p._app.primary().pos().getName(), p._wwar.war(), p._wwar.factor(), p._wwar.wwar(), p._score);
      }
      System.out.flush();
      last_slots.clear();
      last_slots.addAll(Arrays.asList(slots));
    }
  }
}
