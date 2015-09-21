package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import util.ByPlayer;
import util.Config;
import util.MyDatabase;
import util.WeightedWar;
import data.HOF;
import data.Master;
import data.Sort;
import data.TeamWar;
import data.Teams;
import data.Type;
import data.War;

public class BestTeams {
  
  private static class MyPlayer implements Comparable<MyPlayer> {
    public String _id = null;
    public double _war = 0;
    public double _adjustWar = 0;
    @Override
    public int compareTo(MyPlayer arg0) {
      if (_adjustWar != arg0._adjustWar) { return _adjustWar > arg0._adjustWar ? -1 : 1; }
      return _id.compareTo(arg0._id);
    }
  }
  
  private static class MyTeam implements Comparable<MyTeam> {
    public String id() { return _team.teamID(); }
    public String lgID() { return _team.lgID(); }
    public String name() { return _team.name() == null ? "???" : _team.name(); }
    public int    year() { return _team.yearID(); }
    public int    wins() { return _team.wins(); }
    public int    games() { return _team.games(); }
    
    public MyTeam(Teams T, MyFranchise F) { _team = T; _franch = F; }
    
    @Override
    public int compareTo(MyTeam arg0) {
      if (_adjustWar != arg0._adjustWar) { return _adjustWar > arg0._adjustWar ? -1 : 1; }
      int cmp = name().compareTo(arg0.name());
      if (cmp != 0) { return cmp; }
      return year() - arg0.year();
    }

    public MyFranchise _franch = null;
    public Teams _team = null;
    public double _war = 0;
    public double _adjustWar = 0;
    public double _missingWar = 0;
    public ArrayList<MyPlayer> _players = new ArrayList<>();
  }
  
  private static class MyFranchise {
    public MyFranchise(String id) { _id = id; }
    
    public TreeMap<Integer, MyTeam> _seasons = new TreeMap<>();
    public HashMap<String, MyPlayer> _players = new HashMap<>();

    public String _id = null;
    public int _count = 0;
    
    public void updatePlayers(MyTeam T) {
      for (MyPlayer P : T._players) {
        if (P._war < 0) { continue; } // skip the bad players
        MyPlayer FP = _players.get(P._id);
        if (FP == null || FP._war < P._war) { _players.put(P._id, P); }
      }
    }
  }

  public static double getWins(War w)      { return w.waa(); }
  public static double getWins(TeamWar tw) { return tw.waa(); }
  
  public static void main(String[] args) throws Exception {
    Config config = new Config(BestTeams.class);
    final int FIRST_YEAR = config.getInt("year.first");
    final int MAX_FRANCHISE_CT = config.getInt("franchise.ct.max");
    final HashSet<String> LEAGUES = new HashSet<>(config.getStringList("leagues"));
    final int MAX_TEAMS = config.getInt("teams.max");
    final int TOP_PLAYERS = config.getInt("players.top");
    final int MIN_WWAR = config.getInt("players.wwar.min");
    final int MIN_WAR = config.getInt("players.war.min");
    
    HOF.Table HT = null;
    Teams.Table TT = null;
    Master.Table MT = null;
    WeightedWar.ByID wwBy = new WeightedWar.ByID();
    War.ByTeam wBy = new War.ByTeam();
    try (MyDatabase db = new MyDatabase()) {
      HT = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED);
      TT = new Teams.Table(db);
      MT = new Master.Table(db, Sort.UNSORTED);
      War.Table WT = new War.Table(db, Type.BAT);
      wwBy.addAll(new WeightedWar.Tally(WT));
      wBy.addAll(WT);
      WT = new War.Table(db, Type.PITCH);
      wwBy.addAll(new WeightedWar.Tally(WT));
      wBy.addAll(WT);
    }
    TeamWar.Table TWT = new TeamWar.Table(TT, wBy);
    
    HashMap<String, MyFranchise> franchises = new HashMap<>();
    ArrayList<MyTeam> teams = new ArrayList<>();
    for (Teams T : TT) {
      if (T.yearID() < FIRST_YEAR) { continue; }
      if (!LEAGUES.contains(T.lgID())) { continue; }
      String fID = T.franchID();
      MyFranchise F = franchises.get(fID);
      if (F == null) { franchises.put(fID, F = new MyFranchise(fID)); }
      MyTeam team = new MyTeam(T, F);
      F._seasons.put(T.yearID(), team);
      teams.add(team);
    }
    
