package strat.client.model;

import java.util.EnumMap;

public class FieldCollector implements Collector {
  
  public EnumMap<Position, double[]> _outs = new EnumMap<>(Position.class);
  public EnumMap<Position, double[]> _hits = new EnumMap<>(Position.class);
  public EnumMap<Position, double[]> _fields = new EnumMap<>(Position.class);

  private static void add(EnumMap<Position, double[]> map, Position pos, double weight) {
    double[] ct = map.get(pos);
    if (ct == null) { map.put(pos, ct = new double[1]); ct[0] = 0; }
    ct[0] += weight;
  }
  
  @Override public void collect(SimplePlay play, double weight, ToggleState toggles) {
    if (play._fielder == null) { return; }
    switch (play._type) {
      case FIELD:
        add(_fields, play._fielder, weight);
        break;
      case SINGLE: case DOUBLE:
        add(_hits, play._fielder, weight);
        break;
      case OUT_LONG: case OUT_MEDIUM: case OUT_MEDSHORT: case OUT_SHORT: case LINE: case POP: case FOUL:
        add(_outs, play._fielder, weight);
        break;
      default:
        System.err.format("Oops. Forgot %s for %s : ", play._type.name(), play._fielder.code());
        
    }
  }
}
