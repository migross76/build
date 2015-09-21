package strat.driver;

import strat.client.model.Batter;
import strat.client.model.Fielding;
import strat.client.model.Pitcher;
import strat.server.Load;

/** This generates a list of player ids and positions, ideal starting list for mypos.txt */
public class PositionList {
  public static void main(String[] args) throws Exception {
    Load load = new Load();
    for (Batter b : load._batters) {
      System.out.format("%sb", b._id);
      int best_field = 6;
      for (Fielding f : b._fielding.values()) {
        System.out.format("\t%s", f._pos.code());
        if (f._field < best_field) { best_field = f._field; }
      }
      if (best_field > 2) { System.out.print("\tdh"); }
      System.out.println();
    }
    for (Pitcher p : load._pitchers) {
      System.out.format("%sp\t%s\n", p._id, (p._closer > 3 && p._starter < 9) || p._starter < 6 ? "rp" : "sp");
    }
  }
}
