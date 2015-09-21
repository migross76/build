package strat.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import strat.client.model.Batter;
import strat.client.model.Dice;
import strat.client.model.FieldChart;
import strat.client.model.Fielding;
import strat.client.model.GameLog;
import strat.client.model.GameState;
import strat.client.model.LineupLog;
import strat.client.model.LineupRest;
import strat.client.model.ParkInfo;
import strat.client.model.Pitcher;
import strat.client.model.PlayLog;
import strat.client.model.Position;
import strat.client.model.RandomBase;
import strat.client.model.RunExpChart;
import strat.client.model.Team;
import strat.client.model.ValueCollector;
import strat.server.DataStore;
import strat.server.PlayerInfo;
import strat.server.RandomServer;
import strat.server.TeamInfo;
import strat.server.TeamPrinter;
import strat.shared.BaseState;
import strat.sim.Manager;
import strat.sim.Scorer;
import strat.sim.Simulator;

public class SimSeasons {  
  private DataStore _store = new DataStore();
  private GameState _gs = null;
  private RandomBase _rand = new RandomServer(12345);
  private final RunExpChart _re;
  private final FieldChart _fc;
  private final Simulator _sim;
  
  public static boolean RANDOM_TEAM = false;

  private static Pitcher convertPitcher(PlayerInfo info, ParkInfo park) {
    Pitcher p = new Pitcher(info._id, info._mainPos, new ArrayList<>(Arrays.asList(info._cardData.split("\n"))), park);
    ValueCollector vcL = new ValueCollector(p._asL);
    ValueCollector vcR = new ValueCollector(p._asR);
    p._woba = vcL._total / vcL._ct + vcR._total / vcR._ct;
    return p;
  }
  
  private static Batter convertBatter(PlayerInfo info, ParkInfo park) {
    return new Batter(info._id, info._mainPos, new ArrayList<>(Arrays.asList(info._cardData.split("\n"))), park);
  }
  
  private static Comparator<Pitcher> COMPARE_PITCHER = new Comparator<Pitcher>() {
    @Override public int compare(Pitcher arg0, Pitcher arg1) {
      double cmp = arg0._woba - arg1._woba;
      if (cmp != 0) { return cmp < 0 ? -1 : 1; }
      return arg0._id.compareTo(arg1._id);
    }
  };
  
  private void populatePitchers(ArrayList<PlayerInfo> info, Team team) {
    if (RANDOM_TEAM) {
      team._pitcher = convertPitcher(info.get(_rand.nextInt(8) < 5 ? 0 : 1), _gs._park);
      switch (_rand.nextInt(4)) {
        case 0: team._pitcher._bfLimit = 27; break;
        case 1: team._pitcher._bfLimit = 36; break;
        case 2: team._pitcher._bfLimit = 36; break;
        case 3: team._pitcher._bfLimit = 45; break;
      }
      for (int i = 5; i != info.size(); ++i) {
        Pitcher p = convertPitcher(info.get(i), _gs._park);
        p._bfLimit = (i-3) * 3; // 6, 9, 12
        team._bullpen.add(p);
      }
    } else {
      for (int i = 0; i != 5; ++i) {
        team._rotation.add(convertPitcher(info.get(i), _gs._park));
      }
      Collections.sort(team._rotation, COMPARE_PITCHER);
      team._pitcher = team._rotation.get(_rand.nextInt(9) / 2); // 01, 23, 45, 67, 8
      for (int i = 5; i != info.size(); ++i) {
        team._bullpen.add(convertPitcher(info.get(i), _gs._park));
      }
      Collections.sort(team._bullpen, COMPARE_PITCHER);
      team._bullpen.get(0)._bfLimit = 8 + _rand.nextInt(4);
      team._bullpen.get(1)._bfLimit = 5 + _rand.nextInt(3);
      team._bullpen.get(2)._bfLimit = 10000;
      if (_rand.nextInt(3) == 0) { team._bullpen.add(team._bullpen.remove(0)); } // main reliever needs a rest every third game
      if (team._pitcher._woba < team._bullpen.get(0)._woba) { team._pitcher._bfLimit = 40 + _rand.nextInt(9); }
      else if (team._pitcher._woba < team._bullpen.get(2)._woba) { team._pitcher._bfLimit = 31 + _rand.nextInt(9); }
      else { team._pitcher._bfLimit = 22 + _rand.nextInt(9); }
    }
    team.addFielder(Position.PITCH, team._pitcher);
    team._pitcher._firstBatter = 0;
  }
  
  private static void orderByWoba(ArrayList<Batter> list, Batter b, boolean useFielding) {
    double mine = b._bwoba + (useFielding ? b._fwoba : 0);
    for (int i = 0; i != list.size(); ++i) {
      double yours = list.get(i)._bwoba + (useFielding ? list.get(i)._fwoba : 0);
      if (mine > yours) { list.add(i, b); return; }
    }
    list.add(b);
  }
  
