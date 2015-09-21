package app;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.TreeMap;
import util.ByPlayer;
import util.MyDatabase;
import data.Appearances;
import data.Position;

// Show the overlap in positions, to get a sense for which positions are more interchangeable
// For each player that played multiple positions, count the minimum games of the two, and add that to the total
public class TwoGloves {

  private static class Overlap {
    public Overlap(Position pos1, Position pos2) { _pos1 = pos1; _pos2 = pos2; }
    public Position _pos1 = null;
    public Position _pos2 = null;
    public int _total = 0;
  }
  
  private static class Year {
    public Year(int yearID) { _yearID = yearID; }
    public int _yearID = 0;
    public Overlap[] _overlaps = new Overlap[144];

    private static int offset(Position one, Position two) {
      return one.ordinal() * Position.values().length + two.ordinal();
    }
    
    public void add(Appearances A) {
      for (Appearances.Use one : A) {
        for (Appearances.Use two : A) {
          if (one.pos().compareTo(two.pos()) < 0) {
            int offset = offset(one.pos(), two.pos());
            double g = Math.min(one.games(), two.games());
            Overlap O = _overlaps[offset];
            if (O == null) { _overlaps[offset] = O = new Overlap(one.pos(), two.pos()); }
            O._total += g;
            O = _total._overlaps[offset];
            if (O == null) { _total._overlaps[offset] = O = new Overlap(one.pos(), two.pos()); }
            O._total += g;
            _used.add(offset);
          }
        }
      }
    }
    
    public static Year _total = new Year(0);
    public static HashSet<Integer> _used = new HashSet<>();
  }

  public static void main(String[] args) throws SQLException {
    TreeMap<Integer, Year> years = new TreeMap<>();

    Appearances.ByID aBy = new Appearances.ByID();
    try (MyDatabase db = new MyDatabase()) { aBy.addAll(new Appearances.Table(db)); }
    for (ByPlayer<Appearances> S : aBy) {
      for (Appearances A : S) {
        Year Y = years.get(A.yearID());
        if (Y == null) { years.put(A.yearID(), Y = new Year(A.yearID())); }
        Y.add(A);
      }
    }
    
    System.out.print("Year");
    for (int i = 0; i != 144; ++i) {
      if (!Year._used.contains(i)) { continue; }
      Overlap O = Year._total._overlaps[i];
      System.out.format("\t%s-%s", O._pos1.getName(), O._pos2.getName());
    }
    System.out.println();
    for (Year year : years.values()) {
      System.out.print(year._yearID);
      for (int i = 0; i != 144; ++i) {
        if (!Year._used.contains(i)) { continue; }
        Overlap O = year._overlaps[i];
        if (O == null) { System.out.print("\t"); } else { System.out.format("\t%d", O._total); }
      }
      System.out.println();
    }
    System.out.print("Total");
    for (int i = 0; i != 144; ++i) {
      if (!Year._used.contains(i)) { continue; }
      Overlap O = Year._total._overlaps[i];
      System.out.format("\t%d", O._total);
    }
    System.out.println();
  }

}
