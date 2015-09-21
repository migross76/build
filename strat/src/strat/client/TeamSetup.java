package strat.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import strat.client.event.LeagueInfoRequest;
import strat.client.event.LeagueInfoResponse;
import strat.client.event.TeamReadyToggle;
import strat.client.event.TeamRequest;
import strat.client.event.TeamResponse;
import strat.client.model.Batter;
import strat.client.model.Fielding;
import strat.client.model.Pitcher;
import strat.client.model.Position;
import strat.client.model.Team;
import strat.client.model.TeamLocation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.web.bindery.event.shared.binder.EventBinder;
import com.google.web.bindery.event.shared.binder.EventHandler;

public class TeamSetup {
  private final MyEventBinder _bindEvent = GWT.create(MyEventBinder.class);
  interface MyEventBinder extends EventBinder<TeamSetup> {/*binder*/}

  private static String boxSelection(ListBox lb, boolean hasNull) {
    int index = lb.getSelectedIndex();
    if (hasNull && index == 0) { return null; }
    return lb.getValue(index);
  }
  
  private static class PositionInfo {
    int _slot = 0;
    ListBox _posBox = new ListBox();
    ListBox _batBox = new ListBox();
    Position _pos = null;
    String _batID = null;
    
    public PositionInfo(Iterable<Position> poses, int slot) {
      _batBox.addItem("Select...");
      for (Position pos : poses) {
        _posBox.addItem(pos.code().toUpperCase());
      }
      _posBox.setItemSelected(slot, true);
      _pos = Position.map(_posBox.getValue(slot).toLowerCase());
      _slot = slot;
    }
  }
  
  private PositionInfo[] _lineup = new PositionInfo[9];

  public void onTeamSelected() {
    String team = boxSelection(_team, true);
    if (team == null) { _pitch.clear(); }
    else { Static.fire(new TeamRequest(_league, team, _location)); }
  }
  
  public void clear() {
    _team.clear();
    _pitch.clear();
  }
  
  private ChangeHandler _posSelect = new ChangeHandler() {
    @Override public void onChange(ChangeEvent event) {
      PositionInfo newPI = null, oldPI = null;
      ListBox myBox = (ListBox)event.getSource();
      Position myPos = Position.map(boxSelection(myBox, false).toLowerCase());
      for (PositionInfo pi : _lineup) {
        if (pi._posBox == myBox) { newPI = pi; }
        else if (pi._pos == myPos) { oldPI = pi; }
      }
      if (oldPI == null || newPI == null) { throw new IllegalArgumentException("could not find old or new PI"); }
      ListBox myBat = oldPI._batBox;
      String myBatID = oldPI._batID;
      // set values of oldPI
      oldPI._batBox = newPI._batBox;
      oldPI._batID = newPI._batID;
      oldPI._pos = newPI._pos;
      for (int i = 0; i != oldPI._posBox.getItemCount(); ++i) {
        if (oldPI._posBox.getValue(i).toLowerCase().equals(oldPI._pos.code())) { oldPI._posBox.setItemSelected(i, true); }
      }
      // set values of newPI
      newPI._batBox = myBat;
      newPI._batID = myBatID;
      newPI._pos = myPos;
      _bat.setWidget(oldPI._slot, 2, oldPI._batBox);
      _bat.setWidget(newPI._slot, 2, newPI._batBox);
    }
  };
  
  public boolean isReady() { return _ready; }
  
  public Team createTeam() {
    Team team = new Team();
    
    team._name = boxSelection(_team, true);
    for (int i = 0; i != _lineup.length; ++i) {
      Batter b = _batters.remove(_lineup[i]._batID);
      team.addFielder(_lineup[i]._pos, team._lineup[i] = b);
    }
    team._bench.addAll(_batters.values());
    Pitcher p = _starters.remove(boxSelection(_pitch, true));
    team.addFielder(Position.PITCH, team._pitcher = p);
    team._rotation.addAll(_starters.values());
    team._bullpen.addAll(_bullpen.values());
    return team;
  }
  
  private void setReady() {
    _ready = _pitch.getSelectedIndex() != 0;
    for (PositionInfo pi : _lineup) {
      if (pi._batID == null) { _ready = false; break; }
    }
  }
  
  public Pitcher getPitcher() {
    return _starters.get(boxSelection(_pitch, true));
  }
  
  private ChangeHandler _pitchSelect = new ChangeHandler() {
    @Override public void onChange(ChangeEvent event) {
      if (_ready && _pitch.getSelectedIndex() == 0) { Static.fire(new TeamReadyToggle(false)); }
      setReady();
      if (_ready) { Static.fire(new TeamReadyToggle(true)); }
    }
  };
  
  private ChangeHandler _batSelect = new ChangeHandler() {
    @Override public void onChange(ChangeEvent event) {
      PositionInfo newPI = null, oldPI = null;
      ListBox myBox = (ListBox)event.getSource();
      String myBat = boxSelection(myBox, true);
      for (PositionInfo pi : _lineup) {
        if (pi._batBox == myBox) { newPI = pi; }
        else if (Objects.equals(myBat, pi._batID)) { oldPI = pi; }
      }
      if (newPI == null) { throw new IllegalArgumentException("could not find new PI"); }
      newPI._batID = myBat;
      if (oldPI != null) {
        oldPI._batBox.setItemSelected(0, true);
        oldPI._batID = null;
        if (_ready) { _ready = false; Static.fire(new TeamReadyToggle(false)); } // was ready, but no longer
      } else {
        setReady();
      }
      if (_ready) { Static.fire(new TeamReadyToggle(true)); }
    }
  };