  private Batter selectDH(Team team, ArrayList<Batter> dhs) {
    if (RANDOM_TEAM) {
      return dhs.remove(_rand.nextInt(dhs.size()));
    }
    double bestWoba = 0; Batter best = null;
    for (Batter dh : dhs) {
      Position pos = Position.map(dh._primary);
      Batter b = (Batter)team._fielders.get(pos);
      double diff = dh._fwoba - b._fwoba;
      double woba = dh._bwoba + (diff > 0 ? diff : 0);
      if (bestWoba < woba) { bestWoba = woba; best = dh; }
    }
    dhs.remove(best);
    return best;
  }
  
  private static void ordering(ArrayList<Batter> list, Batter b, boolean useFielding) {
    if (RANDOM_TEAM) { list.add(b); } else { orderByWoba(list, b, useFielding); } 
  }

  private Collection<LineupLog> populateBatters(ArrayList<PlayerInfo> info, Team team, Pitcher versus) {
    HashMap<Batter, LineupLog> lineuplog = new HashMap<>();
    HashMap<String, ArrayList<Batter>> _batters = new HashMap<>();
    for (PlayerInfo pi : info) {
      Batter b = convertBatter(pi, _gs._park);
      ArrayList<Batter> list = _batters.get(b._primary);
      if (list == null) { _batters.put(b._primary, list = new ArrayList<>()); }
      ValueCollector vc = new ValueCollector(b.selectColumns(versus._pitches));
      Position pos = Position.map(b._primary);
      Fielding bf = b._fielding.get(pos);
      b._bwoba = vc._total / vc._ct * 12;
      b._fwoba = - _fc.woba(bf) * bf._pos.weight();
      ordering(list, b, true);
      lineuplog.put(b, new LineupLog(b, versus._pitches));
    }
    Dice dice = new Dice(_rand);
    EnumSet<Position> restPos = LineupRest.selectPositions(dice);
    ArrayList<Batter> lineup = new ArrayList<>();
    ArrayList<Batter> dhs = new ArrayList<>();
    for (Position pos : EnumSet.range(Position.CATCH, Position.RIGHT)) {
      ArrayList<Batter> list = _batters.get(pos.code());
      Batter selected = list.get(0);
      lineuplog.get(selected)._primary = true;
      if (restPos.contains(pos) && LineupRest.restPlayer(selected.pa(), dice) && !RANDOM_TEAM) {
        lineuplog.get(selected)._resting = true;
        ordering(team._bench, selected, true);
        selected = list.get(1);
      } else {
        ordering(dhs, list.get(1), false);
      }
      team.addFielder(pos, selected);
      orderByWoba(lineup, selected, false);
    }
    // designated hitter
    Batter selected = selectDH(team, dhs);
    lineuplog.get(selected)._primary = true; lineuplog.get(selected)._asDH = true;
    if (restPos.contains(Position.DH) && LineupRest.restPlayer(selected.pa(), dice) && !RANDOM_TEAM) {
      lineuplog.get(selected)._resting = true;
      ordering(team._bench, selected, true);
      selected = selectDH(team, dhs);
      lineuplog.get(selected)._asDH = true;
    }
    for (Batter b : dhs) { ordering(team._bench, b, true); }
    orderByWoba(lineup, selected, false);
    
    if (!RANDOM_TEAM) {
      Position pos = Position.map(selected._primary);
      Batter firstPick = (Batter)team._fielders.get(pos);
      if (selected._fwoba > firstPick._fwoba) {
        lineuplog.get(selected)._asDH = false;
        lineuplog.get(firstPick)._asDH = true;
        team.addFielder(pos, selected);
        selected = firstPick;
        selected._onfield = null;
      }
    }
    team.addFielder(Position.DH, selected);
    team._lineup[0] = lineup.get(2);
    team._lineup[1] = lineup.get(1);
    team._lineup[2] = lineup.get(3);
    team._lineup[3] = lineup.get(0);
    for (int i = 4; i != 9; ++i) { team._lineup[i] = lineup.get(i); }
    for (int i = 0; i != 9; ++i) { lineuplog.get(team._lineup[i])._num = i+1; }
    return lineuplog.values();
  }
  
  private ArrayList<LineupLog> prepare() throws IOException {
    HashSet<String> claimed = new HashSet<>();
    ArrayList<LineupLog> lineuplog = new ArrayList<>();
    TeamInfo vis = _store.generateTeam(claimed);
    TeamInfo home = _store.generateTeam(claimed);
    _gs = new GameState(_re, _fc);
    _gs._park = ParkInfo.AVERAGE;
    populatePitchers(vis._pitchers, _gs._vis);
    populatePitchers(home._pitchers, _gs._home);
    lineuplog.addAll(populateBatters(vis._batters, _gs._vis, _gs._home._pitcher));
    lineuplog.addAll(populateBatters(home._batters, _gs._home, _gs._vis._pitcher));
    return lineuplog;
  }
  
