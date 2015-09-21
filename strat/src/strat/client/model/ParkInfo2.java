package strat.client.model;

//2013 11 17 7 17 7 14 3 13 20 20 5 11 18 18 1 7 16 16 0 6 Fenway Park

//KEY
//Year (JJA) D/Gd D/Av N/Gd N/Av (AMSO) D/Gd D/Av N/Gd N/Av
//   (Good) S/L S/R HR/L HR/R (Average) ... (Bad) ... Park Name
public class ParkInfo2 {
  public static class Splits {
    public int _sLeft;
    public int _sRight;
    public int _rLeft;
    public int _rRight;
  }
  
  public String _name;
  public int _year;
  public int _good_range;
  public int _avg_range;
  
  public Splits _good;
  public Splits _avg;
  public Splits _bad;
}
