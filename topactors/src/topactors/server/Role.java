package topactors.server;

import java.util.ArrayList;
import topactors.shared.Oscar;

public class Role implements Comparable<Role>, java.io.Serializable {
  private static final long serialVersionUID = -7352511576060766810L;

  private static double VOICE_PENALTY = 0.75;
  
  public enum Level {
    STAR(2.0), APPEAR(1.0), EXTRA(0.5);
    
    Level(double weight) { _weight = weight; }
    
    public double getWeight() { return _weight; }
    
    private double _weight = 0;
  }
  
  public enum Type {
    ACTOR(1.0), SELF(0.5);
    Type(double weight) { _weight = weight; }
    public double getWeight() { return _weight; }
    private double _weight = 0;
  }
  
  public String getUniqueID() { return _actor._id + "|" + _movie._id; }
  
  public Role() { }
  public Role(String id, String name, Movie M, Actor A) { _id = id; _name = name; _movie = M; _actor = A; }
  
  public String _id = null;
  public String _name = null;
  public Movie  _movie = null;
  public Actor  _actor = null;
  public double _sat = 0; // Stars Above 'Troy'
  public double _sat_series = 0;
  public Type   _type = Type.SELF;
  public Level  _level = Level.EXTRA;
  public Oscar  _oscar = Oscar.NONE;
  public boolean _voice = false;
  public int    _series = 1;
  
  public String update(Role upR) {
    if (upR._name != null) { _name = upR._name; }
    if (upR._level != Level.EXTRA) { _level = upR._level; }
    if (upR._type != Type.SELF)  { _type = upR._type; }
    if (upR._oscar != Oscar.NONE)  { _oscar = upR._oscar; }
    if (upR._id != null) { String id = _id; _id = upR._id; return id; }
    return null; // no change to the id
  }

  public void calculate() {
    double voice_penalty = _voice ? VOICE_PENALTY : 1.0;
    _sat = _oscar.boostScore(_movie._sat * _level.getWeight() * _type.getWeight() * voice_penalty);
    //System.out.format("%s : mov=%.1f lev=%.1f osc=%.1f sat=%.1f old=%.1f\n", _movie._name, _movie._sat, _level.getWeight(), _oscar.getScore(), _sat, oldsat);
  }
  
  public double calculateSeries() {
    double oldsat = _sat_series;
    _sat_series = _sat / _series;
    if (_sat_series < 0) { _sat_series = 0; }
    //System.out.format("%s : sat=%.1f ser=%.1f old=%.1f\n", _movie._name, _sat, _sat_series, oldsat);
    return _sat_series - oldsat;
  }
  /*
  public String toString() {
    return String.format(" %s[%s] = %s in %s : %.3f :: %d/%.3f : %s", _name, _id, _actor._name, _movie._name, _sat, _series, _sat_series, _oscar);
  }*/

  @Override
  public int compareTo(Role o) {
    if (_sat_series != o._sat_series) { return _sat_series > o._sat_series ? -1 : 1; }
    if (_sat != o._sat) { return _sat > o._sat ? -1 : 1; }
    if (_movie._sat != o._movie._sat) { return _movie._sat > o._movie._sat ? -1 : 1; }
    if (!_movie._name.equals(o._movie._name)) { return _movie._name.compareTo(o._movie._name); } 
    return _actor._name.compareTo(o._actor._name);
  }

  public double adjustSeries(ArrayList<Role> roles, boolean fresh) {
    if (_id == null) { return 0; } // won't match anything
    double diff = 0;
    int old_series = _series;
    for (Role R : roles) {
      if (!_id.equals(R._id)) { continue; }
      if (fresh) { // then the one with the lower sat is the one occurring lower in the series
        if (_sat < R._sat) { ++_series; } else { ++R._series; }
      } else // both had series numbers, and it's just an adjustment
      if (R._sat > _sat && R._series > old_series) { // out-of-order, this one has too low of a series id
        --R._series; ++_series;
      } else if (R._sat < _sat && R._series < old_series) { // out-of-order, this one has too high of a series id
        ++R._series; --_series;
      } else { continue; } // else in the correct order (for these two)
      diff += R.calculateSeries();
    }
    return diff;
  }  
}
