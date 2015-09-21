package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedWar;
import data.ELO;
import data.Master;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;

/* Inspired by http://www.highheatstats.com/2012/05/baseball-mount-rushmores/?utm_source=rss&utm_medium=rss&utm_campaign=baseball-mount-rushmores
 * 
 * Pick the top N players for each active team/franchise.
 * - Treats WAR for their team separate from WAR for other teams
 * - Overall WAR = 1.0 team, 1.0 others
 * - Team WAR = 1.0 team, 0.0 others
 * - Good Rushmore = 1.0 team, -0.5 others
 * - They Played Where? = -2.0 team, 1.0 others
 */
public class RushmoreNominate {
  private static final int YEAR_ELIGIBLE_START = 1801;
  private static final int YEAR_ELIGIBLE_END = 2013;
  private static final int YEARS_ROLLBACK = 25;
  private static final int YEAR_ELECT_START = 1901 + YEARS_ROLLBACK;
  private static final int YEAR_ELECT_END = 2020;
  
  public static class Player implements Comparable<Player> {
    public Player(Master M, double total, int finalYear) { _master = M; _total = total; _finalYear = finalYear; }
    
    public Master _master = null;
    private double _team = 0;
    public final double _total;
    public final int _finalYear;
    public double _value = 0;
    
    public void addSeason(double team) { _team += team; _value = Math.sqrt(Math.max(_team, 1) * Math.max(_total, 0)); }

    @Override public int compareTo(Player arg0) {
      if (_value != arg0._value) { return _value > arg0._value ? -1 : 1; } 
      return _master.playerID().compareTo(arg0._master.playerID());
    }
  }
  
  public static class Chosen implements Comparable<Chosen> {
    public Player _player;
    public Teams _team;
    public String _team_id;
    public int _slot;
    public double _elo;
    public int _year;
    
    public int _eliminated_by = -1;

    @Override public int compareTo(Chosen arg0) {
      int cmp = 0;
      if ((cmp = Double.compare(_player._total, arg0._player._total)) != 0) { return -cmp; }
      return Integer.compare(_slot, arg0._slot);
    }
  }
  
  private static void add(TreeMap<String, ArrayList<Player>> map, String id, Master M, double year, double total, int finalYear) {
    if (id == null) { System.err.println("Unknown id"); return; }
    ArrayList<Player> list = map.get(id);
    if (list == null) { map.put(id, list = new ArrayList<>()); }
    Player P = list.isEmpty() ? null : list.get(list.size() - 1);
    if (P == null || P._master != M) { list.add(P = new Player(M, total, finalYear)); }
    P.addSeason(year);
  }
  
  private static class TeamTracker {
    public TeamTracker(Teams.Table TT) {
      for (Teams t : TT.year(YEAR_ELIGIBLE_END)) { _active.add(t.franchID()); }
      _equiv.put("TBD", "TBA"); _equiv.put("FLO", "MIA"); _equiv.put("CAL", "LAA"); _equiv.put("ANA", "LAA"); _equiv.put("ML4", "MIL");
      _equiv.put("BR3", "BRO"); _equiv.put("CN2", "CIN"); _equiv.put("PT1", "PIT"); _equiv.put("SL4", "SLN");
    }

    public String lookup(Teams t) {
      if (t == null) { return "???"; }
      if (!_active.contains(t.franchID())) { return "XXX"; }
      String id = _equiv.get(t.teamID());
      if (id != null) { return id; }
      return t.teamID();
    }
    
    private HashSet<String> _active = new HashSet<>();
    private HashMap<String, String> _equiv = new HashMap<>(); // teams renamed, but didn't move
  }
  