    for (TeamWar TW : TWT) {
      if (TW.yearID() < FIRST_YEAR) { continue; }
      String fID = TT.getFranchiseID(TW.teamID());
      MyFranchise F = franchises.get(fID);
      if (F == null) { continue; }
      MyTeam T = F._seasons.get(TW.yearID());
      if (T == null) { System.err.println("Can't find " + fID + " : " + TW.teamID() + " : " + TW.yearID()); continue; }
      T._war = getWins(TW);
      T._adjustWar = T._war * 162 / T.games();
      ByPlayer<War> by = wBy.get(TW.teamID() + TW.yearID());
      T._missingWar = T._war - getWins(by.total());
      for (War W : by) {
        MyPlayer P = new MyPlayer();
        P._id = W.playerID();
        P._war = getWins(W) * 162 / T.games();
        P._adjustWar = P._war;
        T._players.add(P);
      }
    }
    ArrayList<MyTeam> selected = new ArrayList<>();
    for (MyFranchise F : franchises.values()) {
      ArrayList<MyTeam> seasons = new ArrayList<>(F._seasons.values());
      Collections.sort(seasons);
      MyTeam best = seasons.get(0);
      teams.remove(best);
      ++F._count;
      F.updatePlayers(best);
      selected.add(best);
    }
    while (selected.size() != MAX_TEAMS) {
      Collections.sort(teams);
      MyTeam best = null;
      for (MyTeam T : teams) {
        if (best != null && T._adjustWar < best._adjustWar) { break; }
        MyFranchise F = T._franch;
        if (F._count >= MAX_FRANCHISE_CT) { T._adjustWar = 0; continue; }
        T._adjustWar = T._war * 162 / T.games();
        for (MyPlayer P : T._players) {
          if (P._war < 0) { continue; } // skip the bad players
          MyPlayer FP = F._players.get(P._id);
          if (FP != null) {
            double penalty = Math.min(P._war, FP._war); // don't subtract more than his current value
            P._adjustWar = P._war - penalty;
            T._adjustWar -= penalty;
          }
        }
        if (best == null || T._adjustWar > best._adjustWar) { best = T; }
      }
      if (best == null) { break; } // only possible if we're out of teams
      teams.remove(best);
      MyFranchise F = best._franch;
      ++F._count;
      F.updatePlayers(best);
      selected.add(best);
    }
    Collections.sort(selected);
    System.out.println("Rank\tReg\tSeed\tYear\tTeam\tFranch\tLg\tW\tG\taWAR\tWAR\tmWAR\tStars (aWAR/WAR/wWAR)");
    int rank = 0;
    for (MyTeam T : selected) {
      ++rank;
      System.out.format("%d\t%d\t%d\t%d\t%s\t%s\t%s\t%d\t%d\t%.1f\t%.1f\t%.1f", rank, ((rank-1) % 8 < 4) ? (rank - 1) % 4 + 1 : 4 - (rank - 1) % 4, (int)Math.ceil(rank / 4.0), T.year(), T.id(), T._franch._id, T.lgID(), T.wins(), T.games(), T._adjustWar, T._war, T._missingWar);
      Collections.sort(T._players);
      for (int i = 0, e = T._players.size(); i != e; ++i) {
        MyPlayer P = T._players.get(i);
        Master M = MT.byID(P._id);
        HOF H = HT.idFirst(M.hofID());
        ByPlayer<WeightedWar> WWby = wwBy.get(P._id);
        double wwar = WWby == null ? 0 : WWby.total().wwar();
        if (i < TOP_PLAYERS || wwar >= MIN_WWAR || P._war >= MIN_WAR || P._war != P._adjustWar) {
          System.out.format("\t%c %s%s (%.1f/%.1f/%.1f)", M.nameFirst().charAt(0), M.nameLast(), H == null ? "" : "*", P._adjustWar, P._war, wwar);
        }
      }
      System.out.println();
    }
  }
}
