package crawl;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import util.MyDatabase;
import data.Master;
import data.Sort;
import data.Teams;
import db.CachedStatement;

/*
   http://www.baseball-reference.com/leagues/MLB/2009-value-pitching.shtml
<table class="sortable  stats_table" id="players_value_pitching">
Rk      Age     Tm  IP  GS  R   Rrep    Rdef    aLI     RAR     WAR     Salary  Acquired
   http://www.baseball-reference.com/leagues/MLB/2009-value-batting.shtml
<table class="sortable  stats_table" id="players_value_batting">
<thead><tr><th tip='>Col</th>
<tbody><tr><td><a href="/players/a/abreubo01.shtml">Bobby&nbsp;Abreu</a>*</td>
   Rk       Age     Tm  PA  Rbat    Rbaser  Rroe    Rdp     Rfield  Rpos    Rrep    RAR     WAR     oRAR    oWAR    dWAR    Salary  Acquired    Pos. Summary
*/
   
public class Crawl {
  public static class WarTeam {
    public String _teamID = null;
    public int _year = 0;
    public int _pa = 0;
    public int _ipOuts = 0;
    public int _GS = 0;
    public double _war = 0;
    public int _salary = 0;

    public WarTeam(String teamID, int year) { _teamID = teamID; _year = year; }
  }
  public static class WarSummary {
    public int _countStat = 0;
    public double _maxWAR = -100;
    public double _sumWAR = 0;
    public double _sumPosWAR = 0;
    
    public void add(int count, double war) {
      if (_maxWAR < war) { _maxWAR = war; }
      _countStat += count;
      _sumWAR += war;
      if (war > 0) { _sumPosWAR += war; }
    }
  }
  private HashMap<String, WarSummary> _sumPitch = new HashMap<>();
  private HashMap<String, WarSummary> _sumBat = new HashMap<>();
  private HashMap<String, WarTeam>    _sumTeam = new HashMap<>();
  
  private static final String INSERT_SUMMARY_BAT =
    "INSERT INTO bbrefSummaryBat VALUES (?, ?, ?, ?, ?)";
  public void ingestSummaryBat(MyDatabase db) throws SQLException {
    System.out.println("Ingesting summary batter info : " + _sumBat.size()); System.out.flush();
    CachedStatement stmt = db.prepare(INSERT_SUMMARY_BAT);
    for (Map.Entry<String, WarSummary> entry : _sumBat.entrySet()) {
      stmt.setString(1, entry.getKey());
      stmt.setDouble(2, entry.getValue()._countStat);
      stmt.setDouble(3, entry.getValue()._maxWAR);
      stmt.setDouble(4, entry.getValue()._sumWAR);
      stmt.setDouble(5, entry.getValue()._sumPosWAR);
      stmt.executeUpdate();
    }
  }

  private static final String INSERT_SUMMARY_PITCH =
    "INSERT INTO bbrefSummaryPitch VALUES (?, ?, ?, ?, ?)";
  public void ingestSummaryPitch(MyDatabase db) throws SQLException {
    System.out.println("Ingesting summary pitcher info : " + _sumPitch.size()); System.out.flush();
    CachedStatement stmt = db.prepare(INSERT_SUMMARY_PITCH);
    for (Map.Entry<String, WarSummary> entry : _sumPitch.entrySet()) {
      stmt.setString(1, entry.getKey());
      stmt.setDouble(2, entry.getValue()._countStat);
      stmt.setDouble(3, entry.getValue()._maxWAR);
      stmt.setDouble(4, entry.getValue()._sumWAR);
      stmt.setDouble(5, entry.getValue()._sumPosWAR);
      stmt.executeUpdate();
    }
  }
  
  private static final String INSERT_SUMMARY_TEAM =
    "INSERT INTO bbrefValueTeam VALUES (?, ?, ?, ?, ?," + " ?, ?)";
  public void ingestSummaryTeam(MyDatabase db) throws SQLException {
    System.out.println("Ingesting summary team info : " + _sumTeam.size()); System.out.flush();
    CachedStatement stmt = db.prepare(INSERT_SUMMARY_TEAM);
    for (WarTeam WT : _sumTeam.values()) {
      stmt.setString(1, WT._teamID);
      stmt.setInt(2, WT._year);
      stmt.setInt(3, WT._pa);
      stmt.setInt(4, WT._ipOuts);
      stmt.setInt(5, WT._GS);
      stmt.setDouble(6, WT._war);
      stmt.setInt(7, WT._salary);
      stmt.executeUpdate();
    }
  }

