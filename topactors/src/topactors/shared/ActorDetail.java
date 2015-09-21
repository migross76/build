package topactors.shared;

public class ActorDetail implements Comparable<ActorDetail>, java.io.Serializable {
  private static final long serialVersionUID = 5660817794966472737L;

  public String  _name = null;
  public double  _score = 0;
  public double  _sat = 0;
  public boolean _processed = false;
  public boolean _star = false;
  public boolean _self = false;
  public int     _series = 0;
  public boolean _voice = false;
  public Oscar   _oscar_role = null;
  public Oscar   _oscar_movie = null;

  @Override
  public int compareTo(ActorDetail arg0) {
    if (_sat != arg0._sat) { return _sat > arg0._sat ? -1 : 1; }
    return _name.compareTo(arg0._name);
  }
}
