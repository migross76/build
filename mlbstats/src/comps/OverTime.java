package comps;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.TreeMap;
import util.MyDatabase;
import db.CachedStatement;

public class OverTime {
  
  private static class Div {
    public Div(double num, int den) { _num = num; _den = den; }
    public double _num = 0;
    public int _den = 0;
  }
  
  private static class Result {
    public Result ( ) { }
    public int _pa = 0;
    public double _woba_ct = 0;
    public int _ipouts = 0;
    public double _er = 0;
    public double _fip_ct = 0;
    
    public double wOBA() { return _woba_ct / _pa; }
    public double ERA() { return _er / _ipouts * 27.0; }
    public double FIP() { return _fip_ct / _ipouts + 3.20; }
  }
  
  private static class Season {
    public Season(int year) { _year = year; }
    public int _year = 0;
    public Result _total = new Result();
    public Result _first_2year = new Result();
    public Result _second_2year = new Result();
    public Result _post_expected = new Result();
    public Result _post_actual = new Result();
  }
  
  private static class Seasons implements Iterable<Season> {
    public Season get(int year) {
      Season S = _seasons.get(year);
      if (S == null) { _seasons.put(year, S = new Season(year)); }
      return S;
    }
    
    @Override public Iterator<Season> iterator() { return _seasons.values().iterator(); }
    
    private TreeMap<Integer, Season> _seasons = new TreeMap<>();
  }
  
  private static final String SQL_LEAGUE_BAT =
    "SELECT playerID, yearID, sum(AB), sum(H), sum(2B), sum(3B), sum(hr), sum(bb), sum(ibb), sum(hbp), sum(sh), sum(sf) " +
    "FROM batting where lgid in ('AL', 'NL') GROUP BY playerID, yearID";
  private static final String SQL_POST_BAT =
    "SELECT sum(AB), sum(H), sum(2B), sum(3B), sum(hr), sum(bb), sum(ibb), sum(hbp), sum(sh), sum(sf) " +
    "FROM battingpost WHERE playerID = ? AND yearID = ?";
  private static final void getLeagueBat(MyDatabase db, Seasons seasons) throws Exception {
    CachedStatement bat = db.prepare(SQL_LEAGUE_BAT);
    CachedStatement post = db.prepare(SQL_POST_BAT);
    Div[] div = new Div[3]; // all null by default
    int yone = 0, ytwo = 0;
    String lastID = null;
    try (ResultSet RS = bat.executeQuery()) {
      while (RS.next()) {
        String ID = RS.getString(1);
        if (!ID.equals(lastID)) { div[1] = div[2] = null; yone = ytwo = 0; lastID = ID; }
        int year = RS.getInt(2);
        Season S = seasons.get(year);
        double woba = Bat.getWobaCount(RS, 3);
        int pa = Bat.getPA(RS, 3);
        div[0] = new Div(woba, pa);
        Result R = S._total;
        R._woba_ct += div[0]._num;
        R._pa += div[0]._den;
        if (div[1] != null && yone + 2 == year) { // three consecutive years
          int minpa = Math.min(div[1]._den, div[2]._den);
          if (minpa != 0) {
            Season Spre = seasons.get(year - 1);
            Spre._first_2year._woba_ct += div[1]._num / div[1]._den * minpa; Spre._first_2year._pa += minpa;
            Spre._second_2year._woba_ct += div[2]._num / div[2]._den * minpa; Spre._second_2year._pa += minpa;
          }
        }
        div[1] = div[2]; div[2] = div[0];
        yone = ytwo; ytwo = year;
        if (div[0]._den != 0) {
          post.setString(1, ID);
          post.setInt(2, year);
          try (ResultSet RSpost = post.executeQuery()) {
            if (RSpost.next()) {
              Div postD = new Div(Bat.getWobaCount(RSpost, 1), Bat.getPA(RSpost, 1));
              S._post_expected._pa += postD._den;
              S._post_expected._woba_ct += div[0]._num * postD._den / div[0]._den;
              S._post_actual._pa += postD._den;
              S._post_actual._woba_ct += postD._num;
            }
          }
        }
      }
    }
  }
  
