package strat.server;

import db.Database;
import java.sql.SQLException;

public class MyDatabase extends Database {
  public MyDatabase() throws SQLException {
    super("mlbstats");
  }
}
