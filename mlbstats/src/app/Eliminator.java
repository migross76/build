package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import util.ByPlayer;
import util.MyDatabase;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

// Select the best active player(s) for each year
// Once a player is selected, they are zeroed out, so the next year a new player is selected
// It is possible for a player to show up again, if they collect enough value to climb to the top once more
public class Eliminator {
  private static class Player implements Comparable<Player> {
    public Player(War W) { _info = W; }
    public War _info = null;
    public double _war = 0;
    @Override public int compareTo(Player o) {
      if (_war != o._war) { return _war > o._war ? -1 : 1; } 
      return _info.compareTo(o._info);
    }
  }
  
  private static final int FIRST_YEAR = 1885;
  private static final int PER_YEAR = 2;
  
  public static void main(String[] args) throws Exception {
    Master.Table MT = null;
    War.ByYear by = new War.ByYear();
    War.ByID id = new War.ByID();
    try (MyDatabase db = new MyDatabase()) {
      MT = new Master.Table(db, Sort.UNSORTED);
      War.Table wt = new War.Table(db, Type.BAT);
      by.addAll(wt);
      id.addAll(wt);
      wt = new War.Table(db, Type.PITCH);
      by.addAll(wt);
      id.addAll(wt);
    }
    HashMap<String, Player> map = new HashMap<>();
    for (ByPlayer<War> wBy : by) {
      int yr = 0;
      ArrayList<Player> list = new ArrayList<>();
      for (War W : wBy) {
        if (yr == 0) { yr = W.yearID(); }
        Player P = map.get(W.playerID());
        if (P == null) { map.put(W.playerID(), P = new Player(W)); }
        list.add(P);
        P._war += W.war();
      }
      if (yr < FIRST_YEAR) { continue; }
      Collections.sort(list);
      System.out.print(yr);
      for (int i = 0; i != PER_YEAR; ++i) {
        Player P = list.get(i);
        map.remove(P._info.playerID());
        Master M = MT.byID(P._info.playerID());
        System.out.format("\t%s %s\t%.1f\t%.1f", M.nameFirst(), M.nameLast(), P._war, id.get(P._info.playerID()).total().war());
      }
      System.out.println();
    }
  }
}
