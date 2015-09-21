package draft;

import java.util.TreeSet;
import data.Appearances;
import data.Master;

public class Player implements Comparable<Player> {
  public Player(Master m) {
    _master = m;
    _app = new Appearances(_master.playerID(), null, 0);
  }
  public void setSpan(int firstYear, int lastYear) {
    _firstYear = firstYear;
    _lastYear = lastYear;
  }
  public void add(double war) { _war += war; }
  public void add(Appearances a) { _app.add(a); }
  
  public String playerID() { return _master.playerID(); }
  
  @Override
  public int compareTo(Player P) {
    if (_war != P._war) { return (int)Math.signum(P._war - _war); } 
    return playerID().compareTo(P.playerID());
  }
  
  public final Master _master;
  public final Appearances _app;
  
  public int _firstYear = 3000;
  public int _lastYear = 0;
  
  public double _war = 0;
  public TreeSet<String> _mlb_teams = new TreeSet<>();

/*
  public float _posrar = 0;
  public float _repl = 0;
  public int _salary = 0;
*/
}
