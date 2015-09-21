package strat.client.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class ColumnPlay extends Play {
  @Override public void collect(Collector c, double weight, ToggleState toggles) {
    for (int i = 0; i != _plays.length; ++i) {
      _plays[i].collect(c, weight * (i < 6 ? i + 1 : 11 - i), toggles);
    }
  }

  @Override public SimplePlay calculate(Dice dice, EnumSet<TogglePlay.Type> toggles) {
    int result = dice.roll(6, 6);
    return _plays[result - 2].calculate(dice, toggles);
  }

  public ColumnPlay(List<String> lines, Play parkSingle, Play parkHomer) {
    for (int i = 0; i != _plays.length; ++i) {
      Play play = null;
      while ((play = Play.parse(lines.remove(0), parkSingle, parkHomer)) == null) { /*no-op*/ }
      _plays[i] = play;
    }
  }

  @Override public void print(ArrayList<PrintRow> rows, boolean isShort) {
    for (int i = 0; i != _plays.length; ++i) {
      PrintRow pr = new PrintRow();
      pr._die = (i+2) + "";
      rows.add(pr);
      _plays[i].print(rows, false);
    }
  }
  
  private Play[] _plays = new Play[11];
}
