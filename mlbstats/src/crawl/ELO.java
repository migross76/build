package crawl;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import util.MyDatabase;
import data.Master;
import data.Sort;
import db.CachedStatement;

public class ELO {

  private static Pattern PLAY_HREF = Pattern.compile("<a href=\"/players/[a-z]/([^>]*)\\.shtml\">");
  private static Pattern MGR_HREF = Pattern.compile("<a href=\"/managers/([^>]*)\\.shtml\">");
  private static Pattern UPDATE_PATT = Pattern.compile(" csk=\"([-0-9]+? [0-9:]+?)\">");
  
  /*package*/ static final String CODE_BATTER  = "ratings0";
  /*package*/ static final String CODE_PITCHER = "ratings1";
  
  private static final String INSERT_ELO = "INSERT INTO rELO VALUES (?, ?, ?, ?, ?, ?)";
  public void ingest(MyDatabase db, Date date, String id) throws Exception {
    System.out.format("Ingesting ELO on %tY-%<tm-%<td : ", date); System.out.flush();
    CachedStatement stmt = db.prepare(INSERT_ELO);
    StringBuilder SB = _fetcher.getPage(date);
    boolean isPitcher = id.equals(CODE_PITCHER);

    Parser P = new Parser(SB);
    if (!P.filter("<table class=\"sortable  suppress_pre suppress_link suppress_more stats_table\" id=\"" + id + "\">",
                  "<tbody>", "</tbody>")) {
      throw new Exception("Unable to find ELO section");
    }
    int start = P.getStart();
    int end = P.getEnd();
    int i = 0;
    while (true) {
      P.setRange(start, end);
      if (!P.filter("<tr", "</tr>")) { break; }
      ArrayList<String> columns = P.getColumns("<td", ">", "</td>");
      start = P.getEnd();
      if (columns.size() < 5) { continue; }
      Matcher match = PLAY_HREF.matcher(columns.get(1));
      if (!match.find()) {
        match = MGR_HREF.matcher(columns.get(1));
        if (!match.find()) { System.err.format("Cannot locate ID in string %s\n", columns.get(1)); }
      }
      String bbrefID = match.group(1);
      Master M = _master.bbrefID(bbrefID);
      if (M == null) { System.err.format("Can't find player with ID %s [%s]\n", bbrefID, columns.get(1)); continue; }
      int rating = Integer.parseInt(columns.get(2));
      int w = Integer.parseInt(columns.get(4));
      int l = Integer.parseInt(columns.get(5));
      Matcher uMatch = UPDATE_PATT.matcher(P.getCurrentString());
      String update = null;
      if (uMatch.find()) { update = uMatch.group(1); }
      
      stmt.setString(1, M.playerID());
      stmt.setInt(2, rating);
      stmt.setInt(3, w);
      stmt.setInt(4, l);
      stmt.setString(5, update);
      stmt.setBoolean(6, isPitcher);
      stmt.executeUpdate();
      ++i;
    }
    System.out.println(i + " found");
  }

  public ELO(String dir) throws Exception {
    File F = new File(dir);
    F.mkdirs();
    dir = F.getCanonicalPath();
    _fetcher = new Fetcher("http://www.baseball-reference.com/friv/ratings.cgi", dir + "/ELO-%tY-%<tm-%<td.html");
    try (MyDatabase db = new MyDatabase()) {
      _master = new Master.Table(db, Sort.UNSORTED);
    }
  }
  
  private Master.Table _master = null;
  private Fetcher _fetcher = null;

  public static void main(String[] args) throws Exception {
    ELO elo = new ELO("C:/build/mlbstats/cache/elo");
    Date D = new Date();
    try (MyDatabase db = new MyDatabase()) {
      elo.ingest(db, D, CODE_BATTER);
      elo.ingest(db, D, CODE_PITCHER);
    }
  }
}
