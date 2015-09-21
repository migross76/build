package strat.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;
import strat.client.model.Batter;
import strat.client.model.Dice;
import strat.client.model.FatigueState;
import strat.client.model.FieldChart;
import strat.client.model.Fielding;
import strat.client.model.GameState;
import strat.client.model.LineupRest;
import strat.client.model.ParkInfo;
import strat.client.model.Pitcher;
import strat.client.model.PlayLog;
import strat.client.model.Position;
import strat.client.model.RandomBase;
import strat.client.model.RunExpChart;
import strat.client.model.RunLog;
import strat.client.model.Team;
import strat.client.model.ValueCollector;
import strat.server.DataStore;
import strat.server.PlayerInfo;
import strat.server.RandomServer;
import strat.sim.Manager;
import strat.sim.Scorer;
import strat.sim.Simulator;

/** Compute run expectancy for batting, pitching, and fielding (per position)
 * This runs a total number of games */
public class SimBatters implements Manager, Scorer {  
  private DataStore _store = new DataStore();
  private RandomBase _rand = new RandomServer(12345);
  private final RunExpChart _re;
  private final FieldChart _fc;
  private final Simulator _sim;
  private ArrayList<Pitcher> _pitchers = new ArrayList<>();
  private EnumMap<Position, ArrayList<Batter>> _batters = new EnumMap<>(Position.class);

  public static class BatStats {
    public BatStats(Batter player) { _player = player; }
    
    public final Batter _player;
    public int _g = 0;
    public int _g_active = 0;
    public double _re_bat = 0;
    public double _re_run = 0;
    
    public int _g_lh = 0;
    public int _g_rh = 0;
    public double _re_lh_card = 0;
    public double _re_rh_card = 0;
  }
  
  public static class PitchStats {
    public PitchStats(Pitcher player) { _player = player; }
    
    public final Pitcher _player;
    public int _g = 0;
    public int _g_cg = 0;
    public int _g_ineffective = 0;
    public int _g_outofgas = 0;
    public double _re_pit = 0;
    public int _bf_all = 0;
    public int _bf_active = 0;
    public int _bf_relief = 0;
    public int _bf_closer = 0;
    
    public double _bf_lh = 0;
    public double _re_lh_card = 0;
    public double _bf_rh = 0;
    public double _re_rh_card = 0;
    
    public FatigueState.Reliever _relief = null;
    public FatigueState.Reliever _closer = null;
  }
  
  public static class Counter {
    public int _g = 0;
    public double _re = 0;
    public double rate(int g) { return _re / _g * g; }
  }
  
  public TreeMap<String, BatStats> _stats_bat = new TreeMap<>();
  public TreeMap<String, PitchStats> _stats_pit = new TreeMap<>();
  public EnumMap<Position, TreeMap<String, Counter>> _stats_field = new EnumMap<>(Position.class);
  
  private static void orderByWoba(ArrayList<Batter> list, Batter b) {
    for (int i = 0; i != list.size(); ++i) {
      if (b._bwoba > list.get(i)._bwoba) { list.add(i, b); return; }
    }
    list.add(b);
  }
  
