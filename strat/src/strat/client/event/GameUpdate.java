package strat.client.event;

import strat.client.model.PlayLog;
import strat.client.model.TeamLocation;
import com.google.web.bindery.event.shared.binder.GenericEvent;

public class GameUpdate extends GenericEvent {
  public final PlayLog _log;
  public final int _vRuns;
  public final int _hRuns;
  public final TeamLocation _batting;
  public final boolean _isOver;
  
  public GameUpdate(PlayLog log, int vRuns, int hRuns, TeamLocation batting, boolean isOver) {
    _log = log;
    _vRuns = vRuns;
    _hRuns = hRuns;
    _batting = batting;
    _isOver = isOver;
  }
}
