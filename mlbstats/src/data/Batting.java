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

//batting
/*
playerID  | varchar(9)
yearID    | int(11)
stint     | int(11)
teamID    | varchar(3)
lgID      | varchar(2)
G         | int(11)
G_batting | int(11)
AB        | int(11)
R         | int(11)
H         | int(11)
2B        | int(11)
3B        | int(11)
HR        | int(11)
RBI       | int(11)
SB        | int(11)
CS        | int(11)
BB        | int(11)
SO        | int(11)
IBB       | int(11)
HBP       | int(11)
SH        | int(11)
SF        | int(11)
GIDP      | int(11)
G_old     | int(11)
*/

public class Batting implements Groupable<Batting>, Comparable<Batting> {
  public static Comparator<Batting> groupYear = new Comparator<Batting>() {
    @Override public int compare(Batting o1, Batting o2) { return o2._yearID - o1._yearID; }
  };
  public static Comparator<Batting> groupID = new Comparator<Batting>() {
    @Override public int compare(Batting o1, Batting o2) { return o1._playerID.compareTo(o2._playerID); }
  };
  public static Comparator<Batting> bestWar = new Comparator<Batting>() {
    @Override public int compare(Batting o1, Batting o2) {
      if (o1 == null) { return o2 == null ? 0 : 1; }
      if (o2 == null) { return -1; }
      if (o1._PA != o2._PA) { return o1._PA < o2._PA ? -1 : 1; }
      if (o1._yearID != o2._yearID) { return o1._yearID < o2._yearID ? -1 : 1; }
      return o1._playerID.compareTo(o2._playerID);
    }
  };
  private static ByPlayer.GroupKey<String, Batting> BY_ID = new ByPlayer.GroupKey<String, Batting>() {
    @Override public String groupBy(Batting V) { return V.playerID(); }
  };
  public static ByPlayer.GroupKey<String, Batting> BY_TEAM = new ByPlayer.GroupKey<String, Batting>() {
    @Override public String groupBy(Batting V) { return V.teamID() + V.yearID(); }
  };
  private static ByPlayer.GroupKey<Integer, Batting> BY_YEAR = new ByPlayer.GroupKey<Integer, Batting>() {
    @Override public Integer groupBy(Batting V) { return V.yearID(); }
  };
  public static class ByID extends ByPlayer.Group<String, Batting> {
    public ByID() { super(BY_ID, bestWar, groupYear); }
  }
  public static class ByYear extends ByPlayer.Group<Integer, Batting> {
    public ByYear() { super(BY_YEAR, null, groupID); }
  }
  public static class ByTeam extends ByPlayer.Group<String, Batting> {
    public ByTeam() { super(BY_TEAM, null, groupID); }
  }

  public String   playerID() { return _playerID; }
  public String   leagueID() { return _lgID; }
  public String   teamID()   { return _teamID; }
  public int      yearID()   { return _yearID; }
  public int      g() { return _G; }
  public int      g_bat() { return _G_batting; }
  public int      ab() { return _AB; }
  public int      r() { return _R; }
  public int      h() { return _H; }
  public int      d() { return _2B; }
  public int      t() { return _3B; }
  public int      hr() { return _HR; }
  public int      bi() { return _RBI; }
  public int      sb() { return _SB; }
  public int      cs() { return _CS; }
  public int      bb() { return _BB; }
  public int      so() { return _SO; }
  public int      ibb() { return _IBB; }
  public int      hbp() { return _HBP; }
  public int      sh() { return _SH; }
  public int      sf() { return _SF; }
  public int      gidp() { return _GIDP; }
  public int      pa() { return _PA; }
  public double   avg() { return _H / (double)_AB; }
  public double   slg() { return (_H + _2B + _3B * 2 + _HR * 3) / (double)_AB; }
  public double   obp() { return (_H + _BB + _HBP) / (double)(_PA - _SH); }

  @Override public int compareTo(Batting W) {
    if (W == null) { return -1; }
    if (_PA != W._PA) { return _PA < W._PA ? -1 : 1; }
    if (_yearID != W._yearID) { return _yearID < W._yearID ? -1 : 1; }
    return _playerID.compareTo(W._playerID);
  }

