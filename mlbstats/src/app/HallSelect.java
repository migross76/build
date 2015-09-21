package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
 * Start with the first 10 blocks of candidates, and elect 10 players
 * Then, replace the oldest block with a new one, and elect 1 player
 */
public class HallSelect {
  // 20 | 15 | 10 for best HOF-eligible draft
  // 100 | 10 | 1 for best players
  // 80 | 10 | 1 for Poz standards
  private static final int PER_BLOCK = 80;
  private static final int START_BLOCKS = 10;
  private static final int MIN_YEARS = 1;
  
  
  private static Master.Table _mt = null;
  private static WeightedWar.ByID _wwBy = null;
  private static WeightedApp.ByID _waBy = null;
  
  private static class Player implements Comparable<Player> {
    public final Master _master;
    public final WeightedWar _wwar;
    public final WeightedApp _app;

    @Override public int compareTo(Player arg0) {
      double cmp = _wwar.wwar() - arg0._wwar.wwar();
      if (cmp != 0) { return cmp > 0 ? -1 : 1; }
      cmp = _wwar.waa_pos() - arg0._wwar.waa_pos();
      if (cmp != 0) { return cmp > 0 ? -1 : 1; }
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
  
  private static void select(ArrayList<Player> candidates, Player start, Player end) {
    Player elect = candidates.remove(0);
    System.out.format("%s\t%s\t%s %s\t%s\t%s\t%.1f\t%.2f\t%.1f\t", start._master.birthday(), end._master.birthday(), elect._master.nameFirst(), elect._master.nameLast(), elect._master.birthday(),
                      elect._app.primary().pos().getName(), elect._wwar.war(), elect._wwar.factor(), elect._wwar.wwar());
  }
  
  private static void remove(ArrayList<Player> candidates, ArrayList<Player> list, int start) {
    for (int end = start + PER_BLOCK; start != end; ++start) {
      Player del = list.get(start);
      if (candidates.remove(del)) {
        System.out.format("\t%s %s\t%s\t%s\t%.1f\t%.2f\t%.1f", del._master.nameFirst(), del._master.nameLast(), del._master.birthday(),
            del._app.primary().pos().getName(), del._wwar.war(), del._wwar.factor(), del._wwar.wwar());
      }
    }
    System.out.println();
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
    
    ArrayList<Player> players = new ArrayList<>();
    for (ByPlayer<WeightedWar> wwBy : _wwBy) {
      int years = 0, lastYear = 0;
      for (WeightedWar ww : wwBy) { if (lastYear != ww.yearID() && !ww.leagueID().equals("FL")) { lastYear = ww.yearID(); ++years; } }
      if (years < MIN_YEARS) { continue; }
      Master M = _mt.byID(wwBy.total().playerID());
      ByPlayer<WeightedApp> waBy = _waBy.get(M.playerID());
      WeightedApp WA = waBy == null ? null : waBy.total();
      players.add(new Player(M, wwBy, WA));
    }
    System.out.println("Total = " + players.size() + "\n");
    Collections.sort(players, BY_BIRTH);
    
    ArrayList<Player> candidates = new ArrayList<>();

    System.out.print("Start\tEnd\tName\tDOB\tPos\tWAR\tFactor\twWAR\tMissed\tDOB\tPos\tWAR\tFactor\twWAR");
    int cand_end = PER_BLOCK * START_BLOCKS;
    candidates.addAll(players.subList(0, cand_end));
    Collections.sort(candidates);
    candidates.subList(START_BLOCKS * 2 - 1, candidates.size()).clear();
    for (int i = 0; i != START_BLOCKS; ++i) { // TODO only keep players eligible to be elected
      System.out.println();
      select(candidates, players.get(0), players.get(cand_end - 1));
    }

    int cand_start = 0;
    while (cand_end + PER_BLOCK < players.size()) {
      remove(candidates, players, cand_start);
      cand_start += PER_BLOCK;
      candidates.addAll(players.subList(cand_end, cand_end + PER_BLOCK));
      Collections.sort(candidates);
      candidates.subList(START_BLOCKS, candidates.size()).clear();
      cand_end += PER_BLOCK;
      select(candidates, players.get(cand_start), players.get(cand_end - 1));
    }
    remove(candidates, players, cand_start);
  }

}
