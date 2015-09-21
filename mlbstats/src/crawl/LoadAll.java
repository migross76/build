package crawl;

import java.io.File;
import java.util.Date;
import util.MyDatabase;

public class LoadAll {
  public static void main(String[] args) throws Exception {
    try (MyDatabase db = new MyDatabase()) {
      // load SQL schema(s)
      LoadSchema.load(new File("C:/build/mlbstats/sql/tables-lahman.sql"));
      LoadSchema.load(new File("C:/build/mlbstats/sql/tables-bbref.sql"));
      // load Lahman CSV files
      ImportCSV.load(db, new File("C:/build/lahman-csv_2014-02-14"));
      // crawl BBRef WAR files
      BBRefCSV C = new BBRefCSV(BBRefCSV.CACHE);
      C.fetch(db, "bat", "rbWar", BBRefCSV.BAT_FIELDS);
      C.fetch(db, "pitch", "rpWar", BBRefCSV.PITCH_FIELDS);
      // crawl ELO files
      ELO elo = new ELO("C:/build/mlbstats/cache/elo");
      Date D = new Date();
      elo.ingest(db, D, ELO.CODE_BATTER);
      elo.ingest(db, D, ELO.CODE_PITCHER);
      // crawl schedule files
      new Schedule(db, BBRefCSV.CACHE).execute(db);
    }
  }
}
