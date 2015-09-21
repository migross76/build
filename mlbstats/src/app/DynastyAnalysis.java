package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

public class DynastyAnalysis {
  private static final String CACHE = "C:/build/mlbstats/cache";
  private static final int YEAR_START = 2010;
  private static final int YEARS = 3;
  private static final int YEAR_END = YEAR_START + YEARS - 1;
  private static final boolean DYNASTY = YEAR_END == 2012;
  private static final int PRINT_MIN = 400;

  private static Master.Table _mt = null;
  private static WeightedWar.ByID _wwBy = null;
  private static WeightedApp.ByID _waBy = null;
  
  private static class Prospect {
    public String _name = null;
    public int _order = 0;
    public String _pos = null;
  }
  
  private static class Dynasty {
    public String _name = null;
    public int _order = 0;
    public int _age = 0;
    //public String _owner = null;
    public String _pos = null;
  }
  
  private static class Player implements Comparable<Player> {
    public Master _master = null;
    public ByPlayer<WeightedWar> _wwar = null;
    public ByPlayer<WeightedApp> _wapp = null;
    public Dynasty _dyn = null;
    public Prospect[] _prospect = new Prospect[YEARS];
    
    public double score() { return _score_waa + _score_age + _score_prosp; }
    
    public double _score_waa = 0;
    public double _score_age = 0;
    public double _score_prosp = 0;
    
    public String name() {
      if (_master != null) { return _master.nameFirst() + " " + _master.nameLast(); }
      if (_dyn != null) { return _dyn._name; }
      for (Prospect p : _prospect) {
        if (p != null) { return p._name; }
      }
      return "[empty]";
    }
    
    public int age() {
      if (_master != null) { return _master.age(YEAR_END); }
      if (_dyn != null) { return _dyn._age; }
      return -1;
    }
    
    public String pos() {
      if (_wapp != null) {
        WeightedApp wa_total = new WeightedApp(_wapp.total().playerID(), _wapp.total().yearID());
        for (WeightedApp wa : _wapp) {
          if (wa.yearID() >= YEAR_START && wa.yearID() <= YEAR_END) {
            wa_total.add(wa);
          }
        }
        WeightedApp.Use primary = wa_total.primary();
        if (primary != null) { return primary.pos().getName(); }
      }
      if (_dyn != null) { return _dyn._pos; }
      for (Prospect p : _prospect) {
        if (p != null) { return p._pos; }
      }
      return "??";
    }
    
    private static final double NO_WAA = -100;
    public String waa(int yr) {
      double waa = NO_WAA;
      if (_wwar != null) {
        for (WeightedWar ww : _wwar) {
          if (ww.yearID() == YEAR_START + yr) { if (waa == NO_WAA) { waa = ww.waa(); } else { waa += ww.waa(); } }
        }
      }
      return waa == NO_WAA ? "" : String.format("%.1f", waa);
    }
    
    public String nextWar() {
      boolean found = false;
      double war = 0;
      if (_wwar != null) {
        for (WeightedWar ww : _wwar) {
          if (ww.yearID() == YEAR_END + 1) { war += ww.war(); found = true; }
        }
      }
      return found ? String.format("%.1f", war) : "";
    }
    
    public int draft() {
      return _dyn != null ? _dyn._order : 500;
    }
    
    public String draftStr() {
      return _dyn != null ? Integer.toString(_dyn._order) : "";
    }

    public String prospect(int yr) {
      if (_prospect[yr] != null) { return String.format("%d", _prospect[yr]._order); }
      return "";
    }
    
    public void calc() {
      if (_wwar != null) {
        for (WeightedWar ww : _wwar) {
          if (ww.yearID() >= YEAR_START && ww.yearID() <= YEAR_END) {
            _score_waa += ww.waa() * (ww.yearID() - YEAR_START + 2);
          }
        }
      }
      for (int i = _prospect.length - 1; i != -1; --i) {
        if (_prospect[i] != null) {
          _score_prosp += 30 / /*(4-i)*/2 * Math.pow(_prospect[i]._order, -.50);
          break;
        }
      }
      int age = age();
      if (age != -1) { _score_age += (27 - age); }
    }

    @Override public int compareTo(Player p) {
      double cmp = score() - p.score();
      if (cmp != 0) { return cmp > 0 ? -1 : 1; }
      return name().compareTo(p.name());
    }
  }

  private static class PlayerMap extends HashMap<String, Player> {
    private static final long serialVersionUID = 1L;
    public void addMapping(String from, String to) {
      _map.put(from.toLowerCase(), to.toLowerCase());
    }
    
    @Override public Player get(Object key) {
      String key2 = _map.get(key);
      return super.get(key2 == null ? key : key2);
    }

