package util;

import java.util.HashMap;
import java.util.TreeMap;
import data.Type;
import data.War;

public class Projection {
  public class Record {
    public Record() { _exp = new double[_year_wts.length]; }
    
    public double expected() {
      double tot = 0;
      for (int i = 0; i != _exp.length; ++i) { tot += _exp[i] * _year_wts[i]; }
      return tot;
    }
    public double actual() { return _act; }
    
    public void add(double val, int off) {
      if (off == -1) { _act += val; }
      else if (off < -1) { return; }
      else if (off < _exp.length) { _exp[off] += val; } 
    }
    
    public final double[] _exp;
    public double _act = 0;
  }
  
  public class Spread {
    private Spread(int slots) { _vals = new double[slots]; }
    
    /*package*/ void add(double e_pct, int a_off, double a_pct) {
      _vals[a_off] += e_pct * (1 - a_pct);
      _vals[a_off+1] += e_pct * a_pct;
      _ct += e_pct;
    }
    public final double[] _vals;
    public double _ct = 0;
  }
  
  public class Table {
    private Table(int exp_slots, int act_slots) {
      _spreads = new Spread[exp_slots];
      for (int i = 0; i != exp_slots; ++i) { _spreads[i] = new Spread(act_slots); }
    }
    
    /*package*/ void add(Record r) {
      double exp = r.expected() / _exp_amt;
      double act = r.actual() / _act_amt;
      int e_off = (int)Math.floor(exp); double e_pct = exp - e_off;
      if (e_off + 1 >= _exp_slots) { e_off = _exp_slots - 2; e_pct = 1.0; } 
      int a_off = (int)Math.floor(act); double a_pct = act - a_off;
      if (a_off + 1 >= _act_slots) { a_off = _act_slots - 2; a_pct = 1.0; } 
      
try {      
      _spreads[e_off].add(1 - e_pct, a_off, a_pct);
      _spreads[e_off+1].add(e_pct, a_off, a_pct);
} catch (Exception e) {
  System.out.format("%.2f %.2f : %d %.2f %d %.2f\n", exp, act, e_off, e_pct, a_off, a_pct);
}
    }
    
    public void print() {
      for (int i = 0; i != _act_slots; ++i) { System.out.format("\t%d", i*_act_amt); }
      System.out.print("\tCt\n");
      for (int i = 0; i != _spreads.length; ++i) {
        if (_spreads[i]._ct == 0) { continue; }
        System.out.format("%d", i * _exp_amt);
        for (int j = 0; j != _spreads[i]._vals.length; ++j) {
          System.out.format("\t%.2f", _spreads[i]._vals[j] * 100.0 / _spreads[i]._ct);
        }
        System.out.format("\t%.1f\n", _spreads[i]._ct);
      }
    }
    
    public final Spread[] _spreads;
  }
  
  private TreeMap<String, Table> _tables = new TreeMap<>();

  public void print(String key) {
    System.out.print(key);
    _tables.get(key).print();
  }
  
  public void add(String key, Record r) {
    Table t = _tables.get(key);
    if (t == null) { _tables.put(key, t = new Table(_exp_slots, _act_slots)); }
    t.add(r);
  }
  
  public Projection(double[] year_wts, int exp_amt, int exp_max, int act_amt, int act_max) {
    _year_wts = year_wts;
    double tot = 0;
    for (int i = 0; i != _year_wts.length; ++i) { tot += _year_wts[i]; }
    for (int i = 0; i != _year_wts.length; ++i) { _year_wts[i] /= tot; }
    System.out.println();
    _exp_amt = exp_amt;
    _exp_max = exp_max;
    _exp_slots = _exp_max / _exp_amt + 1;
    _act_amt = act_amt;
    _act_max = act_max;
    _act_slots = _act_max / _act_amt + 1;
  }
  
