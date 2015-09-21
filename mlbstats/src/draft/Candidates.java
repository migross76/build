package draft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import data.Master;

public class Candidates implements Iterable<Player> {
  public String getName() { return _name; }
  
  public Candidates(String name) { _name = name; }
  
  @Override public Iterator<Player> iterator() {
    return _players.values().iterator();
  }

  public Player getPlayer(String playerID) {
    return _players.get(playerID);
  }
  
  public Player addPlayer(Master m) {
    Player P = _players.get(m.playerID());
    if (P == null) { _players.put(m.playerID(), P = new Player(m)); }
    return P;
  }

  public Set<Player> toSortedPlayers() {
    return new TreeSet<>(_players.values());
  }
  
  private String _name = null;
  private HashMap<String, Player> _players = new HashMap<>();
}
