package strat.sim;

import strat.client.model.GameState;

// handles setting the roster, the starting lineup, and making roster moves
public interface Manager {
  boolean replacePitcher(GameState gs);
}