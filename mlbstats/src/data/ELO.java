package data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import util.MyDatabase;
import db.CachedStatement;

// playerID   | varchar(9)
// rating     | smallint(5) unsigned
// w          | smallint(6)
// l          | smallint(6)
// lastUpdate | varchar(20)
// isPitcher  | tinyint(1)
public class ELO {

  public String playerID()   { return _playerID; }
  public int    rating()  { return _rating; }
  public Type   type() { return _type; }
  public double norm() { return _norm; }
  
  private String _playerID = null;
  private int    _rating = 0;
  private Type   _type = null;
  private double _norm = 0;
  
  private ELO() { }
  
  public static class Table {
    private void fetch(MyDatabase db) throws SQLException {
      CachedStatement PS = db.prepare("SELECT playerID, rating, isPitcher FROM rELO");
      try (ResultSet RS = PS.executeQuery()) {
        while (RS.next()) {
          ELO R = new ELO();
          R._playerID = RS.getString(1);
          R._rating = RS.getInt(2);
          R._type = RS.getBoolean(3) ? Type.PITCH : Type.BAT;
          R._norm = R._type == Type.PITCH ? normalizePitcher(R._rating) : R._rating;
         // R._norm = R._type == Type.PITCH ? guessPitcherWAR(R._rating) : guessBatterWAR(R._rating);
          addID(R._playerID, R);
        }
      }
    }
/*    
    private static double normalizePitcher(int rating) {
      return (rating - 1492) * 1.3 + 1492;
      //double bwar = 0.000193 * rating * rating - 0.741 * rating + 755.61;
      //return 0.0377 * bwar * bwar - 3.2892 * bwar + 1936;
    }
*/
    // match so the pitchers are roughly half as many as the batters (at least for the first 100 pitchers)
    private static double normalizePitcher(int rating) {
      return 1.4343*rating - 751.14;
      //double bwar = 0.000193 * rating * rating - 0.741 * rating + 755.61;
      //return 0.0377 * bwar * bwar - 3.2892 * bwar + 1936;
    }
    
    // best fit equation, as calculated by Excel; R^2 = 0.99544
    //private static double guessPitcherWAR(int rating) {
    //  return 0.0000000042446*Math.pow(rating, 3.0503);
    //}
    
    // best fit equation, as calculated by Excel; R^2 = 0.99750
    //private static double guessBatterWAR(int rating) {
    //  return 0.000000021731*Math.pow(rating, 2.8233);
    //}
    
    private void addID(String id, ELO R) {
      if (id != null) { _byID.put(id, R); }
    }
    
    public Table(MyDatabase db) throws SQLException {
      _byID = new HashMap<>();
      fetch(db);
    }
    
    public Iterable<ELO> all() { return _byID.values(); }
    public ELO id(String ID) { return _byID.get(ID); }
    
    private Map<String, ELO> _byID = null;
  }
  
  public static void main(String[] args) throws SQLException {
    ELO.Table T = null;
    try (MyDatabase db = new MyDatabase()) { T = new ELO.Table(db); }
    ELO R = T.id("yountro01");
    System.out.format("%s : %d [%s] : %.1f\n", R.playerID(), R.rating(), R.type().code(), R.norm());
    R = T.id("maddugr01");
    System.out.format("%s : %d [%s] : %.1f\n", R.playerID(), R.rating(), R.type().code(), R.norm());
  }
}
