package app;

import java.util.List;
import util.MyDatabase;
import data.HOF;
import data.Master;
import data.Sort;

/*
 * determine first, last, best, worst HOF % votes for players
 * used to rank HOFers
 */
public class HOFVotes {
  public static void main(String[] args) throws Exception {
    Master.Table mt = null;
    HOF.Table ht = null;
    try (MyDatabase db = new MyDatabase()) {
      mt = new Master.Table(db, Sort.SORTED);
      ht = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ALL);
    }
    for (List<HOF> player : ht.byID().values()) {
      Master m = mt.byID(player.get(0).hofID());
      double first = 0, last = 0, max = 0, min = 100;
      for (HOF hof : player) {
        if (hof.votes() == 0) { continue; }
        double pct = hof.percent();
        if (first == 0 && (hof.yearID() >= 1964 || pct >= 0.10)) { first = pct; } // things were wacky before '64
        last = pct;
        if (max < pct) { max = pct; }
        if (min > pct) { min = pct; }
      }
      if (m != null && max > 0) {
        System.out.format("%s %s\t%.2f\t%.2f\t%.2f\t%.2f\t%.4f\n", m.nameFirst(), m.nameLast(), first, last, min, max, (first + max) / 2);
      }
    }
  }
}
