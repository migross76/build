package topactors.shared;

import java.util.HashSet;

public class Update implements java.io.Serializable {
  private static final long serialVersionUID = -1195277445156888483L;

  public HashSet<BestActor> _bestActors = new HashSet<BestActor>();
  public HashSet<NextInfo>  _nextMovies = new HashSet<NextInfo>();
  public HashSet<NextInfo>  _nextActors = new HashSet<NextInfo>();
  public LastProcessed      _last = null;
  
  public int _processed_movies = 0;
  public int _processed_actors = 0;

  protected NextInfo _n = null;
  protected BestActor _b = null;
}
