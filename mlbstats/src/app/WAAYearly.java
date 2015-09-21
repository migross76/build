package app;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedWar;
import data.Appearances;
import data.Position;
import data.Type;
import data.War;

/*
 * Compare WAA, WAR, pWAA, pWAR for top 200 of pWAA and pWAR
 * Also, compute how much total pWAR is replacement value (pWAR - pWAA)
 */
public class WAAYearly {
  private static WeightedWar.ByID _wwBy = null;
  
  private static Appearances.ByID byApp = new Appearances.ByID();
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
//    WWT.adjustByPosition(AT); no WAR adjustment
    _wwBy.addAll(WWT);
    byApp.addAll(AT);
  }
  
  public static class ByPosition {
    public double _pwar = 0;
    public double _pwaa = 0;
    
    public double rperc() { return (_pwar - _pwaa) / _pwar * 100.0; }
    public ByPosition() { }
  }
  
  public static void main(String[] args) throws Exception {
    // table initialization
    try (MyDatabase db = new MyDatabase()) {
      Appearances.Table AT = new Appearances.Table(db);
      _wwBy = new WeightedWar.ByID();
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }

    TreeMap<Integer, EnumMap<Position, ByPosition>> map = new TreeMap<>();
    
    for (ByPlayer<WeightedWar> wwBy : _wwBy) {
      ByPlayer<Appearances> appBy = byApp.get(wwBy.total().playerID());
      if (appBy == null) { continue; } // no appearances for this player
      for (int i = 0; i != wwBy.size(); ++i) {
        WeightedWar ww = wwBy.get(i);
        if (ww.war() > 0) {
          EnumMap<Position, ByPosition> poses = map.get(ww.yearID());
          if (poses == null) {
            map.put(wwBy.get(i).yearID(), poses = new EnumMap<>(Position.class));
          }
          Appearances app = null;
          for (Appearances a : appBy) {
            if (a.teamID().equals(ww.teamID())) { app = a; break; }
          }
          if (app == null) { continue; } // no position for this player's team/season
          double totalG = 0;
          for (Appearances.Use use : app) { totalG += use.games(); }
          for (Appearances.Use use : app) {
            ByPosition bp = poses.get(use.pos());
            if (bp == null) { poses.put(use.pos(), bp = new ByPosition()); }
            bp._pwar += use.games() / totalG * ww.war();
            if (ww.waa() > 0) { bp._pwaa += use.games() / totalG * ww.waa(); }
          }
        }
      }
    }
    System.out.print("Year");
    for (Position p : Position.values()) { System.out.format("\t%s\t\t", p.name()); }
    System.out.println();
    for (Map.Entry<Integer, EnumMap<Position, ByPosition>> entry : map.entrySet()) {
      System.out.print(entry.getKey());
      for (Position p : Position.values()) {
        ByPosition bp = entry.getValue().get(p);
        if (bp == null) { System.out.print("\tN/A\tN/A\tN/A"); }
        else {
          System.out.format("\t%.1f\t%.1f\t%.1f", bp.rperc(), bp._pwaa, bp._pwar);
        }
      }
      System.out.println();
    }
  }
}
