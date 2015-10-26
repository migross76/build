package crawl;

import java.io.File;
import java.util.Date;
import util.MyDatabase;

// * Download CSV Lahman database from http://www.seanlahman.com/baseball-archive/statistics/ to C:/build/mlbstats/cache/
// * Unzip and reset LAHMAN_PATH
// * Read Lahman README and update tables-lahman.sql based on "What's New"
// * Download http://chadwick-bureau.com/data/bbdb/master.csv and update C:/build/mlbstats/cache/.../Master.csv with it
// * Update BBRefCSV MID_YEAR value, if there is WAR data that doesn't line up with Lahman data
// * Rename bbref-csv-bat.txt and bbref-csv-pitch.txt to force a download of a newer version
// * Run program
// * Verify results with:
//   - select m.nameFirst, m.nameLast, m.debut, w.war from master m, rbwar w where w.yearID = YEAR and m.playerID = w.playerID and w.war > 4 order by debut;
//   - select m.nameFirst, m.nameLast, m.birthYear, r.rating, r.ispitcher from relo r, master m where m.playerID = r.playerID and m.birthYear > 1980 order by r.rating desc limit 50;
// * Update tables-lahman with new table names to delete
//
// TODO Schedule should record T (tie) and "*-wo" (walk-offs)
// TODO Automatically download/unzip Lahman database (may need to supply a date)
// TODO Automatically download/run Master.csv
// TODO Figure out a way to record CSV date, so it can get a new file on-demand
// 
// ! If something fails midway through, comment out the completed pieces, make adjustments, and try again
public class LoadAll {
  private static final String LAHMAN_PATH = "C:/build/mlbstats/cache/lahman-csv_2015-01-24";
  
  public static void main(String[] args) throws Exception {
    try (MyDatabase db = new MyDatabase()) {
      // load SQL schema(s)
      //LoadSchema.load(new File("C:/build/mlbstats/sql/tables-lahman.sql"));
      //LoadSchema.load(new File("C:/build/mlbstats/sql/tables-bbref.sql"));
      // load Lahman CSV files
      //ImportCSV.load(db, new File(LAHMAN_PATH));
      // crawl BBRef WAR files
      //BBRefCSV C = new BBRefCSV(BBRefCSV.CACHE);
      //C.fetch(db, "bat", "rbWar", BBRefCSV.BAT_FIELDS);
      //C.fetch(db, "pitch", "rpWar", BBRefCSV.PITCH_FIELDS);
      // crawl ELO files
      //ELO elo = new ELO("C:/build/mlbstats/cache/elo");
      //Date D = new Date();
      //elo.ingest(db, D, ELO.CODE_BATTER);
      //elo.ingest(db, D, ELO.CODE_PITCHER);
      // crawl schedule files
      new Schedule(db, BBRefCSV.CACHE).execute(db);
    }
  }
}
