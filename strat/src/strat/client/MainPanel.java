package strat.client;

import strat.client.event.AdvanceRequest;
import strat.client.event.GameUpdate;
import strat.client.event.InitResponse;
import strat.client.event.PlayRequest;
import strat.client.model.PlayLog;
import strat.client.model.RunLog;
import strat.client.model.TeamLocation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.binder.EventBinder;
import com.google.web.bindery.event.shared.binder.EventHandler;

public class MainPanel extends Composite {
  private final MyEventBinder _bindEvent = GWT.create(MyEventBinder.class);
  interface MyEventBinder extends EventBinder<MainPanel> {/*binder*/}
  private final MyUiBinder _bindUI = GWT.create(MyUiBinder.class);
  interface MyUiBinder extends UiBinder<Widget, MainPanel> {/*binder*/}

  interface MyStyle extends CssResource {
    public String scored();
  }
  
  public MainPanel() {
    _bindEvent.bindEventHandlers(this, Static.eventBus());
    initWidget(_bindUI.createAndBindUi(this));
  }
  
  /** @param event indicates what type of handler needed */
  @SuppressWarnings("static-method") @EventHandler void onInit(InitResponse event) {
    DialogBox db = new DialogBox();
    db.setText("New Game");
    db.setWidget(new NewDialog(db));
    db.center();
  }
  
  @EventHandler void onGameUpdate(GameUpdate event) {
    _log = event._log;
    _vScore.setText(Integer.toString(event._vRuns));
    _hScore.setText(Integer.toString(event._hRuns));
    if (event._isOver) { _pitch.setEnabled(false); }
    _inning.setText((_log._top ? "T" : "B") + _log._inning);
    _outs.setText(Integer.toString(_log._e_bases._outs));
    
    _firstBase.setText(_log._e_bases._onbase[0] == null ? "" : _log._e_bases._onbase[0]._nameLast);
    _secondBase.setText(_log._e_bases._onbase[1] == null ? "" : _log._e_bases._onbase[1]._nameLast);
    _thirdBase.setText(_log._e_bases._onbase[2] == null ? "" : _log._e_bases._onbase[2]._nameLast);
    
    _playbyplay.insertRow(0);
    _playbyplay.setText(0, 0, _log._pitcher._nameLast);
    _playbyplay.setText(0, 1, _log._batter._nameLast);
    StringBuffer play = new StringBuffer();
    if (_log._main_play != null) {
      play.append(_log._main_play.createShortLine());
    }
    if (_log._range_play != null) {
      play.append(" → ").append(_log._range_play._name).append("(").append(_log._range_play._play._type.code()).append(") + e").append(_log._error_play);
    }
    if (_log._special != null) {
      if (play.length() != 0) { play.append(" → "); }
      play.append(_log._special.name());
    }
    if (_log._runner != null) {
      play.append(" → ").append(_log._runner._type.name()).append("(").append(_log._runner._safe_max).append("/").append(_log._runner._hold_max).append(")");
    }
    _playbyplay.setText(0, 2, play.toString());
    if (!_log._scored.isEmpty() || (_log._runner != null && _log._runner._scored)) { _playbyplay.getRowFormatter().setStyleName(0, style.scored()); }
    
    boolean advance = _log._runner != null && _log._runner._type == RunLog.Type.HOLD;
    _hOpt1.setVisible(advance && event._batting == TeamLocation.HOME);
    _vOpt1.setVisible(advance && event._batting == TeamLocation.VISITOR);
  }
  
  /** @param event indicates what type of handler needed */
  @SuppressWarnings("static-method") @UiHandler({"_pitch"}) void onPitch(ClickEvent event) {
    Static.fire(new PlayRequest());
  }
  
  /** @param event indicates what type of handler needed */
  @UiHandler({"_hOpt1", "_vOpt1"}) void onAdvance(ClickEvent event) {
    Static.fire(new AdvanceRequest(_log, false));
  }
  
  @UiField MyStyle style;
  @UiField HasText _vScore;
  @UiField HasText _inning;
  @UiField HasText _outs;
  @UiField HasText _hScore;

  @UiField HasText _firstBase;
  @UiField HasText _secondBase;
  @UiField HasText _thirdBase;
  
  @UiField FlexTable _playbyplay;
  
  private PlayLog _log = null;
  
  @UiField Button _pitch;
  @UiField Button _vOpt1;
  @UiField Button _hOpt1;
  
  @UiField TeamPanel _vis;
  @UiField TeamPanel _home;
}