  private static final String SQL_LEAGUE_PITCH =
    "SELECT yearID, sum(ipouts), sum(er), sum(hr), sum(bb), sum(so), sum(ibb), sum(hbp) " +
    " FROM pitching where lgid in ('AL', 'NL') GROUP BY yearID";
  private static final void getLeaguePitch(MyDatabase db, Seasons seasons) throws Exception {
    CachedStatement pitch = db.prepare(SQL_LEAGUE_PITCH);
    try (ResultSet RS = pitch.executeQuery()) {
      while (RS.next()) {
        int year = RS.getInt(1);
        int ipouts = RS.getInt(2);
        int er = RS.getInt(3);
        int fipwt = Pitch.getFipCount(RS, 4);
        Season S = seasons.get(year);
        Result R = S._total;
        R._ipouts = ipouts;
        R._er = er;
        R._fip_ct = fipwt;
      }
    }
  }    
  
/*
  private static final String SQL_POST_PITCH =
    "SELECT m.playerID, m.yearID, m.ipouts, m.er, m.hr, m.bb, m.so, m.ibb, m.hbp " +
    "SELECT             p.round,  p.ipouts, p.er, p.hr, p.bb, p.so, p.ibb, p.hbp " +
    " FROM pitching m, pitchingpost p where m.playerID = p.playerID and m.yearID = p.yearID";
  private static final void getPostPitch(TreeMap<Integer, Result> expected, TreeMap<Integer, Result> actual) throws Exception {
    PreparedStatement pitch = Database.prepare(SQL_LEAGUE_PITCH);
    ResultSet pRS = pitch.executeQuery();
    while (pRS.next()) {
      int year = pRS.getInt(1);
      int ipouts = pRS.getInt(2);
      int er = pRS.getInt(3);
      int hr = pRS.getInt(4);
      int nibb = pRS.getInt(5);
      int so = pRS.getInt(6);
      int ibb = pRS.getInt(7); nibb -= ibb;
      int hbp = pRS.getInt(8); nibb += hbp;
      Result R = results.get(year);
      R._ipouts = ipouts;
      R._era = era(er, ipouts);
      R._fip = fip(hr, nibb, so, ipouts);
    }
  }    
*/
  
  public static void main(String[] args) throws Exception {
    Seasons seasons = new Seasons();
    try (MyDatabase db = new MyDatabase()) {
      getLeagueBat(db, seasons);
      getLeaguePitch(db, seasons);
    }    
    
    
    System.out.println("Yr\tPA\twOBA\tIP Outs\tERA\tFIP\tpPA\tpwOBA1\tpwOBA2\tpwOBA+\t*PA\t*wOBA1\t*wOBA2\t*wOBA+");
    double lastWOBA = 0;
    for (Season S : seasons) {
      Result R = S._total;
      System.out.format("%d\t%d\t%.3f\t%d\t%.2f\t%.2f", S._year, R._pa, R.wOBA(), R._ipouts, R.ERA(), R.FIP());
      System.out.format("\t%d\t%.3f\t%.3f\t%.1f", S._post_expected._pa, S._post_expected.wOBA(), S._post_actual.wOBA(), S._post_actual.wOBA() / S._post_expected.wOBA() * 100);
      if (S._first_2year._pa != 0) {
        // double woba1 = S._first_2year.wOBA();
        double woba = S._second_2year.wOBA();
        if (lastWOBA != 0) { woba *= lastWOBA / S._first_2year.wOBA(); } // keep it glued relative to the first number
        lastWOBA = S._second_2year.wOBA();
        System.out.format("\t%d\t%.3f", S._first_2year._pa, woba);
      }
      System.out.println();
    }
  }
}
