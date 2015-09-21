package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.Master;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;

// Come up with the best players by WeightedWAR, organized by primary franchise and position
// Ideally:
// TODO Try to balance the franchise split, by choosing the player based on franchise(+position) rank.
//   - Example: Kevin Brown is 8th best LAD SP, but the best FLA SP (so choose FLA)
// TODO Be able to swap out wWAR for WAR, H(bat), HR(bat), SO(pitch), etc.
// TODO Sort by adjusted wWAR (2*franchise wWAR, capped at career wWAR)
// TODO Use current franchises if at all possible (so Cy Young ends up in BOS)
public class Organize {
  private WeightedApp.ByID byWA = new WeightedApp.ByID();
  private WeightedWar.ByIDTeam byWW = new WeightedWar.ByIDTeam();

  private Master.Table MT = null;
  private Teams.Table  TT = null;
  
  private static final int TOP_NUM = 500;
  private static final int LAST_YEAR = 2011;
  private static final int DEBUNK_FRANCHISE_PENALTY = -100;
  
  public void execute() {
    HashSet<String> activeFranch = new HashSet<>();
    for (Teams T : TT.year(LAST_YEAR)) {
      activeFranch.add(T.franchID());
    }
    
    ArrayList<WeightedWar> list = new ArrayList<>();
    for (ByPlayer<WeightedWar> WWby : byWW) {
      list.add(WWby.total());
    }
    Collections.sort(list);
    for (int i = 0; i != TOP_NUM; ++i) {
      WeightedWar WW = list.get(i);
      Master M = MT.byID(WW.playerID());
      WeightedApp WA = byWA.get(M.playerID()).total();
      ByPlayer<WeightedWar> WWby = byWW.get(WW.playerID());
      
      HashMap<String, Double> byFranch = new HashMap<>();
      for (WeightedWar byTeam : WWby) {
        String franchID = TT.getFranchiseID(byTeam.teamID());
        Double D = byFranch.get(franchID);
        byFranch.put(franchID, byTeam.wwar() + (D == null ? (activeFranch.contains(franchID) ? 0 : DEBUNK_FRANCHISE_PENALTY) : D));
      }
      String bestFranch = null;
      double bestWWAR = -1000;
      for (Map.Entry<String, Double> E : byFranch.entrySet()) {
        if (E.getValue() > bestWWAR) {
          bestWWAR = E.getValue(); bestFranch = E.getKey();
        }
      }
      if (!activeFranch.contains(bestFranch)) { bestWWAR -= DEBUNK_FRANCHISE_PENALTY; } // adjust back, so it only affects the choice
      double adjustWWAR = bestWWAR * 2;
      if (adjustWWAR > WW.wwar()) { adjustWWAR = WW.wwar(); }
      
      System.out.format("%s\t%s\t%s %s\t%.1f\t%.1f\t%s\t%.1f\t%.1f\n", WA.primary().pos().getShort(), WA.primary().pos().getName(), M.nameFirst(), M.nameLast(), WW.war(), WW.wwar(), bestFranch, bestWWAR, adjustWWAR);
    }
  }
  
  private void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    War.Table WT = new War.Table(db, type);
    WeightedWar.Tally WWT = new WeightedWar.Tally(WT); 
    WWT.adjustByPosition(AT);
    byWW.addAll(WWT);
    WeightedApp.Tally WA = new WeightedApp.Tally(WT, AT);
    byWA.addAll(WA);
  }
  
  public Organize() throws SQLException {
    try (MyDatabase db = new MyDatabase()) {
      Appearances.Table AT = new Appearances.Table(db);
      MT = new Master.Table(db, Sort.UNSORTED);
      TT = new Teams.Table(db);
      assemble(db, AT, Type.BAT);
      // assemble(db, AT, Type.PITCH);
    }
  }

  public static void main(String[] args) throws SQLException {
    new Organize().execute();
  }

}
