package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.Master;
import data.Position;
import data.Sort;
import data.Type;
import data.War;

/*
 * Compute my Hall of Fame selection (or top 100, or something like that) based on a combination of criteria:
 * 
 * 1. Raw performance. Equates to WAR, probably positive seasonal WAR; pitchers only get credit for positive batting WAR
 *    OR positive WAA, and let Longevity handle replacement level
 *    ** Basic WAA leads to low catcher scores: 5 of 200 (Avg 16/Low 11), 2/100 (Avg 7.7/Low 6), 0/50 (Avg 4.4/Low 3)
 *    ** Also, no RP in top 200 (Eck and Smoltz are in as SP)
 * 1b. Missed seasonal WAR. Estimate WAR lost due to segregation, time in other leagues, military service, blackballing, banning, being blocked/held back, strikes
 *     Maybe partial credit for injuries, early retirement; compute an average curve divided by age 26-28 WAR/650. Also, compute % of players who played each age.
 *     Consider just doing 5-4-3 of next closest years, then chopping off 0.5-1 WAR; repeat until below replacement.
 *     If injury time is redone, then prorate partial seasons as well (so 5PA ~= 0PA, still)
 * 2. Playoff performance. Maybe use Tango's simplified WAR calculations. Only adjust for fielding in rare circumstances (see Robinson, Brooks)
 * 3. Excellence factor. The amount to multiply 1 & 2 by. Should be similar to the factor in wWAR, but level it out based on adding missed time.
 *    Also, playoff time should be directly added to the seasonal total. Maybe with playing time; maybe just as a bonus. Probably +/- 50% range.
 *    ** Simple pWAA^2 / 10 yields: from the top 200 players, the highest 20 "peak" (most value from excellence) players are Ruth, Hornsby, Bonds, WJohnson, and 16 pitchers born before 1872.
 * 4. Longevity. The bonus for being above replacement level for a long time.
 *    Longevity algorithm?
 *    - pWAR - pWAA (replacement wins above replacement level); career total, subtract 30
 *    - Rose = 10.5, Damon = 4.3, Vizquel = 1.2, Griffey = 0.6, Henderson = 8.4, Ripken = 9.3, Cobb = 18.9!, W. Johnson = 24.2
 *    - Consider capping a season, say at 2.5 Rep+
 *    - W. Johnson = 16.9 (max 3.5), Maddux = 6.9 (max 2.2), Ryan = 14.1 (max 3.0), Clemens = 14.8 (max 2.5)
 * 5. Acclaim. Credit for MVPs/CYs, All-star appearances, HOF selection, Gold gloves. Consider also Black/Gray Ink, HOF Monitor. Max 20% of raw.
 * ?. Off-Field. Mostly manual/subjective process of giving credit for other jobs in baseball: scout, coach, manager, GM, broadcaster, owner, union, ambassador.
 *    Determine criteria. 0-10%.
 * ?. Character.  Subjective evaluation of steroids/cheating, charity work, fan/teammate relations, work ethic, trouble with law/morals. -10 to 10%.
 */
public class MyHall {
  private static Master.Table _mt = null;
  private static Appearances.ByID _atBy = null;
  private static WeightedWar.ByID _wwBy = null;
  private static WeightedApp.ByID _waBy = null;
  
  private static class Player implements Comparable<Player> {
    public final Master _master;
    public final WeightedWar _wwar;
    public final WeightedApp _app;
    
    public double _value = 0;
    public double _great = 0;
    public double _uGreat = 0;
    public double _longevity = 0;
    public double _seasons = 0;
    
    public double total() { return _value + _great + _uGreat + _longevity; }

    @Override public int compareTo(Player arg0) {
      int cmp = -Double.compare(this.total(), arg0.total());
      if (cmp != 0) { return cmp; }
      return _wwar.playerID().compareTo(arg0._wwar.playerID());
    }
    
    public Player(Master master, ByPlayer<WeightedWar> all, ByPlayer<Appearances> appBy, WeightedApp wapp) {
      _master = master; _wwar = all.total();
      _app = wapp;
      
      _seasons = all.total().seasontime();
      _value = all.total().waa_pos();
      
      for (WeightedWar ww : all) {
        double st = ww.seasontime();
        double wp = ww.war_pos();
        double ap = ww.waa_pos();
        
        if (appBy != null) {
          Appearances A = null;
          for (Appearances a : appBy) { if (a.yearID() == ww.yearID()) { A = a; break; } }
          if (A != null) {
            double g_tot = 0;
            for (Appearances.Use pos : A) { g_tot += pos.games(); }
            double c_time = g_tot == 0 ? 0 : A.games(Position.CATCH) / g_tot;
            st += st * c_time * 0.1; ap += ap * c_time * 0.1; wp += wp * c_time * 0.1;
            wp += 2.0 * c_time * st; ap += 2.0 * c_time * st;
          }
        }
        if (wp > 0) { _longevity += Math.min(wp - ap, 2.5); }
        if (ap > 0) {
          double great = ap * ap / 10;
          _great += great;
          _uGreat += great * (1 / st - 1);
        }
      }
      _longevity = _longevity > 25 ? (_longevity - 25) / 2 : 0;
    }
  }

  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    _atBy.addAll(AT);
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
    _wwBy.addAll(WWT);
    _waBy.addAll(new WeightedApp.Tally(WWT, AT));
  }
  
  public static void main(String[] args) throws Exception {
    // table initialization
    _wwBy = new WeightedWar.ByID();
    _waBy = new WeightedApp.ByID();
    _atBy = new Appearances.ByID();
    try (MyDatabase db = new MyDatabase()) {
      _mt = new Master.Table(db, Sort.SORTED);
      Appearances.Table AT = new Appearances.Table(db);
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }
    
    ArrayList<Player> players = new ArrayList<>();
    for (ByPlayer<WeightedWar> wwBy : _wwBy) {
      Master M = _mt.byID(wwBy.total().playerID());
      ByPlayer<Appearances> appBy = _atBy.get(M.playerID());
      ByPlayer<WeightedApp> waBy = _waBy.get(M.playerID());
      WeightedApp wapp = waBy == null ? null : waBy.total();
      players.add(new Player(M, wwBy, appBy, wapp));
    }
    Collections.sort(players);
    
    System.out.print("Rk\tPlayer\tPos\tYOB\tPlaytime\tWAR\tpWAA\tSeasons\tValue\tGreatness\tuGreat\tLongevity\tTotal\n");
    for (int i = 0; i != 500; ++i) {
      Player p = players.get(i);
      System.out.format("%d\t%s %s\t%s\t%d\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\n", i+1, p._master.nameFirst(), p._master.nameLast(), p._app.primary().pos().getName(), p._master.yearBirth(),
                        p._wwar.playtime(), p._wwar.war(), p._wwar.waa_pos(),
                        p._seasons, p._value, p._great, p._uGreat, p._longevity, p.total());
    }
    
  }
}
