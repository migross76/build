package app;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import util.ByPlayer;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.ELO;
import data.HOF;
import data.Master;
import data.Position;
import data.Sort;
import data.Type;
import data.War;

/*
 * Goal: Select top N(=112) players for HOF
 * - possibly restricted by retirement year(2007) or birth year(1968)
 * - Gather up as many different rankings as possible
 * - Normalize such that N+1 = 0, average of Elite(N/5) = 100
 * - Drop best and worst scores; average the rest
 * - Compute based on overall score, and per position
 * - Merge name lists - based on refID, first/last, nicknames, 
 * 
 * HOF Distribution :
 * - 31 Starters, 5 Relievers (1 Swingman)
 * - C=8, 1B=9, 2B=10, 3B=6, SS=11, LF=11, CF=8, RF=12, DH=1 (9.5 per position, or 9 per pos + 4 other)
 * - Average of Top 20 for each fielder, Top 70 SP, Top 10 RP is norm value
 * - Combine these results, then normalize for 0-100 score
 * - Treat DH as secondary position (EMartinez=3B, Thomas=1B), MR/CL as RP
 * 
 * Ranking sources:
 * - rWAR
 * - wWAR (mine based on rWAR)
 * - norm WAR : rWAR * rWAR / careerNorm (average IP/PA of top 100 WAR)
 *   - turn IP & PA into "seasons", then divide total WAR by total seasons
 * - rWAA (positive seasons only)
 * - HOF votes : best overall % - which is better 80% in year 3 or 75% in year 2?
 *   - Chance distribution:: 1:43, 2:7, 3:9, 4:8, 5:6, 6:3, 7:4, 8:2, 9:5, 10:3, 11:6, 12:1, 13:3, 14:2, 15+:5
 *   - 100 = 95% year 1, 50 = 75% year 1, 0 = 75% year 15+
 * - Hall of 100 - only goes to 125, and no relative score
 *   - use GAR to supplement?  rWAR + 1.6 * (Top 5 seasons), repl is avg(20-30th per pos)
 * - Hall of Stats
 * - fWAR
 * - JAWS : average of rWAR + top 7 seasons
 * - WSAB (Win Shares Above Bench) [BaseballGauge]
 * - HOFm (Monitor)
 * - HOFs (Career Standards)
 * - Hall of Merit : votes relative per year
 *   - Can this be combined into one overall ranking?
 *   - Is there another ranking on the site that will work better?
 * - ELO : combine batting and pitching
 *   - Put it on a WAR scale, assuming top pitching isn't undervalued
 *   - Or, adjust one to be on the scale of another (somehow)
 *   - Pit = 1.019E-06x3 - 6.196E-03x2 + 1.264E+01x - 6.782E+03 (Bat) [5 to 2 ratio, 0.972 R^2]
 * - Baseball Prospectus???
 * - Black/Gray Ink?, HR/ERA+/OPS+/K?????
 * - 4+ win seasons only (ideally, subtract 4 from each season)
 * - 2*WAA - WAR : i.e., subtract another replacement level from WAA, making it roughly 4W/season
 * - positive component RAA (each component is added only if it is positive)
 *   - ideally, flatten out rBat by splitting into 2 (OB vs. SLG) or 3 (BB/SO vs. BABIP vs. SLG) components
 * 
 * Make ranks based on many different scores:
 * - WAR, wWAR, WAR/career
 * - Top N per position, of the above
 * Norm value is weighted average of +/-10% of number (9.5 means between 8.55 and 10.45 -- so 9@.5/.95 and 10@.5/.95)
 * How to calculate norm for DHs?
 */
public class TopRanks {
  private static final int YEAR_YOUNGEST = 1968;
//  private static final int YEAR_RETIRE = 2020;
  private static final int TOTAL_PICKS = 112;
//  private static final int POS_PICKS = 10;
//  private static final int SP_PICKS = 35;
//  private static final int RP_PICKS = 5;

  private enum ScoreType implements Comparator<Player> {
    WAR("WAR"),
    WWAR("wWAR"),
//    CAREER("Career"),
    ELO("ELO");
    
    public String getName() { return _name; }

    ScoreType(String name) { _name = name; }
    private final String _name;
    @Override public int compare(Player arg0, Player arg1) {
      Double s0 = arg0._raw.get(this), s1 = arg1._raw.get(this);
      if (s0 == null) { return s1 == null ? 0 : 1; }
      else if (s1 == null) { return -1; }
      if (s0.equals(s1)) { return 0; }
      return s0.doubleValue() > s1.doubleValue() ? -1 : 1;
    }
  }
  
