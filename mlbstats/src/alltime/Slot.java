package alltime;

import java.util.ArrayList;
import java.util.EnumSet;
import data.Position;

public class Slot {
  public Slot(int id, String pos, int available, double weight, double fieldBonus) {
    _id = id;
    _name = pos;
    _available = available;
    _pos = Position.parse(pos);
    _weight = weight;
    _fieldBonus = fieldBonus;
  }
  
  public boolean available() { return _seasons.size() < _available; }
  
  public boolean supports(Position pos) { return _pos.contains(pos); }
  
  public void addPlayer(Season S) { _seasons.add(S); }
  public int getID() { return _id; }
  public String getName() { return _name; }
  public ArrayList<Season> getSeasons() { return _seasons; }
  public EnumSet<Position> getPosition() { return _pos; }
  public double getWeight() { return _weight; }
  public double getFieldBonus() { return _fieldBonus; }
  
  private int _id = -1;
  private String _name = null;
  private int _available = 1;
  private ArrayList<Season> _seasons = new ArrayList<>();
  private EnumSet<Position> _pos = null;
  private double _weight = 1;
  private double _fieldBonus = 0;
}
