package strat.client.model;

import java.util.EnumMap;

public class REValueCollector implements Collector {
  
  public REValueCollector(REPlayChart chart, ColumnPlay[] cols) {
    this(chart);
    for (ColumnPlay cp : cols) { cp.collect(this, 1, new ToggleState()); }
  }
  
  public REValueCollector(REPlayChart chart) {
    _chart = chart;
  }
  
  private final REPlayChart _chart;
  public double _total = 0;
  public EnumMap<TogglePlay.Type, double[]> _toggles = new EnumMap<>(TogglePlay.Type.class); // value is amount changed when switching to alternate

  public double _ct = 0;
  public double _ob = 0;
  public double _xb = 0;

  @Override public void collect(SimplePlay play, double weight, ToggleState toggles) {
    double[] ct = null;
    String playStr = play.createShortLine();
    switch (play._type) {
      case WALK: case HBP: case SINGLE: _ob += weight; break;
      case DOUBLE: _xb += weight; _ob += weight; break;
      case TRIPLE: _xb += weight * 2; _ob += weight; break;
      case HOMERUN: _xb += weight * 3; _ob += weight; break;
      default: break;
    }
    double val = _chart.getRE(playStr) * weight;
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
    if (toggles._alt.isEmpty()) {
      _total += val;
      _ct += weight;
    }
  }
}