  private void setLineup(Team team, Pitcher versus) {
    team._fatigue = new FatigueState.Starter(team._pitcher);
    PitchStats ps = _stats_pit.get(team._pitcher._id);
    ps._relief = new FatigueState.Reliever(team._pitcher, FatigueState.Reliever.Type.AS_RELIEVER);
    ps._closer = new FatigueState.Reliever(team._pitcher, FatigueState.Reliever.Type.AS_CLOSER);
    team.addFielder(Position.PITCH, team._pitcher);
    ++ps._g;
    ++_stats_field.get(Position.PITCH).get(team._pitcher._id)._g;
    Dice dice = new Dice(_rand);
    EnumSet<Position> restPos = LineupRest.selectPositions(dice);

    ArrayList<Batter> lineup = new ArrayList<>();
    for (Map.Entry<Position, ArrayList<Batter>> entry : _batters.entrySet()) {
      ArrayList<Batter> batters = entry.getValue();
      Batter b = batters.get(_rand.nextInt(batters.size()));
      team.addFielder(entry.getKey(), b);
      BatStats bs = _stats_bat.get(b._id);
      ++bs._g;
      if (versus.handed() == 'R') { ++bs._g_rh; } else { ++bs._g_lh; }
      if (entry.getKey() != Position.DH) {
        ++_stats_field.get(entry.getKey()).get(b._id)._g;
      }
      if (!restPos.contains(entry.getKey()) || !LineupRest.restPlayer(b.pa(), dice)) {
        ++bs._g_active; // not a "rest" game
      }
      ValueCollector vc = new ValueCollector(b.selectColumns(versus._pitches));
      b._bwoba = vc._total / vc._ct * 12;
      orderByWoba(lineup, b);
    }
    team._lineup[0] = lineup.get(2);
    team._lineup[1] = lineup.get(1);
    team._lineup[2] = lineup.get(3);
    team._lineup[3] = lineup.get(0);
    for (int i = 4; i != 9; ++i) { team._lineup[i] = lineup.get(i); }
  }
  
  // TODO pick unique players per position
  // TODO replace duplicate players across positions (choose one randomly to keep)
  // TODO flag 'resting' players
  private void prepare(GameState gs) {
    gs._vis._pitcher = _pitchers.get(_rand.nextInt(_pitchers.size()));
    gs._home._pitcher = _pitchers.get(_rand.nextInt(_pitchers.size()));
    setLineup(gs._vis, gs._home._pitcher);
    setLineup(gs._home, gs._vis._pitcher);
  }
  
  @Override public boolean replacePitcher(GameState gs) {
    return false;
  }
  
  @Override public void rosters(Team vis, Team home) {
    /* nothing to record */
  }

  @Override public void relieve(GameState gs, Pitcher p) { /* not possible */ }

  @Override public void play(GameState gs, PlayLog pl) {
    FatigueState.Reason fatigue = gs.defense()._fatigue.state();
    PitchStats ps = _stats_pit.get(pl._pitcher._id);
    BatStats bs = _stats_bat.get(pl._batter._id);
    if (pl._main_play != null) {
      double re_plus = pl._e_bases._runexp - pl._s_bases._runexp + pl._scored.size();
      ps._re_pit -= re_plus;
      if (pl._batter.selectHanded(pl._pitcher.handed()) == 'R') {
        ++ps._bf_rh; if (pl._is_main_pitcher_card) { ps._re_rh_card -= re_plus; } 
      } else {
        ++ps._bf_lh; if (pl._is_main_pitcher_card) { ps._re_lh_card -= re_plus; }
      }
      bs._re_bat += re_plus;
      if (!pl._is_main_pitcher_card) {
        if (pl._pitcher.handed() == 'R') { bs._re_rh_card += re_plus; } else { bs._re_lh_card += re_plus; }
      }
      if (pl._range_play != null) {
        _stats_field.get(pl._fielder._onfield._pos).get(pl._fielder._id)._re -= re_plus;
      }
      if (fatigue == null) { ++ps._bf_active; } ++ps._bf_all;
      if (gs.inning() > 4 && ps._relief.collect(pl) == null) { ++ps._bf_relief; }
      if (gs.inning() > 7 && ps._closer.collect(pl) == null) { ++ps._bf_closer; }
    }
    if (pl._runner != null) {
      RunLog rl = pl._runner;
      double re_plus = rl._bases._runexp - pl._e_bases._runexp + (rl._scored ? 1 : 0);
      _stats_bat.get(rl._runner._id)._re_run += re_plus;
      if (rl._cause != RunLog.Cause.STEAL) { // batter gets credit as well
        bs._re_bat += re_plus;
      }
      if (pl._fielder != null) { // fielder's "fault"
        _stats_field.get(pl._fielder._onfield._pos).get(pl._fielder._id)._re -= re_plus;
      } else { // must be the pitcher and catcher's fault
        _stats_field.get(Position.PITCH).get(pl._pitcher._id)._re -= re_plus;
        _stats_field.get(Position.CATCH).get(gs.defense()._fielders.get(Position.CATCH)._id)._re -= re_plus;
      }
    }
  }
  
