package crawl;

import java.util.ArrayList;
import util.MyDatabase;
import data.Master;
import data.Sort;
import data.Teams;
import db.CachedStatement;

/* TODO: 2005 had a 'T'ie */
public class Team {
  private static final String CACHE = "C:/build/mlbstats/cache/bbref";
  private static final int MAX_NEW_PAGES = 840;
  private static final boolean INGEST_ALL = false;
  
  private static int parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static double parseDouble(String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static int getIPOuts(String ip) {
    String[] ipParts = ip.split("\\.");
    return Integer.parseInt(ipParts[0])*3 + Integer.parseInt(ipParts[1]);
  }
  
  private static final String NON_PITCHERS = "23456789OD";
  
  private static final String INSERT_BATTER =
    "INSERT INTO rValueBat VALUES " +
       "(?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?," +
       " ?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?," + " ?, ?)";
  private void fetch_batting(MyDatabase db, Teams T, Parser P) throws Exception {
    BBRefUtil.filter(P, "players_value_batting");
    int start = P.getStart();
    int end = P.getEnd();
    int i = 0;
    ArrayList<String> columns = null;
    CachedStatement stmt = db.prepare(INSERT_BATTER);
    while (true) {
      P.setRange(start, end);
      if ((columns = BBRefUtil.getColumns(P)) == null) { break; }
      start = P.getEnd();
      if (columns.size() < 18) { continue; }
      Master M = BBRefUtil.matchPlayerID(columns.get(0), _master);
      if (M == null) { continue; }
      // compute isPitcher
      String possumm = columns.get(17);
      boolean isPitcher = false;
      for (int i_ch = 0, e_ch = possumm.length(); i_ch != e_ch; ++i_ch) {
        char ch = possumm.charAt(i_ch);
        if (ch == '1') { isPitcher = true; break; }
        if (NON_PITCHERS.indexOf(ch) != -1) { break; }
      } // flag as non-pitcher, unless it's sure they're a pitcher

      stmt.setString(1, M.playerID());
      stmt.setInt(2, T.yearID());
      stmt.setString(3, M.bbrefID());
      stmt.setInt(   4, parseInt(columns.get(1))); // age
      stmt.setString(5, T.teamID());
      stmt.setInt(   6, parseInt(columns.get(2))); // pa
      for (int j = 7; j != 15; ++j) {
        stmt.setInt(j, parseInt(columns.get(j-4))); // PA thru RAR; happens to line up
      }
      stmt.setDouble(15, parseDouble(columns.get(11))); // rWAR
      stmt.setInt(   16, parseInt(columns.get(12))); // oRAR
      stmt.setDouble(17, parseDouble(columns.get(13))); // oWAR
      stmt.setDouble(18, parseDouble(columns.get(14))); // dWAR
      stmt.setInt(   19, BBRefUtil.parseCurrency(columns.get(15))); // salary
      stmt.setString(20, columns.get(16)); // acquired
      stmt.setString(21, possumm); // possumm
      stmt.setInt(   22, isPitcher ? 1 : 0); // isPitcher
      stmt.executeUpdate();
      ++i;
    }
    System.out.print(" : " + i + " batters");
  }

  private static final String INSERT_PITCHER =
    "INSERT INTO rValuePitch VALUES " +
       "(?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?)";
  private void fetch_pitching(MyDatabase db, Teams T, Parser P) throws Exception {
    BBRefUtil.filter(P, "players_value_pitching");
    int start = P.getStart();
    int end = P.getEnd();
    int i = 0;
    ArrayList<String> columns = null;
    CachedStatement stmt = db.prepare(INSERT_PITCHER);
    while (true) {
      P.setRange(start, end);
      if ((columns = BBRefUtil.getColumns(P)) == null) { break; }
      start = P.getEnd();

      if (columns.size() < 12) { continue; }
      Master M = BBRefUtil.matchPlayerID(columns.get(0), _master);

      stmt.setString( 1, M.playerID());
      stmt.setInt(    2, T.yearID());
      stmt.setString( 3, M.bbrefID());
      stmt.setInt(    4, parseInt(columns.get(1))); // age
      stmt.setString( 5, T.teamID());
      stmt.setInt(    6, getIPOuts(columns.get(2))); // IP
      stmt.setInt(    7, Integer.parseInt(columns.get(3))); // GS
      stmt.setInt(    8, Integer.parseInt(columns.get(4))); // R
      stmt.setInt(    9, Integer.parseInt(columns.get(5))); // Rrep
      stmt.setInt(   10, Integer.parseInt(columns.get(6))); // Rdef
      stmt.setDouble(11, parseDouble(columns.get(7))); // aLi
      stmt.setInt(   12, Integer.parseInt(columns.get(8))); // RAR
      stmt.setDouble(13, parseDouble(columns.get(9))); // rWAR
      stmt.setInt(   14, BBRefUtil.parseCurrency(columns.get(10))); // salary
      stmt.setString(15, columns.get(11)); // acquired
      stmt.executeUpdate();
      ++i;
    }
    System.out.print(" : " + i + " pitchers");
  }

  private static final String INSERT_SCHEDULE =
    "INSERT INTO rSchedule VALUES " +
       "(?, ?, ?, ?, ?," + " ?, ?, ?, ?)";
  private boolean fetch_schedule(MyDatabase db, Teams T) throws Exception {
    boolean new_page = !_sched.exists(T.teamIDBR(), T.yearID());
    if (new_page || INGEST_ALL) {
      System.out.format("Ingesting %s[%d] : schedule", T.teamID(), T.yearID()); System.out.flush();
      StringBuilder SB = _sched.getPage(T.teamIDBR(), T.yearID());
      Parser P = new Parser(SB);
      BBRefUtil.filter(P, "team_schedule");
      int start = P.getStart();
      int end = P.getEnd();
      int i = 0;
      ArrayList<String> columns = null;
      CachedStatement stmt = db.prepare(INSERT_SCHEDULE);
      while (true) {
        P.setRange(start, end);
        if ((columns = BBRefUtil.getColumns(P)) == null) { break; }
        start = P.getEnd();
        if (columns.size() < 9) { continue; }
        int gameNum = 0;
        try { gameNum = Integer.parseInt(columns.get(1)); } catch (NumberFormatException e) { continue; }
        
        stmt.setString(1, T.teamID()); // Team
        stmt.setInt(2, T.yearID()); // Year
        stmt.setInt(3, gameNum); // Game Num
        stmt.setString(4, BBRefUtil.matchStanding(columns.get(2))); // Date
        stmt.setBoolean(5, !columns.get(5).equals("@")); // Home/Away
        stmt.setString(6, BBRefUtil.matchTeamID(columns.get(6), _teams, T.yearID()).teamID()); // Opponent
        stmt.setBoolean(7, columns.get(7).equals("W")); // Win/Loss
        boolean win = columns.get(7).equals("W");
        if (!win && !columns.get(7).equals("L")) { System.err.println("Not a win or loss : " + columns.get(7)); }
        stmt.setInt(8, Integer.parseInt(columns.get(8))); // RS
        stmt.setInt(9, Integer.parseInt(columns.get(9))); // RA
        stmt.executeUpdate();
        ++i;
      }
      System.out.println(" : " + i + " games");
    }
    return new_page;
  }

  private boolean fetch_stats(MyDatabase db, Teams T) throws Exception {
    boolean new_page = !_stats.exists(T.teamIDBR(), T.yearID());
    if (new_page || INGEST_ALL) {
      System.out.format("Ingesting %s[%d] : stats", T.teamID(), T.yearID()); System.out.flush();
      StringBuilder SB = _stats.getPage(T.teamIDBR(), T.yearID());
      Parser P = new Parser(SB);
      fetch_batting(db, T, P);
      P.reset();
      fetch_pitching(db, T, P);
      System.out.println();
    }
    return new_page;
  }

  private void execute(MyDatabase db) throws Exception {
    int new_pages = 0;
    for (int i_yr = _teams.yearLast(), e_yr = _teams.yearFirst() - 1; i_yr != e_yr; --i_yr) {
      for (Teams T : _teams.year(i_yr)) {
        if (fetch_schedule(db, T)) { ++new_pages; }
        if (fetch_stats(db, T)) { ++new_pages; }
        if (new_pages >= MAX_NEW_PAGES) { return; }
      }
    }
  }
  
  private Team(MyDatabase db, String dir) throws Exception {
    _master = new Master.Table(db, Sort.UNSORTED);
    _teams = new Teams.Table(db);
    _sched = new Fetcher("http://www.baseball-reference.com/teams/%1$s/%2$d-schedule-scores.shtml", dir + "/%2$d/sched-%1$s.html");
    _stats = new Fetcher("http://www.baseball-reference.com/teams/%1$s/%2$d.shtml", dir + "/%2$d/stats-%1$s.html");
  }
  
  private Master.Table _master = null;
  private Teams.Table _teams = null;
  private Fetcher _sched = null;
  private Fetcher _stats = null;
  
  
  public static void main(String[] args) throws Exception {
    try (MyDatabase db = new MyDatabase()) {
      new Team(db, CACHE).execute(db);
    }
    System.out.println("Finished");
  }
}
