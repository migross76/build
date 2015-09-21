package draft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import data.Master;

public class AllStarElector implements Elector {
  private static final double MIN_SEASON_WAR = 1.5;
  
  public static class Elected implements Comparable<Elected> {
    @Override public int compareTo(Elected arg0) {
      if (_times != arg0._times) { return _times > arg0._times ? -1 : 1; } 
      return _master.playerID().compareTo(arg0._master.playerID());
    }

    public Elected(Master m) { _master = m; }
    
    public final Master _master;
    public double _times = 0;
  }
  
  public boolean addPlayer(Roster R, Player P) {
    try {
      for (Slot S : R) {
        if (S.available() && S.supports(P._app.primary().pos())) {
          S.setPlayer(P);
          Elected e = _elected.get(P._master.playerID());
          if (e == null) { _elected.put(P._master.playerID(), e = new Elected(P._master)); }
          e._times += P._lastYear - P._firstYear + 1;
          return true;
        }
      }
    } catch (NullPointerException e) { System.err.println("No position for " + P.playerID()); }
    return false;
  }
  
  public List<Elected> elected() {
    ArrayList<Elected> elected = new ArrayList<>(_elected.values());
    Collections.sort(elected);
    return elected;
  }
  
  @Override public List<Roster> elect(Candidates C) {
    Roster roster = new Roster(C.getName(), _pos);
    Set<Player> players = C.toSortedPlayers();
    int left = roster.size();
    for (Player P : players) {
      int years = P._lastYear - P._firstYear + 1;
      if (P._war / years < MIN_SEASON_WAR) { break; }
      if (addPlayer(roster, P) && --left == 0) { break; }
    }
    return Arrays.asList(roster);
  }
  
  public AllStarElector(String... pos) {
    _pos = pos;
  }
  
  private String[] _pos = null;
  
  private HashMap<String, Elected> _elected = new HashMap<>();
}
