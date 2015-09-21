package alltime;

import data.Appearances;

public abstract class Season implements Comparable<Season> {
  public Season(Career C, double playingTimeStd) { _career = C; PLAYING_TIME_STD = playingTimeStd; }
  
  public Career _career = null;
  public final double PLAYING_TIME_STD;
  public Appearances _app = null;
  public int _age = 0;
  public int _year = 0;
  public String _team = null;
  public double _war = 0;
  public int _countStat = 0;
  
  public double _warWeight = 0;
  public double _warNorm = 0;
  public double _warReplace = 0;
  
  public int _slotID = -1;

  @Override
  public int compareTo(Season S) {
    int cmp = _career.compareTo(S._career);
    if (cmp != 0) { return cmp; }
    return _year - S._year;
  }

  @Override
  public boolean equals(Object O) {
    if (!(O instanceof Season)) { return false; }
    Season S = (Season)O;
    if (!_career.equals(S._career)) { return false; }
    return _year == S._year;
  }
  
  public void compute(double adjust) {
    _warNorm = (_war + _career._warNorm) * adjust / (_countStat + adjust);
    _warWeight = _warNorm * 4 + _career._warWeight;
  }
  
  @Override
  public int hashCode() { return _career.hashCode() * _year; }
}