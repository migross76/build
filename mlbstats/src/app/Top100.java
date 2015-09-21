package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.Master;
import data.Position;
import data.Sort;
import data.Type;
import data.War;

// Print out the top players at each position
// - Top = highest career wWAR
// - Position = appearances weighted by seasonal wWAR
// Used to compare collection curves by position
// - Ultimately used to determine that catchers needed a boost to be fair
public class Top100 {
  private static WeightedApp.ByID byWA = new WeightedApp.ByID();
  private static WeightedWar.ByID byWW = new WeightedWar.ByID();
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
//    WWT.adjustByPosition(AT);
    byWW.addAll(WWT);
    WeightedApp.Tally WA = new WeightedApp.Tally(WWT, AT);
    byWA.addAll(WA);
  }
  
  public static void main(String[] args) throws Exception {
    Master.Table MT = null;
    try (MyDatabase db = new MyDatabase()) {
      Appearances.Table AT = new Appearances.Table(db);
      MT = new Master.Table(db, Sort.UNSORTED);
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }
    
    EnumMap<Position, ArrayList<WeightedWar>> byPos = new EnumMap<>(Position.class);
    for (ByPlayer<WeightedWar> WWby : byWW) {
      ByPlayer<WeightedApp> WA = byWA.get(WWby.total().playerID());
      if (WA == null || WA.total() == null) { continue; }
      WeightedApp.Use primary = WA.total().primary();
      if (primary == null) { continue; }
      ArrayList<WeightedWar> list = byPos.get(primary.pos());
      if (list == null) { byPos.put(primary.pos(), list = new ArrayList<>()); }
      list.add(WWby.total());
    }
/*
    for (Map.Entry<Position, ArrayList<WeightedWar>> E : byPos.entrySet()) {
      System.out.format("%s\tName\tWWAR\tWAR\tpWAR\tWAA\tpWAA\tRep\tpRep\tpR%%\n", E.getKey().getName());
      int i = 0;
      Collections.sort(E.getValue());
      for (WeightedWar WW : E.getValue()) {
        Master M = MT.byID(WW.playerID());
        System.out.format("%d\t%s %s\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\n", i+1, M.nameFirst(), M.nameLast(), WW.wwar(), WW.war(), WW.war_pos(), WW.waa(), WW.waa_pos(), WW.war() - WW.waa(), WW.war_pos() - WW.waa_pos(), (WW.war_pos() - WW.waa_pos()) * 100 / WW.war_pos());
        if (++i == 100) { break; }
      }
      System.out.println();
    }
*/
    System.out.print("Repl");
    for (Map.Entry<Position, ArrayList<WeightedWar>> E : byPos.entrySet()) {
      System.out.print("\t" + E.getKey().getName());
      Collections.sort(E.getValue(), new Comparator<WeightedWar>() {
        @Override public int compare(WeightedWar arg0, WeightedWar arg1) {
          return -Double.compare(arg0.war() - arg0.waa(), arg1.war() - arg1.waa());
        }
        
      });
    }
    System.out.println();
    for (int i = 0; i != 200; ++i) {
      System.out.print(i+1);
      for (Map.Entry<Position, ArrayList<WeightedWar>> E : byPos.entrySet()) {
        System.out.format("\t%.1f", E.getValue().get(i).war() - E.getValue().get(i).waa());
      }
      System.out.println();
    }
    
  }
}
