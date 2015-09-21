package topactors.shared;

public class LastProcessed implements java.io.Serializable {
  private static final long serialVersionUID = 795005176183455284L;

  public LastProcessed() { }
  
  public LastProcessed(String actorName, String movieName, double actorSAT, double roleSAT) {
    set(actorName, movieName, actorSAT, roleSAT);
  }
  
  public void set(String actorName, String movieName, double actorSAT, double roleSAT) {
    _actorName = actorName;
    _movieName = movieName;
    _actorSAT = actorSAT;
    _roleSAT = roleSAT;
  }
  
  public String _actorName = null;
  public String _movieName = null;
  public double _actorSAT = 0;
  public double _roleSAT = 0;
}
