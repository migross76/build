package topactors.shared;

public class NextInfo implements Comparable<NextInfo>, java.io.Serializable {
  private static final long serialVersionUID = 4474010340986534514L;

  private static final double MULTIPLIER = 0.25;
  private static final int ZERO_BONUS = 3;
  
  @Override
  public int compareTo(NextInfo arg0) {
    if (_score != arg0._score) { return _score > arg0._score ? -1 : 1; }
    return _name.compareTo(arg0._name);
  }

  public NextInfo ( ) { }
  
  public NextInfo ( String id, String name, double sat, int occurrences, double bonus ) {
    _id = id;
    _name = name;
    _sat = sat;
    _occurrences = occurrences;
    double mult = _sat > MULTIPLIER ? MULTIPLIER : _sat; // keep low-scoring values out of the way
    double occur = _occurrences == 0 ? ZERO_BONUS : _occurrences; // extra credit for a (movie) that was an original source
    _score = _sat + occur * mult + bonus;
  }
  
  public String _id = null;
  public String _name = null;
  public double _sat = 0.0;
  public int    _occurrences = 0;
  private double _score = 0;
}
