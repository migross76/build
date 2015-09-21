package data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import util.ByPlayer;

public class TeamWar {

  public String teamID() { return _id; }
  public int    yearID() { return _year; }
  public int    games()  { return _g; }
  public double war()    { return _war; }
  public double waa()    { return _waa; }
  
  private String _id = null;
  private int _year = 0;
  private int _g = 0;
  private double _war = 0;
  private double _waa = 0;

  public static class Table implements Iterable<TeamWar> {
    public Table(Teams.Table tt, War.ByTeam wBy) {
      _data = new ArrayList<>();
      for (Teams t : tt) {
        ByPlayer<War> by = wBy.get(t.teamID() + t.yearID());
        TeamWar team = new TeamWar();
        team._id = t.teamID();
        team._year = t.yearID();
        team._g = t.games();
        team._war = by.total().war();
        team._waa = by.total().waa();
        _data.add(team);
      }
    }
    
    @Override public Iterator<TeamWar> iterator() { return _data.iterator(); }
    
    private List<TeamWar> _data = null;
  }
}
