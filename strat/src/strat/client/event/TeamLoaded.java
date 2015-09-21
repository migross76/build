package strat.client.event;

import strat.client.model.Team;
import strat.client.model.TeamLocation;
import com.google.web.bindery.event.shared.binder.GenericEvent;

public class TeamLoaded extends GenericEvent {
  public final Team _team;
  public final TeamLocation _location;
  
  public TeamLoaded(Team team, TeamLocation location) { _team = team; _location = location; }
}
