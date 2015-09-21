package topactors.server;

import java.io.IOException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import db.CachedStatement;

public class Repository implements Cache {  
  public void setDelay(long delay) { _fetchDelay = delay; }

  private static final String GET_CACHE = "SELECT data FROM cache WHERE file_type = ? AND imdb_id = ?";
  private static final String PUT_CACHE = "INSERT INTO cache (file_type, imdb_id, data, process_date) VALUES (?, ?, ?, ?)";
  @Override
  public StringBuilder fetch(String type, String id, String url) throws IOException {
    try {
      CachedStatement get_cache = _database.prepare(GET_CACHE);
      get_cache.setString(1, type);
      get_cache.setString(2, id);
      try (ResultSet RS = get_cache.executeQuery()) {
        if (RS.next()) { return new StringBuilder(RS.getString(1)); }
      }
    } catch (SQLException e) { throw new IOException("Database query issue", e); }
    // else; new page
    System.out.println("*** Downloading " + type + " : " + id);
    long current = Calendar.getInstance().getTimeInMillis();
    long wait = _lastFetch + _fetchDelay - current;
    _lastFetch = current;
    try {
      if (wait > 0) { Thread.sleep(wait); }
    } catch (InterruptedException e) { e.printStackTrace(); }
    StringBuilder page = Fetcher.getPage(url);
    try {
      CachedStatement put_cache = _database.prepare(PUT_CACHE);
      put_cache.setString(1, type);
      put_cache.setString(2, id);
      put_cache.setString(3, page.toString());
      put_cache.setDate(4, new Date(current));
      put_cache.executeUpdate();
    } catch (SQLException e) { throw new IOException("Database update issue", e); }
    return page;
  }
  
  public Repository(MyDatabase database) {
    _database = database;
  }
  
  private final MyDatabase _database;
  private long _lastFetch = 0;
  private long _fetchDelay = 5000;
}
