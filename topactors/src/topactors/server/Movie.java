package topactors.server;

import topactors.shared.Oscar;

public class Movie extends Processable implements Comparable<Movie> {
  public String _name = null;
  public int    _year = 0;
  public int    _votes = 0;
  public double _rating = 0;
  public Oscar  _oscar = Oscar.NONE;
  public double _score = 0;
  public double _sat = 0; // Stars Above 'Troy'
  
  public Movie() { }
  
  public Movie(String id, String name) { _id = id; _name = name; }

  public void calculate() {
    if (_score == 0 && _votes != 0 && _rating != 0) {
      _score = computeScore(_rating, _votes);
    } else if (_rating == 0 && _votes != 0 && _score != 0) {
      _rating = computeRating(_score, _votes);
    }
    _sat = _oscar.boostScore(_score, _votes);
  }

  public void update(Movie M) {
    if (_processed != null) { return; }
    if (M._name != null) { _name = M._name; }
    if (M._year != 0) { _year = M._year; }
    if (M._oscar != Oscar.NONE) { _oscar = M._oscar; }
    _processed = M._processed;
    if (_votes == 0 || _rating == 0 || M._processed != null) { // either this is the official one, or we had no info before
      _votes = M._votes;
      _rating = M._rating;
      _score = _sat = 0;
    }
  }
  /*
  public String toString() {
    return String.format(" %s : %s [%d] : %d : %.1f : %.2f : %.3f : %s", _id, _name, _year, _votes, _rating, _score, _sat, _oscar);
  }*/


  @Override
  public int compareTo(Movie arg0) {
    if (_sat != arg0._sat) { return _sat > arg0._sat ? -1 : 1; }
    return _name.compareTo(arg0._name);
  }
/*  @Override
  public int compareTo(Movie arg0) {
    if (_sat != arg0._sat) { return _sat > arg0._sat ? -1 : 1; }
    return _name.compareTo(arg0._name);
  }
*/
  
  private static final double MIN_VOTES = 3000;
  private static final double MEAN_RATING = 6.9;
  
  // s = v/(v+V)*r + V/(v+V)*R
  private static double computeScore(double rating, int votes) {
    return (votes / (votes+MIN_VOTES)) * rating + (MIN_VOTES / (votes+MIN_VOTES)) * MEAN_RATING;
  }
  
  // r = (s*(v+V) - R*V) / v 
  private static double computeRating(double score, int votes) {
    return (score*(votes+MIN_VOTES) - MIN_VOTES * MEAN_RATING) / votes;
  }
}