  private void recordGameEnd(Team t) {
    PitchStats ps = _stats_pit.get(t._pitcher._id);
    FatigueState.Reason reason = t._fatigue.state();
    if (reason == null) { ++ps._g_cg; }
    else if (reason == FatigueState.Reason.OUT_OF_GAS) { ++ps._g_outofgas; }
    else if (reason == FatigueState.Reason.INEFFECTIVE) { ++ps._g_ineffective; }
  }
  
  private void recordGameEnd(GameState gs) {
    recordGameEnd(gs._home);
    recordGameEnd(gs._vis);
  }
  
  private void printResults() {
    System.out.print("Name\tBt\tActive\tBat\tRun\tOff\tLH\tRH");
    EnumMap<Position, Counter> field_avg = new EnumMap<>(Position.class);
    Counter run_avg = new Counter();
    for (BatStats bs : _stats_bat.values()) {
      ++run_avg._g; run_avg._re += bs._re_run * 162.0 / bs._g;
    }
    for (Position pos : EnumSet.range(Position.CATCH, Position.RIGHT)) {
      System.out.format("\t%s", pos.code().toUpperCase());
      Counter c = new Counter();
      field_avg.put(pos, c);
      for (Counter f : _stats_field.get(pos).values()) {
        ++c._g; c._re += f.rate(162);
      }
    }
    System.out.println("\tBest\tTot\tnTot");
    double avg_run_rate = run_avg.rate(1);
    for (Map.Entry<String, BatStats> idEntry : _stats_bat.entrySet()) {
      String id = idEntry.getKey();
      BatStats bat = idEntry.getValue();
      if (bat._g == 0) { continue; }
      double active = bat._g_active / (double)bat._g;
      double per162 = 162.0 / bat._g;
      double re_bat = bat._re_bat * per162;
      double re_run = bat._re_run * per162 - avg_run_rate;
      double re_lh = bat._re_lh_card * 81.0 / bat._g_lh;
      double re_rh = bat._re_rh_card * 81.0 / bat._g_rh;
      double re_repl = 25;
      System.out.format("%s\t%s\t%.3f\t%.2f\t%.2f\t%.2f\t%.0f\t%.0f", id, bat._player.handed(), active, re_bat, re_run, re_bat + re_run, re_lh, re_rh);
      Position best_pos = Position.DH;
      double best_field = -100000000;
      int total_g = 0;
      for (Position pos : EnumSet.range(Position.CATCH, Position.RIGHT)) {
        double adjust = _pos_adjust.containsKey(pos) ?  -_pos_adjust.get(pos) : field_avg.get(pos).rate(1);
        Counter field = _stats_field.get(pos).get(id);
        System.out.print("\t");
        if (field != null && field._g > 0) {
          total_g += field._g;
          double re_field = field.rate(162) - adjust;
          System.out.format("%.2f", re_field);
          if (best_field < re_field) { best_field = re_field; best_pos = pos; }
        }
      }
      if (best_field < -10 && total_g < bat._g) { best_pos = Position.DH; best_field = 0; } 
      System.out.format("\t%s\t%.2f\t%.2f\n", best_pos.code().toUpperCase(), re_bat + re_run + best_field, (re_bat + re_run + best_field + re_repl) * active);
    }
    
    System.out.println("\nName\tPt\tCG%\tGAS%\tINEFF%"+"\tBF%\tsBF\trBF\tcBF"+"\tPit\tLH\tRH\tFld\tTot\tnTot");
    for (Map.Entry<String, PitchStats> idEntry : _stats_pit.entrySet()) {
      String id = idEntry.getKey();
      PitchStats pitch = idEntry.getValue();
      if (pitch._g == 0) { continue; }
      Counter field = _stats_field.get(Position.PITCH).get(id);
      double bf_all = pitch._bf_all / (double)pitch._g;
      double per36 = 36.0 / pitch._g;
      double active = pitch._bf_active / (double)pitch._bf_all;
      double re_main = pitch._re_pit * per36;
      double re_field = field._re * per36;
      double re_lh = pitch._re_lh_card * per36 * pitch._bf_all / pitch._bf_lh / 2;
      double re_rh = pitch._re_rh_card * per36 * pitch._bf_all / pitch._bf_rh / 2;
      double re_repl = 25;
      double g = pitch._g;
      System.out.format("%s\t%s\t%.2f\t%.2f\t%.2f"+"\t%.2f\t%.1f\t%.1f\t%.1f"+"\t%.2f\t%.0f\t%.0f\t%.2f\t%.2f\t%.2f\n",
          id, pitch._player.handed(), pitch._g_cg / g, pitch._g_outofgas / g, pitch._g_ineffective / g,
          active, bf_all, pitch._bf_relief / g, pitch._bf_closer / g,
          re_main, re_lh, re_rh, re_field, re_main + re_field, (re_main + re_field + re_repl) * active);
    }
  }
  
