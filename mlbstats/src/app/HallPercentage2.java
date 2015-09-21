package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.HOF;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

public class HallPercentage2 {
  
  private static Master.Table _master = null;
  private static HOF.Table _hof = null;
  private static WeightedWar.ByID _wwBy = null;
  private static WeightedApp.ByID _waBy = null;
  
  private static class Player {
    public int year() { return _pMaster.yearBirth(); }
    public String primary() { return _pWA == null ? "??" : _pWA.primary().pos().getName(); }
    public boolean hof() { return _pHOF != null || _pMaster.playerID().equals("larkiba01") || _pMaster.playerID().equals("santoro01"); }

    public Player(Master m) {
      _pMaster = m;
      //_pWW = _wwBy.get(m.playerID());
      _pWA = _waBy.get(m.playerID()).total();
      _pHOF = _hof.idFirst(m.hofID());
    }
    
    private final Master _pMaster;
    //private final ByPlayer<WeightedWar> _pWW;
    private final WeightedApp _pWA;
    private final HOF _pHOF;
  }
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
    WWT.adjustByPosition(AT);
    _wwBy.addAll(WWT);
    _waBy.addAll(new WeightedApp.Tally(WWT, AT));
  }
  
  private static void print(String name, Collection<Player> list) {
    int hof = 0;
    for (Player p : list) { if (p.hof()) { ++hof; } }
    System.out.format("%s\t%d\t%d\t%.1f%%\n", name, hof, list.size(), hof * 100.0 / list.size());
  }
  
  public static void main(String[] args) throws Exception {
    // table initialization
    System.out.println("Loading...");
    _wwBy = new WeightedWar.ByID();
    _waBy = new WeightedApp.ByID();
    try (MyDatabase db = new MyDatabase()) {
      _master = new Master.Table(db, Sort.SORTED);
      _hof = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED);
      Appearances.Table AT = new Appearances.Table(db);
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }

    System.out.println("Processing...");
    ArrayList<Player> total = new ArrayList<>();
    TreeMap<Integer, ArrayList<Player>> byYear = new TreeMap<>();
    TreeMap<String, ArrayList<Player>> byPos = new TreeMap<>();
    for (Master m : _master.all()) {
      ByPlayer<WeightedWar> wwBy = _wwBy.get(m.playerID());
      if (wwBy == null) { continue; }
      int yrs = 0, lastYear = 0;
      for (WeightedWar WW : wwBy) {
        if (WW.yearID() != lastYear) { ++yrs; lastYear = WW.yearID(); }
      }
      if (yrs < 10) { continue; }
      Player p = new Player(m);
      total.add(p);
      ArrayList<Player> list = byYear.get(p.year());
      if (list == null) { byYear.put(p.year(), list = new ArrayList<>()); }
      list.add(p);
      list = byPos.get(p.primary());
      if (list == null) { byPos.put(p.primary(), list = new ArrayList<>()); }
      list.add(p);
    }
    print("Total", total);
    ArrayList<Player> year5 = new ArrayList<>();
    for (Map.Entry<Integer, ArrayList<Player>> e : byYear.entrySet()) {
      year5.addAll(e.getValue());
//      print(e.getKey().toString(), e.getValue());
      if (e.getKey() % 1 == 0) { print(e.getKey() + "", year5); year5.clear(); } 
    }
    for (Map.Entry<String, ArrayList<Player>> e : byPos.entrySet()) {
      print(e.getKey(), e.getValue());
    }
  }

}
