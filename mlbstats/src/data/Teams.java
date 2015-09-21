package data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import util.MyDatabase;
import db.CachedStatement;

//yearID         | smallint(4) unsigned
//lgID           | char(2)
//teamID         | char(3)
//franchID       | char(3)
//divID          | char(1)
//Rank           | smallint(3) unsigned
//G              | smallint(3) unsigned
//Ghome          | int(3)
//W              | smallint(3) unsigned
//L              | smallint(3) unsigned
//DivWin         | enum('Y','N')
//WCWin          | enum('Y','N')
//LgWin          | enum('Y','N')
//WSWin          | enum('Y','N')
//R              | smallint(4) unsigned
//AB             | smallint(4) unsigned
//H              | smallint(4) unsigned
//2B             | smallint(4) unsigned
//3B             | smallint(3) unsigned
//HR             | smallint(3) unsigned
//BB             | smallint(4) unsigned
//SO             | smallint(4) unsigned
//SB             | smallint(3) unsigned
//CS             | smallint(3) unsigned
//HBP            | smallint(3)
//SF             | smallint(3)
//RA             | smallint(4) unsigned
//ER             | smallint(4)
//ERA            | decimal(4,2)
//CG             | smallint(3) unsigned
//SHO            | smallint(3) unsigned
//SV             | smallint(3) unsigned
//IPouts         | int(5)
//HA             | smallint(4) unsigned
//HRA            | smallint(4) unsigned
//BBA            | smallint(4) unsigned
//SOA            | smallint(4) unsigned
//E              | int(5)
//DP             | int(4)
//FP             | decimal(5,3)
//name           | varchar(50)
//park           | varchar(255)
//attendance     | int(7)
//BPF            | int(3)
//PPF            | int(3)
//teamIDBR       | char(3)
//teamIDlahman45 | char(3)
//teamIDretro    | char(3)
public class Teams {
  public int     yearID() { return _yearID; }
  public String  lgID() { return _lgID; }
  public String  teamID() { return _teamID; }
  public String  franchID() { return _franchID; }
  public int     games() { return _games; }
  public int     wins() { return _wins; }
  public int     losses() { return _losses; }
  public double  winpct() { return _wins / (double)_games; }
  public boolean divWin() { return _divWin; }
  public boolean wcWin() { return _wcWin; }
  public boolean lgWin() { return _lgWin; }
  public boolean wsWin() { return _wsWin; }
  public int     runs() { return _runs; }
  public int     runsAllowed() { return _ra; }
  public String  teamIDBR() { return _teamIDBR; }
  
  public boolean isAlNl() { return _lgID.equals("AL") || _lgID.equals("NL"); }
  
  public String name() { return _name; }

  private int     _yearID = 0;
  private String  _lgID = null;
  private String  _teamID = null;
  private String  _franchID = null;
  private int     _games = 0;
  private int     _wins = 0;
  private int     _losses = 0;
  private boolean _divWin = false;
  private boolean _wcWin = false;
  private boolean _lgWin = false;
  private boolean _wsWin = false;
  private int     _runs = 0;
  private int     _ra = 0;
  private String  _teamIDBR = null;
  
  private String  _name = null;

  private Teams() { }
  
  public static class Table implements Iterable<Teams> {
    @Override public Iterator<Teams> iterator() { return _teams.iterator(); }
    
    public int yearFirst() { return _index.firstKey(); }
    public int yearLast() { return _index.lastKey(); }
    
    public Teams team(int year, String id) {
      Map<String, Teams> yTeam = _index.get(year);
      return yTeam == null ? null : yTeam.get(id);
    }
    
    public ArrayList<Teams> year(int year) {
      return year(year, false);
    }
    
    public ArrayList<Teams> year(int year, boolean alnlOnly) {
      Map<String, Teams> yTeam = _index.get(year);
      if (yTeam == null) { return null; }
      ArrayList<Teams> teams = new ArrayList<>();
      for (Map.Entry<String, Teams> E : yTeam.entrySet()) {
        if (alnlOnly && !E.getValue().isAlNl()) { continue; }
        if (E.getKey().equals(E.getValue().teamID())) { teams.add(E.getValue()); }
      }
      return teams;
    }
    
    public String getFranchiseID(String teamID) {
      return _franchMap.get(teamID);
    }
/*
    public double getAverageGamesPlayed(int year) {
      return _yearMap.get(year);
    }
*/
    private static boolean isYes(String val) {
      return val != null && val.equals("Y");
    }
    
    private void indexTeam(Teams T) {
      _teams.add(T);
      _franchMap.put(T._teamID, T._franchID);
      Map<String, Teams> byTeam = _index.get(T.yearID());
      if (byTeam == null) { _index.put(T.yearID(), byTeam = new TreeMap<>()); }
      byTeam.put(T.teamID(), T);
      if (!T.teamID().equals(T.teamIDBR())) { byTeam.put(T.teamIDBR(), T); }
      // Cannot put franchID in there as well, as 1914-1915 BLT will conflict
      // 1. Franchise ID for the 1914-1915 St. Louis Browns
      // 2. BR Team ID for the 1914-1915 Baltimore Terrapins
    }
    
    public Table(MyDatabase db) throws SQLException {
      CachedStatement PS = db.prepare("SELECT yearID, lgID, teamID, franchID, G, W, L, DivWin, WCWin, LgWin, WSWin, R, RA, teamIDBR FROM teams");
      try (ResultSet RS = PS.executeQuery()) {
        while (RS.next()) {
          Teams T = new Teams();
          T._yearID = RS.getInt(1);
          T._lgID = RS.getString(2);
          T._teamID = RS.getString(3);
          T._franchID = RS.getString(4);
          T._games = RS.getInt(5);
          T._wins = RS.getInt(6);
          T._losses = RS.getInt(7);
          T._divWin = isYes(RS.getString(8));
          T._wcWin = isYes(RS.getString(9));
          T._lgWin = isYes(RS.getString(10));
          T._wsWin = isYes(RS.getString(11));
          T._runs = RS.getInt(12);
          T._ra = RS.getInt(13);
          T._teamIDBR = RS.getString(14);
          indexTeam(T);
        }
      }
    }
  
//    private HashMap<Integer, Double> _yearMap = new HashMap<Integer, Double>();
    private HashMap<String, String> _franchMap = new HashMap<>();
    private ArrayList<Teams> _teams = new ArrayList<>();
    
    private TreeMap<Integer, Map<String, Teams>> _index = new TreeMap<>();
  }
}
