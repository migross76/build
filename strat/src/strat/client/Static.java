package strat.client;

import strat.client.model.LineupBot;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.web.bindery.event.shared.binder.GenericEvent;

public class Static {
  public static void fire(GenericEvent event) { _bus.fireEvent(event); }
 
  public static EventBus eventBus() { return _bus; }
  
  public static LineupBot lineupBot() { return _lineupBot; }
  
  private static EventBus _bus = new SimpleEventBus();
  private static LineupBot _lineupBot = new LineupBot();
}
