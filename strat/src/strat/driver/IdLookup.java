package strat.driver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.ResultSet;
import strat.server.MyDatabase;
import db.CachedStatement;

public class IdLookup {
  public static final String filename = "C:/build/strat/heroes-list.txt";
  
  public static void main(String[] args) throws Throwable {
    try (MyDatabase db = new MyDatabase()) {
      CachedStatement ps = db.prepare("SELECT playerID FROM master M WHERE nameFirst = ? AND nameLast = ?");
      try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
        String name = null;
        while ((name = br.readLine()) != null) {
          if (name.isEmpty()) { continue; }
          int comma = name.lastIndexOf(", ");
          String first = name.substring(comma + 2);
          String last = name.substring(0, comma);
          ps.setString(1, first);
          ps.setString(2, last);
          System.out.print(name);
          try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              System.out.format("\t%s", rs.getString(1));
            }
          }
          System.out.println();
        }
      }
    }
  }
}
