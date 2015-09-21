package strat.client.event;

import com.google.web.bindery.event.shared.binder.GenericEvent;

public class LeagueInfoRequest extends GenericEvent {
  public LeagueInfoRequest(String leagueName) { _name = leagueName; }
  
  public final String _name;
}
