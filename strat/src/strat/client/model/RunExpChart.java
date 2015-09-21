package strat.client.model;

import strat.shared.BaseState;

public class RunExpChart {
  private static int index(BaseState bases) {
    int index = bases._outs * 8;
    if (bases._onbase[0] != null) { index += 1; }
    if (bases._onbase[1] != null) { index += 2; }
    if (bases._onbase[2] != null) { index += 4; }
    return index;
  }
  
  public double frequency(BaseState bases) {
    int index = index(bases);
    return _plays[index] / (double)_total;
  }
  
  public double getRE(int outs, boolean onFirst, boolean onSecond, boolean onThird) {
    if (outs == 3) { return 0; }
    int index = outs * 8;
    if (onFirst) { index += 1; }
    if (onSecond) { index += 2; }
    if (onThird) { index += 4; }
    return _runs[index] / (double)_plays[index];
  }
  
  public void setRE(BaseState bases) {
    if (bases._outs == 3) { bases._runexp = 0; }
    else {
      int index = index(bases);
      bases._runexp = _runs[index] / (double)_plays[index];
    }
  }
  
  public double re(BaseState bases) {
    if (bases._outs == 3) { return 0; }
    int index = index(bases);
    return _runs[index] / (double)_plays[index];
  }
  
  public RunExpChart(String data) {
    String[] lines = data.split("\n");
    for (int i = 0; i != _plays.length; ++i) {
      String[] tokens = lines[i].split("\t");
      _runs[i] = Integer.parseInt(tokens[2]);
      _total += _plays[i] = Integer.parseInt(tokens[3]);
    }
  }
  
  private int[] _plays = new int[24];
  private int[] _runs = new int[24];
  private int _total = 0;
}
