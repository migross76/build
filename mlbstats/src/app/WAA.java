package app;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

/*
 * Compare WAA, WAR, pWAA, pWAR for top 200 of pWAA and pWAR
 * Also, compute how much total pWAR is replacement value (pWAR - pWAA)
 */
public class WAA {
  private static Master.Table _master = null;
  private static WeightedWar.ByID _wwBy = null;
  
  private static WeightedApp.ByID byWA = new WeightedApp.ByID();
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
//    WWT.adjustByPosition(AT); no WAR adjustment
    _wwBy.addAll(WWT);
    WeightedApp.Tally WA = new WeightedApp.Tally(WWT, AT);
    byWA.addAll(WA);
  }
  
  public static class Player {
    public int _waa_rank = -1;
    public int _war_rank = -1;
    public double _pwar = 0;
    public double _rperc = 0;
    WeightedWar _ww = null;
    WeightedApp _wa = null;
    public Player(ByPlayer<WeightedWar> ww, WeightedApp wa) {
      _ww = ww.total();
      _wa = wa;
      for (WeightedWar w : ww) {
        if (w.war() > 0) { _pwar += w.war(); }
      }
      _rperc = (_pwar - _ww.waa_pos()) / _pwar * 100.0;
    }
    
    public static Comparator<Player> bywar = new Comparator<Player>() {
      @Override public int compare(Player o1, Player o2) {
        double cmp = o1._pwar - o2._pwar;
        if (cmp == 0) { return 0; }
        return cmp > 0 ? -1 : 1;
      }
    };

    public static Comparator<Player> bywaa = new Comparator<Player>() {
      @Override public int compare(Player o1, Player o2) {
        double cmp = o1._ww.waa_pos() - o2._ww.waa_pos();
        if (cmp == 0) { return 0; }
        return cmp > 0 ? -1 : 1;
      }
    };

    public static Comparator<Player> byperc = new Comparator<Player>() {
      @Override public int compare(Player o1, Player o2) {
        double cmp = o1._rperc - o2._rperc;
        if (cmp == 0) { return 0; }
        return cmp > 0 ? -1 : 1;
      }
    };
  }
  
  public static void main(String[] args) throws Exception {
    // table initialization
    try (MyDatabase db = new MyDatabase()) {
      _master = new Master.Table(db, Sort.SORTED);
      Appearances.Table AT = new Appearances.Table(db);
      _wwBy = new WeightedWar.ByID();
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }

    ArrayList<Player> byWW = new ArrayList<>();
    HashSet<Player> publish = new HashSet<>();
    
    for (ByPlayer<WeightedWar> ww : _wwBy) {
      ByPlayer<WeightedApp> waBy = byWA.get(ww.total().playerID());
      byWW.add(new Player(ww, waBy == null ? null : waBy.total()));
    }
    Collections.sort(byWW, Player.bywar);
    for (int i = 0; i != 800; ++i) {
      byWW.get(i)._war_rank = i+1;
      if (i < 200) { publish.add(byWW.get(i)); }
    }
    Collections.sort(byWW, Player.bywaa);
    for (int i = 0; i != 800; ++i) {
      byWW.get(i)._waa_rank = i+1;
      if (i < 200) { publish.add(byWW.get(i)); }
    }
    byWW.clear();
    byWW.addAll(publish);
    Collections.sort(byWW, Player.byperc);

    System.out.println("R#\tA#\tName\tPos\tYOB\tWAR\tpWAR\tWAA\tpWAA\tpR%");
    for (Player p : byWW) {
      Master m = _master.byID(p._ww.playerID());
      System.out.format("%d\t%d\t%s %s\t%s\t%d\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\n", p._war_rank, p._waa_rank, m.nameFirst(), m.nameLast(), p._wa.primary().pos().getName(), m.yearBirth(), p._ww.war(), p._pwar, p._ww.waa(), p._ww.waa_pos(), p._rperc);
    }
  }
}
