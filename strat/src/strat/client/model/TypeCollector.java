package strat.client.model;

import java.util.EnumMap;

public class TypeCollector implements Collector {
  
  public TypeCollector(ColumnPlay[] cols) {
    for (ColumnPlay cp : cols) { cp.collect(this, 1, new ToggleState()); }
  }
  
  public double woba() { return _woba / _ct; }
  
  private double _woba = 0;
  private double _ct = 0;
  public EnumMap<Play.Type, double[]> _plays = new EnumMap<>(Play.Type.class);
  public EnumMap<TogglePlay.Type, double[]> _toggles = new EnumMap<>(TogglePlay.Type.class);

  @Override public void collect(SimplePlay play, double weight, ToggleState toggles) {
    double[] ct = _plays.get(play._type);
    if (ct == null) { _plays.put(play._type, ct = new double[1]); ct[0] = 0; }
    ct[0] += weight;
    for (TogglePlay.Type toggle : toggles._main) {
      ct = _toggles.get(toggle);
      if (ct == null) { _toggles.put(toggle, ct = new double[1]); ct[0] = 0; }
      ct[0] += weight;
    }
    _woba += play._type.woba_wt() * weight;
    if (play._type != Play.Type.FIELD) { _ct += weight; }
  }
}
