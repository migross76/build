package crawl;

import java.util.ArrayList;
import util.MyDatabase;
import data.Teams;
import db.CachedStatement;

/* TODO: 2005 had a 'T'ie */
public class Schedule {
  private static final String CACHE = "C:/build/mlbstats/cache/bbref";
  private static final int MAX_NEW_PAGES = 840;
  private static final boolean INGEST_ALL = true;
  
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
        boolean win = columns.get(7).startsWith("W"); // "W" or "W-wo" (walk-off)
        if (!win && !columns.get(7).startsWith("L")) { // "L" or "L-wo" (walk-off)
          System.err.println("Not a win or loss : " + columns.get(7));
        }
        stmt.setBoolean(7, win); // Win/Loss
        stmt.setInt(8, Integer.parseInt(columns.get(8))); // RS
        stmt.setInt(9, Integer.parseInt(columns.get(9))); // RA
        stmt.executeUpdate();
        ++i;
      }
      System.out.println(" : " + i + " games");
    }
    return new_page;
  }

  public void execute(MyDatabase db) throws Exception {
    int new_pages = 0;
    for (int i_yr = _teams.yearLast(), e_yr = _teams.yearFirst() - 1; i_yr != e_yr; --i_yr) {
      for (Teams T : _teams.year(i_yr)) {
        if (fetch_schedule(db, T)) { ++new_pages; }
        if (new_pages >= MAX_NEW_PAGES) { return; }
      }
    }
  }
  
  public Schedule(MyDatabase db, String dir) throws Exception {
    _teams = new Teams.Table(db);
    _sched = new Fetcher("http://www.baseball-reference.com/teams/%1$s/%2$d-schedule-scores.shtml", dir + "/%2$d/sched-%1$s.html");
  }
  
  private Teams.Table _teams = null;
  private Fetcher _sched = null;
  
  
  public static void main(String[] args) throws Exception {
    try (MyDatabase db = new MyDatabase()) {
      new Schedule(db, CACHE).execute(db);
    }
    System.out.println("Finished");
  }
}
