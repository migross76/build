package strat.client.event;

import java.util.ArrayList;
import strat.client.model.Batter;
import strat.client.model.Pitcher;
import strat.client.model.TeamLocation;
import com.google.web.bindery.event.shared.binder.GenericEvent;

public class TeamResponse extends GenericEvent {
  public TeamResponse(ArrayList<Batter> batters, ArrayList<Pitcher> pitchers, TeamLocation location) { _batters = batters; _pitchers = pitchers; _location = location; }
  
  public final Iterable<Batter> _batters;
  public final Iterable<Pitcher> _pitchers;
  public final TeamLocation _location;
}
