package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
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
 * Print out the top 700 players by wWAR, and their best 5-year run
 * Use this to figure out which eras have the most/strongest candidates
 */
public class HallEra {
  private static Master.Table _mt = null;
  private static WeightedWar.ByID _wwBy = null;
  private static WeightedApp.ByID _waBy = null;
  
  private static class Player implements Comparable<Player> {
    public final Master _master;
    public final ByPlayer<WeightedWar> _career;
    public final WeightedWar _wwar;
    public final WeightedApp _app;
    public int _peak_yr_start = 0;
    public int _peak_yr_end = 0;

    @Override public int compareTo(Player arg0) {
      if (_wwar != arg0._wwar) { return _wwar.wwar() > arg0._wwar.wwar() ? -1 : 1; }
      return _master.playerID().compareTo(arg0._master.playerID());
    }
    
    public Player(Master M, ByPlayer<WeightedWar> WW, WeightedApp WA) {
      _master = M;
      _career = WW;
      _wwar = WW.total();
      _app = WA;
      double best_ww = 0;
      for (int i = 0; i != WW.size(); ++i) {
        int yr_start = WW.get(i).yearID();
        int yr_end = 0;
        double total = 0;
        for (int j = i; j != WW.size() && WW.get(j).yearID() < yr_start + 5; ++j) {
          total += WW.get(j).wwar();
          yr_end = WW.get(j).yearID();
        }
        if (best_ww < total) { best_ww = total; _peak_yr_start = yr_start; _peak_yr_end = yr_end; }
      }
    }
    
  }
  
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
    
    ArrayList<Player> players = new ArrayList<>();
    for (ByPlayer<WeightedWar> wwBy : _wwBy) {
      Master M = _mt.byID(wwBy.total().playerID());
      ByPlayer<WeightedApp> waBy = _waBy.get(M.playerID());
      WeightedApp WA = waBy == null ? null : waBy.total();
      players.add(new Player(M, wwBy, WA));
    }
    Collections.sort(players);
    
    System.out.print("Rk\tName\tPos\tYOB\tFirst\tLast\tiPeak\tePeak\twWAR\n");
    for (int i = 0; i != 700; ++i) {
      Player p = players.get(i);
      System.out.format("%3d\t%s %s\t%s\t%d\t%d\t%d\t%d\t%d\t%.1f\n", 
                        i+1, p._master.nameFirst(), p._master.nameLast(), p._app.primary().pos().getName(),
                        p._master.yearBirth(), p._career.first().yearID(), p._career.last().yearID(),
                        p._peak_yr_start, p._peak_yr_end, p._wwar.wwar());
    }
  }
}