    @Override public Player put(String key, Player val) {
      String key2 = _map.get(key);
      return super.put(key2 == null ? key : key2, val);
    }
    private final HashMap<String, String> _map = new HashMap<>();
  }
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
    WWT.adjustByPosition(AT);
    _wwBy.addAll(WWT);
    _waBy.addAll(new WeightedApp.Tally(WWT, AT));
  }
  
  public static void main(String[] args) throws Exception {
    // table initialization
//    System.out.println("Loading...");
    _wwBy = new WeightedWar.ByID();
    _waBy = new WeightedApp.ByID();
    try (MyDatabase db = new MyDatabase()) {
      _mt = new Master.Table(db, Sort.SORTED);
      Appearances.Table AT = new Appearances.Table(db);
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }

    ArrayList<Player> players = new ArrayList<>();
    PlayerMap byName = new PlayerMap();
    
    byName.addMapping("Mike Stanton", "Giancarlo Stanton");
    byName.addMapping("CC Sabathia", "C.C. Sabathia");
    byName.addMapping("Danny Haren", "Dan Haren");
    byName.addMapping("Alexis Rios", "Alex Rios");
    byName.addMapping("Kendry Morales", "Kendrys Morales");
    byName.addMapping("Howie Kendrick", "Howard Kendrick");
    byName.addMapping("Philip Hughes", "Phil Hughes");
    byName.addMapping("Mike Morse", "Michael Morse");
    byName.addMapping("Michael Fiers", "Mike Fiers");
    byName.addMapping("Tom Milone", "Tommy Milone");
    byName.addMapping("A. J. Griffin", "A.J. Griffin");
    byName.addMapping("Manuel Banuelos", "Manny Banuelos");
    
    for (ByPlayer<WeightedWar> wwar : _wwBy) {
      if (wwar.last().yearID() >= YEAR_START && wwar.first().yearID() <= YEAR_END + 1) {
        Player p = new Player();
        p._wwar = wwar;
        p._master = _mt.byID(wwar.total().playerID());
        p._wapp = _waBy.get(wwar.total().playerID());
        players.add(p);
        byName.put((p._master.nameFirst() + " " + p._master.nameLast()).toLowerCase(), p);
      }
    }
//    System.out.println("Reading...");
    String line = null;
    if (DYNASTY) {
      try (BufferedReader br = new BufferedReader(new FileReader(new File(CACHE, "dynasty2013.tsv")))) {
        br.readLine(); // skip the header
        while ((line = br.readLine()) != null) {
          String[] cols = line.split("\t");
          Dynasty dyn = new Dynasty();
          dyn._order = Integer.parseInt(cols[1]);
          //dyn._owner = cols[2];
          dyn._name = cols[3];
          dyn._pos = cols[4];
          dyn._age = Integer.parseInt(cols[6]);
          Player p = byName.get(dyn._name.toLowerCase());
          if (p == null) {
            players.add(p = new Player());
            byName.put(dyn._name.toLowerCase(), p);
          }
          p._dyn = dyn;
        }
      }
    }
    
    try (BufferedReader br = new BufferedReader(new FileReader(new File(CACHE, "prospects.tsv")))) {
      int pos = -1;
      while ((line = br.readLine()) != null) {
        String[] cols = line.split("\t");
        if (cols.length == 1) { pos = Integer.parseInt(cols[0]) - YEAR_START - 1; continue; }
        
        if (pos > YEAR_END - YEAR_START) { continue; }
        if (pos < 0) { break; }
  
        Prospect pros = new Prospect();
        pros._order = Integer.parseInt(cols[0]);
        pros._name = cols[1];
        pros._pos = cols[2];
        Player p = byName.get(pros._name.toLowerCase());
        if (p == null) {
          players.add(p = new Player());
          byName.put(pros._name.toLowerCase(), p);
        }
        p._prospect[pos] = pros;
      }
    }
    
    
//    System.out.println("Processing...");
    
    for (Player p : players) { p.calc(); }
    Collections.sort(players);
    
    int ct = 0;
    System.out.println("#\tDraft\tName\tAge\tPos\tW1\tP1\tW2\tP2\tW3\tP3\tsWAA\tsPros\tsAge\tScore\tNext\tDiff\tAbs");
    for (Player p : players) {
      if ((p._wwar == null || p._wwar.first().yearID() > YEAR_END) && p._score_prosp == 0 && p._dyn == null) { continue; } // only eligible players
      if (++ct > PRINT_MIN && p._dyn == null) { continue; }
      double diff = (Math.log(p.draft()) - Math.log(ct))/Math.log(2);
      System.out.format("%d\t%s\t%s\t%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%.2f\t%.2f\t%.2f\t%.2f\t%s\t%.1f\t%.5f\n", ct, p.draftStr(), p.name(), p.age(), p.pos(), p.waa(0), p.prospect(0), p.waa(1), p.prospect(1), p.waa(2), p.prospect(2), p._score_waa, p._score_prosp, p._score_age, p.score(), p.nextWar(), diff, Math.abs(diff));
    }
    
  }
}
