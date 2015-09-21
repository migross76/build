package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import util.ByPlayer;
import util.MyDatabase;
import data.Appearances;
import data.Master;
import data.Sort;
import data.Teams;

/*
 * 5 year rolling average for starting year
 * Tabulate yearly totals for number of players with 7+ seasons (both start of career and switching teams)
 * - Make sure not to count those that switched teams mid-year
 * Normalize annual count by number of franchises playing for 7+ years
 * - starting is N to N+7
 * - yearly is N-7 to N
 */

/*
 * 
SP :  35.8 |10.1
C  :  99.4 | 8.0
1B : 121.6 | 7.4
2B : 120.9 | 7.2
3B : 118.9 | 7.5
SS : 120.6 | 7.6
LF : 120.8 | 6.7
CF : 120.0 | 6.8
RF : 121.0 | 6.9
DH : 103.4 | 2.0
RP :  51.4 | 6.9

 * Try to capture players that play 7 years worth of games for a team
 * SP :  35G/yr => 245+G
 * RP :  50G/yr => 350+G
 * C  : 100G/yr => 700+G
 * BT : 120G/yr => 840+G
 */

public class OneFranchise {
  private static final double YRS = 7;

  private static class Player implements Comparable<Player> {
    public Appearances _app = null;
    
    public String  id() { return _app.playerID(); }
    public String  _franchise = null;
    public String  _teams = "";
    public int     _games = 0;
    public double  _seasons = 0;
    public boolean _start = true;
    public boolean _end = true;
    public int     _startYear = 0;
    public int     _endYear = 0;
    
    public Player ( Appearances A, String franchise, boolean isStart ) {
      _app = A.create();
      _start = isStart;
      _startYear = A.yearID();
      _franchise = franchise;
    }

    @Override
    public int compareTo(Player arg0) {
      if (_app.primary().pos() != arg0._app.primary().pos()) { return _app.primary().pos().compareTo(arg0._app.primary().pos()); }
      if (_games != arg0._games) { return arg0._games - _games; }
      return id().compareTo(arg0.id());
    }
    
    @Override public String toString ( ) {
      return String.format("%s\t%s\t%d\t%s\t%.2f\t" +
      		               "%s\t%d\t%d\t%s",
          _franchise, _teams, _games, _app.primary().pos().getName(), _seasons,
          _start ? "[" : "-", _startYear, _endYear, _end ? "]" : "-");
    }
  }
  
  
  public static void main(String[] args) throws SQLException {
    Teams.Table TT = null;
    Master.Table MT = null;
    Appearances.ByIDTeam Aby = new Appearances.ByIDTeam();
    try (MyDatabase db = new MyDatabase()) {
      TT = new Teams.Table(db);
      MT = new Master.Table(db, Sort.UNSORTED);
      Aby.addAll(new Appearances.Table(db));
    }
    
    ArrayList<Player> players = new ArrayList<>();
    Player P = null;
    for (ByPlayer<Appearances> AA : Aby) {
      boolean newPlayer = true;
      for (Appearances A : AA) {
        String franchise = TT.getFranchiseID(A.teamID());
        if (P == null || !franchise.equals(P._franchise) || !A.playerID().equals(P.id())) {
          if (P != null) {
            P._end = newPlayer;
            if (P._seasons >= YRS) { P._app.sort(); players.add(P); }
          }
          P = new Player(A, franchise, newPlayer);
          P._teams = A.teamID();
        }
        P._endYear = A.yearID();
        if (!P._teams.endsWith(A.teamID())) { P._teams = P._teams + "-" + A.teamID(); }
        P._app.add(A);
        for (Appearances.Use pos : A) {
          P._games += pos.games();
          P._seasons += pos.games() / pos.pos().getGamesPerSeason(); 
        }
        newPlayer = false;
      }
    }
    if (P == null) { throw new SQLException("couldn't find any player that meets the criteria"); }
    P._end = true;
    if (P._seasons >= YRS) { P._app.sort(); players.add(P); }
    
    Collections.sort(players);
    for (Player PP : players) {
      Master M = MT.byID(PP.id());
      System.out.format("%s %s\t%s\n", M == null ? PP.id() : M.nameFirst(), M == null ? "" : M.nameLast(), PP);
    }
/*    
    ArrayList<Player> players = new ArrayList<Player>();
    Player P = new Player();
    PreparedStatement PS = Database.prepare(QUERY);
    ResultSet RS = PS.executeQuery();
    int[][] counts = new int[200][6];
    while (RS.next()) {
      String id = RS.getString(1);
      String franchise = RS.getString(2);
      int year = RS.getInt(3);
      String team = RS.getString(7);
      if (!id.equals(P._id) || !franchise.equals(P._franchise)) {
        boolean sameteam = !id.equals(P._id);
        if (P.qualify(sameteam)) { players.add(P); }
        P = new Player(id, RS.getString(5) + " " + RS.getString(6), franchise, year, sameteam);
        P._start = sameteam;
        P._teams = team;
      }
      P._endYear = year;
      if (!P._teams.endsWith(team)) { P._teams = P._teams + "-" + team; }
      int games = RS.getInt(8);
      P._games += games;
      for (int i = 0; i != POS.length; ++i) {
        int posg = RS.getInt(i+SQL_FIRST_POS);
        P._pos[i] += posg;
        if (G_YR[i] != DEFAULT_G) { games -= posg; P._seasons += posg / G_YR[i]; }
      }
      if (games > 0) { P._seasons += games / DEFAULT_G; }
      if (P._seasons > YRS) {
        ++counts[year - 1850][4];
        if (P._start) { ++counts[year - 1850][5]; } 
      }
    }
    if (P.qualify(true)) { players.add(P); }
    
    Collections.sort(players);
    for (Player P2 : players) {
      System.out.println(P2);
      ++counts[P2._startYear - 1850][0];
      if (P2._start) { ++counts[P2._startYear - 1850][1]; }
      if (P2._end) { ++counts[P2._startYear - 1850][2]; }
      if (P2._start && P2._end) { ++counts[P2._startYear - 1850][3]; }
    }
    
    System.out.println();
    for (int i = 0; i != 200; ++i) {
      if (counts[i][0] != 0 || counts[i][4] != 0) {
        int year = i + 1850;
        double f_ct = Franchise.info().getActiveFranchises(year, year+5);
        if (f_ct == 0) { f_ct = 1; }
        System.out.print(i + 1850);
        for (int j = 0; j != 4; ++j) {
          System.out.format("\t%.1f", counts[i][j] / f_ct);
        }
        f_ct = Franchise.info().getActiveFranchises(year-5, year);
        if (f_ct == 0) { f_ct = 1; }
        for (int j = 4; j != 6; ++j) {
          System.out.format("\t%.1f", counts[i][j] / f_ct);
        }
        System.out.println();
      }
    }
*/
  }

}
