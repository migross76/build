package strat.client.model;

import java.util.LinkedList;

public abstract class FatigueState {
  public enum Reason { INEFFECTIVE, OUT_OF_GAS; }
  
  // track state so it stays fatigued "forever"
  
  public Reason state() { return _reason; }
  
  protected Reason _reason = null;
  
  protected FatigueState() { }
  
  public abstract Reason collect(PlayLog log);
  
  protected static boolean updateHits(PlayLog log, LinkedList<int[]> walkshits) {
    SimplePlay play = log._range_play != null ? log._range_play._play : log._main_play;
    if (play != null && (play._type == Play.Type.WALK || play._type.isHit())) {
      ++walkshits.get(0)[0];
      return true;
    }
    return false;
  }
  
  public static class Starter extends FatigueState {
    private final int _pow;
    private final LinkedList<int[]> _runs = new LinkedList<>();
    private final LinkedList<int[]> _walkshits = new LinkedList<>();
    
    public Starter(Pitcher p) { _pow = p._starter; }
  
    @Override public Reason collect(PlayLog log) {
      if (_reason != null) { return _reason; }
      if (log._leadoff) { _runs.addFirst(new int[1]); _walkshits.addFirst(new int[1]); }
      int scored = log._scored.size() + (log._runner != null && log._runner._scored ? 1 : 0);
      if (scored != 0) {
        int[] last = _runs.get(0);
        last[0] += scored;
        int total = last[0];
        if (total > 4) { _reason = Reason.INEFFECTIVE; } // 5+ runs this inning
        else if (_runs.size() > 1) { total += _runs.get(1)[0]; if (total > 5) { _reason = Reason.INEFFECTIVE; } } // 6+ runs in last 2 innings
        else if (_runs.size() > 2) { total += _runs.get(2)[0]; if (total > 6) { _reason = Reason.INEFFECTIVE; } } // 7+ runs in last 3 innings
      } else if (_runs.size() >= _pow) { // reached point of weakness
        if (updateHits(log, _walkshits)) {
          if (_walkshits.get(0)[0] > 2) { _reason = Reason.OUT_OF_GAS; } // 3+ H/BB this inning
          else if (_walkshits.size() > 1 && _walkshits.get(0)[0] + _walkshits.get(1)[0] > 3) { _reason = Reason.OUT_OF_GAS; } // 4+ H/BB in last 2 innings
        }
      }
      return _reason;
    }
  }

  public static class Reliever extends FatigueState {
    public final int _pow;
    public final int _close;
    public final LinkedList<int[]> _r_walkshits = new LinkedList<>();
    public final LinkedList<int[]> _c_walkshits = new LinkedList<>();
    public enum Type { AS_CLOSER, AS_RELIEVER, AS_BOTH }
    public final Type _type;
  
    @Override public Reason collect(PlayLog log) {
      if (_reason != null) { return _reason; }
      if (_type != Type.AS_CLOSER && _r_walkshits.size() > (_pow - 1) * 3) { // test the reliever conditions
        if (updateHits(log, _r_walkshits)) {
          int total = _r_walkshits.get(0)[0];
          if (_r_walkshits.size() > 1) { total += _r_walkshits.get(1)[0]; }
          if (_r_walkshits.size() > 2) { total += _r_walkshits.get(2)[0]; }
          if (total > 2) { _reason = Reason.OUT_OF_GAS; }
        }
      }
      if (_reason == null && _type != Type.AS_RELIEVER) { // test the closer conditions
        if (_close < 0) { _reason = Reason.INEFFECTIVE; }
        else if (_c_walkshits.size() > _close) {
          if (updateHits(log, _r_walkshits)) { _reason = Reason.INEFFECTIVE; }
        }
      }
      int outs = (log._runner != null ? log._runner._bases._outs : log._e_bases._outs) - log._s_bases._outs;
      for (int i = 0; i != outs; ++i) {
        _r_walkshits.add(new int[1]);
        _c_walkshits.add(new int[1]);
      }
      if (_reason == null && _r_walkshits.size() > (_pow + 2) * 3) { _reason = Reason.OUT_OF_GAS; }
      return _reason;
    }
    
    public Reliever(Pitcher p) { this(p, Type.AS_BOTH); }
    
    public Reliever(Pitcher p, Type type) {
      _type = type;
      _pow = p._reliever;
      _close = p._closer;
      _r_walkshits.addFirst(new int[1]);
      _c_walkshits.addFirst(new int[1]);
    }
  }

}
