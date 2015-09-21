package strat.client.event;

import strat.client.model.PlayLog;
import com.google.web.bindery.event.shared.binder.GenericEvent;

public class AdvanceRequest extends GenericEvent {
  public AdvanceRequest(PlayLog log, boolean needsLead) { _log = log; _needsLead = needsLead; }
  
  public final PlayLog _log;
  public final boolean _needsLead;
}
