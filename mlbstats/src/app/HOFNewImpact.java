package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import util.MyDatabase;
import data.HOF;
import data.Sort;

public class HOFNewImpact {
  
  private static Comparator<HOF> BY_SCORE = new Comparator<HOF>() {
    @Override public int compare(HOF arg0, HOF arg1) {
      return arg1.votes() - arg0.votes();
    }
  };
  
  public void apply() {
    System.out.print("Year\tDropped\tFirst\tPrev\tCurr\tNew\tImpact\tImp%");
    for (int i = 0; i != 5; ++i) { System.out.format("\tTop#%1$d\tN-1#%1$d\tN#%1$d\tDiff#%1$d", i+1); }
    System.out.println();
    HashMap<String, Double> last_round = new HashMap<>();
    ArrayList<HOF> last_top = new ArrayList<>();
    for (List<HOF> hofs : _hof.byYear().values()) {
      double first_time = 0, dropped = 0, prev = 0, curr = 0;
      int year = 0;
      for (HOF hof : hofs) {
        if (!"BBWAA".equals(hof.votedBy())) { continue; }
        Double last_perc = last_round.get(hof.hofID());
        if (last_perc == null) {
          first_time += hof.percent();
        } else {
          prev += last_perc;
          curr += hof.percent();
          last_round.remove(hof.hofID());
        }
        year = hof.yearID();
      }
      if (year == 0) { continue; }
      for (Double perc : last_round.values()) { dropped += perc; }
      for (Iterator<HOF> i_last = last_top.iterator(); i_last.hasNext(); ) {
        HOF last = i_last.next();
        if (last_round.containsKey(last.hofID())) { i_last.remove(); }
        
      }
      System.out.format("%d\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f\t%.2f", year, dropped, first_time, prev, curr, first_time - dropped, curr - prev);
      last_round.clear();
      for (HOF hof : hofs) { if (!"BBWAA".equals(hof.votedBy())) { continue; } last_round.put(hof.hofID(), hof.percent()); }
      // y = -0.7894x + 0.1382
      double expected = -0.7894 * (curr - prev) + 0.1382;
      double exp_pct = expected / (first_time + curr);
      System.out.format("\t%.2f", exp_pct);
      if (last_top.size() >= 5) {
        for (int i = 0; i != 5; ++i) {
          HOF hof = last_top.get(i);
          double new_top = last_round.get(hof.hofID());
          System.out.format("\t%s\t%.2f\t%.2f\t%.2f", hof.hofID(), hof.percent(), new_top, new_top - hof.percent() + exp_pct);
        }
      }
      System.out.println();
      last_top.clear();
      for (HOF hof : hofs) { if (!"BBWAA".equals(hof.votedBy())) { continue; } last_top.add(hof); }
      Collections.sort(last_top, BY_SCORE);
    }
  }

  public HOFNewImpact(MyDatabase db) throws SQLException {
    _hof = new HOF.Table(db, Sort.SORTED, HOF.Selection.ALL);
  }
  
  public static void main(String[] args) throws SQLException {
    try (MyDatabase db = new MyDatabase()) {
      new HOFNewImpact(db).apply();
    }
  }
  
  private final HOF.Table _hof;
}
