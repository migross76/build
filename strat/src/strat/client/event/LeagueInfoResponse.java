package strat.client.event;

import java.util.ArrayList;
import com.google.web.bindery.event.shared.binder.GenericEvent;

public class LeagueInfoResponse extends GenericEvent {
  public LeagueInfoResponse(ArrayList<String> teams) { _teams = teams; }
  
  public final Iterable<String> _teams;
}
