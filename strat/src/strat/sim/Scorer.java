package strat.sim;

import strat.client.model.GameState;
import strat.client.model.Pitcher;
import strat.client.model.PlayLog;
import strat.client.model.Team;

// tracks all the statistics of the game; also, sets the rules of it
public interface Scorer {
  public void rosters(Team vis, Team home);
  public void relieve(GameState gs, Pitcher p);
  public void play(GameState gs, PlayLog log);
}