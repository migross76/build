package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.HOF;
import data.ELO;
import data.Master;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;
import data.War.ByYear;

// Inspired by Bill James' idea to select "veterans" for the HOF by having a nomination and tournament.
// My rules:
// * Start at 1926
// * Grab BBWAA electees starting at age 45 (e.g., 1881)
//   - Idea: re-compute electees with new candidate sets
// * Players eligible for tournament after 5 years of election (e.g., 1876)
// * Run the tournament based on 25 years ago (e.g., 1901)
// * Number of tourney champs are equal to teams / 16, rounded down; leftovers carry over to next season
// * Franchise selection is on the success rate of the given year; 1. playoff success; 2. winning %; 3. run differential
// * Franchise picks player based on geometric mean of franchise contribution and overall contribution, as measured by positive WAA
// * Seeds are determined by WAR - do I re-seed after each round, or 
// * Matches are determined by ELO (normalize pitcher to batter rating)

public class HallJamesTourney {
  
  private static final int AGE_ELECT = 45;
  private static final int ELECT_MAX = 5;
  private static final int AGE_TOURNEY = AGE_ELECT + ELECT_MAX;
  private static final int TEAM_YEAR = 25;
  private static final int YEAR_FIRST = 1926;
  
  private static class Player {
    public Master _master;
    public HOF _hof;
    public int _hof_ballots = 0;
    public double _hof_best = 0;
    public ByPlayer<War> _war;
    public ELO _elo;
    public double _pwaa = 0;
  }
  private final HashMap<Integer, ArrayList<Player>> _players = new HashMap<>();
  
  private static class ElectionYear {
    public ArrayList<Player> _elected = new ArrayList<>();
    public ArrayList<Player> _eligible = new ArrayList<>(); // first year eligible for "tournament"
  }
  public TreeMap<Integer, ElectionYear> _years = new TreeMap<>();
  public ElectionYear getElectionYear(int year) {
    ElectionYear ey = _years.get(year);
    if (ey == null) { _years.put(year, ey = new ElectionYear()); }
    return ey;
  }
  
  public static final Comparator<Player> BY_ELO = new Comparator<Player>() {
    @Override public int compare(Player o1, Player o2) {
      double cmp = o1._elo.norm() - o2._elo.norm();
      if (cmp != 0) { return cmp > 0 ? -1 : 1; }
      return o1._master.playerID().compareTo(o2._master.playerID());
    }
    
  };
  
  public HashMap<String, String> _franch_map = new HashMap<>();
  
  private static class TeamPlayer {
    public TeamPlayer(String franchise, Player player) { _franchise = franchise; _player = player; }
    
    public final String _franchise;
    public final Player _player;
    public double _pwaa = 0;
  }
  private final HashMap<String, ArrayList<TeamPlayer>> _franchises = new HashMap<>();
  
  private ArrayList<Player> _eligible = new ArrayList<>();

  public void select(int year) {
    int year_franchise = year - TEAM_YEAR;
    ArrayList<Teams> teams = _tt.year(year_franchise, true);
    int team_ct = teams.size() + _team_remainder;
    int selections = team_ct / 16;
    _team_remainder = team_ct % 16;
    ElectionYear ey = _years.get(year);
    _eligible.addAll(ey._eligible);
    Collections.sort(_eligible, BY_ELO);
    
    //System.out.format("%d\t%d\t%d\t%d", year, teams.size(), selections, ey._eligible.size());
    for (Player p : ey._elected) {
      System.out.format("%d\tE\t%s %s\t%d\t%.1f\t%.0f\t%.1f\n", year, p._master.nameFirst(), p._master.nameLast(), p._master.yearBirth(), p._hof_best * 100, p._elo.norm(), p._war.total().war());
    }
    selections = 1;
    for (int i = 0; i != selections; ++i) {
      Player p = _eligible.get(i);
      System.out.format("%d\tT\t%s %s\t%d\t%.1f\t%.0f\t%.1f\n", year, p._master.nameFirst(), p._master.nameLast(), p._master.yearBirth(), p._hof_best * 100, p._elo.norm(), p._war.total().war());
    }
    for (int i = 0; i != selections; ++i) { _eligible.remove(0); }
//    for (int i = selections; i != 2; ++i) { System.out.print("\t"); }
//    System.out.println();
  }
  
