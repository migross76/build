package strat.shared;

import java.util.ArrayList;
import strat.client.model.Batter;

public class BaseState {
  public BaseState() { }
  public BaseState(BaseState bs) { set(bs); }
  
  public double   _runexp = 0;
  public int      _outs = 0;
  public Batter[] _onbase = new Batter[3];
  
  public int lead() { for (int i = _onbase.length; i != 0; --i) { if (_onbase[i-1] != null) { return i-1; } } return -1; }
  
  public boolean onbase() { for (int i = 0; i != _onbase.length; ++i) { if (_onbase[i] != null) { return true; } } return false; }
  
  public void reset() { _outs = 0; for (int i = 0; i != _onbase.length; ++i) { _onbase[i] = null; } }
  
  public void set(BaseState state) {
    _runexp = state._runexp;
    _outs = state._outs;
    for (int i = 0; i != _onbase.length; ++i) { _onbase[i] = state._onbase[i]; }
  }
  
  /** Advance all baserunners, regardless of whether they're forced
   * @param bases the number of bases to advance
   * @param scored the list collecting the batters scoring off base (may be null)
   * @return the number of runners scored
   */
  public int advance(int bases, ArrayList<Batter> scored) {
    int num_scored = 0;
    for (int i = 2; i != -1; --i) {
      if (_onbase[i] != null) {
        if (i + bases > 2) {
          if (scored != null) { scored.add(_onbase[i]); }
          ++num_scored;
        } else {
          _onbase[i + bases] = _onbase[i];
        }
        _onbase[i] = null;
      }
    }
    return num_scored;
  }
  
  /** Force advancement around the bases
   * @param base the base that forces advancement
   * @param scored the list collecting the batters scoring off base (may be null)
   * @return the number of runners scoring
   */
  public int advanceForced(int base, ArrayList<Batter> scored) {
    Batter move = null;
    for (int i = base; i != 3; ++i) {
      Batter newMove = _onbase[i];
      _onbase[i] = move;
      move = newMove;
      if (move == null) { break; }
    }
    if (move != null) {
      if (scored != null) { scored.add(move); }
      return 1;
    }
    return 0;
  }
  
  public void out(int base) {
    _onbase[base] = null;
    ++_outs;
  }
}
