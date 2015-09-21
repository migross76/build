package topactors.shared;

public class BestActor implements Comparable<BestActor>, java.io.Serializable {
  private static final long serialVersionUID = 7839846903272496807L;

  @Override
  public int compareTo(BestActor arg0) {
    if (_sat != arg0._sat) { return _sat > arg0._sat ? -1 : 1; }
    return _name.compareTo(arg0._name);
  }
  
  public String _id = null;
  public String _name = null;
  public Gender _gender = null;
  public boolean _processed = false;
  public double _sat = 0; // Stars Above 'Troy'

  public int _movies = 0;
  public int _movies_unprocessed = 0;
  public int _stars = 0;
  
  public int _oscars = 0;
  public int _nominations = 0;
  
  
}
