package strat.client.model;

// 2013 10 17 7 17 8 15 4 15 13 7 19 14 12 6 18 13 10 4 16 11 Camden Yards
// 2013 11 17 7 17 7 14 3 13 20 20 5 11 18 18 1 7 16 16 0 6 Fenway Park

// KEY
// Year (JJA) D/Gd D/Av N/Gd N/Av (AMSO) D/Gd D/Av N/Gd N/Av
//      (Good) S/L S/R HR/L HR/R (Average) ... (Bad) ... Park Name

public class ParkInfo {
  public static ParkInfo AVERAGE = new ParkInfo("7 s- l4", "7 s- l6", "9 r b9", "9 r b7", "9 s= b9", "9 s= b7");
  
  public ParkInfo(String sLeft, String sRight, String rLeft, String rRight, String wLeft, String wRight) {
    _sLeft = Play.parse(sLeft, null, null);
    _sRight = Play.parse(sRight, null, null);
    _rLeft = Play.parse(rLeft, null, null);
    _rRight = Play.parse(rRight, null, null);
    _wLeft = Play.parse(wLeft, null, null);
    _wRight = Play.parse(wRight, null, null);
  }
  
  public final Play _sLeft;
  public final Play _sRight;
  public final Play _rLeft;
  public final Play _rRight;
  public final Play _wLeft;
  public final Play _wRight;
}
