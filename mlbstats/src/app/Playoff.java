package app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import util.MyDatabase;
import data.Teams;

/*
 * Compute historical playoff likelihood based on number of team wins
 */
public class Playoff {

  private static final int START_YEAR = 1901;
  private static final String[] LEAGUES = { "AL", "NL" };
  
  private static final Comparator<Teams> _team_comp = new Comparator<Teams>() {
    @Override public int compare(Teams arg0, Teams arg1) {
      if (arg0.divWin() != arg1.divWin() && arg0.yearID() != 1981 && arg1.yearID() != 1981) { return arg0.divWin() ? -1 : 1; }
      if (arg0.winpct() != arg1.winpct()) { return arg0.winpct() - arg1.winpct() > 0 ? -1 : 1; }
      if (arg0.wins() != arg1.wins()) { return arg1.wins() - arg0.wins(); }
      return arg0.teamID().compareTo(arg1.teamID());
    }
  };
  
  private static class WinRatio {
    public WinRatio(int wins) { _wins = wins; for (int i = 0; i != _playoff.length; ++i) { _playoff[i] = 0; } }
    public int _wins = 0;
    public double[] _playoff = new double[5];
    public double _out = 0;
    public double _total = 0;
    
    public void add(int slot, double weight) {
      if (slot < 5) { _playoff[slot] += weight; } else { _out += weight; }
      _total += weight;
    }
  }
  private static class RatioMap extends TreeMap<Integer, WinRatio> {
    private static final long serialVersionUID = -5151294250908586139L;

    public WinRatio build(int wins) {
      WinRatio WR = get(wins);
      if (WR == null) { put(wins, WR = new WinRatio(wins)); }
      return WR;
    }
  }
  
  
  private void execute() {
    TreeMap<String, ArrayList<Teams>> years = new TreeMap<>();
    for (Teams T : _teams) {
      if (T.yearID() < START_YEAR) { continue; }
      if (!_lgs.contains(T.lgID())) { continue; }
      String id = T.yearID() + "\t" + T.lgID();
      ArrayList<Teams> teams = years.get(id);
      if (teams == null) { years.put(id, teams = new ArrayList<>()); }
      teams.add(T);
    }
    RatioMap ratio = new RatioMap();
    for (Map.Entry<String, ArrayList<Teams>> E : years.entrySet()) {
      ArrayList<Teams> teams = E.getValue();
      if (teams.size() < 14) { continue; }
      Collections.sort(teams, _team_comp);
      for (int i = 0, e = teams.size(); i != e; ++i) {
        Teams T = teams.get(i);
        if (T.games() == 162) { // exact
          ratio.build(T.wins()).add(i, 1.0);
        } else { // partial
          double per162 = T.wins() * 162.0 / T.games();
          int lowend = (int)Math.floor(per162);
          ratio.build(lowend).add(i, per162 - lowend);
          int highend = (int)Math.ceil(per162);
          ratio.build(highend).add(i, highend - per162);
        }
      }
    }
    System.out.println("W\t#\t%1\t%2\t%3\t%4\t%5\t%In\t%Out");
    for (WinRatio WR : ratio.values()) {
      System.out.format("%d\t%.1f", WR._wins, WR._total);
      double in = 0;
      for (double perc : WR._playoff) { System.out.format("\t%.2f", perc / WR._total); in += perc; }
      System.out.format("\t%.2f\t%.2f\n", in / WR._total, WR._out / WR._total);
    }
  }

  private Playoff() throws Exception {
    try (MyDatabase db = new MyDatabase()) { _teams = new Teams.Table(db); }
    _lgs.addAll(Arrays.asList(LEAGUES));
  }
  
  private HashSet<String> _lgs = new HashSet<>();
  private Teams.Table _teams = null;
  
  public static void main(String[] args) throws Exception {
    new Playoff().execute();
  }
}
