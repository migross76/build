package util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import db.CachedStatement;

public class Derby {
  public static class Span implements Comparable<Span> {
    public String _name = null;
    public String _id = null;
    public int _born = 0;
    public int _first_year = 0;
    public int _last_year = 0;
    public int _ab = 0;
    public int _hr = 0;
    public int _so = 0;
    public int _bip = 0;
    public double _hr_bip = 0;
    public void compute() {
      _bip = _ab - _so;
      _hr_bip = _hr * 500.0 / _bip;
    }
    
    public Span ( String id, String name, int born, int year ) { _id = id; _name = name; _born = born; _first_year = year; }
    @Override
    public int compareTo(Span arg0) {
      double bip_diff = _hr_bip - arg0._hr_bip;
      if (bip_diff != 0) { return bip_diff > 0 ? -1 : 1; }
      if (_bip != arg0._bip) { return arg0._bip - _bip; }
      return _id.compareTo(_id); 
    }
  }
  

  private static final String QUERY =
    "SELECT m.playerID, b.yearID, m.nameFirst, m.nameLast, m.birthYear, b.AB, b.HR, b.SO FROM master m, batting b WHERE m.playerID = b.playerID ORDER BY m.playerID, b.yearID";
  
  /**
   * @param args
   * @throws SQLException 
   */
  public static void main(String[] args) throws SQLException {
    HashMap<String, Span> spans = new HashMap<>();
    try (MyDatabase db = new MyDatabase()) {
      CachedStatement PS = db.prepare(QUERY);
      try (ResultSet RS = PS.executeQuery()) {
        while (RS.next()) {
          String id = RS.getString(1);
          int year = RS.getInt(2);
          for (int i_y = year - 4; i_y != year + 1; ++i_y) {
            String key = id + ":" + i_y;
            Span S = spans.get(key);
            if (S == null) { spans.put(key, S = new Span(id, RS.getString(3) + " " + RS.getString(4), RS.getInt(5), year)); }
            S._ab += RS.getInt(6);
            S._hr += RS.getInt(7);
            S._so += RS.getInt(8);
            S._last_year = year;
          }
        }
      }
    }
    ArrayList<Span> qual = new ArrayList<>();
    for (Span S : spans.values()) {
      if (S._ab >= 2000 && (S._last_year - S._first_year == 4 || S._last_year == 2010)) { S.compute(); qual.add(S); } 
    }
    Collections.sort(qual);
    HashSet<String> used = new HashSet<>();
    int ct = 0;
    System.out.println("Name\tBorn\tYear\tSpan\tAB\tHR\tSO\tRate");
    for (Span S : qual) {
      if (used.add(S._id)) {
        System.out.format("%s\t%d\t%d\t%d\t%d\t%d\t%d\t%.2f\n", S._name, S._born, S._first_year + 2, S._last_year - S._first_year + 1, S._ab, S._hr, S._so, S._hr_bip);
        if (++ct == 200) { break; }
      }
    }
  }

}
