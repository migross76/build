package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
 * Elect players as soon as they're the best active player.
 * Also elect those within a certain percentage of the top player (wWAR isn't that precise)
 * Consider giving bonus points for finishing in the top N
 */
public class HallAnnual {
  private static Master.Table _mt = null;
  private static WeightedWar.ByID _wwBy = null;
  private static WeightedApp.ByID _waBy = null;
  
  private static class Player implements Comparable<Player> {
    public final Master _master;
    public final ByPlayer<WeightedWar> _career;
    public final WeightedWar _wwar;
    public final WeightedApp _app;
    public double _progress = 0;

    @Override public int compareTo(Player arg0) {
      if (_progress != arg0._progress) { return _progress > arg0._progress ? -1 : 1; }
      if (_master.yearBirth() != arg0._master.yearBirth()) { return _master.yearBirth() - arg0._master.yearBirth(); } // older players first
      return _master.playerID().compareTo(arg0._master.playerID());
    }
    
    public void setProgress(int yearID) {
      _progress = 0;
      for (WeightedWar ww : _career) {
        if (ww.yearID() > yearID) { break; }
        if (ww.wwar() > 0) { _progress += ww.wwar(); }
      }
    }
    
    public void print(String line) {
      System.out.format("%s\t%s %s\t%s\t%d\t%d\t%d\t%.1f\t%.1f\t%.1f", line, _master.nameFirst(), _master.nameLast(), _app.primary().pos().getName(),
          _master.yearBirth(), yearFirst(), yearLast(), _progress, _wwar.war(), _wwar.wwar());
    }
    
    public int yearFirst() { return _career.first().yearID(); }
    public int yearLast() { return _career.last().yearID(); }
    
    public Player(Master M, ByPlayer<WeightedWar> WW, WeightedApp WA) {
      _master = M;
      _career = WW;
      _wwar = WW.total();
      _app = WA;
    }
  }
  
  private static Comparator<Player> BY_DEBUT = new Comparator<Player>() {
    @Override public int compare(Player arg0, Player arg1) {
      return arg0.yearFirst() - arg1.yearFirst();
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
    
    ArrayList<Player> players = new ArrayList<>();
    for (ByPlayer<WeightedWar> wwBy : _wwBy) {
      Master M = _mt.byID(wwBy.total().playerID());
      ByPlayer<WeightedApp> waBy = _waBy.get(M.playerID());
      WeightedApp WA = waBy == null ? null : waBy.total();
      players.add(new Player(M, wwBy, WA));
    }
    Collections.sort(players, BY_DEBUT);
    ArrayList<Player> compete = new ArrayList<>(), dq = new ArrayList<>();
    
    System.out.print("Year\tName\tPos\tYOB\tFirst\tLast\tElect\tWAR\twWAR\t#2\t\t#3\t\t#4\t\t#5\n");
    int curr_year = 1880;
    for (int yr = 1901; yr != 2013; ++yr) {
      for (Iterator<Player> i_p = compete.iterator(); i_p.hasNext(); ) {
        Player p = i_p.next();
        if (p.yearLast() <= curr_year) {
          i_p.remove(); dq.add(p);
        }
      }
      if (++curr_year != yr) { ++curr_year; }
      for (Iterator<Player> i_p = players.iterator(); i_p.hasNext(); ) {
        Player p = i_p.next();
        if (p.yearFirst() <= curr_year) { i_p.remove(); compete.add(p); }
        else { break; }
      }
      for (Player p : compete) { p.setProgress(curr_year); }
      Collections.sort(compete);
      Player w = compete.remove(0);
      w.print(Integer.toString(curr_year));
      while (compete.get(0)._progress * 1.02 >= w._progress) {
        compete.remove(0).print("\n" + curr_year);
      }
      for (int i = 0; i != 3; ++i) {
        Player n = compete.get(i);
        System.out.format("\t%s %s\t%.1f", n._master.nameFirst(), n._master.nameLast(), n._progress);
      }
      System.out.println();
    }
    Collections.sort(dq);
    System.out.println();
    for (int i = 0; i != 50; ++i) {
      dq.get(i).print("N/A"); System.out.println();
    }
  }

}
