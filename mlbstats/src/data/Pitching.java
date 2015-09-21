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

//pitching
/*
playerID | varchar(9)
yearID   | int(11)
stint    | int(11)
teamID   | varchar(3)
lgID     | varchar(2)
W        | int(11)
L        | int(11)
G        | int(11)
GS       | int(11)
CG       | int(11)
SHO      | int(11)
SV       | int(11)
IPouts   | int(11)
H        | int(11)
ER       | int(11)
HR       | int(11)
BB       | int(11)
SO       | int(11)
BAOpp    | double
ERA      | double
IBB      | int(11)
WP       | int(11)
HBP      | int(11)
BK       | int(11)
BFP      | int(11)
GF       | int(11)
R        | int(11)
SH       | int(11)
SF       | int(11)
GIDP     | int(11)
*/

public class Pitching implements Groupable<Pitching>, Comparable<Pitching> {
  public static Comparator<Pitching> groupYear = new Comparator<Pitching>() {
    @Override public int compare(Pitching o1, Pitching o2) { return o2._yearID - o1._yearID; }
  };
  public static Comparator<Pitching> groupID = new Comparator<Pitching>() {
    @Override public int compare(Pitching o1, Pitching o2) { return o1._playerID.compareTo(o2._playerID); }
  };
  public static Comparator<Pitching> bestWar = new Comparator<Pitching>() {
    @Override public int compare(Pitching o1, Pitching o2) {
      if (o1 == null) { return o2 == null ? 0 : 1; }
      if (o2 == null) { return -1; }
      if (o1._IP3 != o2._IP3) { return o1._IP3 < o2._IP3 ? -1 : 1; }
      if (o1._yearID != o2._yearID) { return o1._yearID < o2._yearID ? -1 : 1; }
      return o1._playerID.compareTo(o2._playerID);
    }
  };
  private static ByPlayer.GroupKey<String, Pitching> BY_ID = new ByPlayer.GroupKey<String, Pitching>() {
    @Override public String groupBy(Pitching V) { return V.playerID(); }
  };
  public static ByPlayer.GroupKey<String, Pitching> BY_TEAM = new ByPlayer.GroupKey<String, Pitching>() {
    @Override public String groupBy(Pitching V) { return V.teamID() + V.yearID(); }
  };
  private static ByPlayer.GroupKey<Integer, Pitching> BY_YEAR = new ByPlayer.GroupKey<Integer, Pitching>() {
    @Override public Integer groupBy(Pitching V) { return V.yearID(); }
  };
  public static class ByID extends ByPlayer.Group<String, Pitching> {
    public ByID() { super(BY_ID, bestWar, groupYear); }
  }
  public static class ByYear extends ByPlayer.Group<Integer, Pitching> {
    public ByYear() { super(BY_YEAR, null, groupID); }
  }
  public static class ByTeam extends ByPlayer.Group<String, Pitching> {
    public ByTeam() { super(BY_TEAM, null, groupID); }
  }

  public String   playerID() { return _playerID; }
  public String   leagueID() { return _lgID; }
  public String   teamID()   { return _teamID; }
  public int      yearID()   { return _yearID; }
  public int      w() { return _W; }
  public int      l() { return _L; }
  public int      g() { return _G; }
  public int      gs() { return _GS; }
  public int      cg() { return _CG; }
  public int      sho() { return _SHO; }
  public int      sv() { return _SV; }
  public int      ip3() { return _IP3; }
  public int      h() { return _H; }
  public int      er() { return _ER; }
  public int      hr() { return _HR; }
  public int      bb() { return _BB; }
  public int      so() { return _SO; }
  public int      ibb() { return _IBB; }
  public int      wp() { return _WP; }
  public int      hbp() { return _HBP; }
  public int      bk() { return _BK; }
  public int      bfp() { return _BFP; }
  public int      gf() { return _GF; }
  public int      r() { return _R; }
  public int      sh() { return _SH; }
  public int      sf() { return _SF; }
  public int      gidp() { return _GIDP; }
  public double   era() { return _ER * 27.0 / _IP3; }
  public double   whip() { return (_BB + _H) * 3.0 / _IP3; }

  @Override public int compareTo(Pitching W) {
    if (W == null) { return -1; }
    if (_IP3 != W._IP3) { return _IP3 < W._IP3 ? -1 : 1; }
    if (_yearID != W._yearID) { return _yearID < W._yearID ? -1 : 1; }
    return _playerID.compareTo(W._playerID);
  }

