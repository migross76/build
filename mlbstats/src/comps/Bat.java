package comps;

import java.sql.ResultSet;

class Bat {
  public Bat add(Bat B) {
    _main_woba += B._main_woba;
    _comp_woba += B._comp_woba;
    _pa += B._pa;
    return this;
  }
  
  public double _main_woba = 0;
  public double _comp_woba = 0;
  public int _pa = 0;

  public double mainwOBA() { return _main_woba / _pa; }
  public double compwOBA() { return _comp_woba / _pa; }

  public static final String SQL = " sum(ab), sum(h), sum(2b), sum(3b), sum(hr), sum(bb), sum(ibb), sum(hbp), sum(sh), sum(sf)";
  
  public static final double getWobaCount(ResultSet RS, int col) throws Exception {
    // AB, H, 2B, 3B, HR, BB, IBB, HBP, SH, SF
    return RS.getInt(col + 1) * 0.90 // H
         + RS.getInt(col + 2) * 0.34 // 2B
         + RS.getInt(col + 3) * 0.66 // 3B
         + RS.getInt(col + 4) * 1.05 // HR
         + RS.getInt(col + 5) * 0.72 // BB
         - RS.getInt(col + 6) * 0.72 // IBB
         + RS.getInt(col + 7) * 0.75;// HBP
  }
  
  public static final int getPA(ResultSet RS, int col) throws Exception {
    // AB, H, 2B, 3B, HR, BB, IBB, HBP, SH, SF
    return RS.getInt(col + 0) // AB
         + RS.getInt(col + 5) // BB
         + RS.getInt(col + 7) // HBP
         + RS.getInt(col + 8) // SH
         + RS.getInt(col + 9);// SF
  }
  
  public static final String COLS = "\t\t\t\t";
  public static final String HEADER = "\tPA\tComp\tMain\t%";
  
  @Override public String toString() {
    if (_pa == 0) {
      return COLS;
    } else if (_comp_woba == 0) {
      return String.format("\t%d\t\t%.3f\t", _pa, mainwOBA());
    } else {
      return String.format("\t%d\t%.3f\t%.3f\t%.1f", _pa, compwOBA(), mainwOBA(), _main_woba / _comp_woba * 100.0);
    }
  }
}