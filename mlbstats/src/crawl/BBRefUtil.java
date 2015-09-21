package crawl;

import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import data.Master;
import data.Teams;

public class BBRefUtil {
  public static void filter(Parser P, String id) throws IOException {
    if (!P.filter("<table class=\"sortable  stats_table\" id=\"" + id + "\">",
        "<tbody>", "</tbody>")) {
      throw new IOException("Unable to find " + id);
    }
  }
  
  public static ArrayList<String> getColumns(Parser P) {
    if (!P.filter("<tr", "</tr>")) { return null; }
    return P.getColumns("<td", ">", "</td>");
  }
  
  private static Pattern HREF_PLAYER = Pattern.compile("<a href=\"/players/[a-z]/([^>]*)\\.shtml\">");
  public static Master matchPlayerID(String column, Master.Table MT) {
    Matcher match = HREF_PLAYER.matcher(column);
    if (!match.find()) { System.err.println("Unable to match player ID for : " + column); return null; }
    Master M = MT.bbrefID(match.group(1));
    if (M == null) { System.err.println("Unable to find master record for : " + match.group(1)); }
    return M;
  }
  
  private static Pattern HREF_TEAM = Pattern.compile("<a href=\"/teams/([A-Z]+)/[^>]*\\.shtml\"");
  public static Teams matchTeamID(String column, Teams.Table TT, int year) {
    Matcher M = HREF_TEAM.matcher(column);
    if (!M.find()) { return null; }
    String bbrefTeamID = M.group(1);
    return TT.team(year, bbrefTeamID);
  }
  
  private static Pattern HREF_STANDING = Pattern.compile("<a href=\"/games/standings.cgi\\?date=([0-9-]+)\"");
  public static String matchStanding(String column) {
    Matcher M = HREF_STANDING.matcher(column);
    return M.find() ? M.group(1) : null;
  }

  private static NumberFormat CURRENCY_FMT = NumberFormat.getCurrencyInstance();
  public static int parseCurrency(String value) {
    try {
      return CURRENCY_FMT.parse(value).intValue();
    } catch (ParseException e) {
      return -1;
    }
  }
  
}
