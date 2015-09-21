package data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import util.ByPlayer;
import db.CachedStatement;
import util.MyDatabase;

//yearID     | smallint(4)
//teamID     | char(3)
//lgID       | char(2)
//playerID   | char(9)
//experience | tinyint(2) unsigned
//G_all      | tinyint(3) unsigned
//GS         | tinyint(3) unsigned
//G_batting  | tinyint(3) unsigned
//G_defense  | tinyint(3) unsigned
//G_p        | tinyint(3) unsigned
//G_c        | tinyint(3) unsigned
//G_1b       | tinyint(3) unsigned
//G_2b       | tinyint(3) unsigned
//G_3b       | tinyint(3) unsigned
//G_ss       | tinyint(3) unsigned
//G_lf       | tinyint(3) unsigned
//G_cf       | tinyint(3) unsigned
//G_rf       | tinyint(3) unsigned
//G_of       | tinyint(3) unsigned
//G_dh       | tinyint(3) unsigned
//G_ph       | tinyint(3) unsigned
//G_pr       | tinyint(3) unsigned

//PITCHING
//yearID   | smallint(4) unsigned
//teamID   | char(3)
//lgID     | char(2)
//playerID | varchar(9)
//G        | smallint(3) unsigned
//GS       | smallint(3) unsigned
//GF       | smallint(3) unsigned
public class Appearances implements Groupable<Appearances>, Iterable<Appearances.Use> {

  public String   playerID() { return _playerID; }
  public String   teamID()   { return _teamID; }
  public int      yearID()   { return _yearID; }
  
  public double   games(Position pos) {
    for (Use P : _use) { if (P._pos == pos) { return P._g; } }
    return 0;
  }
  public Use primary() { return _use.isEmpty() ? null : _use.get(0); }
  @Override public Iterator<Use> iterator() { return _use.iterator(); }
  
  public void add(Position pos, double g) {
    if (g == 0) { return; }
    for (Use P : _use) { if (P._pos == pos) { P._g += g; return; } }
    _use.add(new Use(pos, g));
  }
  public void sort() { Collections.sort(_use); }
  
  @Override
  public void add(Appearances t) {
    for (Use P : t) { add(P.pos(), P.games()); }
    sort();
  }
  @Override public Appearances create() {
    return new Appearances(_playerID, _teamID, _yearID);
  }


  public Appearances(String playerID, String teamID, int yearID) {
    _playerID = playerID;
    _teamID = teamID;
    _yearID = yearID;
  }

  private String   _playerID = null;
  private String   _teamID = null;
  private int      _yearID = 0;
  private List<Use> _use = new ArrayList<>();
  
  public static class Use implements Comparable<Use> {
    public Position pos()      { return _pos; }
    public double   games()    { return _g; }
    
    private Use(Position pos, double g) { _pos = pos; _g = g; }

    private Position _pos = null;
    private double   _g = 0;
    @Override
    public int compareTo(Use arg0) {
      if (_g != arg0._g) { return _g > arg0._g ? -1 : 1; }
      return _pos.compareTo(arg0._pos);
    }
  }
  
  public static Comparator<Appearances> groupID = new Comparator<Appearances>() {
    @Override public int compare(Appearances o1, Appearances o2) { return o1._playerID.compareTo(o2._playerID); }
  };
  public static Comparator<Appearances> groupYear = new Comparator<Appearances>() {
    @Override public int compare(Appearances o1, Appearances o2) { return o2._yearID - o1._yearID; }
  };
  public static Comparator<Appearances> groupYearTeam = new Comparator<Appearances>() {
    @Override public int compare(Appearances o1, Appearances o2) {
      if (o2._yearID != o1._yearID) { return o2._yearID - o1._yearID; }
      return o1._teamID.compareTo(o2._teamID);
    }
  };
  private static ByPlayer.GroupKey<String, Appearances> BY_ID = new ByPlayer.GroupKey<String, Appearances>() {
    @Override public String groupBy(Appearances V) { return V.playerID(); }
  };
  private static ByPlayer.GroupKey<Integer, Appearances> BY_YEAR = new ByPlayer.GroupKey<Integer, Appearances>() {
    @Override public Integer groupBy(Appearances V) { return V.yearID(); }
  };
  public static class ByID extends ByPlayer.Group<String, Appearances> {
    public ByID() { super(BY_ID, null, groupYear); }
  }
  public static class ByIDTeam extends ByPlayer.Group<String, Appearances> {
    public ByIDTeam() { super(BY_ID, null, groupYearTeam); }
  }
  public static class ByYear extends ByPlayer.Group<Integer, Appearances> {
    public ByYear() { super(BY_YEAR, null, groupID); }
  }

  public static class Table implements Iterable<Appearances> {

    @Override public Iterator<Appearances> iterator() { return _list.iterator(); } 
    
    private static final String SQL_BAT =
      "SELECT playerID, teamID, yearID, G_c, G_1b, G_2b, G_3b, G_ss, G_lf, G_cf, G_rf, G_dh FROM appearances";
    private void fetchBat(MyDatabase db) throws SQLException {
      CachedStatement PS = db.prepare(SQL_BAT);
      try (ResultSet RS = PS.executeQuery()) {
        while (RS.next()) {
          Appearances A = new Appearances(RS.getString(1), RS.getString(2), RS.getInt(3));
          int i = 4;
          for (Position pos : EnumSet.range(Position.CATCH, Position.DESIG)) {
            int g = RS.getInt(i++);
            A.add(pos, g);
          }
          _list.add(A);
        }
      }
    }

    private static final String SQL_PITCH =
      "SELECT playerID, teamID, yearID, G, GS, GF FROM pitching";
    private void fetchPitch(MyDatabase db) throws SQLException {
      CachedStatement PS = db.prepare(SQL_PITCH);
      try (ResultSet RS = PS.executeQuery()) {
        while (RS.next()) {
          Appearances A = new Appearances(RS.getString(1), RS.getString(2), RS.getInt(3));
          int g = RS.getInt(4);
          int gs = RS.getInt(5);
          int gf = RS.getInt(6);
          A.add(Position.STARTER, gs);
          A.add(Position.MIDDLE, g - gs - gf);
          A.add(Position.CLOSER, gf);
          _list.add(A);
        }
      }
    }
    
    public Table(MyDatabase db) throws SQLException {
      _list = new ArrayList<>();
      fetchBat(db);
      fetchPitch(db);
      for (Appearances A : _list) { A.sort(); }
    }
    
    private List<Appearances> _list = null;
    
  }

  private static void print(Appearances A) {
    System.out.format("%s[%d] : ", A._playerID, A._yearID);
    for (Use P : A) {
      System.out.format(" %s[%.0f]", P._pos.getName(), P._g);
    }
    System.out.println();
  }

  private static void print(ByPlayer<Appearances> BP) {
    System.out.println("\n" + BP.first().playerID());
    for (Appearances A : BP) { print(A); }
    print(BP.total());
  }
  
  public static void main(String[] args) throws SQLException {
    Table T = null;
    try (MyDatabase db = new MyDatabase()) { T = new Appearances.Table(db); }
    int i = 0;
    for (Appearances A : T) {
      if (++i == 10) { break; }
      print(A);
    }
    ByIDTeam by = new ByIDTeam();
    by.addAll(T);
    print(by.get("ruthba01"));
    print(by.get("rosepe01"));
    print(by.get("eckerde01"));
    print(by.get("larkiba01"));
  }
}
