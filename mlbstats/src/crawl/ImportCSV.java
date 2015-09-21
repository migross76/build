package crawl;

import java.io.File;
import java.io.FileFilter;
import java.sql.ResultSet;
import java.sql.SQLException;
import util.MyDatabase;
import db.CachedStatement;

public class ImportCSV {
  private static final String DIR = "C:/build/lahman-csv_2014-02-14";
  
  private static final String SQL =
    "LOAD DATA LOCAL INFILE '%s' INTO TABLE %s" +
    " FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '\"'" +
    " LINES TERMINATED BY '\r\n' IGNORE 1 LINES";
  private static final String SQL_COUNT = "SELECT COUNT(*) FROM %s";
  
  public static void load(MyDatabase db, File F, String table) throws SQLException {
    String S = String.format(SQL, F.getAbsolutePath().replaceAll("\\\\", "/"), table);
    CachedStatement PS = db.prepare(S);
    PS.execute();
  }
  
  public static void load(MyDatabase db, File dir) throws SQLException {
    FileFilter FF = new FileFilter() {
      @Override public boolean accept(File arg0) {
        return (arg0.isFile() && arg0.getName().endsWith(".csv")); 
      }
    };
    for (File F : dir.listFiles(FF)) {
      String table_name = F.getName().replaceAll(".csv", "");
      System.out.print("Importing " + table_name + " : ");
      load(db, F, table_name);
      CachedStatement ps = db.prepare(String.format(SQL_COUNT, table_name));
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) { System.out.println(rs.getInt(1) + " records"); }
      }
    }
    System.out.println("\nComplete");
  }
  
  public static void main(String[] args) throws Exception {
    File dir = new File(DIR);
    FileFilter FF = new FileFilter() {
      @Override public boolean accept(File arg0) {
        return (arg0.isFile() && arg0.getName().endsWith(".csv")); 
      }
    };
    try (MyDatabase db = new MyDatabase()) {
      for (File F : dir.listFiles(FF)) {
        System.out.println("Importing " + F.getName());
        load(db, F, F.getName().replaceAll(".csv", ""));
      }
    }
    System.out.println("\nComplete");
  }
}
