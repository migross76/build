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
public class WeightedWar implements Groupable<WeightedWar>, Comparable<WeightedWar> {
  public static Comparator<WeightedWar> groupYear = new Comparator<WeightedWar>() {
    @Override public int compare(WeightedWar o1, WeightedWar o2) { return o2.yearID() - o1.yearID(); }
  };
  public static Comparator<WeightedWar> groupId = new Comparator<WeightedWar>() {
    @Override public int compare(WeightedWar o1, WeightedWar o2) { return o1.playerID().compareTo(o2.playerID()); }
  };
  public static Comparator<WeightedWar> groupTeam = new Comparator<WeightedWar>() {
    @Override public int compare(WeightedWar o1, WeightedWar o2) { return o1._war_info.teamID().compareTo(o2._war_info.teamID()); }
  };
  public static Comparator<WeightedWar> bestWar = new Comparator<WeightedWar>() {
    @Override public int compare(WeightedWar o1, WeightedWar o2) {
      if (o1 == null) { return o2 == null ? 0 : 1; }
      if (o2 == null) { return -1; }
      if (o1._wwar != o2._wwar) { return o1._wwar > o2._wwar ? -1 : 1; }
      if (o1._war != o2._war) { return o1._war > o2._war ? -1 : 1; }
      if (o1._playtime != o2._playtime) { return o1._playtime < o2._playtime ? -1 : 1; }
      if (o1.yearID() != o2.yearID()) { return o1.yearID() < o2.yearID() ? -1 : 1; }
      return o1.playerID().compareTo(o2.playerID());
    }
  };
  public static Filter<WeightedWar> filterPositive = new Filter<WeightedWar>() {
    @Override public boolean satisfied(WeightedWar o1) { return o1._war >= 0; }
  };
  private static ByPlayer.GroupKey<String, WeightedWar> BY_ID = new ByPlayer.GroupKey<String, WeightedWar>() {
    @Override public String groupBy(WeightedWar V) { return V.playerID(); }
  };
  private static ByPlayer.GroupKey<Integer, WeightedWar> BY_YEAR = new ByPlayer.GroupKey<Integer, WeightedWar>() {
    @Override public Integer groupBy(WeightedWar V) { return V.yearID(); }
  };
  public static class ByID extends ByPlayer.Group<String, WeightedWar> {
    public ByID() { super(BY_ID, bestWar, groupYear); }
  }
  public static class ByIDAlone extends ByPlayer.Group<String, WeightedWar> {
    public ByIDAlone() { super(BY_ID, bestWar, null); }
  }
  public static class ByIDTeam extends ByPlayer.Group<String, WeightedWar> {
    public ByIDTeam() { super(BY_ID, bestWar, groupTeam); }
  }
  public static class ByYear extends ByPlayer.Group<Integer, WeightedWar> {
    public ByYear() { super(BY_YEAR, null, groupId); }
  }

  public String playerID() { return _war_info.playerID(); }
  public int yearID() { return _war_info.yearID(); }
  public String leagueID() { return _war_info.leagueID(); }
  public String teamID() { return _war_info.teamID(); }
  public double waa() { return _waa; }
  public double waa_pos() { return _waa_pos; }
  public double war() { return _war; }
  public double war_pos() { return _war_pos; }
  public double wwar() { return _wwar; }
  public double factor() { return _wwar / _war; }
  public double playtime() { return _playtime; }
  public double seasontime() { return _seasontime; }
  
  private War _war_info = null;
  private double _war = 0;
  private double _war_pos = 0;
  private double _waa = 0;
  private double _waa_pos = 0;
  private double _wwar = 0;
  private double _playtime = 0;
  private double _seasontime = 0;
  
  public static boolean WEIGHT_WAR = true;

  @Override public void add(WeightedWar WW) {
    _war += WW._war;
    _war_pos += WW._war_pos;
    _waa += WW._waa;
    _waa_pos += WW._waa_pos;
    _wwar += WW._wwar;
    _playtime += WW._playtime;
    _seasontime += WW._seasontime;
  }
  @Override public WeightedWar create() { return new WeightedWar(_war_info); }

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
    _wwar = _war * (WEIGHT_WAR ? weightedWARFactor(Math.abs(_war) * season_pt / _playtime) : Math.sqrt(season_pt / _playtime));
    _seasontime = _playtime / season_pt;
  }
  
  private WeightedWar(War W) {
    _war_info = W;
    _war = W.war();
    _waa = W.waa() + W.waa_adj();
    if (W.waa() + W.waa_adj() > 0) { _waa_pos += W.waa() + W.waa_adj(); }
    if (W.war() > 0) { _war_pos += W.war(); }
    _playtime = W.playtime();
  }
  
  @Override
  public int compareTo(WeightedWar arg0) {
    if (_wwar != arg0._wwar) { return _wwar > arg0._wwar ? -1 : 1; }
    return _war_info.playerID().compareTo(arg0._war_info.playerID());
  }

  public static class Tally implements Iterable<WeightedWar> {
    @Override public Iterator<WeightedWar> iterator() { return _list.iterator(); }

    public void adjustByPosition(Appearances.Table AT) {
      Appearances.ByID aBy = new Appearances.ByID();
      aBy.addAll(AT);
      for (WeightedWar WW : _list) {
        ByPlayer<Appearances> byA = aBy.get(WW._war_info.playerID());
        if (byA == null) { continue; }
        Appearances A = null;
        for (Appearances a : byA) { if (a.yearID() == WW._war_info.yearID()) { A = a; break; } }
        if (A != null) { WW.adjustByPosition(A, _playtime); WW.updateWWar(_playtime); }
      }
    }
    
    public Tally(War.Table WT) {
      this(WT, WT.type().playtime());
    }
    
    public Tally(War.Table WT, int playtime) {
      _playtime = playtime;
      for (War W : WT) {
        WeightedWar WW = new WeightedWar(W);
        WW.updateWWar(_playtime);
        _list.add(WW);
      }
    }

    private ArrayList<WeightedWar> _list = new ArrayList<>();
    private final int _playtime;
  }
  
  private static void print(ByPlayer<WeightedWar> P) {
    if (P == null) { System.out.println("could not find player"); return; }
    WeightedWar filter = P.filter(WeightedWar.filterPositive);
    System.out.format("%s\t%d-%d\t%.1f\t%.1f\t%.1f\t%.1f\n",
          P.best()._war_info.playerID(),
          P.first()._war_info.yearID(),
          P.last()._war_info.yearID(),
          P.total()._war,
          P.total()._wwar,
          P.best()._wwar,
          filter != null ? filter._wwar : -1);
  }
  
  public static void main(String[] args) throws SQLException {
    Appearances.Table AT = null;
    WeightedWar.Tally BT = null;
    WeightedWar.Tally PT = null;
    try (MyDatabase db = new MyDatabase()) {
      AT = new Appearances.Table(db);
      BT = new WeightedWar.Tally(new War.Table(db, Type.BAT));
      PT = new WeightedWar.Tally(new War.Table(db, Type.PITCH));
    }
    BT.adjustByPosition(AT);

    WeightedWar.ByID by = new WeightedWar.ByID();
    by.addFilter(WeightedWar.filterPositive);
    by.addAll(BT);
    by.addAll(PT);
    print(by.iterator().next());
    print(by.get("ruthba01"));
    print(by.get("piazzmi01"));
    print(by.get("benchjo01"));
  }
}
