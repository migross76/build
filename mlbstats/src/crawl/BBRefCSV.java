package crawl;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import util.MyDatabase;
import data.Master;
import data.Sort;
import data.Teams;
import db.CachedStatement;

/*
Ingesting rbWar
Unknown ID : harriwi10 : Will Harris,27,harriwi10,2012,COL,1,NL,0,20,17.7,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.10,0.00,0.00,0.1,0.1,0.1,0.1,0.01,0.01,0.01,0.01,0.01,0.01,0.00,NULL,Y,4.29447,4.28947,0.07895,4.28947,1.845,1.845,0.5005,0.5005,0.5005,0.5000
Ingesting rpWar
Unknown ID : harriwi10 : Will Harris,27,harriwi10,2012,COL,1,NL,20,0,53,0,53,18,8.508,-0.634,-1.102,120,118.247,10.614,63,0.0136,-81.000,-7.386,-7.531,-5.958,0.186,0.7480,-0.6,NULL,4.28947,4.66602,1.868,0.4608,-0.7840,0.0654,4.35980,1.849,0.4925,0.1558
 */
public class BBRefCSV {

  private static class BadLineException extends IllegalArgumentException { 
    private static final long serialVersionUID = -5545276268861912689L;
    BadLineException(String message) { super(message); }
  }
  
  private static final int MID_YEAR = 2015;
  
  private final Fetcher      _fetcher;
  private final Master.Table _master;
  private final Teams.Table  _teams;

  public void fetch(MyDatabase db, String page, String table, String fields) throws IOException, SQLException {
    System.out.println("Ingesting " + table); System.out.flush();
    StringBuilder SB = _fetcher.getPage(page);
    String[] lines = SB.toString().split("\n");
    StringBuilder SQL = new StringBuilder("INSERT INTO ");
    SQL.append(table).append(" VALUES (");
    boolean first = true;
    for (int i = 0; i != fields.length(); ++i) {
      if (fields.charAt(i) == '-') { continue; }
      if (!first) { SQL.append(", "); } else { first = false; }
      SQL.append("?");
    }
    CachedStatement PS = db.prepare(SQL.append(")").toString());
    for (String line : lines) {
      try {
        if (line.startsWith("name_")) { continue; }
        String[] cols = line.split(",");
        int insertCol = 0;
        int year = 0;
        PS.clearParameters();
        for (int c = 0; c != cols.length; ++c) {
          char code = fields.charAt(c);
          if (cols[c].equals("NULL") || cols[c].equals("")) {
            int sqlType = 0;
            switch (code) {
              case '-' : continue;
              case 'i' : sqlType = Types.INTEGER; break;
              case 'd' : sqlType = Types.DECIMAL; break;
              case 's' : sqlType = Types.CHAR; break;
              default: throw new BadLineException("Unexpected null field : " + code);
            }
            PS.setNull(++insertCol, sqlType);
            continue;
          }
          switch (code) {
            case '-' : continue;
            case 'i' : PS.setInt(++insertCol, Integer.parseInt(cols[c])); break;
            case 'd' : PS.setDouble(++insertCol, Double.parseDouble(cols[c])); break;
            case 's' : PS.setString(++insertCol, cols[c]); break;
            case 'Y' :
              PS.setInt(++insertCol, year = Integer.parseInt(cols[c]));
              if (year == MID_YEAR) { --year; }
              break;
            case 'P' :
              Master M = _master.bbrefID(cols[c]);
              if (M == null) { M = _master.byID(cols[c]); }
              if (M == null) { throw new BadLineException("Unknown ID : " + cols[c]); }
              PS.setString(++insertCol, M.playerID());
              break;
            case 'T' :
              String name = cols[c];
              Teams t = _teams.team(year, name);
              if (t == null) { throw new BadLineException("Cannot find team : " + name + " @ " + year); }
              PS.setString(++insertCol, t.teamID());
              break;
            default: throw new IllegalArgumentException("Unknown code : " + code);
          }
        }
        PS.executeUpdate();
      } catch (BadLineException T) {
        System.err.println(T.getMessage() + " : " + line); System.err.flush();
      } catch (Throwable T) {
        System.err.println(line);
        T.printStackTrace(System.err);
      }
    }
    
  }
  
