package strat.sim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import strat.client.model.Batter;
import strat.client.model.FieldChart;
import strat.client.model.Fielding;
import strat.client.model.GameState;
import strat.client.model.LineupLog;
import strat.client.model.ParkInfo;
import strat.client.model.Pitcher;
import strat.client.model.Position;
import strat.client.model.RandomBase;
import strat.client.model.Team;
import strat.client.model.ValueCollector;
import strat.server.DataStore;
import strat.server.PlayerInfo;
import strat.server.RandomServer;
import strat.server.TeamInfo;

public class RandomTeamSelector {
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
  
  private void populatePitchers(ArrayList<PlayerInfo> info, Team team, ParkInfo park) {
    team._pitcher = convertPitcher(info.get(_rand.nextInt(8) < 5 ? 0 : 1), park);
    switch (_rand.nextInt(4)) {
      case 0: team._pitcher._bfLimit = 27; break;
      case 1: team._pitcher._bfLimit = 36; break;
      case 2: team._pitcher._bfLimit = 36; break;
      case 3: team._pitcher._bfLimit = 45; break;
    }
    for (int i = 5; i != info.size(); ++i) {
      Pitcher p = convertPitcher(info.get(i), park);
      p._bfLimit = (i-3) * 3; // 6, 9, 12
      team._bullpen.add(p);
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
    return dhs.remove(_rand.nextInt(dhs.size()));
  }
  
  private static void ordering(ArrayList<Batter> list, Batter b, boolean useFielding) {
    list.add(b);
  }

  private Collection<LineupLog> populateBatters(ArrayList<PlayerInfo> info, Team team, Pitcher versus, ParkInfo park) {
    HashMap<Batter, LineupLog> lineuplog = new HashMap<>();
    HashMap<String, ArrayList<Batter>> _batters = new HashMap<>();
    for (PlayerInfo pi : info) {
      Batter b = convertBatter(pi, park);
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
    ArrayList<Batter> lineup = new ArrayList<>();
    ArrayList<Batter> dhs = new ArrayList<>();
    for (Position pos : EnumSet.range(Position.CATCH, Position.RIGHT)) {
      ArrayList<Batter> list = _batters.get(pos.code());
      Batter selected = list.get(0);
      lineuplog.get(selected)._primary = true;
      ordering(dhs, list.get(1), false);
      team.addFielder(pos, selected);
      orderByWoba(lineup, selected, false);
    }
    // designated hitter
    Batter selected = selectDH(team, dhs);
    lineuplog.get(selected)._primary = true; lineuplog.get(selected)._asDH = true;
    for (Batter b : dhs) { ordering(team._bench, b, true); }
    orderByWoba(lineup, selected, false);
    
    team.addFielder(Position.DH, selected);
    team._lineup[0] = lineup.get(2);
    team._lineup[1] = lineup.get(1);
    team._lineup[2] = lineup.get(3);
    team._lineup[3] = lineup.get(0);
    for (int i = 4; i != 9; ++i) { team._lineup[i] = lineup.get(i); }
    for (int i = 0; i != 9; ++i) { lineuplog.get(team._lineup[i])._num = i+1; }
    return lineuplog.values();
  }
  
  public ArrayList<LineupLog> prepare(GameState gs) throws IOException {
    HashSet<String> claimed = new HashSet<>();
    ArrayList<LineupLog> lineuplog = new ArrayList<>();
    TeamInfo vis = _store.generateTeam(claimed);
    TeamInfo home = _store.generateTeam(claimed);
    populatePitchers(vis._pitchers, gs._vis, gs._park);
    populatePitchers(home._pitchers, gs._home, gs._park);
    lineuplog.addAll(populateBatters(vis._batters, gs._vis, gs._home._pitcher, gs._park));
    lineuplog.addAll(populateBatters(home._batters, gs._home, gs._vis._pitcher, gs._park));
    return lineuplog;
  }
  
  public RandomTeamSelector(DataStore store, FieldChart fc) { _store = store; _fc = fc; }
  
  private final DataStore _store;
  private final FieldChart _fc;
  private final RandomBase _rand = new RandomServer(12345);
}
