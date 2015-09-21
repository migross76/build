package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import data.Appearances;
import data.HOF;
import data.HOF.Selection;
import data.Master;
import data.Position;
import data.Sort;
import data.Type;
import data.War;

public class Cluster {
  public static final int MAX_SEASONS = 35;
  public static final double CUTOFF = 10;
  
  public static class Player implements Comparable<Player> {
    public Master _master;
    public Position _pos;
    public double[] _seasons = new double[MAX_SEASONS];

    @Override public int compareTo(Player arg0) {
      return _master.playerID().compareTo(arg0._master.playerID());
    }
  }
  
  public static class Group implements Comparable<Group> {
    public ArrayList<Player> _players = new ArrayList<>();
    
    public void print() {
      boolean slash = false;
      for (Player p : _players) {
        if (slash) { System.out.print('/'); } else { slash = true; }
        System.out.print(p._master.playerID());
      }
    }

    @Override public int compareTo(Group arg0) {
      int cmp = Integer.compare(_players.size(), arg0._players.size());
      if (cmp != 0) { return cmp; }
      return _players.get(0)._master.playerID().compareTo(arg0._players.get(0)._master.playerID());
    }
  }
  
  public static class Pair implements Comparable<Pair> {
    public Group _one;
    public Group _two;
    public double _diff;
    
    public void print() {
      _one.print();
      System.out.print('\t');
      _two.print();
      System.out.format("\t%.1f\n", _diff);
    }

    @Override public int compareTo(Pair arg1) {
      int cmp = Double.compare(_diff, arg1._diff);
      if (cmp != 0) { return cmp; }
      return _one._players.get(0)._master.playerID().compareTo(_two._players.get(0)._master.playerID());
    }
  }
  
  private double execute() {
    Pair chosen = _pairs.remove(0);
    chosen.print();
    // clean up existing references
    _groups.remove(chosen._one);
    _groups.remove(chosen._two);
    for (Iterator<Pair> i_pair = _pairs.iterator(); i_pair.hasNext(); ) {
      Pair p = i_pair.next();
      if (p._one == chosen._one || p._one == chosen._two || p._two == chosen._one || p._two == chosen._two) { i_pair.remove(); }
    }
    // update with new group, pairs
    Group newg = chosen._one;
    newg._players.addAll(chosen._two._players);
    Collections.sort(newg._players);
    for (Group g : _groups) {
      createPair(g, newg);
    }
    Collections.sort(_pairs);
    _groups.add(newg);
    
    return chosen._diff;
  }
  
  public void print() {
    Collections.sort(_groups);
    for (Group g : _groups) { g.print(); System.out.println(); }
  }
  
  private WeightedApp.ByID _byWA = new WeightedApp.ByID();
  private War.ByID _byWar = new War.ByID();
  private Master.Table _mt = null;
  private HOF.Table _ht = null;
  
  private ArrayList<Group> _groups = new ArrayList<>();
  private ArrayList<Pair> _pairs = new ArrayList<>();
  
  private void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    War.Table WT = new War.Table(db, type);
    _byWar.addAll(WT);
    _byWA.addAll(new WeightedApp.Tally(WT, AT));
  }
  
  private void createGroup(String id) {
    if (id.endsWith("99")) { return; }
    Player p = new Player();
    p._master = _mt.byID(id);
    if (p._master == null) { System.err.println("Couldn't find master for " + id); return; }
    ByPlayer<WeightedApp> wa = _byWA.get(p._master.playerID());
    if (wa == null) { System.err.println("Couldn't find weighted app for " + p._master.playerID()); return; }
    p._pos = wa.total().primary().pos();
    int i = 0;
    for (War w : _byWar.get(p._master.playerID())) {
      if (w.waa() <= 0) { continue; }
      p._seasons[i++] = w.waa();
    }
    Arrays.sort(p._seasons);
    Group g = new Group();
    g._players.add(p);
    _groups.add(g);
  }
  
  private void createPair(Group one, Group two) {
    Pair pair = new Pair();
    pair._one = one;
    pair._two = two;
    for (Player p1 : pair._one._players) {
      for (Player p2 : pair._two._players) {
        for (int k = 0; k != MAX_SEASONS; ++k) {
          pair._diff += Math.abs(p1._seasons[k] - p2._seasons[k]);
        }
      }
    }
    pair._diff /= pair._one._players.size() * pair._two._players.size();
    _pairs.add(pair);
  }

  public Cluster() throws SQLException {
    try (MyDatabase db = new MyDatabase()) {
      Appearances.Table at = new Appearances.Table(db);
      _mt = new Master.Table(db, Sort.UNSORTED);
      _ht = new HOF.Table(db, Sort.UNSORTED, Selection.ELECTED);
      assemble(db, at, Type.BAT);
      assemble(db, at, Type.PITCH);
    }
/*    
    for (HOF hof : _ht.year(2014)) {
      createGroup(hof.hofID());
    }
*/    
    for (String id : _ht.byID().keySet()) {
      createGroup(id);
    }
    
    for (int i = 0; i != _groups.size() - 1; ++i) {
      Group g1 = _groups.get(i);
      for (int j = i + 1; j != _groups.size(); ++j) {
        createPair(g1, _groups.get(j));
      }
    }
    Collections.sort(_pairs);
  }
  
  public static void main(String[] args) throws SQLException {
    Cluster main = new Cluster();
    while (main.execute() < CUTOFF) { /* no-op */ }
    System.out.println();
    main.print();
  }
}
