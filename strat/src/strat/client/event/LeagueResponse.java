package strat.client.event;

import java.util.ArrayList;
import com.google.web.bindery.event.shared.binder.GenericEvent;

public class LeagueResponse extends GenericEvent {
  public LeagueResponse(ArrayList<String> leagues) { _leagues = leagues; }
  
  public final Iterable<String> _leagues;
}
