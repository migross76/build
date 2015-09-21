package strat.client.model;

import java.util.EnumMap;

public class ValueCollector implements Collector {
  
  public ValueCollector(ColumnPlay[] cols) {
    for (ColumnPlay cp : cols) { cp.collect(this, 1, new ToggleState()); }
  }
  
  public double _total = 0;
  public EnumMap<TogglePlay.Type, double[]> _toggles = new EnumMap<>(TogglePlay.Type.class); // value is amount changed when switching to alternate

  public double _ct = 0;

  @Override public void collect(SimplePlay play, double weight, ToggleState toggles) {
    double[] ct = null;
    double val = play._type.woba_wt() * weight;
    for (TogglePlay.Type toggle : toggles._main) {
//System.out.println(toggle.code() + "\tMAIN\t" + play._type.code() + "\t" + val);
      ct = _toggles.get(toggle);
      if (ct == null) { _toggles.put(toggle, ct = new double[1]); }
      ct[0] -= val;
    }
    for (TogglePlay.Type toggle : toggles._alt) {
//System.out.println(toggle.code() + "\tALT\t" + play._type.code() + "\t" + val);
      ct = _toggles.get(toggle);
      if (ct == null) { _toggles.put(toggle, ct = new double[1]); }
      ct[0] += val;
    }
    if (toggles._alt.isEmpty() && play._type != Play.Type.FIELD) {
      _total += val;
      _ct += weight;
    }
  }
}
