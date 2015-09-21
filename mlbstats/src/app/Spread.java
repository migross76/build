package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import util.ByPlayer;
import util.MyDatabase;
import data.Type;
import data.War;

// Show the standard deviation of WAR each year, based on the players with the most PA each year
// - Can this be used to show that the talent has gotten more consistent over the years, and
//   therefore harder to have a stellar season compared to "replacement" level?
public class Spread {
  private static final int MIN_PLAYTIME = 300;
  private static final int TOP_PLAYERS = 100;
  
  private static void calculate(ArrayList<War> list, int size) {
    double avg = 0, avgpa = 0;
    for (int i = 0; i != size; ++i) { avg += list.get(i).war(); avgpa += list.get(i).playtime(); }
    avg /= size; avgpa /= size;
    double stdev = 0;
    for (int i = 0; i != size; ++i) { stdev += (list.get(i).war() - avg) * (list.get(i).war() - avg); }
    stdev = Math.sqrt(stdev / (size-1));
    System.out.format("\t%d\t%.1f\t%.2f\t%.2f", size, avgpa, avg, stdev);
  }
  
  public static void main(String[] args) throws Exception {
    War.ByYear by = new War.ByYear();
    try (MyDatabase db = new MyDatabase()) { by.addAll(new War.Table(db, Type.BAT)); }
    
    System.out.println("Yr\tCt\tPA\tWAR\tStdev\tCt\tPA\tWAR\tStdev\n");
    for (ByPlayer<War> wBy : by) {
      ArrayList<War> qual = new ArrayList<>();
      for (War W : wBy) { if (W.playtime() >= MIN_PLAYTIME) { qual.add(W); } }
      if (qual.isEmpty()) { continue; }
      Collections.sort(qual, new Comparator<War>() {
        @Override public int compare(War arg0, War arg1) {
          if (arg0.playtime() != arg1.playtime()) { return arg1.playtime() - arg0.playtime(); }
          return arg0.compareTo(arg1);
        }
        
      });
      int yr = qual.get(0).yearID();
      System.out.print(yr);
      calculate(qual, qual.size());
      if (TOP_PLAYERS <= qual.size()) { calculate(qual, TOP_PLAYERS); }
      System.out.println();
    }
  }
}