  public void simulate() {
    for (int i = 0; i != GAMES; ++i) {
      if (i % 10000 == 9999) { System.out.println("Playing game #" + (i+1)); }
      GameState gs = new GameState(_re, _fc);
      gs._park = ParkInfo.AVERAGE;
      prepare(gs);
      _sim.playGame(gs, this, this);
      recordGameEnd(gs);
    }
    printResults();
  }
  
  private TreeMap<String, EnumSet<Position>> _eligible = new TreeMap<>();
  private EnumMap<Position, Double> _pos_adjust = new EnumMap<>(Position.class);
  
  private void loadEligible(String data) {
    for (String line : data.split("\n")) {
      String[] tokens = line.split("\t");
      if (tokens.length < 2) { continue; }
      EnumSet<Position> set = EnumSet.noneOf(Position.class);
      for (int i = 1; i != tokens.length; ++i) { set.add(Position.map(tokens[i])); }
      _eligible.put(tokens[0], set);
    }
  }
  
  private void loadPositionalAdjustment(String data) {
    for (String line : data.split("\n")) {
      String[] tokens = line.split("\t");
      if (tokens.length < 2) { continue; }
      _pos_adjust.put(Position.map(tokens[0]), Double.parseDouble(tokens[1]));
    }
  }
  
  public SimBatters() throws IOException {
    _re = new RunExpChart(DataStore.getRunExpInfo());
    _fc = new FieldChart();
    _fc.load(DataStore.getFieldInfo());
    loadEligible(DataStore.getEligibility());
    loadPositionalAdjustment(DataStore.getPositionalAdjustment());
    _sim = new Simulator(_re);
    
    for (Position p : EnumSet.range(Position.CATCH, Position.RIGHT)) {
      _batters.put(p, new ArrayList<Batter>());
      _stats_field.put(p, new TreeMap<String, Counter>());
    }
    _stats_field.put(Position.PITCH, new TreeMap<String, Counter>());
    _batters.put(Position.DH, new ArrayList<Batter>());
    
    for (PlayerInfo pi : _store.allPlayers()) {
      if (pi._isPitcher) {
        Pitcher p = new Pitcher(pi._id, pi._mainPos, new ArrayList<>(Arrays.asList(pi._cardData.split("\n"))), ParkInfo.AVERAGE);
        _pitchers.add(p);
        _stats_pit.put(pi._id, new PitchStats(p));
        _stats_field.get(Position.PITCH).put(pi._id, new Counter());
      } else {
        Batter b = new Batter(pi._id, pi._mainPos, new ArrayList<>(Arrays.asList(pi._cardData.split("\n"))), ParkInfo.AVERAGE);
        _stats_bat.put(pi._id, new BatStats(b));
        EnumSet<Position> eligible = _eligible.get(pi._id);
        for (Fielding f : b._fielding.values()) {
          if (eligible == null || eligible.contains(f._pos)) { 
            _batters.get(f._pos).add(b);
            _stats_field.get(f._pos).put(pi._id, new Counter());
          }
        }
        if (eligible == null || eligible.contains(Position.DH)) {
          _batters.get(Position.DH).add(b);
        }
      }
    }
  }
  
  private static final int GAMES = 4000000;
  
  public static void main(String[] args) throws Exception {
    new SimBatters().simulate();
  }
}
