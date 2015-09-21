package strat.client.event;

import strat.client.model.TeamLocation;
import com.google.web.bindery.event.shared.binder.GenericEvent;

public class TeamRequest extends GenericEvent {
  public TeamRequest(String league, String team, TeamLocation location) { _league = league; _team = team; _location = location; }
  
  public final String _league;
  public final String _team;
  public final TeamLocation _location;
}