  @EventHandler void onLoadRoster(TeamResponse event) {
    if (event._location != _location) { return; }
    _pitch.clear();
    _pitch.addItem("Select...", (String)null);
    for (Pitcher p : event._pitchers) {
      if (p._primary.equals("sp")) { _pitch.addItem(p.name(), p._id); _starters.put(p._id, p); }
      else { _bullpen.put(p._id, p); }
    }
    _pitch.addChangeHandler(_pitchSelect);
    _pitch.setSelectedIndex(1); // FIXME temporary hack to fill in teams quicker, for debugging purposes
    for (int i_lu = 0; i_lu != _lineup.length; ++i_lu) {
      _lineup[i_lu] = new PositionInfo(_pos_list, i_lu);
      _lineup[i_lu]._posBox.addChangeHandler(_posSelect);
      _lineup[i_lu]._batBox.addChangeHandler(_batSelect);
    }
    for (Batter b : event._batters) {
      for (PositionInfo pi : _lineup) {
        if (pi._pos.code().equals(b._primary)) { pi._batBox.addItem(b.name(), b._id); break; }
      }
      _batters.put(b._id, b);
    }
    for (Batter b : event._batters) {
      String name = b.name();
      for (Fielding f : b._fielding.values()) {
        for (PositionInfo pi : _lineup) {
          if (pi._pos == f._pos && !pi._pos.code().equals(b._primary)) { pi._batBox.addItem(name, b._id); break; }
        }
      }
      _lineup[8]._batBox.addItem(name, b._id);
    }
    for (int i_row = 0; i_row != _bat.getRowCount(); ++i_row) {
      PositionInfo pi = _lineup[i_row];
//      int index = pi._pos == Position.DH ? pi._batBox.getItemCount() - 1 : 1;
//      pi._batBox.setSelectedIndex(index); // FIXME temporary hack to fill in teams quicker, for debugging purposes
//      pi._batID = pi._batBox.getValue(index);
      _bat.setText(i_row, 0, "" + (i_row + 1));
      _bat.setWidget(i_row, 1, pi._posBox);
      _bat.setWidget(i_row, 2, pi._batBox);
    }
    setReady();
    if (_ready) { Static.fire(new TeamReadyToggle(true)); }
  }
  
  @EventHandler void onLoadTeams(LeagueInfoResponse event) {
    _team.clear();
    _team.addItem("Select...");
    for (String team : event._teams) {
      _team.addItem(team);
    }
  }
  
  @EventHandler public void onLeague(LeagueInfoRequest event) {
    _league = event._name;
  }
  
  public void constructLineup(Pitcher versus) {
    ArrayList<Batter> eligible = new ArrayList<>(_batters.values());
    HashMap<Position, Batter> selected = new HashMap<>();
    for (PositionInfo pi : _lineup) {
      if (pi._pos != null && pi._batID != null) {
        selected.put(pi._pos, _batters.get(pi._batID));
      }
    }
    ArrayList<Batter> lineup = Static.lineupBot().generate(selected, eligible, versus);
    for (Map.Entry<Position, Batter> entry : selected.entrySet()) {
      entry.getValue()._onfield = entry.getValue()._fielding.get(entry.getKey());
    }
    HashMap<Position, ListBox> byPosBatBox = new HashMap<>();
    for (PositionInfo pi : _lineup) { byPosBatBox.put(pi._pos, pi._batBox); }
    for (int i = 0; i != lineup.size(); ++i) {
      PositionInfo pi = _lineup[i];
      pi._batID = lineup.get(i)._id;
      pi._pos = lineup.get(i)._onfield == null ? Position.DH : lineup.get(i)._onfield._pos;
      pi._posBox.setSelectedIndex(_pos_list.indexOf(pi._pos));
      pi._batBox = byPosBatBox.get(pi._pos);
      _bat.setWidget(i, 2, pi._batBox);
      for (int j = 0; j != pi._batBox.getItemCount(); ++j) {
        if (pi._batBox.getValue(j).equals(pi._batID)) {
          pi._batBox.setItemSelected(j, true);
          break;
        }
      }
    }
    setReady();
    if (_ready) { Static.fire(new TeamReadyToggle(true)); }
  }
  
  public TeamSetup(ListBox team, ListBox pitch, Grid bat, TeamLocation location) {
    _bindEvent.bindEventHandlers(this, Static.eventBus());
    _team = team;
    _pitch = pitch;
    _bat = bat;
    _location = location;
    _bat.resize(9, 3);
    _pos_list = new ArrayList<>(EnumSet.range(Position.CATCH, Position.RIGHT));
    _pos_list.add(Position.DH);
  }

  private HashMap<String, Batter> _batters = new HashMap<>();
  private HashMap<String, Pitcher> _starters = new HashMap<>();
  private HashMap<String, Pitcher> _bullpen = new HashMap<>();
  private boolean _ready = false;
  private final ListBox _team;
  private final ListBox _pitch;
  private final Grid _bat;
  private final TeamLocation _location;
  private final ArrayList<Position> _pos_list;
  private String _league = null;
}
