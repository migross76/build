package strat.client.model;

import java.util.ArrayList;
import java.util.HashSet;

public class GameLog {
  public void addTeam(Team t) {
    _starters.add(t._pitcher);
    for (Batter b : t._lineup) { _starters.add(b); }
  }
  
  public ArrayList<LineupLog> _lineup = new ArrayList<>();
  
  public HashSet<Player> _starters = new HashSet<>();
  public HashSet<Player> _subs = new HashSet<>();
  public ArrayList<PlayLog> _plays = new ArrayList<>();
  public Pitcher _winner = null;
  public Pitcher _loser = null;
}