  private final double[] _year_wts;
  private final int _exp_amt;
  private final int _exp_max;
  private final int _exp_slots;
  private final int _act_amt;
  private final int _act_max;
  private final int _act_slots;
  
  
  public static void main(String[] args) throws Exception {
    War.ByID wt = new War.ByID();
    try (MyDatabase db = new MyDatabase()) { wt.addAll(new War.Table(db, Type.BAT)); }
    double[] wts = {4, 3, 2};
    Projection pj = new Projection(wts, 100, 800, 100, 700);
    for (ByPlayer<War> by : wt) {
      int i_yr = Math.max(by.first().yearID(), 1960);
      int e_yr = Math.min(by.last().yearID(), 2011);
      if (i_yr > e_yr) { continue; }
//      if (by.total().war() < 20) { continue; }
      int next_start = 0;
      for (; by.get(next_start).yearID() < i_yr - wts.length + 1; ++next_start) { /* goal is to find next_start */ }
      for (; i_yr != e_yr; ++i_yr) {
        if (i_yr >= 1980 && i_yr <= 1980 + wts.length) { continue; } // includes strike year of 1981
        if (i_yr >= 1993 && i_yr <= 1994 + wts.length) { continue; } // includes strikes years of 1994, 1995
        Record r = pj.new Record();
//        int first_yr = -1;
        for (int i = next_start; i != by.size(); ++i) {
          War w = by.get(i);
//          int y = w.yearID();
//          if (first_yr == -1) { first_yr = y; }
//          else if (first_yr != -2 && y != first_yr) { next_start = i; first_yr = -2; }
          r.add(w.playtime(), i_yr - w.yearID());
        }
        if (r.expected() == 0) { continue; }
        pj.add("tot", r);
      }
    }
    pj.print("tot");
  }
  
  public static class Player {
    public int[] _tms = new int[4];
  }
  
  public static class StatLine {
    public double[] _vals = new double[10];
    public double _ct = 0;
  }
  
  public static void main_old(/*String[] args*/) throws Exception {
    War.ByYear wt = new War.ByYear();
    try (MyDatabase db = new MyDatabase()) { wt.addAll(new War.Table(db, Type.BAT)); }
    
    StatLine[] stats = new StatLine[10];
    for (int i = 0; i != stats.length; ++i) { stats[i] = new StatLine(); }
    for (int yr = 1960; yr != 2012; ++yr) {
      HashMap<String, Player> players = new HashMap<>();
      for (int offset = 0; offset != 4; ++offset) {
        for (War w : wt.get(yr + offset - 3)) {
          Player p = players.get(w.playerID());
          if (p == null) { players.put(w.playerID(), p = new Player()); }
          p._tms[offset] += w.playtime();
        }
      }
      for (Player p : players.values()) {
        if (p._tms[2] + p._tms[3] == 0) { continue; }
        double exp = (p._tms[0] * 0.2 + p._tms[1] * 0.3 + p._tms[2] * 0.4) / 90.0;
        if (exp == 0) { continue; }
        double act = p._tms[3] / 100.0;
        int e_off = (int)Math.floor(exp);
        int a_off = (int)Math.floor(act);
        double e_perc = exp - e_off;
        double a_perc = act - a_off;
        stats[e_off]._vals[a_off] += (1 - e_perc) * (1 - a_perc);
        stats[e_off+1]._vals[a_off] += e_perc * (1 - a_perc);
        stats[e_off]._vals[a_off+1] += (1 - e_perc) * a_perc;
        stats[e_off+1]._vals[a_off+1] += e_perc * a_perc;
        stats[e_off]._ct += 1 - e_perc;
        stats[e_off+1]._ct += e_perc;
      }
    }
    System.out.print("Expected");
    for (int i = 0; i != 10; ++i) { System.out.format("\t%d", i*100); }
    System.out.print("\tCt\n");
    for (int i = 0; i != stats.length; ++i) {
      if (stats[i]._ct == 0) { continue; }
      System.out.format("%d", i * 100);
      for (int j = 0; j != stats[i]._vals.length; ++j) {
        System.out.format("\t%.2f", stats[i]._vals[j] * 100.0 / stats[i]._ct);
      }
      System.out.format("\t%.1f\n", stats[i]._ct);
    }
  }
}
