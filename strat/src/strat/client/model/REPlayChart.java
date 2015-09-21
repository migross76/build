package strat.client.model;

import java.util.HashMap;

public class REPlayChart {
  public double frequency(String play) {
    return _plays.get(play)[1] / _total;
  }
  
  public double getRE(String play) {
    double[] vals = _plays.get(play);
    if (vals == null) {
      play = play.replaceAll("\\(.*?\\)", "(#)");
      vals = _plays.get(play);
    }
    return vals[0];
  }
  
  public REPlayChart(String data) {
    String[] lines = data.split("\n");
    for (String line : lines) {
      String[] tokens = line.split("\t");
      double[] vals = new double[3];
      _plays.put(tokens[0], vals);
      vals[1] = Double.parseDouble(tokens[1]);
      vals[2] = Double.parseDouble(tokens[2]);
      if (tokens.length > 5) { vals[2] += Double.parseDouble(tokens[5]); } // runner RE+
      vals[0] = vals[2] / vals[1];
      if (!tokens[0].contains("(#)")) { _total += vals[1]; }
    }
  }
  
  private HashMap<String, double[]> _plays = new HashMap<>();
  private double _total = 0;
}
