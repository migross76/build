package topactors.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import topactors.shared.Gender;
import topactors.shared.Oscar;
import db.CachedStatement;

public class DataStore implements InitHandler {

  private class ActorStore {
    private static final String SELECT =
      "SELECT imdb_id, name, gender, processed FROM actor";
    private static final String UPDATE =
      "UPDATE actor SET name=?, gender=?, processed=?, sat=? WHERE imdb_id=?";
    private static final String INSERT =
      "INSERT actor (imdb_id, name, gender, processed, sat) VALUES (?,?,?,?,?)";
    
    public void load(MyDatabase db, Data D) throws SQLException {
      HashSet<String> processed = new HashSet<String>();
      // Actors
      CachedStatement sActor = db.prepare(SELECT);
      try (ResultSet RS = sActor.executeQuery()) {
        while (RS.next()) {
          Actor A = new Actor();
          A._id = RS.getString(1);
          A._name = RS.getString(2);
          A._gender = Gender.valueOf(RS.getString(3));
          if (RS.getBoolean(4)) { processed.add(A._id); }
          D.add(A);
          _store.put(A._id, A);
        }
      }
      findProcessDates(db, "actor_rate", _store, processed);
    }
    
    public void update(MyDatabase db, Data D) throws SQLException {
      // Actors
      CachedStatement iActor = db.prepare(INSERT);
      CachedStatement uActor = db.prepare(UPDATE);
      for (Actor A : D._actors) {
        int ofs = 0;
        boolean update = _store.containsKey(A._id);
        CachedStatement PS = update ? uActor : iActor;
        if (!update) {
          PS.setString(++ofs, A._id);
          _store.put(A._id, A);
        }
        PS.setString(++ofs, A._name);
        PS.setString(++ofs, A._gender.toString());
        PS.setBoolean(++ofs, A._processed != null);
        PS.setDouble(++ofs, A._sat);
        if (update) { PS.setString(++ofs, A._id); }
        PS.executeUpdate();
      }
    }
    
    public Actor get(String id) { return _store.get(id); }

    private HashMap<String, Actor> _store = new HashMap<String, Actor>();
  }
  
  private class MovieStore {
    private static final String SELECT =
      "SELECT imdb_id, name, year, votes, rating, score, oscar, processed FROM movie";
    private static final String UPDATE =
      "UPDATE movie SET name=?, year=?, votes=?, rating=?, score=?, oscar=?, processed=?, sat=? WHERE imdb_id=?";
    private static final String INSERT =
      "INSERT movie (imdb_id, name, year, votes, rating, score, oscar, processed, sat) VALUES (?,?,?,?,?,?,?,?,?)";
    public void load(MyDatabase db, Data D) throws SQLException {
      HashSet<String> processed = new HashSet<String>();
      CachedStatement sMovie = db.prepare(SELECT);
      try (ResultSet RS = sMovie.executeQuery()) {
        while (RS.next()) {
          Movie M = new Movie();
          M._id = RS.getString(1);
          M._name = RS.getString(2);
          M._year = RS.getInt(3);
          M._votes = RS.getInt(4);
          M._rating = RS.getDouble(5);
          M._score = RS.getDouble(6);
          M._oscar = Oscar.valueOf(RS.getString(7));
          if (RS.getBoolean(8)) { processed.add(M._id); }
          D.add(M);
          _store.put(M._id, M);
        }
      }
      findProcessDates(db, "movie_main", _store, processed);
    }

    public void update(MyDatabase db, Data D) throws SQLException {
      CachedStatement iMovie = db.prepare(INSERT);
      CachedStatement uMovie = db.prepare(UPDATE);
      for (Movie M : D._movies) {
        int ofs = 0;
        boolean update = _store.containsKey(M._id);
        CachedStatement PS = update ? uMovie : iMovie;
        if (!update) {
          PS.setString(++ofs, M._id);
          _store.put(M._id, M);
        }
        PS.setString(++ofs, M._name);
        PS.setInt(++ofs, M._year);
        PS.setInt(++ofs, M._votes);
        PS.setDouble(++ofs, M._rating);
        PS.setDouble(++ofs, M._score);
        PS.setString(++ofs, M._oscar.toString());
        PS.setBoolean(++ofs, M._processed != null);
        PS.setDouble(++ofs, M._sat);
        if (update) { PS.setString(++ofs, M._id); }
        PS.executeUpdate();
      }
    }
      