  private static class Player implements Comparable<Player> {
    public Player(Master m) {
      _master_info = m;
      _hof_info = _hof.idFirst(m.hofID());
      _wwar_info = _wwBy.get(m.playerID());
      _wapp_info = _waBy.get(m.playerID());
      
      _raw.put(ScoreType.WAR, _wwar_info.total().war());
      _raw.put(ScoreType.WWAR, _wwar_info.total().wwar());
      // FIXME calculate war_career
//      _raw.put(ScoreType.CAREER, _raw_war / _wwar_info.size());
      // FIXME re-normalize ELO WAR
      ELO elo = _elo.id(m.playerID());
      if (elo != null) { _raw.put(ScoreType.ELO, elo.norm()); }
    }
    
    public final Master _master_info;
    public final HOF _hof_info;
    public final ByPlayer<WeightedWar> _wwar_info;
    public final ByPlayer<WeightedApp> _wapp_info;

    public TreeMap<ScoreType, Double> _raw = new TreeMap<>();
    public TreeMap<ScoreType, Double> _all = new TreeMap<>();
//    public TreeMap<ScoreType, Double> _pos = new TreeMap<>();
    
    private double _total = -1000;
    
    public double total() {
      if (_total == -1000) {
        _total = 0;
        for (Double sc : _all.values()) { _total += sc; }
      }
      return _total;
    }

    @Override public int compareTo(Player o) {
      if (this.total() == o.total()) { return 0; }
      return this.total() > o.total() ? -1 : 1;
    }
  }
  
  private static Master.Table _master = null;
  private static HOF.Table _hof = null;
  private static WeightedWar.ByID _wwBy = null;
  private static WeightedApp.ByID _waBy = null;
  private static ELO.Table _elo = null;
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type)); 
    WWT.adjustByPosition(AT);
    _wwBy.addAll(WWT);
    _waBy.addAll(new WeightedApp.Tally(WWT, AT));
  }
  
  public static void main(String[] args) throws Exception {
    // table initialization
    try (MyDatabase db = new MyDatabase()) {
      _master = new Master.Table(db, Sort.SORTED);
      _hof = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED);
      _elo = new ELO.Table(db);
      Appearances.Table AT = new Appearances.Table(db);
      _wwBy = new WeightedWar.ByID();
      _waBy = new WeightedApp.ByID();
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }

    ArrayList<Player> players = new ArrayList<>();
    HashMap<Position, ArrayList<Player>> bypos = new HashMap<>();
    for (ByPlayer<WeightedWar> byWW : _wwBy) {
      Master m = _master.byID(byWW.total().playerID());
      if (m.playerID() == null) { continue; } // not a player
      if (m.yearBirth() > YEAR_YOUNGEST) { continue; } // too young
      if (m.yearBirth() < YEAR_YOUNGEST - TOTAL_PICKS) { continue; } // too old
      if (byWW.total().wwar() < 0) { continue; } // no chance
      try {
        Player p = new Player(m);
        players.add(p);
        ByPlayer<WeightedApp> wapp_info = p._wapp_info;
        WeightedApp wapp = wapp_info.total();
        WeightedApp.Use position = wapp.primary();
        Position pos = position.pos();
        ArrayList<Player> list = bypos.get(pos);
        if (list == null) { bypos.put(pos, list = new ArrayList<>()); }
        list.add(p);
      } catch (Throwable T) {
        System.err.println("Could not get " + m.playerID() + " : " + T);
      }
    }

    int ct100 = TOTAL_PICKS / 5;
    for (ScoreType type : ScoreType.values()) {
      Collections.sort(players, type);
      double norm0 = players.get(TOTAL_PICKS)._raw.get(type);
      double norm100 = 0;
      for (int i = 0; i != ct100; ++i) {
        norm100 += players.get(i)._raw.get(type);
      }
      norm100 = norm100 / ct100 - norm0;
      for (Player p : players) {
        Double d = p._raw.get(type);
        if (d != null) { p._all.put(type, (d - norm0) / norm100 * 100); }
      }
    }

    Collections.sort(players);
    System.out.print("Rk\tName\tPos\tYOB\tHOF\tTotal");
    for (ScoreType type : ScoreType.values()) {
      System.out.format("\t%s\t", type.getName());
    }
    System.out.println();
    for (int i = 0, e = TOTAL_PICKS * 2; i != e; ++i) {
      Player p = players.get(i);
      System.out.format("%d\t%s %s\t%s\t%d\t%s\t%.1f",
          i+1, p._master_info.nameFirst(), p._master_info.nameLast(),
          p._wapp_info.total().primary().pos().getName(), p._master_info.yearBirth(), p._hof_info == null ? "" : Integer.toString(p._hof_info.yearID()),
          p.total());
      for (ScoreType type : ScoreType.values()) {
        Double d = p._all.get(type);
        if (d == null) { System.out.print("\t\t"); }
        else {
          System.out.format("\t%.1f\t%.1f", d,  p._raw.get(type));
        }
      }
      System.out.println();
    }
  }
}
