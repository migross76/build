package alltime;

import java.util.ArrayList;
import data.Master;

public class Career implements Comparable<Career> {
  public Career(Master m) { _id = m.playerID(); _first = m.nameFirst(); _last = m.nameLast(); }
  
  public boolean _isBatter = true;

  public final String _id;
  public final String _first;
  public final String _last;

  public double _war = 0;
  public int _countStat = 0;
  public int _count = 0;
  
  public double _warNorm = 0;
  public double _warWeight = 0;
  
  public ArrayList<Season> _seasons = new ArrayList<>();

  @Override public int compareTo(Career C) { return _id.compareTo(C._id); }
  
  @Override public boolean equals(Object O) {
    if (!(O instanceof Career)) { return false; }
    return _id.equals(((Career)O)._id);
  }
  
  @Override public int hashCode() { return _id.hashCode(); }
}
