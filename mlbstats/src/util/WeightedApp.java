package util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import data.Appearances;
import data.Groupable;
import data.Position;
import data.Type;
import data.War;

// Weight appearances by a value, such as WAR or wWAR
// - This helps offset a player hanging on at an easier position just above replacement value
// Adjustments of Hall of Famers:
// DH  1B  Frank Thomas
// 1B  2B  Rod Carew
// 1B  3B  Dick Allen
// 1B  SS  Ernie Banks
// 1B  LF  Pete Rose
// 1B  RF  Stan Musial
// LF  RF  Joe Jackson
// RF  LF  Babe Ruth
// CL  SP  Dennis Eckersley
public class WeightedApp implements Iterable<WeightedApp.Use>, Groupable<WeightedApp> {
  public static Comparator<WeightedApp> groupYear = new Comparator<WeightedApp>() {
    @Override public int compare(WeightedApp o1, WeightedApp o2) { return o2.yearID() - o1.yearID(); }
  };
  public static Comparator<WeightedApp> groupId = new Comparator<WeightedApp>() {
    @Override public int compare(WeightedApp o1, WeightedApp o2) { return o1.playerID().compareTo(o2.playerID()); }
  };
  private static ByPlayer.GroupKey<String, WeightedApp> BY_ID = new ByPlayer.GroupKey<String, WeightedApp>() {
    @Override public String groupBy(WeightedApp V) { return V.playerID(); }
  };
  private static ByPlayer.GroupKey<Integer, WeightedApp> BY_YEAR = new ByPlayer.GroupKey<Integer, WeightedApp>() {
    @Override public Integer groupBy(WeightedApp V) { return V.yearID(); }
  };
  public static class ByID extends ByPlayer.Group<String, WeightedApp> {
    public ByID() { super(BY_ID, null, groupYear); }
  }
  public static class ByYear extends ByPlayer.Group<Integer, WeightedApp> {
    public ByYear() { super(BY_YEAR, null, groupId); }
  }

  public String   playerID() { return _playerID; }
  public int      yearID()   { return _yearID; }
  
  public double   games(Position pos) {
    for (Use P : _use) { if (P._pos == pos) { return P._g; } }
    return 0;
  }
  public Use primary() { return _use.isEmpty() ? null : _use.get(0); }
  @Override public Iterator<Use> iterator() { return _use.iterator(); }
  
  
  public void add(Position pos, double g) {
    if (g == 0) { return; }
    for (Use P : _use) { if (P._pos == pos) { P._g += g; sort(); return; } }
    _use.add(new Use(pos, g));
    sort();
  }
  public void sort() { Collections.sort(_use); }

  @Override public void add(WeightedApp WA) {
    for (Use P : WA) { add(P.pos(), P.games()); }
  }
  @Override public WeightedApp create() { return new WeightedApp(_playerID, _yearID); }

  public WeightedApp(String playerID, int yearID) {
    _playerID = playerID;
    _yearID = yearID;
  }

  private String   _playerID = null;
  private int      _yearID = 0;
  private List<Use> _use = new ArrayList<>();
  
  public static class Use implements Comparable<Use> {
    public Position pos()      { return _pos; }
    public double   games()    { return _g; }
    
    private Use(Position pos, double g) { _pos = pos; _g = g; }

    private Position _pos = null;
    private double   _g = 0;
    @Override
    public int compareTo(Use arg0) {
      if (_g != arg0._g) { return _g > arg0._g ? -1 : 1; }
      return _pos.compareTo(arg0._pos);
    }
  }
  
  public static class Tally implements Iterable<WeightedApp> {
    @Override public Iterator<WeightedApp> iterator() { return _list.iterator(); }

    private void add(Appearances.ByID aBy, String playerID, int yearID, double weight) {
      WeightedApp WA = new WeightedApp(playerID, yearID);
      Appearances A = null;
      ByPlayer<Appearances> byA = aBy.get(playerID);
      if (byA == null) { return; }
      for (Appearances a : byA) { if (a.yearID() == yearID) { A = a; break; } }
      if (A == null) { return; }
      int g_tot = 0;
      for (Appearances.Use P : A) { g_tot += P.games(); }
      if (g_tot == 0) { return; }
      for (Appearances.Use P : A) {
        WA.add(P.pos(), P.games() / g_tot * weight);
      }
      _list.add(WA);
    }
    
    public Tally(War.Table WT, Appearances.Table AT) {
      Appearances.ByID aBy = new Appearances.ByID();
      aBy.addAll(AT);
      for (War W : WT) { add(aBy, W.playerID(), W.yearID(), W.war()); }
    }

    public Tally(WeightedWar.Tally WT, Appearances.Table AT) {
      Appearances.ByID aBy = new Appearances.ByID();
      aBy.addAll(AT);
      for (WeightedWar WW : WT) { add(aBy, WW.playerID(), WW.yearID(), WW.wwar()); }
    }

    private ArrayList<WeightedApp> _list = new ArrayList<>();
  }
  

  private static void print(WeightedApp A) {
    System.out.format("%s[%d] : ", A._playerID, A._yearID);
    for (Use P : A) {
      System.out.format(" %s[%.1f]", P._pos.getName(), P._g);
    }
    System.out.println();
  }

  private static void print(ByPlayer<WeightedApp> BP) {
    System.out.println("\n" + BP.first().playerID());
    for (WeightedApp A : BP) { print(A); }
    print(BP.total());
  }
  
  public static void assemble(MyDatabase db, ByID by, Appearances.Table AT, Type type) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
    WWT.adjustByPosition(AT);
    WeightedApp.Tally WA = new WeightedApp.Tally(WWT, AT);
    by.addAll(WA);
  }
  
  public static void main(String[] args) throws SQLException {
    ByID by = new ByID();
    try (MyDatabase db = new MyDatabase()) {
      Appearances.Table AT = new Appearances.Table(db);
      assemble(db, by, AT, Type.BAT);
      assemble(db, by, AT, Type.PITCH);
    }
    print(by.get("ruthba01"));
    print(by.get("rosepe01"));
    print(by.get("eckerde01"));
  }
  
}
