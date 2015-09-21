package topactors.shared;

public enum Oscar {
  WINNER(0.75), NOMINATED(0.5), NONE(0);
  
  Oscar(double score) { _score = score; }
  
  public double getScore() { return _score; }
  
  public double boostScore(double score, int votes) {
    double mscore = score + _score - 7.0;
    if (mscore < 0) { return votes == 0 ? _score : mscore; }
    return Math.sqrt(_score * _score + mscore * mscore) - _score;
  }
  
  public double boostScore(double sat) {
    double mscore = sat + _score;
    if (mscore < 0) { return mscore; }
    return Math.sqrt(_score * _score + mscore * mscore) - _score;
  }
  
  private double _score = 0;
}