  @Override public void add(Pitching b) {
    _W += b._W;
    _L += b._L;
    _G += b._G;
    _GS += b._GS;
    _CG += b._CG;
    _SHO += b._SHO;
    _SV += b._SV;
    _IP3 += b._IP3;
    _H += b._H;
    _ER += b._ER;
    _HR += b._HR;
    _BB += b._BB;
    _SO += b._SO;
    _IBB += b._IBB;
    _WP += b._WP;
    _HBP += b._HBP;
    _BK += b._BK;
    _BFP += b._BFP;
    _GF += b._GF;
    _R += b._R;
    _SH += b._SH;
    _SF += b._SF;
    _GIDP += b._GIDP;
  }
  @Override public Pitching create() { return new Pitching(_playerID, _lgID, _teamID, 0); }

  private Pitching(String playerID, String lgID, String teamID, int yearID) { _playerID = playerID; _lgID = lgID; _teamID = teamID == null ? "" : teamID; _yearID = yearID; }

  private String _playerID = null;
  private String _lgID = null;
  private String _teamID = null;
  private int    _yearID = 0;
  
  private int _W = 0;
  private int _L = 0;
  private int _G = 0;
  private int _GS = 0;
  private int _CG = 0;
  private int _SHO = 0;
  private int _SV = 0;
  private int _IP3 = 0;
  private int _H = 0;
  private int _ER = 0;
  private int _HR = 0;
  private int _BB = 0;
  private int _SO = 0;
  private int _IBB = 0;
  private int _WP = 0;
  private int _HBP = 0;
  private int _BK = 0;
  private int _BFP = 0;
  private int _GF = 0;
  private int _R = 0;
  private int _SH = 0;
  private int _SF = 0;
  private int _GIDP = 0;
  
  public static class Table implements Iterable<Pitching> {
 
    private static final String SQL_BAT =
      "SELECT playerID, lgID, teamID, yearID, W, L, G, GS, CG, SHO, SV, IPOuts, H, ER, HR, BB, SO, IBB, WP, HBP, BK, BFP, GF, R, SH, SF, GIDP FROM pitching";

    public Table(MyDatabase db) throws SQLException {
      _data = new ArrayList<>();
      CachedStatement PS = db.prepare(SQL_BAT);
      try (ResultSet RS = PS.executeQuery()) {
        while (RS.next()) {
          Pitching b = new Pitching(RS.getString(1), RS.getString(2), RS.getString(3), RS.getInt(4));
          b._W = RS.getInt(5);
          b._L = RS.getInt(6);
          b._G = RS.getInt(7);
          b._GS = RS.getInt(8);
          b._CG = RS.getInt(9);
          b._SHO = RS.getInt(10);
          b._SV = RS.getInt(11);
          b._IP3 = RS.getInt(12);
          b._H = RS.getInt(13);
          b._ER = RS.getInt(14);
          b._HR = RS.getInt(15);
          b._BB = RS.getInt(16);
          b._SO = RS.getInt(17);
          b._IBB = RS.getInt(18);
          b._WP = RS.getInt(19);
          b._HBP = RS.getInt(20);
          b._BK = RS.getInt(21);
          b._BFP = RS.getInt(22);
          b._GF = RS.getInt(23);
          b._R = RS.getInt(24);
          b._SH = RS.getInt(25);
          b._SF = RS.getInt(26);
          b._GIDP = RS.getInt(27);
          _data.add(b);
        }
      }
    }
    
    @Override public Iterator<Pitching> iterator() { return _data.iterator(); }
    
    private List<Pitching> _data = null;
    
    private void print() {
      int i = 0;
      for (Pitching A : _data) {
        System.out.format("%s %d[%d]\n", A._playerID, A._yearID, A._IP3);
        if (++i == 10) { break; }
      }
    }
  }
  
  private static void print(ByPlayer<Pitching> P) {
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
    Pitching.Table BT = null;
    try (MyDatabase db = new MyDatabase()) { BT = new Pitching.Table(db); }
    BT.print();
    
    Pitching.ByID BG = new Pitching.ByID();
    BG.addAll(BT);
    print(BG.iterator().next());
    print(BG.get("ruthba01"));
    print(BG.get("beltrca01"));
  }
}
