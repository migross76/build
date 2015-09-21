package topactors.server;
import java.sql.SQLException;
import db.Database;

public class MyDatabase extends Database {
  public MyDatabase() throws SQLException { super("topactors"); }
}
