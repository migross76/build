package data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import util.ByPlayer;
import util.MyDatabase;
import db.CachedStatement;

//bWarBat
/*
playerID           | varchar(9)
yearID             | smallint(4) unsigned
teamID             | char(3)
stintID            | smallint(1) unsigned
lgID               | char(2)
PA                 | smallint(4) unsigned
G                  | smallint(3) unsigned
Inn                | double(5,1)
runs_bat           | double(4,1)
runs_br            | double(3,1)
runs_dp            | double(3,1)
runs_field         | double(3,1)
runs_infield       | double(3,1)
runs_outfield      | double(3,1)
runs_catcher       | double(3,1)
runs_good_plays    | double(3,1)
runs_defense       | double(3,1)
runs_position      | double(3,1)
runs_position_p    | double(3,1)
runs_replacement   | double(3,1)
runs_above_rep     | double(4,1)
runs_above_avg     | double(4,1)
runs_above_avg_off | double(4,1)
runs_above_avg_def | double(3,1)
WAA                | double(4,2)
WAA_off            | double(4,2)
WAA_def            | double(4,2)
WAR                | double(3,1)
WAR_def            | double(3,1)
WAR_off            | double(3,1)
WAR_rep            | double(4,2)
salary             | int(9)
pitcher            | char(1)
teamRpG            | double(7,5)
oppRpG             | double(7,5)
oppRpPA_rep        | double(6,5)
oppRpG_rep         | double(7,5)
pyth_exponent      | double(4,3)
pyth_exponent_rep  | double(4,3)
waa_win_perc       | double(5,4)
waa_win_perc_off   | double(5,4)
waa_win_perc_def   | double(5,4)
waa_win_perc_rep   | double(5,4)
 */

//bWarPitch
/*
playerID              | varchar(9)
yearID                | smallint(4) unsigned
teamID                | char(3)
stintID               | smallint(1) unsigned
lgID                  | char(2)
G                     | smallint(3) unsigned
GS                    | smallint(3) unsigned
IPouts                | smallint(4) unsigned
IPouts_start          | smallint(4) unsigned
IPouts_relief         | smallint(4)
RA                    | smallint(3)
xRA                   | double(6,3)
xRA_sprp_adj          | double(4,3)
xRA_def_pitcher       | double(5,3)
PPF                   | smallint(3)
PPF_custom            | smallint(3)
xRA_final             | double(6,3)
BIP                   | smallint(4)
BIP_perc              | double(5,4)
RS_def_total          | double(6,3)
runs_above_avg        | double(6,3)
runs_above_avg_adj    | double(6,3)
runs_above_rep        | double(6,3)
RpO_replacement       | double(4,3)
GR_leverage_index_avg | double(6,4)
WAR                   | double(3,1)
salary                | int(9)
teamRpG               | double(7,5)
oppRpG                | double(7,5)
pyth_exponent         | double(4,3)
waa_win_perc          | double(5,4)
WAA                   | double(6,4)
WAA_adj               | double(5,4)
oppRpG_rep            | double(7,5)
pyth_exponent_rep     | double(4,3)
waa_win_perc_rep      | double(5,4)
WAR_rep               | double(5,4)
 */
//playerID | varchar(9)
//yearID   | smallint(4) unsigned
//bbrefID  | varchar(9)
//age      | smallint(2) unsigned
//teamID   | char(3)
//IPouts   | int(5) unsigned
//GS       | int(3) unsigned
//R        | smallint(3)
//Rrep     | smallint(3)
//Rdef     | smallint(3)
//aLi      | double(2,1)
//RAR      | smallint(3)
//rWAR     | double(3,1)
//salary   | varchar(12)
//acquired | varchar(50)


// New WAR value, based on CSV files imported from Baseball Reference
// TODO convert all code to use this, then rename to War
// TODO import age information (or link to it somehow)
// TODO import franchise information (or link to it somehow)
public class War implements Groupable<War>, Comparable<War> {
  public static Comparator<War> groupYear = new Comparator<War>() {
    @Override public int compare(War o1, War o2) { return o2._yearID - o1._yearID; }
  };
  public static Comparator<War> groupID = new Comparator<War>() {
    @Override public int compare(War o1, War o2) { return o1._playerID.compareTo(o2._playerID); }
  };
  public static Comparator<War> bestWar = new Comparator<War>() {
    @Override public int compare(War o1, War o2) {
      if (o1 == null) { return o2 == null ? 0 : 1; }
      if (o2 == null) { return -1; }
      if (o1._war != o2._war) { return o1._war > o2._war ? -1 : 1; }
      if (o1._playtime != o2._playtime) { return o1._playtime < o2._playtime ? -1 : 1; }
      if (o1._yearID != o2._yearID) { return o1._yearID < o2._yearID ? -1 : 1; }
      return o1._playerID.compareTo(o2._playerID);
    }
  };
  public static Filter<War> filterPositive = new Filter<War>() {
    @Override public boolean satisfied(War o1) { return o1._war >= 0; }
  };
  private static ByPlayer.GroupKey<String, War> BY_ID = new ByPlayer.GroupKey<String, War>() {
    @Override public String groupBy(War V) { return V.playerID(); }
  };
  public static ByPlayer.GroupKey<String, War> BY_TEAM = new ByPlayer.GroupKey<String, War>() {
    @Override public String groupBy(War V) { return V.teamID() + V.yearID(); }
  };
  private static ByPlayer.GroupKey<Integer, War> BY_YEAR = new ByPlayer.GroupKey<Integer, War>() {
    @Override public Integer groupBy(War V) { return V.yearID(); }
  };
  public static class ByIDAlone extends ByPlayer.Group<String, War> {
    public ByIDAlone() { super(BY_ID, bestWar, null); }
  }
  public static class ByID extends ByPlayer.Group<String, War> {
    public ByID() { super(BY_ID, bestWar, groupYear); }
  }
  public static class ByYear extends ByPlayer.Group<Integer, War> {
    public ByYear() { super(BY_YEAR, null, groupID); }
  }
  public static class ByTeam extends ByPlayer.Group<String, War> {
    public ByTeam() { super(BY_TEAM, null, groupID); }
  }

