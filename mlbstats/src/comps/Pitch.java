package comps;

import java.sql.ResultSet;

class Pitch {
  public double _main_fip = 0;
  public double _comp_fip = 0;
  public double _main_er = 0;
  public double _comp_er = 0;
  public int _ipouts = 0;

  public double mainFIP() { return _main_fip / _ipouts + 3.20; }
  public double compFIP() { return _comp_fip / _ipouts + 3.20; }
  public double mainERA() { return _main_er / _ipouts * 27.0; }
  public double compERA() { return _comp_er / _ipouts * 27.0; }

  public static final int getFipCount(ResultSet RS, int col) throws Exception {
    return RS.getInt(col + 0) * 39 // HR
         + RS.getInt(col + 1) * 9  // BB
         - RS.getInt(col + 2) * 6  // SO
         - RS.getInt(col + 3) * 9  // IBB
         + RS.getInt(col + 4) * 9; // HB
  }
}