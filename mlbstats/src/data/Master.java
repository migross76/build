package data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import util.MyDatabase;
import db.CachedStatement;

//lahmanID     | int(9)
//playerID     | varchar(10)
//managerID    | varchar(10)
//hofID        | varchar(10)
//birthYear    | int(4)
//birthMonth   | int(2)
//birthDay     | int(2)
//birthCountry | varchar(50)
//birthState   | char(2)
//birthCity    | varchar(50)
//deathYear    | int(4)
//deathMonth   | int(2)
//deathDay     | int(2)
//deathCountry | varchar(50)
//deathState   | char(2)
//deathCity    | varchar(50)
//nameFirst    | varchar(50)
//nameLast     | varchar(50)
//nameNote     | varchar(255)
//nameGiven    | varchar(255)
//nameNick     | varchar(255)
//weight       | int(3)
//height       | double(4,1)
//bats         | enum('L','R','B')
//throws       | enum('L','R','B')
//debut        | date
//finalGame    | date
//college      | varchar(50)
//lahman40ID   | varchar(9)
//lahman45ID   | varchar(9)
//retroID      | varchar(9)
//holtzID      | varchar(9)
//bbrefID      | varchar(9)
public class Master {
  
  public String playerID()  { return _playerID; }
  public String hofID()     { return _playerID; }
  public String bbrefID()   { return _bbrefID; }
  public int yearBirth()    { return _birthYear; }
  public int monthBirth()   { return _birthMonth; }
  public int dayBirth()     { return _birthDay; }
  public String birthday()  { return String.format("%04d-%02d-%02d", _birthYear, _birthMonth, _birthDay); }
  public String nameFirst() { return _nameFirst; }
  public String nameLast()  { return _nameLast; }
  public int age(int yearID) { return yearID - yearBirth() - (_birthMonth > 6 ? 1 : 0); }
  
  private String _playerID = null;
  private int    _birthYear = 0;
  private int    _birthMonth = 0;
  private int    _birthDay = 0;
  private String _nameFirst = null;
  private String _nameLast = null;
  private String _bbrefID = null;
  
  private Master() { }
  
  public static class Table {
    private void fetch(MyDatabase db) throws SQLException {
      CachedStatement PS = db.prepare("SELECT playerID, birthYear, birthMonth, birthDay, nameFirst, nameLast, bbrefID FROM master");
      try (ResultSet RS = PS.executeQuery()) {
        while (RS.next()) {
          Master M = new Master();
          M._playerID = RS.getString(1);
          M._birthYear = RS.getInt(2);
          M._birthMonth = RS.getInt(3);
          M._birthDay = RS.getInt(4);
          M._nameFirst = RS.getString(5);
          M._nameLast = RS.getString(6);
          M._bbrefID = RS.getString(7);
          
          if (M._bbrefID != null) { _byBBRefID.put(M._bbrefID, M); }
          addID(M._playerID, M);
          addYear(M._birthYear, M);
        }
      }
    }
    
    private void addID(String id, Master M) {
      if (id != null) { _byID.put(id, M); }
    }
    
    private void addYear(int year, Master M) {
      if (_yearFirst > year) { _yearFirst = year; }
      if (_yearLast < year) { _yearLast = year; }
      List<Master> list = _byYear.get(year);
      if (list == null) { _byYear.put(year, list = new ArrayList<>()); }
      list.add(M);
      _all.add(M);
    }
    
    public Table(MyDatabase db, Sort yearSort) throws SQLException {
      _byID = new HashMap<>();
      _byBBRefID = new HashMap<>();
      _byYear = (yearSort == Sort.SORTED) ? new TreeMap<Integer, List<Master>>() : new HashMap<Integer, List<Master>>();
      _all = new ArrayList<>();
      fetch(db);
    }
    
    public int yearFirst() { return _yearFirst; }
    public int yearLast() { return _yearLast; }
    
    public Map<Integer, List<Master>> byYear() { return _byYear; }
    public Iterable<Master> year(int year) { List<Master> L = _byYear.get(year); return L == null ? _blank : L; }
    public Map<String, Master> byID() { return _byID; }
    public Master byID(String ID) { return _byID.get(ID); }
    public Iterable<Master> all() { return _all; }
    public Master bbrefID(String ID) { return _byBBRefID.get(ID); }
    
    private static final List<Master> _blank = new ArrayList<>();
    
    private Map<Integer, List<Master>> _byYear = null;
    private Map<String, Master> _byID = null;
    private Map<String, Master> _byBBRefID = null;
    private List<Master> _all = null;
    private int _yearFirst = 3000;
    private int _yearLast = 0;
  }
  
  public static void main(String[] args) throws SQLException {
    Master.Table MT = null;
    try (MyDatabase db = new MyDatabase()) { MT = new Master.Table(db, Sort.SORTED); }
    for (Map.Entry<Integer, List<Master>> E : MT.byYear().entrySet()) {
      System.out.format("%d : %d\n", E.getKey(), E.getValue().size());
    }
    Master M = MT.byID("charlos99h");
    System.out.format("%s %s : %d\n", M.nameFirst(), M.nameLast(), M.yearBirth());
    M = MT.byID("larusto01m");
    System.out.format("%s %s : %d\n", M.nameFirst(), M.nameLast(), M.yearBirth());
    M = MT.byID("pujolal01");
    System.out.format("%s %s : %d\n", M.nameFirst(), M.nameLast(), M.yearBirth());
  }
}
