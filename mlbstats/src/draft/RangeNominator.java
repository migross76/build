package draft;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Queue;
import util.ByPlayer;
import util.MyDatabase;
import data.Appearances;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

public class RangeNominator implements Nominator {
  
  private void addAppearances(int yearID) {
    ByPlayer<Appearances> ab = _at.get(yearID);
    for (Appearances a : ab) {
      if (a.primary() == null) { continue; }
      Master m = _mt.byID(a.playerID());
      if (m == null) { continue; }
      for (Candidates C : _candList) { C.addPlayer(m).add(a); }
    }
  }
    
  private void addWar(int yearID) {
    ByPlayer<War> wb = _wt.get(yearID);
    for (War w : wb) {
      Master m = _mt.byID(w.playerID());
      if (m == null) { System.err.println("No Master for " + w.playerID()); continue; }
      for (Candidates C : _candList) { C.addPlayer(m).add(w.war()); }
    }
  }

  @Override public Candidates nominate() throws Exception {
    if (_current == _start - 1) { return null; }
    int start = _current;
    if (_current == _end) { // first time through
      while (start > _start && _current - start < _span - 1) {
        _candList.add(new Candidates((start--) + "-" + _current));
      }
    } else {
      start = _current - _span + 1;
    }
    if (start < _start) { start = _start; }
    else { _candList.add(new Candidates(start + "-" + _current)); }
    addWar(_current);
    addAppearances(_current);
    Candidates c = _candList.remove();
    int end = _current + _span - 1;
    if (end > _end) { end = _end; }
    for (Player p : c) {
      p.setSpan(_current, end);
    }
    --_current;
    return c;
  }

  public RangeNominator(int yearStart, int yearEnd, int span) throws SQLException {
    _start = yearStart;
    _current = _end = yearEnd;
    _span = span;
    _candList = new ArrayDeque<>(span);
    try (MyDatabase db = new MyDatabase()) {
      _mt = new Master.Table(db, Sort.UNSORTED);
      _wt = new War.ByYear();
      _wt.addAll(new War.Table(db, Type.BAT));
      _wt.addAll(new War.Table(db, Type.PITCH));
      _at = new Appearances.ByYear();
      _at.addAll(new Appearances.Table(db));
    }
  }
  
  private final Queue<Candidates> _candList;
  private       int _current = 2012;
  private final int _start;
  private final int _end;
  private final int _span;
  
  private final Master.Table _mt;
  private final War.ByYear _wt;
  private final Appearances.ByYear _at;
}