  private static Comparator<Teams> BY_BEST = new Comparator<Teams>() {
    @Override public int compare(Teams arg0, Teams arg1) {
      if (arg0.wsWin() != arg1.wsWin()) { return arg0.wsWin() ? -1 : 1; }
      if (arg0.lgWin() != arg1.lgWin()) { return arg0.lgWin() ? -1 : 1; }
      if (arg0.divWin() != arg1.divWin()) { return arg0.divWin() ? -1 : 1; }
      if (arg0.wcWin() != arg1.wcWin()) { return arg0.wcWin() ? -1 : 1; }
      if (arg0.winpct() != arg1.winpct()) { return arg0.winpct() > arg1.winpct() ? -1 : 1; }
      int rdiff0 = arg0.runs() - arg0.runsAllowed();
      int rdiff1 = arg1.runs() - arg1.runsAllowed();
      if (rdiff0 != rdiff1) { return rdiff1 - rdiff0; }
      return arg0.teamID().compareTo(arg1.teamID());
    }
  };
  
  public static void main(String[] args) throws Exception {
    Master.Table MT = null;
    Teams.Table TT = null;
    ELO.Table ET = null;
//    HOF.Table HT = null;
    WeightedWar.ByID wwBy = new WeightedWar.ByID();
    wwBy.addFilter(WeightedWar.filterPositive);
    try (MyDatabase db = new MyDatabase()) {
  //    Appearances.Table AT = new Appearances.Table();
      MT = new Master.Table(db, Sort.UNSORTED);
  //    assemble(AT, Type.BAT);
  //    assemble(AT, Type.PITCH);
      TT = new Teams.Table(db);
      ET = new ELO.Table(db);
//      HT = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED);
      wwBy.addAll(new WeightedWar.Tally(new War.Table(db, Type.BAT), 650));
      wwBy.addAll(new WeightedWar.Tally(new War.Table(db, Type.PITCH), 800));
    }
    
    TeamTracker teams = new TeamTracker(TT);
    TreeMap<String, ArrayList<Player>> eligibles = new TreeMap<>();
    ArrayList<Player> globalEligible = new ArrayList<>();
    
    for (ByPlayer<WeightedWar> byWW : wwBy) {
      if (byWW.first().yearID() < YEAR_ELIGIBLE_START) { continue; }
      if (byWW.last().yearID() > YEAR_ELIGIBLE_END) { continue; }
      Master M = MT.byID(byWW.total().playerID());
//      if (HT.idFirst(M.hofID()) == null) { continue; }
      for (WeightedWar ww : byWW) {
        Teams T = TT.team(ww.yearID(), ww.teamID());
        add(eligibles, teams.lookup(T), M, ww.wwar(), byWW.total().wwar(), byWW.last().yearID());
      }
      Player p = new Player(M, byWW.total().wwar(), byWW.last().yearID());
      p.addSeason(byWW.total().wwar());
      globalEligible.add(p);
    }
    for (ArrayList<Player> list : eligibles.values()) { Collections.sort(list); }
    Collections.sort(globalEligible);
    
    ArrayList<Chosen> elected = new ArrayList<>();
    HashSet<String> skipTeam = new HashSet<>();
    ArrayList<Chosen> rejected = new ArrayList<>();
    int total_teams = 0;
    for (int year = YEAR_ELECT_START; year <= YEAR_ELECT_END; ++year) {
      int year_rollback = year - YEARS_ROLLBACK;
      ArrayList<Teams> precincts = new ArrayList<>(TT.year(year_rollback));
      Collections.sort(precincts, BY_BEST);
      total_teams += precincts.size();
      
      ArrayList<Chosen> chosen = new ArrayList<>();
      HashSet<String> selected = new HashSet<>();
      for (Chosen c : elected) { selected.add(c._player._master.playerID()); }
      for (Teams t : precincts) {
        String id = teams.lookup(t);
        if (id.equals("XXX")) { continue; }
        if (skipTeam.contains(id)) { chosen.add(null); continue; }
        Player nominee = null;
        label: for (Player p : eligibles.get(id)) {
          if (selected.contains(p._master.playerID())) { continue; }
          for (Chosen c : rejected) { if (id.equals(c._team_id) && c._player._master.playerID().equals(p._master.playerID())) { continue label; } }
          if (p._finalYear + 6 <= year) { nominee = p; break; }
        }
        if (nominee != null) {
          selected.add(nominee._master.playerID());
          Chosen c = new Chosen();
          c._player = nominee;
          c._team = t;
          c._team_id = id;
          c._slot = chosen.size();
          c._year = year;
          ELO elo = ET.id(c._player._master.playerID());
          c._elo = elo.norm();
          chosen.add(c);
        }
      }
      for (int i = 0; i != chosen.size(); ++i) {
        if (chosen.get(i) == null) {
          label: for (Player p : globalEligible) {
            if (selected.contains(p._master.playerID())) { continue; }
            for (Chosen c : rejected) { if ("---".equals(c._team_id) && c._player._master.playerID().equals(p._master.playerID())) { continue label; } }
            if (p._finalYear + 6 <= year) {
              selected.add(p._master.playerID());
              Chosen c = new Chosen();
              c._player = p;
              c._year = year;
              c._team_id = "---";
              ELO elo = ET.id(c._player._master.playerID());
              c._elo = elo.norm();
              chosen.set(i, c); break;
            }
          }
        }
      }
      Collections.sort(chosen);
      for (int i = 0; i != chosen.size(); ++i) { chosen.get(i)._slot = i; }
      System.out.print(year);
      for (Chosen c : chosen) {
        System.out.format("\t%s\t%s %s\t%.1f\t%d\t%.0f", c._team_id, c._player._master.nameFirst(), c._player._master.nameLast(), c._player._value, c._player._finalYear, c._elo);
      }
      System.out.println();
      int to_select = total_teams / 8;
      total_teams %= 8; // keep track of leftovers
      int e_team = chosen.size();
      int b_team = to_select;
      while (b_team * 2 < e_team) { b_team *= 2; }
//      System.out.format("%d - %d : %d : %d\n", b_team, e_team, to_select, total_teams);
      while (e_team > to_select) {
        for (int i_team = b_team; i_team != e_team; ++i_team) {
          int i_opp = b_team*2-i_team-1;
          int i_main = i_team;
          Chosen team = chosen.get(i_main);
          while (team._eliminated_by != -1) { i_main = team._eliminated_by; team = chosen.get(i_main); }
          Chosen opp = chosen.get(i_opp);
          while (opp._eliminated_by != -1) { i_opp = opp._eliminated_by; opp = chosen.get(i_opp); }
          if (team._elo > opp._elo) { // tie goes to the higher ranked team
            opp._eliminated_by = i_main;
          } else {
            team._eliminated_by = i_opp;
          }
//if (year == 2007) {
//          System.out.format("%d:%s%s @ %d:%s%s\n", i_main+1, teams.lookup(team._team), team._eliminated_by == -1 ? "*" : "", i_opp+1, teams.lookup(opp._team), opp._eliminated_by == -1 ? "*" : "");
//}
        }
//        System.out.println();
        e_team = b_team;
        b_team /= 2;
      }
      skipTeam.clear();
      HashSet<Integer> selectedSlots = new HashSet<>();
      selectedSlots.add(-1);
      for (Chosen c : chosen) {
        if (c._eliminated_by == -1) {
          elected.add(c);
          skipTeam.add(c._team_id);
          selectedSlots.add(c._slot);
//if (year == 2007) { System.out.format("\t%d", c._slot); }
        }
      }
      for (Iterator<Chosen> i_rej = rejected.iterator(); i_rej.hasNext(); ) {
        Chosen rej = i_rej.next();
        if (rej._year < year - 1) { i_rej.remove(); }
      }
      for (Chosen c : chosen) {
        if (!selectedSlots.contains(c._eliminated_by)) { rejected.add(c); if (year == 2007) { System.out.format("%s-%d\n", c._player._master.playerID(), c._eliminated_by);}}
      }
//      System.out.println();
//if (year == 0) {      
//      System.out.format("%d\t%d", year, year_rollback);
//      for (Teams t : precincts) { System.out.format("\t%s", teams.lookup(t)); }
//      System.out.println();
//}
//      if (year > 1930) { break; }
    }
    for (Chosen c : elected) {
      System.out.format("%d\t%s\t%s %s\t%d\t%.1f\t%.1f\t%.0f\n", c._year, c._team == null ? "---" : c._team_id, c._player._master.nameFirst(), c._player._master.nameLast(), c._player._finalYear, c._player._value, c._player._total, c._elo);
    }
    
    //print(eligibles, "Teams");
  }
}
