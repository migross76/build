package comps;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import util.MyDatabase;
import db.CachedStatement;

public class Postseason {
  private enum Round {
    LDS, LCS, WS;
    
    public static Round parse(String round) {
      if (round.contains("LDS")) { return LDS; }
      else if (round.contains("LCS")) { return LCS; }
      else if (round.equals("WS")) { return WS; }
      else return null;
    }
    
    public static int size() { return Round.values().length; }
  }
  
  private static class Season {
    public Season(int year) {
      _year = year;
      for (int i = 0, e = Round.size(); i != e; ++i) {
        _b_post[i] = new Bat();
        _p_post[i] = new Pitch();
      }
    }
    
    public void add(Season S) {
      _b_reg.add(S._b_reg);
      for (int i = 0, e = Round.size(); i != e; ++i) {
        _b_post[i].add(S._b_post[i]);
      }
      _b_post_all.add(S._b_post_all);
    }
    
    public int _year = 0;
    public Bat _b_reg = new Bat();
    public Bat[] _b_post = new Bat[Round.size()];
    public Bat _b_post_all = new Bat();
    //public Pitch _p_reg = new Pitch();
    public Pitch[] _p_post = new Pitch[Round.size()];
  }
  
  private static class Counters {
    public int[] getRaw(String id) { return _map.get(id); }
    
    public int[] get(String id) {
      int[] C = _map.get(id);
      if (C == null) { _map.put(id, C = new int[Round.size()]); }
      return C;
    }
    
    public void clear() { _map.clear(); }
    
    public HashMap<String, int[]> _map = new HashMap<>();
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
    "SELECT playerID, " + Bat.SQL + " FROM batting WHERE lgid in ('AL', 'NL') AND yearID = ? GROUP BY playerID";
  private static final void getLeagueBat(MyDatabase db, Season S, Counters C) throws Exception {
    CachedStatement stmt = db.prepare(SQL_LEAGUE_BAT);
    stmt.setInt(1, S._year);
    try (ResultSet RS = stmt.executeQuery()) {
      while (RS.next()) {
        String ID = RS.getString(1);
        int pa = Bat.getPA(RS, 2);
        if (pa == 0) { continue; }
        double woba_ct = Bat.getWobaCount(RS, 2);
        int[] post_pa = C.getRaw(ID);
        if (post_pa != null) {
          for (int i = 0, e = Round.size(); i != e; ++i) {
            if (post_pa[i] != 0) {
              S._b_post[i]._comp_woba += woba_ct * post_pa[i] / pa;
            }
          }
        }
        S._b_reg._pa += pa;
        S._b_reg._main_woba += woba_ct;
      }
    }
    for (Bat B : S._b_post) { S._b_post_all.add(B); }
    C.clear();
  }
  
  
  private static final String SQL_POST_BAT =
    "SELECT yearID, playerID, round, " + Bat.SQL + " FROM battingpost WHERE lgid in ('AL', 'NL') GROUP BY yearID, playerID, round";
  private static final void getPostBat(MyDatabase db, Seasons seasons) throws Exception {
    CachedStatement post = db.prepare(SQL_POST_BAT);
    Season S = null;
    Counters C = new Counters();
    try (ResultSet postRS = post.executeQuery()) {
      while (postRS.next()) {
        int year = postRS.getInt(1);
        if (S == null) { S = seasons.get(year); }
        else if (year != S._year) {
          getLeagueBat(db, S, C);
          S = seasons.get(year);
        }
        String ID = postRS.getString(2);
        Round R = Round.parse(postRS.getString(3));
        if (R == null) { continue; }
        int pa = Bat.getPA(postRS, 4);
        C.get(ID)[R.ordinal()] += pa;
        Bat B = S._b_post[R.ordinal()];
        B._pa += pa;
        B._main_woba += Bat.getWobaCount(postRS, 4);
      }
    }
    getLeagueBat(db, S, C);
  }
/*
  private static final String SQL_LEAGUE_PITCH =
    "SELECT yearID, sum(ipouts), sum(er), sum(hr), sum(bb), sum(so), sum(ibb), sum(hbp) " +
    " FROM pitching where lgid in ('AL', 'NL') GROUP BY yearID";
  private static final void getLeaguePitch(Seasons seasons) throws Exception {
    PreparedStatement pitch = Database.prepare(SQL_LEAGUE_PITCH);
    ResultSet RS = pitch.executeQuery();
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
*/  
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
      getPostBat(db, seasons);
      //getLeaguePitch(db, seasons);
    }
    
    Season total = new Season(0);
    System.out.format("\tRegular%1$sAll%1$sLDS%1$sLCS%1$sWS\n", Bat.COLS);
    System.out.format("Yr%1$s%1$s%1$s%1$s%1$s\n", Bat.HEADER);
    for (Season S : seasons) {
      System.out.format("%d%s%s", S._year, S._b_reg, S._b_post_all);
      for (int i = 0, e = Round.size(); i != e; ++i) {
        System.out.print(S._b_post[i]);
      }
      System.out.println();
      total.add(S);
    }
    System.out.format("\nTOTAL%s%s", total._b_reg, total._b_post_all);
    for (int i = 0, e = Round.size(); i != e; ++i) {
      System.out.print(total._b_post[i]);
    }
    System.out.println();
  }
}
