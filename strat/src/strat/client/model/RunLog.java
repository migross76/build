package strat.client.model;

import strat.shared.BaseState;

public class RunLog {
  public RunLog(BaseState bases, int base, Cause cause) { _bases.set(bases); _runner = _bases._onbase[base]; _base = base; _cause = cause; }
  
  public enum Cause { HIT, OUT, STEAL }
  public enum Type { HOLD, SAFE, OUT }
  
  public int riskRolls() { return _safe_max + 20 - _hold_max; }
  
  public final Cause _cause;
  public final Batter _runner;
  public final int _base;
  
  public double _out_pct = 0;
  public double _hold_pct = 0;
  public double _safe_pct = 0; // includes extra_pct
  public double _extra_pct = 0; // on top of being safe
  
  public int _err_max = 0;
  public int _safe_max = 0;
  public int _hold_max = 0;

  public Type _type = Type.HOLD;
  
  public BaseState _bases = new BaseState();
  public boolean _scored = false;
}