  public HallJamesTourney(MyDatabase db) throws SQLException {
    _franch_map.put("BL1", "BAL");
    
    _mt = new Master.Table(db, Sort.SORTED);
    _tt = new Teams.Table(db);
    _ht = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ALL);
    _war = new War.ByIDAlone();
    _war.addAll(new War.Table(db, Type.BAT));
    _war.addAll(new War.Table(db, Type.PITCH));

    _elo = new ELO.Table(db);
    
    for (ELO elo : _elo.all()) {
      String playerID = elo.playerID();
      Player p = new Player();
      p._elo = elo;
      p._war = _war.get(playerID);
      p._master = _mt.byID(playerID);

      int born = p._master.yearBirth();

      List<HOF> hof = _ht.id(playerID);
      if (hof != null) {
        for (HOF h : hof) {
          if (h.votedBy().equals("Special Election")) { p._hof = h; p._hof_best = 1.01; p._hof_ballots = 1; break; }
          if (!h.votedBy().equals("BBWAA")) { continue; }
          if (p._hof_best < h.percent()) { p._hof_best = h.percent(); p._hof = h; }
          ++p._hof_ballots;
        }
      }
      if (p._hof_best > 0.75 && p._hof_ballots <= ELECT_MAX) {
        int year = born + AGE_ELECT;
        if (year < YEAR_FIRST) { year = YEAR_FIRST; }
        if (p._hof.votedBy().equals("Special Election")) { year = p._hof.yearID(); }
        getElectionYear(year + p._hof_ballots - 1)._elected.add(p); }
      else if (born + AGE_TOURNEY >= YEAR_FIRST) { getElectionYear(born + AGE_TOURNEY)._eligible.add(p); }
      else { _eligible.add(p); }
/*
//      if (p._hof != null) { year += 45 + p._hof.yearID(); }
      ArrayList<Player> list = _players.get(year);
      if (list == null) { _players.put(year, list = new ArrayList<>()); }
      list.add(p);
      
      HashMap<String, TeamPlayer> tps = new HashMap<>();
      for (War w : p._war) {
        if (w.waa() <= 0) { continue; }
        p._pwaa += w.waa();
        Teams t = _tt.team(w.yearID(), w.teamID());
        String franchise = t == null ? "ZZZ" : t.franchID();
        String fr2 = _franch_map.get(franchise);
        if (fr2 != null) { franchise = fr2; }
        TeamPlayer tp = tps.get(franchise);
        if (tp == null) { tp = new TeamPlayer(franchise, p); }
        tp._pwaa += w.waa(); // FIXME need to group all TeamPlayer by player (only one per player/franchise combo)
      }
      for (TeamPlayer tp : tps.values()) {
        ArrayList<TeamPlayer> tpList = _franchises.get(tp._franchise);
        if (tpList == null) { _franchises.put(tp._franchise, tpList = new ArrayList<>()); }
        tpList.add(tp);
      }
*/
    }
    
    
    

//    Appearances.Table AT = new Appearances.Table(db);
//    assemble(db, AT, Type.BAT);
//    assemble(db, AT, Type.PITCH);
    
  }
  
  int _team_remainder = 0;
  
  private final Master.Table  _mt;
  private final Teams.Table   _tt;
  private final HOF.Table     _ht;
  private final War.ByIDAlone _war;
  private final ELO.Table     _elo;
  
  
  public static void main(String[] args) throws SQLException {
    HallJamesTourney hjt = null;
    try (MyDatabase db = new MyDatabase()) {
      hjt = new HallJamesTourney(db);
    }
    for (int yr = 1926; yr != 2015; ++yr) {
      hjt.select(yr);
    }
  }
}
