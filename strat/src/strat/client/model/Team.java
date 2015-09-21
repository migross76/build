package strat.client.model;

import java.util.ArrayList;
import java.util.EnumMap;

public class Team {
  public String _name;
  
  public Batter[] _lineup = new Batter[9];
  public Pitcher _pitcher = null;
  public FatigueState _fatigue = null;
  public ArrayList<Pitcher> _rotation = new ArrayList<>();
  public ArrayList<Pitcher> _bullpen = new ArrayList<>();
  public ArrayList<Batter> _bench = new ArrayList<>();
  public EnumMap<Position, Player> _fielders = new EnumMap<>(Position.class);
  public int _atBat = 0;
  public int _runs = 0;
  
  public void addFielder(Position pos, Player player) {
    if (pos != Position.DH) {
      player._onfield = player._fielding.get(pos);
      if (player._onfield == null) { throw new IllegalArgumentException("cannot find position " + pos.code() + " for player " + player._id); }
    }
    _fielders.put(pos, player);
  }
  
  public void setLineup(char pitches) {
    double[] wobas = new double[9];
    for (Player p : _fielders.values()) {
      if (p instanceof Pitcher) { continue; }
      ValueCollector vc = new ValueCollector(p.selectColumns(pitches));
      double woba = vc._total / vc._ct;
      int index = 0;
      for (; index != 9; ++index) {
        if (woba > wobas[index]) { break; }
      }
      for (int i = 8; i != index; --i) {
        wobas[i] = wobas[i-1];
        _lineup[i] = _lineup[i-1];
      }
      wobas[index] = woba;
      _lineup[index] = (Batter)p;
    }
    Batter tmp = _lineup[0];
    _lineup[0] = _lineup[2];
    _lineup[2] = _lineup[3];
    _lineup[3] = tmp;
  }
/*  
  public void print() {
    for (Fielder f : _fielders.values()) { System.out.format("%c %s\t", f._player._nameFirst.charAt(0), f._player._nameLast); }
    System.out.println();
    for (Batter b : _lineup) { System.out.format("%c %s\t", b._nameFirst.charAt(0), b._nameLast); }
    System.out.println();
  }
*/
}