    public Movie get(String id) { return _store.get(id); }

    private HashMap<String, Movie> _store = new HashMap<String, Movie>();
  }
  
  private class RoleStore {
    private static final String SELECT =
      "SELECT actor_id, movie_id, imdb_id, name, role_type, level, oscar, voice FROM role";
    private static final String UPDATE =
      "UPDATE role SET imdb_id=?, name=?, role_type=?, level=?, oscar=?, voice=?, series=?, sat=? WHERE actor_id=? AND movie_id=?";
    private static final String INSERT =
      "INSERT role (actor_id, movie_id, imdb_id, name, role_type, level, oscar, voice, series, sat) VALUES (?,?,?,?,?,?,?,?,?,?)";
    public void load(MyDatabase db, Data D) throws SQLException {
      CachedStatement sRole = db.prepare(SELECT);
      try (ResultSet RS = sRole.executeQuery()) {
        while (RS.next()) {
          Role R = new Role();
          R._actor = _actors.get(RS.getString(1));
          R._movie = _movies.get(RS.getString(2));
          if (R._actor == null || R._movie == null) {
            System.err.println("Unable to find full role"); continue;
          }
          R._id = RS.getString(3);
          R._name = RS.getString(4);
          R._type = Role.Type.valueOf(RS.getString(5));
          R._level = Role.Level.valueOf(RS.getString(6));
          R._oscar = Oscar.valueOf(RS.getString(7));
          R._voice = RS.getBoolean(8);
          D.add(R);
          _store.put(R.getUniqueID(), R);
        }
      }
    }

    public void update(MyDatabase db, Data D) throws SQLException {
      CachedStatement iRole = db.prepare(INSERT);
      CachedStatement uRole = db.prepare(UPDATE);
      for (Role R : D._roles) {
        int ofs = 0;
        boolean update = _store.containsKey(R.getUniqueID());
        CachedStatement PS = update ? uRole : iRole;
        if (!update) {
          PS.setString(++ofs, R._actor._id);
          PS.setString(++ofs, R._movie._id);
          _store.put(R.getUniqueID(), R);
        }
        PS.setString(++ofs, R._id);
        PS.setString(++ofs, R._name);
        PS.setString(++ofs, R._type.toString());
        PS.setString(++ofs, R._level.toString());
        PS.setString(++ofs, R._oscar.toString());
        PS.setBoolean(++ofs, R._voice);
        PS.setInt(++ofs, R._series);
        PS.setDouble(++ofs, R._sat_series);
        if (update) {
          PS.setString(++ofs, R._actor._id);
          PS.setString(++ofs, R._movie._id);
        }
        PS.executeUpdate();
      }
    }
      
    private HashMap<String, Role>  _store  = new HashMap<String, Role>(); // actor_id|movie_id
  }

  private static final String CACHE_SELECT =
    "SELECT imdb_id, process_date FROM cache WHERE file_type = ?";
  public DataStore(MyDatabase database) {
    _database = database;
  }

  private static <T extends Processable> void findProcessDates(MyDatabase db, String file_type, HashMap<String, T> map, HashSet<String> processed) throws SQLException {
    CachedStatement sCache = db.prepare(CACHE_SELECT);
    sCache.setString(1, file_type);
    try (ResultSet RS = sCache.executeQuery()) {
      while (RS.next()) {
        String id = RS.getString(1);
        if (!processed.contains(id)) { continue; }
        map.get(id)._processed = RS.getDate(2); // if processed contains the id, then the map must as well
      }
    }
  }
  
  @Override
  public Data init() {
    Data D = new Data();
    try {
      _actors.load(_database, D);
      _movies.load(_database, D);
      _roles.load(_database, D);
    } catch (SQLException e) {
      System.err.println("Unable to load from database");
      e.printStackTrace();
    }
    return D;
  }
  
  public void update(Data D) {
    try {
      _actors.update(_database, D);
      _movies.update(_database, D);
      _roles.update(_database, D);
    } catch (SQLException e) {
      System.err.println("Unable to update database");
      e.printStackTrace();
    }
  }

  private final MyDatabase _database;
  private ActorStore _actors = new ActorStore();
  private MovieStore _movies = new MovieStore();
  private RoleStore  _roles  = new RoleStore();
}