  private static class MyMgr implements Manager {
    @Override public boolean replacePitcher(GameState gs) {
      if (!gs.defense()._bullpen.isEmpty()) {
        Team team = gs.defense();
        int bf_remaining = team._pitcher._bfLimit + team._pitcher._firstBatter - gs.offense()._atBat;
        if (bf_remaining <= 0 || gs.isLeadoff() && bf_remaining < 3) {
          team._pitcher = team._bullpen.remove(0);
          team._pitcher._firstBatter = gs.offense()._atBat;
          team.addFielder(Position.PITCH, team._pitcher);
          return true;
        }
      }
      return false;
    }
  }
  
  private static class Official implements Scorer {
    @Override public void rosters(Team vis, Team home) {
      _gl.addTeam(home);
      _gl.addTeam(vis);
    }

    @Override public void relieve(GameState gs, Pitcher p) {
      _gl._subs.add(p);
    }

    @Override public void play(GameState gs, PlayLog log) {
      _gl._plays.add(log);
    }
    
    public Official(GameLog gl) { _gl = gl; }
    
    private final GameLog _gl;
  }
  
  private static class PlayByPlay implements Scorer {
    @Override public void rosters(Team vis, Team home) {
      TeamPrinter.print(vis, home);
      System.out.println();
    }

    @Override public void relieve(GameState gs, Pitcher p) {
      if (gs.isLeadoff()) {
        System.out.format("Facing %s %s (%c)\n", p._nameFirst, p._nameLast, p.handed());
      }
    }

    @Override public void play(GameState gs, PlayLog log) {
      if (log._leadoff) {
        Pitcher p = log._pitcher;
        System.out.format("Facing %s %s (%c)\n", p._nameFirst, p._nameLast, p.handed());
      }
      boolean gotplay = true;
      if (log._main_play != null) {
        System.out.format("%d\t%s %s (%c) %s\t%s", (log._atBat%9)+1, log._batter._nameFirst, log._batter._nameLast, log._batter.handed(), log._bat_pos.code().toUpperCase(), log._main_play.createShortLine());
        gotplay = true;
      } else {
        System.out.print("\t\t");
      }
      if (log._range_play != null) {
        System.out.format(" -> %s(%c) + e%d", log._range_play._name, log._range_play._play._type.code(), log._error_play);
      }
      if (log._special != null) {
        if (gotplay) { System.out.print(" -> "); }
        System.out.print(log._special.name());
      }
      if (log._runner != null) {
        System.out.format(" -> %s(%d/%d)", log._runner._type.name(), log._runner._safe_max, log._runner._hold_max);
      }
      System.out.print("\t");
      for (Dice.Roll roll : log._dice.rolls()) {
        System.out.print(roll._result + " ");
      }
      BaseState bases = log._runner == null ? log._e_bases : log._runner._bases;
      System.out.format("\t%c%d %d out%s\t", log._top ? 'T' : 'B', log._inning, bases._outs, bases._outs == 1 ? "" : "s");
      for (int i = 0; i != 3; ++i) { System.out.print(bases._onbase[i] == null ? "-" : (i+1) + ""); }
      int runs = log._scored.size();
      if (log._runner != null && log._runner._scored) { ++runs; } 
      System.out.format("\t%.3f\t%d-%d\t", bases._runexp - log._s_bases._runexp + runs, gs._vis._runs, gs._home._runs);
      for (Batter b : log._scored) {
        System.out.format("%s %s  ", b._nameFirst, b._nameLast);
      }
      if (log._runner != null && log._runner._scored) { System.out.format("[%s %s]", log._runner._runner._nameFirst, log._runner._runner._nameLast); }
      System.out.println();
    }
  }
  
  public void playGame() throws IOException {
    prepare();
    _sim.playGame(_gs, new PlayByPlay(), new MyMgr());
  }
  
  public void playSeasons(int seasons) throws IOException {
    for (int i = 0; i != seasons * GAMES_PER_SEASON; ++i) {
      if (i % GAMES_PER_SEASON == 0) { System.out.println("Playing season #" + (i/GAMES_PER_SEASON+1)); }
      GameLog gl = new GameLog();
      gl._lineup = prepare();
      _sim.playGame(_gs, new Official(gl), new MyMgr());
      if (_gs._home._runs > _gs._vis._runs) {
        gl._winner = _gs._home._pitcher; gl._loser = _gs._vis._pitcher;
      } else {
        gl._winner = _gs._vis._pitcher; gl._loser = _gs._home._pitcher;
      }
      _store.saveStats(gl);
    }
    if (seasons > 0) {
      System.out.println();
      _store.printStats(seasons);
    }
  }
  
  public SimSeasons() throws IOException {
    _re = new RunExpChart(DataStore.getRunExpInfo());
    _fc = new FieldChart();
    _fc.load(DataStore.getFieldInfo());
    _sim = new Simulator(_re);
  }
  
  private static final int SEASONS = 100;
  private static final int GAMES = 0;
  private static final int GAMES_PER_SEASON = 810;
  
  public static void main(String[] args) throws Exception {
    SimSeasons sim = new SimSeasons();
    sim.playSeasons(SEASONS);
    for (int i = 0; i != GAMES; ++i) { sim.playGame(); }
  }
}