  private static final String INSERT_PITCHER =
    "INSERT INTO bbrefValuePitch VALUES " +
       "(?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?)";
  public void ingestPitchers(MyDatabase db, int year) throws Exception {
    System.out.print("Ingesting pitchers from " + year + " : "); System.out.flush();
    StringBuilder SB = _fetcher.getPage("pitching", year);
    Parser P = new Parser(SB);
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
      if (columns.size() < 13) { continue; }
      Master M = BBRefUtil.matchPlayerID(columns.get(1), _master);
      double war = parseDouble(columns.get(11));
      int ipOuts = getIPOuts(columns.get(4));
      WarSummary WS = _sumPitch.get(M.playerID());
      if (WS == null) { _sumPitch.put(M.playerID(), WS = new WarSummary()); }
      WS.add(ipOuts, war);
      // find team
      Teams T = BBRefUtil.matchTeamID(columns.get(3), _teams, year);
      stmt.setString( 1, M.playerID());
      stmt.setInt(    2, year);
      stmt.setString( 3, M.bbrefID());
      stmt.setInt(    4, parseInt(columns.get(2))); // age
      stmt.setString( 5, T.teamID());
      stmt.setInt(    6, ipOuts); // IP
      stmt.setInt(    7, Integer.parseInt(columns.get(5))); // GS
      stmt.setInt(    8, Integer.parseInt(columns.get(6))); // R
      stmt.setInt(    9, Integer.parseInt(columns.get(7))); // Rrep
      stmt.setInt(   10, Integer.parseInt(columns.get(8))); // Rdef
      stmt.setDouble(11, parseDouble(columns.get(9))); // aLi
      stmt.setInt(   12, Integer.parseInt(columns.get(10))); // RAR
      stmt.setDouble(13, war); // rWAR
      stmt.setInt(   14, BBRefUtil.parseCurrency(columns.get(12))); // salary
      stmt.setString(15, columns.get(13)); // acquired
      stmt.executeUpdate();
      ++i;
    }
    System.out.println(i + " found");
  }

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

  private static final String NON_PITCHERS = "23456789OD";
  
  private static final String INSERT_BATTER =
    "INSERT INTO bbrefValueBat VALUES " +
       "(?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?," +
       " ?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?," + " ?, ?)";
  public void ingestBatters(MyDatabase db, int year) throws Exception {
    System.out.print("Ingesting batters from " + year + " : "); System.out.flush();
    StringBuilder SB = _fetcher.getPage("batting", year);

    Parser P = new Parser(SB);
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
      if (columns.size() < 20) { continue; }
      Master M = BBRefUtil.matchPlayerID(columns.get(1), _master);
      stmt.setString(1, M.playerID());
      // find team
      Teams T = BBRefUtil.matchTeamID(columns.get(3), _teams, year);
      // compute summary info
      int pa = Integer.parseInt(columns.get(4));
      double war = parseDouble(columns.get(13));
      WarSummary WS = _sumBat.get(M.playerID());
      if (WS == null) { _sumBat.put(M.playerID(), WS = new WarSummary()); }
      WS.add(pa, war);
      // compute isPitcher
      String possumm = columns.get(19);
      boolean isPitcher = false;
      for (int i_ch = 0, e_ch = possumm.length(); i_ch != e_ch; ++i_ch) {
        char ch = possumm.charAt(i_ch);
        if (ch == '1') { isPitcher = true; break; }
        if (NON_PITCHERS.indexOf(ch) != -1) { break; }
      } // flag as non-pitcher, unless it's sure they're a pitcher
      stmt.setInt(2, year);
      stmt.setString(3, M.bbrefID());
      stmt.setInt(   4, parseInt(columns.get(2))); // age
      stmt.setString(5, T.teamID());
      stmt.setInt(   6, pa);
      for (int j = 7; j != 15; ++j) {
        stmt.setInt(j, Integer.parseInt(columns.get(j-2))); // PA thru RAR; happens to line up
      }
      stmt.setDouble(15, war); // rWAR
      stmt.setInt(   16, Integer.parseInt(columns.get(14))); // oRAR
      stmt.setDouble(17, parseDouble(columns.get(15))); // oWAR
      stmt.setDouble(18, parseDouble(columns.get(16))); // dWAR
      stmt.setInt(   19, BBRefUtil.parseCurrency(columns.get(17))); // salary
      stmt.setString(20, columns.get(18)); // acquired
      stmt.setString(21, possumm); // possumm
      stmt.setInt(   22, isPitcher ? 1 : 0); // isPitcher
      stmt.executeUpdate();
      ++i;
    }
    System.out.println(i + " found");
  }

  private static final String INSERT_TEAM_BATTING =
    "INSERT INTO bbrefValueTeamBat VALUES " +
       "(?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?," +
       " ?, ?, ?, ?, ?," + " ?)";
  public void ingestTeamBatting(MyDatabase db, int year) throws Exception {
    System.out.print("Ingesting team batting from " + year + " : "); System.out.flush();
    StringBuilder SB = _fetcher.getPage("batting", year);

    Parser P = new Parser(SB);
    BBRefUtil.filter(P, "teams_values_batting");
    int start = P.getStart();
    int end = P.getEnd();
    int i = 0;
    ArrayList<String> columns = null;
    CachedStatement stmt = db.prepare(INSERT_TEAM_BATTING);
    while (true) {
      P.setRange(start, end);
      if ((columns = BBRefUtil.getColumns(P)) == null) { break; }
      start = P.getEnd();
      if (columns.size() < 15) { continue; }
      Teams T = BBRefUtil.matchTeamID(columns.get(0), _teams, year);
      stmt.setString(1, T.teamID());
      stmt.setInt(2, year);
      // compute summary info
      WarTeam WT = _sumTeam.get(T.teamID() + year);
      if (WT == null) { _sumTeam.put(T.teamID() + year, WT = new WarTeam(T.teamID(), year)); }
      WT._pa += Integer.parseInt(columns.get(1));
      WT._war += Double.parseDouble(columns.get(10));
      WT._salary += BBRefUtil.parseCurrency(columns.get(14));
      for (int j = 1; j != 10; ++j) {
        stmt.setInt(j+2, Integer.parseInt(columns.get(j))); // PA thru RAR;
      }
      stmt.setDouble(12, Double.parseDouble(columns.get(10))); // WAR
      stmt.setInt(13, Integer.parseInt(columns.get(11))); // oRAR
      stmt.setDouble(14, parseDouble(columns.get(12))); // oWAR
      stmt.setDouble(15, parseDouble(columns.get(13))); // dWAR
      stmt.setInt(16, BBRefUtil.parseCurrency(columns.get(14))); // salary
      stmt.executeUpdate();
      ++i;
    }
    System.out.println(i + " found");
  }

  private static int getIPOuts(String ip) {
    String[] ipParts = ip.split("\\.");
    return Integer.parseInt(ipParts[0])*3 + Integer.parseInt(ipParts[1]);
  }
  
  private static final String INSERT_TEAM_PITCHING =
    "INSERT INTO bbrefValueTeamPitch VALUES " +
       "(?, ?, ?, ?, ?," + " ?, ?, ?, ?, ?," +
       " ?)";
  public void ingestTeamPitching(MyDatabase db, int year) throws Exception {
    System.out.print("Ingesting team pitching from " + year + " : "); System.out.flush();
    StringBuilder SB = _fetcher.getPage("pitching", year);

    Parser P = new Parser(SB);
    BBRefUtil.filter(P, "teams_value_pitching");
    int start = P.getStart();
    int end = P.getEnd();
    int i = 0;
    ArrayList<String> columns = null;
    CachedStatement stmt = db.prepare(INSERT_TEAM_PITCHING);
    while (true) {
      P.setRange(start, end);
      if ((columns = BBRefUtil.getColumns(P)) == null) { break; }
      start = P.getEnd();
      if (columns.size() < 10) { continue; }
      Teams T = BBRefUtil.matchTeamID(columns.get(0), _teams, year);
      stmt.setString(1, T.teamID());
      stmt.setInt(2, year);
      int ipOuts = getIPOuts(columns.get(1));
      // compute summary info
      WarTeam WT = _sumTeam.get(T.teamID() + year);
      if (WT == null) { _sumTeam.put(T.teamID() + year, WT = new WarTeam(T.teamID(), year)); }
      WT._ipOuts += ipOuts;
      WT._GS += Integer.parseInt(columns.get(2));
      WT._war += Double.parseDouble(columns.get(8));
      WT._salary += BBRefUtil.parseCurrency(columns.get(9));
      stmt.setInt(3, ipOuts);
      stmt.setInt(    4, Integer.parseInt(columns.get(2))); // GS
      stmt.setInt(    5, Integer.parseInt(columns.get(3))); // R
      stmt.setInt(    6, Integer.parseInt(columns.get(4))); // Rrep
      stmt.setInt(    7, Integer.parseInt(columns.get(5))); // Rdef
      stmt.setDouble( 8, parseDouble(columns.get(6))); // aLi
      stmt.setInt(    9, Integer.parseInt(columns.get(7))); // Rdef
      stmt.setDouble(10, parseDouble(columns.get(8))); // rWAR
      stmt.setInt(   11, BBRefUtil.parseCurrency(columns.get(9))); // salary
      stmt.executeUpdate();
      ++i;
    }
    System.out.println(i + " found");
  }

  private Fetcher _fetcher = null;
  private Master.Table _master = null;
  private Teams.Table _teams = null;

  public Crawl(String dir) throws Exception {
    try (MyDatabase db = new MyDatabase()) {
      _master = new Master.Table(db, Sort.UNSORTED);
      _teams = new Teams.Table(db);
    }
    File F = new File(dir);
    F.mkdirs();
    dir = F.getCanonicalPath();
    _fetcher = new Fetcher("http://www.baseball-reference.com/leagues/MLB/%2$d-value-%1$s.shtml", dir + "/bbref-%1$s-%2$d.html");
  }
  
  private static final String CACHE = "C:/build/mlbstats/cache/bbref";
  private static final int YEAR_START = 1876;
  private static final int YEAR_END = 2011;
  
  public static void main(String[] args) throws Exception {
    
    Crawl C = new Crawl(CACHE);
    int yearstart = YEAR_START - 1;
    int yearend = YEAR_END;
    try (MyDatabase db = new MyDatabase()) {
      for (int year = yearend; year != yearstart; --year) {
        C.ingestPitchers(db, year);
        C.ingestBatters(db, year);
        C.ingestTeamPitching(db, year);
        C.ingestTeamBatting(db, year);
      }
      C.ingestSummaryPitch(db);
      C.ingestSummaryBat(db);
      C.ingestSummaryTeam(db);
    }
  }
}
