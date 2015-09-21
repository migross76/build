package util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import data.Appearances;
import data.Filter;
import data.Groupable;
import data.Position;
import data.Type;
import data.War;

// Compute a weighted version of WAR that emphasizes peak value
// Inspired by: http://www.beyondtheboxscore.com/2010/12/27/1897754/wwar-applying-extra-credit-for-peak-to-wins-above-replacement
// - Function smoothed into a cubic (the factor is quadratic, times the original) by Excel
// - Option to adjust for position (give extra credit to catchers, who seem to get 10% less playing time, and collect WAR at 10% less per 650PA)
// Ideal for selecting HOF candidates, as peak value is often preferred over longevity
public class WeightedWins implements Groupable<WeightedWins>, Comparable<WeightedWins> {
  public static Comparator<WeightedWins> groupYear = new Comparator<WeightedWins>() {
    @Override public int compare(WeightedWins o1, WeightedWins o2) { return o2.yearID() - o1.yearID(); }
  };
  public static Comparator<WeightedWins> groupTeam = new Comparator<WeightedWins>() {
    @Override public int compare(WeightedWins o1, WeightedWins o2) { return o1._war_info.teamID().compareTo(o2._war_info.teamID()); }
  };
  public static Comparator<WeightedWins> bestWar = new Comparator<WeightedWins>() {
    @Override public int compare(WeightedWins o1, WeightedWins o2) {
      if (o1 == null) { return o2 == null ? 0 : 1; }
      if (o2 == null) { return -1; }
      if (o1._value != o2._value) { return o1._value > o2._value ? -1 : 1; }
      if (o1._war != o2._war) { return o1._war > o2._war ? -1 : 1; }
      if (o1._playtime != o2._playtime) { return o1._playtime < o2._playtime ? -1 : 1; }
      if (o1.yearID() != o2.yearID()) { return o1.yearID() < o2.yearID() ? -1 : 1; }
      return o1.playerID().compareTo(o2.playerID());
    }
  };
  public static Filter<WeightedWins> filterPositive = new Filter<WeightedWins>() {
    @Override public boolean satisfied(WeightedWins o1) { return o1._war >= 0; }
  };
  private static ByPlayer.GroupKey<String, WeightedWins> BY_ID = new ByPlayer.GroupKey<String, WeightedWins>() {
    @Override public String groupBy(WeightedWins V) { return V.playerID(); }
  };
  public static class ByID extends ByPlayer.Group<String, WeightedWins> {
    public ByID() { super(BY_ID, bestWar, groupYear); }
  }
  public static class ByIDTeam extends ByPlayer.Group<String, WeightedWins> {
    public ByIDTeam() { super(BY_ID, bestWar, groupTeam); }
  }

  public String playerID() { return _war_info.playerID(); }
  public int yearID() { return _war_info.yearID(); }
  public String teamID() { return _war_info.teamID(); }

  public double waa() { return _waa; }
  public double war() { return _war; }
  public double playtime() { return _playtime; }
  
  public double value() { return _value; }
  
  private War _war_info = null;
  private double _war = 0;
  private double _waa = 0;
  private double _playtime = 0;

  private double _value = 0;

  @Override public void add(WeightedWins WW) {
    _war += WW._war;
    _waa += WW._waa;
    _value += WW._value;
    _playtime += WW._playtime;
  }
  @Override public WeightedWins create() { return new WeightedWins(_war_info); }

  // 0.002*A1^2 + 0.13*A1 + 0.695
  private static double weightedWARFactor(double war) {
    return war * war * 0.002 + war * 0.13 + 0.695;
  }
  
  private void adjustByPosition(Appearances A, int season_pt) {
    if (A == null) { return; }
    double g_tot = 0;
    for (Appearances.Use pos : A) { g_tot += pos.games(); }
    double c_time = g_tot == 0 ? 0 : A.games(Position.CATCH) / g_tot;
    _playtime += _playtime * c_time * 0.1; _war += _war * c_time * 0.1; // Inflate C playing time by 10%
    _war += 1.0 * c_time * _playtime / season_pt; // Inflate C WAR by 1.0/650PA
  }
  
  private void updateWWar(int season_pt) {
    if (_playtime == 0) { _playtime = 1; }
    _value = _war * weightedWARFactor(Math.abs(_war) * season_pt / _playtime);
  }
  
  private WeightedWins(War W) {
    _war_info = W;
    _war = W.war();
    _waa = W.waa();
    _playtime = W.playtime();
  }
  