  @Override public void add(Batting b) {
    _G += b._G;
    _G_batting += b._G_batting;
    _AB += b._AB;
    _R += b._R;
    _H += b._H;
    _2B += b._2B;
    _3B += b._3B;
    _HR += b._HR;
    _RBI += b._RBI;
    _SB += b._SB;
    _CS += b._CS;
    _BB += b._BB;
    _SO += b._SO;
    _IBB += b._IBB;
    _HBP += b._HBP;
    _SH += b._SH;
    _SF += b._SF;
    _GIDP += b._GIDP;
    _PA += b._PA;
  }
  @Override public Batting create() { return new Batting(_playerID, _lgID, _teamID, 0); }

  private Batting(String playerID, String lgID, String teamID, int yearID) { _playerID = playerID; _lgID = lgID; _teamID = teamID == null ? "" : teamID; _yearID = yearID; }

  private String _playerID = null;
  private String _lgID = null;
  private String _teamID = null;
  private int    _yearID = 0;
  private int    _PA = 0;

  private int _G = 0;
  private int _G_batting = 0;
  private int _AB = 0;
  private int _R = 0;
  private int _H = 0;
  private int _2B = 0;
  private int _3B = 0;
  private int _HR = 0;
  private int _RBI = 0;
  private int _SB = 0;
  private int _CS = 0;
  private int _BB = 0;
  private int _SO = 0;
  private int _IBB = 0;
  private int _HBP = 0;
  private int _SH = 0;
  private int _SF = 0;
  private int _GIDP = 0;
  
  public static class Table implements Iterable<Batting> {
 
    private static final String SQL_BAT =
      "SELECT playerID, lgID, teamID, yearID, G, G_batting, AB, R, H, 2B, 3B, HR, RBI, SB, CS, BB, SO, IBB, HBP, SH, SF, GIDP FROM batting";

    public Table(MyDatabase db) throws SQLException {
      _data = new ArrayList<>();
      CachedStatement PS = db.prepare(SQL_BAT);
      try (ResultSet RS = PS.executeQuery()) {
        while (RS.next()) {
          Batting b = new Batting(RS.getString(1), RS.getString(2), RS.getString(3), RS.getInt(4));
          b._G = RS.getInt(5);
          b._G_batting = RS.getInt(6);
          b._AB = RS.getInt(7);
          b._R = RS.getInt(8);
          b._H = RS.getInt(9);
          b._2B = RS.getInt(10);
          b._3B = RS.getInt(11);
          b._HR = RS.getInt(12);
          b._RBI = RS.getInt(13);
          b._SB = RS.getInt(14);
          b._CS = RS.getInt(15);
          b._BB = RS.getInt(16);
          b._SO = RS.getInt(17);
          b._IBB = RS.getInt(18);
          b._HBP = RS.getInt(19);
          b._SH = RS.getInt(20);
          b._SF = RS.getInt(21);
          b._GIDP = RS.getInt(22);
          b._PA = b._AB + b._BB + b._HBP + b._SH + b._SF;
          _data.add(b);
        }
      }
    }
    
    @Override public Iterator<Batting> iterator() { return _data.iterator(); }
    
    private List<Batting> _data = null;
    
    private void print() {
      int i = 0;
      for (Batting A : _data) {
        System.out.format("%s %d[%d]\n", A._playerID, A._yearID, A._PA);
        if (++i == 10) { break; }
      }
    }
  }
  
  private static void print(ByPlayer<Batting> P) {
    if (P == null) { System.out.println("could not find player"); return; }
/*
    System.out.format("%s\t%d-%d\t%.1f\t%.1f\t%.1f\n",
          P.best().playerID(),
          P.first().yearID(),
          P.last().yearID(),
          P.total().war(),
          P.best().war(),
          P.filter(Batting.filterPositive).war());
*/
  }
  
  public static void main(String[] args) throws SQLException {
    Batting.Table BT = null;
    try (MyDatabase db = new MyDatabase()) { BT = new Batting.Table(db); }
    BT.print();
    
    Batting.ByID BG = new Batting.ByID();
    BG.addAll(BT);
    print(BG.iterator().next());
    print(BG.get("ruthba01"));
    print(BG.get("beltrca01"));
  }
}
