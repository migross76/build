package strat.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import strat.client.Static;
import strat.client.event.InitResponse;
import com.google.gwt.core.client.GWT;
import com.google.web.bindery.event.shared.binder.EventBinder;
import com.google.web.bindery.event.shared.binder.EventHandler;

public class LineupBot {
  private final MyEventBinder _bindEvent = GWT.create(MyEventBinder.class);
  interface MyEventBinder extends EventBinder<LineupBot> {/*binder*/}
  
  public boolean RANDOM_TEAM = false;

  private static void selectDH(HashMap<Position, Batter> lineup, ArrayList<Batter> dhs) {
/*
    if (RANDOM_TEAM) {
      return dhs.remove(_rand.nextInt(dhs.size()));
    }
*/
    double bestWoba = 0; Batter best = null;
    for (Batter dh : dhs) {
      Batter curr = lineup.get(Position.map(dh._primary));
      double diff = dh._fwoba - curr._fwoba;
      double woba = dh._bwoba + (diff > 0 ? diff : 0);
      if (bestWoba < woba) { bestWoba = woba; best = dh; }
    }
    if (best == null) { throw new NullPointerException("best was not found"); }
    dhs.remove(best);
    Position pos = Position.map(best._primary);
    Batter other = lineup.get(pos);
    if (other._fwoba < best._fwoba) {
      lineup.put(pos, best); lineup.put(Position.DH, other);
    } else {
      lineup.put(Position.DH, best);
    }
  }
  
  private static class ByWOBA implements Comparator<Batter> {
    @Override public int compare(Batter o1, Batter o2) {
      double w1 = o1._bwoba + (_useFielding ? o1._fwoba : 0);
      double w2 = o2._bwoba + (_useFielding ? o2._fwoba : 0);
      return w1 > w2 ? -1 : 1;
    }
    
    public ByWOBA(boolean useFielding) { _useFielding = useFielding; }
    private final boolean _useFielding;
  }
  private static ByWOBA _byOverall = new ByWOBA(true);
  private static ByWOBA _byOffense = new ByWOBA(false);

  private static void orderByWoba(ArrayList<Batter> list, Batter b, boolean useFielding) {
    double mine = b._bwoba + (useFielding ? b._fwoba : 0);
    for (int i = 0; i != list.size(); ++i) {
      double yours = list.get(i)._bwoba + (useFielding ? list.get(i)._fwoba : 0);
      if (mine > yours) { list.add(i, b); return; }
    }
    list.add(b);
  }
  
  private void ordering(ArrayList<Batter> list, Batter b, boolean useFielding) {
    if (RANDOM_TEAM) { list.add(b); } else { orderByWoba(list, b, useFielding); } 
  }
/*
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
*/
  public ArrayList<Batter> generate(HashMap<Position, Batter> current, ArrayList<Batter> eligible, Pitcher versus) {
    HashMap<String, ArrayList<Batter>> batters = new HashMap<>();
    for (Batter b : eligible) {
      ArrayList<Batter> list = batters.get(b._primary);
      if (list == null) { batters.put(b._primary, list = new ArrayList<>()); }
      ValueCollector vc = new ValueCollector(b.selectColumns(versus._pitches));
      Position pos = Position.map(b._primary);
      Fielding bf = b._fielding.get(pos);
      b._bwoba = vc._total / vc._ct * 12;
      b._fwoba = - _fc.woba(bf) * bf._pos.weight();
      ordering(list, b, true);
    }
    ArrayList<Batter> dhs = new ArrayList<>();
    for (Position pos : EnumSet.range(Position.CATCH, Position.RIGHT)) {
      ArrayList<Batter> list = batters.get(pos.code());
      if (current.containsKey(pos)) {
        list.remove(current.get(pos)); // already selected player
      } else {
        Collections.sort(list, _byOverall);
        current.put(pos, list.remove(0)); // best player
      }
      dhs.addAll(list);
    }
    if (!current.containsKey(Position.DH)) { selectDH(current, dhs); }
    ArrayList<Batter> lineup = new ArrayList<>(current.values());
    Collections.sort(lineup, _byOffense);
    Batter temp = lineup.get(2);
    temp = lineup.set(0, temp); // third-best bats leadoff
    temp = lineup.set(3, temp); // best[#0] bats cleanup
    lineup.set(2, temp); // fourth-best bats in the 3 slot 
    return lineup;
  }
  
/*  
  public ArrayList<Batter> generate(ArrayList<Batter> eligible, Pitcher versus) {
    HashMap<String, ArrayList<Batter>> batters = new HashMap<>();
    for (Batter b : eligible) {
      ArrayList<Batter> list = batters.get(b._primary);
      if (list == null) { batters.put(b._primary, list = new ArrayList<>()); }
      ValueCollector vc = new ValueCollector(b.selectColumns(versus._pitches));
      Position pos = Position.map(b._primary);
      Fielding bf = b._fielding.get(pos);
      b._bwoba = vc._total / vc._ct * 12;
      b._fwoba = - _fc.woba(bf) * bf._pos.weight();
      ordering(list, b, true);
    }
    ArrayList<Batter> lineup = new ArrayList<>();
    ArrayList<Batter> dhs = new ArrayList<>();
    for (Position pos : EnumSet.range(Position.CATCH, Position.RIGHT)) {
      ArrayList<Batter> list = batters.get(pos.code());
      Batter selected = list.get(0);
      ordering(dhs, list.get(1), false);
      orderByWoba(lineup, selected, false);
    }
    Batter selected = selectDH(lineup, dhs);
    orderByWoba(lineup, selected, false);
    Batter temp = lineup.get(2);
    temp = lineup.set(0, temp); // third-best bats leadoff
    temp = lineup.set(3, temp); // best[#0] bats cleanup
    lineup.set(2, temp); // fourth-best bats in the 3 slot
    return lineup;
  }
*/
  @EventHandler void onInit(InitResponse event) {
    _fc = event._field;
  }
  
  public LineupBot() {
    _bindEvent.bindEventHandlers(this, Static.eventBus());
  }
  
  private FieldChart _fc = null;
}
