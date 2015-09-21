package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import util.WeightedApp.Use;
import data.ELO;
import data.HOF;
import data.Master;
import data.Position;
import data.Sort;
import data.Type;
import data.War;

/*
 * Break down the hall of fame selections (by BBWAA) by position
 * - Who to ignore? Not retired long enough; not 10 years; still on ballot; "never on ballot" (e.g., pre-1900), disqualified
 * - How to rank?
 *   - 50% objective (rWAA, GAR, JAWS, wWAR, nWAR, fWAA, Hall of Stats)
 *   - 50% subjective (ELO, Hall of Merit?, Hall of 100, Inner Circle, Hall of Fame?)
 * - Human analysis
 *   - Who are the exceptions - both in and out?  Why might they be exceptions?
 *   - How to apply this forward?  Especially special positions (C, 3B, SP, RP)
 */
public class HallAnalysis {
  
  
  private static Master.Table _mt = null;
  private static WeightedWar.ByID _wwBy = null;
  private static WeightedApp.ByID _waBy = null;
  private static HOF.Table _ht = null;
  private static ELO.Table _et = null;
//  ELO R = T.id("yountro01");
  
  private static class Player implements Comparable<Player> {
    public final Master _master;
    public final ByPlayer<WeightedWar> _career;
    public final WeightedWar _wwar;
    public final WeightedApp _app;
    public final HOF _hof;
    public final ELO _elo;

    @Override public int compareTo(Player arg0) {
      if (_wwar != arg0._wwar) { return _wwar.wwar() > arg0._wwar.wwar() ? -1 : 1; }
      return _master.playerID().compareTo(arg0._master.playerID());
    }
    
    public Player(Master M, ByPlayer<WeightedWar> WW, WeightedApp WA) {
      _master = M;
      _career = WW;
      _wwar = WW.total();
      _app = WA;
      _hof = HallAnalysis._ht.idFirst(M.hofID());
      _elo = HallAnalysis._et.id(M.playerID());
    }
    
  }
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    War.Table WT = new War.Table(db, type);
    WeightedWar.Tally WWT = new WeightedWar.Tally(WT); 
    WWT.adjustByPosition(AT);
    _wwBy.addAll(WWT);
    _waBy.addAll(new WeightedApp.Tally(WT, AT));
  }
  
  public static void main(String[] args) throws Exception {
    // table initialization
    Appearances.Table AT = null;
    _wwBy = new WeightedWar.ByID();
    _waBy = new WeightedApp.ByID();
    try (MyDatabase db = new MyDatabase()) {
      _mt = new Master.Table(db, Sort.SORTED);
      _ht = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED);
      _et = new ELO.Table(db);
      AT = new Appearances.Table(db);
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }
    
    EnumMap<Position, ArrayList<Player>> byPosition = new EnumMap<>(Position.class);
    for (ByPlayer<WeightedWar> wwBy : _wwBy) {
      Master M = _mt.byID(wwBy.total().playerID());
      ByPlayer<WeightedApp> waBy = _waBy.get(M.playerID());
      WeightedApp WA = waBy == null ? null : waBy.total();
      if (WA == null) { continue; }
      Position pos = null;
      for (Use p2 : WA) {
        if (p2.pos() == Position.DESIG) { continue; }
        if (p2.pos() == Position.MIDDLE) { pos = Position.CLOSER; }
        else { pos = p2.pos(); }
        break;
      }
      if (pos == null) { continue; }
      ArrayList<Player> list = byPosition.get(pos);
      if (list == null) { byPosition.put(pos, list = new ArrayList<>()); }
      list.add(new Player(M, wwBy, WA));
    }
    HashSet<String> ok = new HashSet<>();
    ok.add("BBWAA"); ok.add("Run Off"); ok.add("Special Election");
    for (Map.Entry<Position, ArrayList<Player>> entry : byPosition.entrySet()) {
      ArrayList<Player> list = entry.getValue();
      Collections.sort(list);
      int num = entry.getKey() == Position.STARTER ? 30 : 10;
      int ct = 0;
      for (Player p : list) {
        String hof = "";
        if (p._hof != null) { hof = ok.contains(p._hof.votedBy()) ? "*" : "-"; }
        String id = "";
        if (p._career.last().yearID() >= 1927 && p._career.last().yearID() <= 2007 &&
            _ht.onBallot(p._master.hofID()) == null) {
          id = Integer.toString(++ct);
        }
        if (ct > num && !hof.equals("*")) { continue; }
        System.out.format("%s\t%s\t%s %s\t%d\t%d\t%d\t%d\t%.1f\t%s\n",
            p._app.primary().pos().getName(), id, p._master.nameFirst(), p._master.nameLast(), p._master.yearBirth(),
            p._career.first().yearID(), p._career.last().yearID(),
            p._elo.rating(), p._wwar.wwar(), hof);
        
      }
    }
  }
}
