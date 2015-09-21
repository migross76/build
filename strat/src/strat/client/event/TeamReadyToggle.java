package strat.client.event;

import com.google.web.bindery.event.shared.binder.GenericEvent;

public class TeamReadyToggle extends GenericEvent {
  public TeamReadyToggle(boolean ready) { _ready = ready; }
  public final boolean _ready;
}
