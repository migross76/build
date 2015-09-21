package strat.client;

import strat.client.event.LeagueInfoRequest;
import strat.client.event.LeagueRequest;
import strat.client.event.LeagueResponse;
import strat.client.event.TeamLoaded;
import strat.client.event.TeamReadyToggle;
import strat.client.model.TeamLocation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.binder.EventBinder;
import com.google.web.bindery.event.shared.binder.EventHandler;

public class NewDialog extends Composite {
  private final MyEventBinder _bindEvent = GWT.create(MyEventBinder.class);
  interface MyEventBinder extends EventBinder<NewDialog> {/*binder*/}
  private static MyUiBinder _bindUI = GWT.create(MyUiBinder.class);
  interface MyUiBinder extends UiBinder<Widget, NewDialog> {/*binder*/}

  @UiField ListBox _league;
  @UiField ListBox _vTeam;
  @UiField ListBox _hTeam;
  @UiField ListBox _vPitch;
  @UiField ListBox _hPitch;
  @UiField Grid _vBat;
  @UiField Grid _hBat;
  @UiField Button _vAsk;
  @UiField Button _play;
  
  private final TeamSetup _vis;
  private final TeamSetup _home;
  private final PopupPanel _parent;
  
  private static String boxSelection(ListBox lb) {
    int index = lb.getSelectedIndex();
    if (index == 0) { return null; }
    return lb.getItemText(index);
  }
  
  @UiHandler({"_vTeam","_hTeam"}) void onTeamSelect(ChangeEvent event) {
    if (event.getSource() == _vTeam) { _vis.onTeamSelected(); }
    else { _home.onTeamSelected(); }
  }
  
  @UiHandler({"_vAsk","_hAsk"}) void onBotAsk(ClickEvent event) {
    if (event.getSource() == _vAsk) { _vis.constructLineup(_home.getPitcher()); }
    else { _home.constructLineup(_vis.getPitcher()); }
  }
  
  /** @param event indicates what type of handler needed */
  @UiHandler({"_league"}) void onLeagueSelect(ChangeEvent event) {
    String lg = boxSelection(_league);
    if (lg == null) { _vTeam.clear(); _hTeam.clear(); }
    else { Static.fire(new LeagueInfoRequest(lg)); }
  }
  
  /** @param event indicates what type of handler needed */
  @UiHandler({"_play"}) void onPlayBall(ClickEvent event) {
    Static.fire(new TeamLoaded(_home.createTeam(), TeamLocation.HOME));
    Static.fire(new TeamLoaded(_vis.createTeam(), TeamLocation.VISITOR));
    _parent.hide();
  }
  
  /** @param event indicates what type of handler needed */
  @EventHandler void onTeamReady(TeamReadyToggle event) {
    _play.setEnabled(_home.isReady() && _vis.isReady());
  }
  
  @EventHandler void onLoadLeagues(LeagueResponse event) {
    _league.clear();
    _league.addItem("Select...");
    for (String lg : event._leagues) { _league.addItem(lg); }
  }
  
  public NewDialog(PopupPanel parent) {
    _bindEvent.bindEventHandlers(this, Static.eventBus());
    initWidget(_bindUI.createAndBindUi(this));
    _parent = parent;
    _home = new TeamSetup(_hTeam, _hPitch, _hBat, TeamLocation.HOME);
    _vis = new TeamSetup(_vTeam, _vPitch, _vBat, TeamLocation.VISITOR);
    Static.fire(new LeagueRequest());
  }

}
