package topactors.server;

import topactors.shared.Gender;

public class Actor extends Processable implements Comparable<Actor> {

  public Actor() { }
  
  public Actor(String id, String name) { _id = id; _name = name; }

  @Override
  public int compareTo(Actor arg0) {
    if (_sat != arg0._sat) { return _sat > arg0._sat ? -1 : 1; }
    return _name.compareTo(arg0._name);
  }

  public void update(Actor obj) {
    if (_gender == Gender.Unknown) { _gender = obj._gender; }
    if (_sat < obj._sat) { _sat = obj._sat; }
  }
  
  public String _name = null;
  public double _sat = 0; // Stars Above 'Troy'
  public Gender _gender = Gender.Unknown;
  /*
  public String toString() {
    return String.format(" %s[%s] : %.3f", _name, _id, _sat);
  }*/
}
