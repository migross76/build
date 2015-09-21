package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.HOF;
import data.Master;
import data.War;
import data.Sort;
import data.Type;

// Select 1000 candidates for the best players ever.
// - All hall-of-famers, plus the top career wWAR to fill out the rest
// This was a preliminary try to see what would come out for a social-media determined HOF/Top 100
// Inspired by: http://joeposnanski.si.com/2012/02/03/a-chain-experiment/
// And: http://www.insidethebook.com/ee/index.php/site/comments/best_player_not_on_chain_game/
public class Top1000 {
  public static int TOTAL_PLAYERS = 1000;
  
  public static class Player {
    public Player(Master M) { _master = M; }
    
    public Master _master = null;
    public WeightedWar _ww = null;
    public WeightedApp _wa = null;
  }
  private static WeightedApp.ByID byWA = new WeightedApp.ByID();
  private static WeightedWar.ByID byWW = new WeightedWar.ByID();
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
    WWT.adjustByPosition(AT);
    byWW.addAll(WWT);
    WeightedApp.Tally WA = new WeightedApp.Tally(WWT, AT);
    byWA.addAll(WA);
  }
  
  public static void main(String[] args) throws Exception {
    HOF.Table hof = null;
    Master.Table MT = null;
    try (MyDatabase db = new MyDatabase()) {
      hof = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED);
      Appearances.Table AT = new Appearances.Table(db);
      MT = new Master.Table(db, Sort.UNSORTED);
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }
    
    HashMap<Master, Player> players = new HashMap<>();
    for (List<HOF> H : hof.byID().values()) {
      Master M = MT.byID(H.get(0).hofID());
      Player P = new Player(M);
      ByPlayer<WeightedWar> wwBy = byWW.get(M.playerID());
      if (wwBy != null) { P._ww = wwBy.total(); }
      ByPlayer<WeightedApp> waBy = byWA.get(M.playerID());
      if (waBy != null) { P._wa = waBy.total(); }
      players.put(M, P);
    }
    ArrayList<WeightedWar> list = new ArrayList<>();
    for (ByPlayer<WeightedWar> WWby : byWW) {
      list.add(WWby.total());
    }
    Collections.sort(list);

    for (WeightedWar WW : list) {
      Master M = MT.byID(WW.playerID());
      if (players.containsKey(M)) { continue; }
      Player P = new Player(M);
      P._ww = WW;
      P._wa = byWA.get(M.playerID()).total();
      players.put(M, P);
      if (players.size() == TOTAL_PLAYERS) { break; }
    }

    ArrayList<Player> byDOB = new ArrayList<>(players.values());
    Collections.sort(byDOB, new Comparator<Player>() {
      @Override
      public int compare(Player arg0, Player arg1) {
        String b0 = arg0._master.birthday(), b1 = arg1._master.birthday();
        int cmp = b0.compareTo(b1);
        if (cmp != 0) { return cmp; }
        return arg0._master.playerID().compareTo(arg1._master.playerID());
      }
    });
    for (Player P : byDOB) {
      WeightedApp.Use pos = null;
      if (P._wa != null && P._wa.primary() != null) { pos = P._wa.primary(); }
      System.out.format("%s\t%s %s\t%s\t%.1f\t%.1f\n", pos == null ? "??" : pos.pos().getName(), P._master.nameFirst(), P._master.nameLast(), P._master.birthday(), P._ww == null ? 0 : P._ww.war(), P._ww == null ? 0 : P._ww.wwar());
    }
  }
}