  @Override
  public int compareTo(WeightedWins arg0) {
    if (_value != arg0._value) { return _value > arg0._value ? -1 : 1; }
    return _war_info.playerID().compareTo(arg0._war_info.playerID());
  }

  public static class Tally implements Iterable<WeightedWins> {
    public Type type() { return _type; }
    
    @Override public Iterator<WeightedWins> iterator() { return _list.iterator(); }

    public void adjustByPosition(Appearances.Table AT) {
      Appearances.ByID aBy = new Appearances.ByID();
      aBy.addAll(AT);
      for (WeightedWins WW : _list) {
        ByPlayer<Appearances> byA = aBy.get(WW._war_info.playerID());
        if (byA == null) { continue; }
        Appearances A = null;
        for (Appearances a : byA) { if (a.yearID() == WW._war_info.yearID()) { A = a; break; } }
        if (A != null) { WW.adjustByPosition(A, _type.playtime()); WW.updateWWar(_type.playtime()); }
      }
    }
    
    private Tally(Type type) { _type = type; }
    private void add(WeightedWins ww) { _list.add(ww); }

    private final ArrayList<WeightedWins> _list = new ArrayList<>();
    private final Type _type;
  }
  
  public static class Builder {
    protected void startingValue(WeightedWins ww) {
      ww._value = ww._waa;
    }
/*    
    public static void adjustByPosition(Appearances.Table AT) {
      Appearances.ByID aBy = new Appearances.ByID();
      aBy.addAll(AT);
      for (WeightedWins WW : _list) {
        ByPlayer<Appearances> byA = aBy.get(WW._war_info.playerID());
        if (byA == null) { continue; }
        Appearances A = null;
        for (Appearances a : byA) { if (a.yearID() == WW._war_info.yearID()) { A = a; break; } }
        if (A != null) { WW.adjustByPosition(A, _type.playtime()); WW.updateWWar(_type.playtime()); }
      }
    }
   
    protected void adjustForPosition(WeightedWins ww) {
      if (A == null) { return; }
      double g_tot = 0;
      for (Appearances.Use pos : A) { g_tot += pos.games(); }
      double c_time = g_tot == 0 ? 0 : A.games(Position.CATCH) / g_tot;
      _playtime += _playtime * c_time * 0.1; _war += _war * c_time * 0.1; // Inflate C playing time by 10%
      _war += 1.0 * c_time * _playtime / season_pt; // Inflate C WAR by 1.0/650PA
      
    }
    
    protected void adjustForPlayingTime(WeightedWins ww) {
      
    }
    
    protected void adjustForGreatness(WeightedWins ww) {
      ww._value *= ww._value / 4;
    }
*/    
    
    public Tally create(War.Table table) {
      Tally tally = new Tally(table.type());
      for (War w : table) {
        WeightedWins ww = new WeightedWins(w);
        startingValue(ww);
/*
        adjustForPosition(ww);
        adjustForPlayingTime(ww);
        adjustForGreatness(ww);
*/
        tally.add(ww);
      }
      return tally;
    }
  }
  
  private static void print(ByPlayer<WeightedWins> P) {
    if (P == null) { System.out.println("could not find player"); return; }
    WeightedWins filter = P.filter(WeightedWins.filterPositive);
    System.out.format("%s\t%d-%d\t%.1f\t%.1f\t%.1f\t%.1f\n",
          P.best()._war_info.playerID(),
          P.first()._war_info.yearID(),
          P.last()._war_info.yearID(),
          P.total()._war,
          P.total()._value,
          P.best()._value,
          filter != null ? filter._value : -1);
  }
  
  public static void main(String[] args) throws SQLException {
    Appearances.Table AT = null;
    WeightedWins.Tally BT = null;
    WeightedWins.Tally PT = null;
    try (MyDatabase db = new MyDatabase()) {
      AT = new Appearances.Table(db);
/*
      BT = new WeightedWins.Tally(new War.Table(db, Type.BAT));
      PT = new WeightedWins.Tally(new War.Table(db, Type.PITCH));
*/
    }
//    BT.adjustByPosition(AT);

    WeightedWins.ByID by = new WeightedWins.ByID();
    by.addFilter(WeightedWins.filterPositive);
    by.addAll(BT);
    by.addAll(PT);
    print(by.iterator().next());
    print(by.get("ruthba01"));
    print(by.get("piazzmi01"));
    print(by.get("benchjo01"));
  }
}
