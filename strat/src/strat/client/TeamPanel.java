package strat.client;

import strat.client.event.GameUpdate;
import strat.client.event.TeamLoaded;
import strat.client.model.Batter;
import strat.client.model.Pitcher;
import strat.client.model.Team;
import strat.client.model.TeamLocation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.binder.EventBinder;
import com.google.web.bindery.event.shared.binder.EventHandler;

public class TeamPanel extends Composite {
  private final MyEventBinder _bindEvent = GWT.create(MyEventBinder.class);
  interface MyEventBinder extends EventBinder<TeamPanel> {/*binder*/}
  private static MyUiBinder _bindUI = GWT.create(MyUiBinder.class);
  interface MyUiBinder extends UiBinder<Widget, TeamPanel> {/*binder*/}
  
  interface MyStyle extends CssResource {
    public String active();
    public String inactive();
  }
  
  @UiConstructor public TeamPanel(String location) {
    _bindEvent.bindEventHandlers(this, Static.eventBus());
    _location = TeamLocation.valueOf(location);
    initWidget(_bindUI.createAndBindUi(this));
  }
  
  @EventHandler void onGameUpdate(GameUpdate event) {
    _lineup.getRowFormatter().setStyleName(_currBatter, "");
    _lineup.getRowFormatter().setStyleName(_team._atBat % 9, event._batting == _location && !event._isOver ? style.active() : style.inactive());
    _currBatter = _team._atBat % 9;
  }

  @EventHandler void onTeamLoad(TeamLoaded event) {
    if (event._location != _location) { return; }
    _team = event._team;
    _name.setText(_team._name);
    for (int i = 0; i != 9; ++i) {
      _lineup.setText(i, 0, _team._lineup[i] == null ? "[NULL]" : _team._lineup[i].name());
    }
    for (Batter b : _team._bench) {
      _bench.setText(_bench.getRowCount(), 0, b == null ? "[NULL]" : b.name());
    }
    _pitching.setText(0, 0, _team._pitcher == null ? "[NULL]" : _team._pitcher.name());
    for (Pitcher p : _team._bullpen) {
      _bullpen.setText(_bullpen.getRowCount(), 0, p == null ? "[NULL]" : p.name());
    }
    _lineup.getRowFormatter().setStyleName(0, _location == TeamLocation.VISITOR ? style.active() : style.inactive());
  }
  
  private final TeamLocation _location;
  private Team _team = null;
  @UiField MyStyle style;
  @UiField HasText _name;
  @UiField(provided=true) Grid _lineup = new Grid(9, 1);
  @UiField FlexTable _bench;
  @UiField FlexTable _pitching;
  @UiField FlexTable _bullpen;
  
  private int _currBatter = 0;
}