  public String   playerID() { return _playerID; }
  public String   leagueID() { return _lgID; }
  public String   teamID()   { return _teamID; }
  public int      yearID()   { return _yearID; }
  public double   war()      { return _war; }
  public double   waa()      { return _waa; }
  public double   waa_adj()  { return _waa_adj; }
  public double   rar()      { return _rar; }
  public double   rField()   { return _rField; }
  public double   rPos()     { return _rPos; }
  public int      playtime() { return _playtime; }

  @Override public int compareTo(War W) {
    if (W == null) { return -1; }
    if (_war != W._war) { return _war > W._war ? -1 : 1; }
    if (_playtime != W._playtime) { return _playtime < W._playtime ? -1 : 1; }
    if (_yearID != W._yearID) { return _yearID < W._yearID ? -1 : 1; }
    return _playerID.compareTo(W._playerID);
  }

  @Override public void add(War W) {
    _war += W._war;
    _waa += W._waa;
    _waa_adj += W._waa_adj;
    _rar += W._rar;
    _rField += W._rField;
    _rPos += W._rPos;
    _playtime += W._playtime;
  }
  @Override public War create() { return new War(_playerID, _lgID, _teamID, 0); }

  private War(String playerID, String lgID, String teamID, int yearID) { _playerID = playerID; _lgID = lgID; _teamID = teamID == null ? "" : teamID; _yearID = yearID; }

  private String _playerID = null;
  private String _lgID = null;
  private String _teamID = null;
  private int    _yearID = 0;
  private double _war = 0;
  private double _waa = 0;
  private double _waa_adj = 0;
  private double _rar = 0;
  private double _rField = 0;
  private double _rPos = 0;
  private int    _playtime = 0;
  
  public static class Table implements Iterable<War> {
    
    private static final String SQL_BAT =
      "SELECT playerID, lgID, teamID, yearID, WAA, WAR, PA, runs_above_rep, runs_defense, runs_position FROM rbWAR";
    private static final String SQL_PITCH =
      "SELECT playerID, lgID, teamID, yearID, WAA, WAR, IPouts, runs_above_rep, WAA_adj FROM rpWAR";

    private void fetch(MyDatabase db, Type type, String sql) throws SQLException {
      CachedStatement PS = db.prepare(sql);
      try (ResultSet RS = PS.executeQuery()) {
        while (RS.next()) {
          War W = new War(RS.getString(1), RS.getString(2), RS.getString(3), RS.getInt(4));
          double waa = RS.getDouble(5);
          double war = RS.getDouble(6);
          double playtime = RS.getInt(7);
          double rar = RS.getDouble(8);
          W._waa += waa;
          W._war += war;
          W._playtime += playtime;
          W._rar += rar;
          if (type == Type.BAT) {
            W._rField += RS.getDouble(9);
            W._rPos += RS.getDouble(10);
          } else {
            W._waa_adj += RS.getDouble(9);
          }
          _data.add(W);
        }
      }
    }
    
    public Type type() { return _type; }
    
    public Table(MyDatabase db, Type type) throws SQLException {
      _type = type;
      _data = new ArrayList<>();
      fetch(db, type, type == Type.BAT ? SQL_BAT : SQL_PITCH);
    }
    
    @Override public Iterator<War> iterator() { return _data.iterator(); }
    
    private Type _type = null;
    private List<War> _data = null;
    
    private void print() {
      int i = 0;
      for (War A : _data) {
        System.out.format("%s %d[%.1f/%d]\n", A._playerID, A._yearID, A._war, A._playtime);
        if (++i == 10) { break; }
      }
    }
  }
  
  private static void print(ByPlayer<War> P) {
    if (P == null) { System.out.println("could not find player"); return; }
    System.out.format("%s\t%d-%d\t%.1f\t%.1f\t%.1f\n",
          P.best().playerID(),
          P.first().yearID(),
          P.last().yearID(),
          P.total().war(),
          P.best().war(),
          P.filter(War.filterPositive).war());
  }
  
  public static void main(String[] args) throws SQLException {
    War.Table BT = null;
    War.Table PT = null;
    try (MyDatabase db = new MyDatabase()) { BT = new War.Table(db, Type.BAT); PT = new War.Table(db, Type.PITCH); }
    BT.print();
    PT.print();
    
    War.ByID BG = new War.ByID();
    BG.addFilter(War.filterPositive);
    BG.addAll(BT);
    print(BG.iterator().next());
    print(BG.get("ruthba01"));
    print(BG.get("beltrca01"));
    War.ByID PG = new War.ByID();
    PG.addFilter(War.filterPositive);
    PG.addAll(PT);
    print(PG.get("ruthba01"));
    print(PG.get("leecl02"));
    
  }
}