  public BBRefCSV(String dir) throws Exception {
    try (MyDatabase db = new MyDatabase()) {
      _master = new Master.Table(db, Sort.UNSORTED);
      _teams = new Teams.Table(db);
    }
    File F = new File(dir);
    F.mkdirs();
    dir = F.getCanonicalPath();
    _fetcher = new Fetcher("http://www.baseball-reference.com/data/war_daily_%1$s.txt", dir + "/bbref-csv-%1$s.txt");
  }

  /*package*/ static final String CACHE = "C:/build/mlbstats/cache/bbref";

  /* name_common,age,player_ID,year_ID,team_ID,stint_ID,
   * lg_ID,PA,G,Inn,runs_bat,
   * runs_br,runs_dp,runs_field,runs_infield,runs_outfield,
   * runs_catcher,runs_good_plays,runs_defense,runs_position,runs_position_p,
   * runs_replacement,runs_above_rep,runs_above_avg,runs_above_avg_off,runs_above_avg_def,
   * WAA,WAA_off,WAA_def,WAR,WAR_def,
   * WAR_off,WAR_rep,salary,pitcher,teamRpG,
   * oppRpG,oppRpPA_rep,oppRpG_rep,pyth_exponent,pyth_exponent_rep,
   * waa_win_perc,waa_win_perc_off,waa_win_perc_def,waa_win_perc_rep
   */

  /* Ben Zobrist,35,zobribe01,2011,TBR,1,
   * AL,674,156,1375.0,25.9,
   * 2.6,1.6,27.0,1.0,0.0,
   * 0.0,1.0,29.0,0.5,0.0,
   * 22.9,82.5,59.6,30.6,29.5,
   * 6.41,3.42,3.23,8.5,3.2,
   * 5.5,2.07,4687300,N,4.63277,
   * 4.43662,0.07946,4.09331,1.875,1.842,
   * 0.5400,0.5203,0.5201,0.5370
   */
  
  /*package*/ static final String BAT_FIELDS = "-iPYTi" + "siidd" + "ddddd" + "ddddd" + "ddddd" + "ddddd" + "ddisd" + "ddddd" + "dddd";

  /* name_common,age,player_ID,year_ID,team_ID,stint_ID,
   * lg_ID,G,GS,IPouts,IPouts_start,
   * IPouts_relief,RA,xRA,xRA_sprp_adj,xRA_def_pitcher,
   * PPF,PPF_custom,xRA_final,BIP,BIP_perc,
   * RS_def_total,runs_above_avg,runs_above_avg_adj,runs_above_rep,RpO_replacement,
   * GR_leverage_index_avg,WAR,salary,teamRpG,oppRpG,
   * pyth_exponent,waa_win_perc,WAA,WAA_adj,oppRpG_rep,
   * pyth_exponent_rep,waa_win_perc_rep,WAR_rep
   */
  
  /* Walter Johnson,35,johnswa01,1911,WSH,1,  //name
   * AL,40,37,967,NULL,  // lg_id
   * NULL,119,167.871,0.000,-4.568,  // IPouts_relief
   * 98,NULL,168.990,1021,0.2175,  // PPF
   * -21.000,49.990,49.751,86.537,0.210, // RS_def_total
   * 1.0000,8.2,7000,4.62115,3.37738,  // GR_leverage_index_avg
   * 1.809,0.6381,5.5240,-0.2616,5.54488, // pyth_exponent
   * 1.937,0.4127,2.9045 // pyth_exponent_rep
   */

  /*package*/ static final String PITCH_FIELDS = "-iPYTi" + "siiii" + "iiddd" + "iddid" + "ddddd" + "ddidd" + "ddddd" + "ddd";
  
  public static void main(String[] args) throws Exception {
    BBRefCSV C = new BBRefCSV(CACHE);
    try (MyDatabase db = new MyDatabase()) {
      C.fetch(db, "bat", "rbWar", BAT_FIELDS);
      C.fetch(db, "pitch", "rpWar", PITCH_FIELDS);
    }
  }
}
