package strat.client.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class SplitPlay extends Play {
  public final int _split;
  public final Play _first;
  public final Play _second;
  
  @Override public SimplePlay calculate(Dice dice, EnumSet<TogglePlay.Type> toggles) {
    Play chosen = dice.roll(20) <= _split ? _first : _second;
    return chosen.calculate(dice, toggles);
  }
  
  public SplitPlay(List<String> tokens, Play parkSingle, Play parkHomer) {
    _split = Integer.parseInt(tokens.remove(0));
    _first = Play.parse(tokens, parkSingle, parkHomer);
    _second = Play.parse(tokens, parkSingle, parkHomer);
  }

  @Override public void collect(Collector c, double weight, ToggleState toggles) {
    _first.collect(c, weight * (_split / 20.0), toggles);
    _second.collect(c, weight * (1.0 - _split / 20.0), toggles);
  }

  @Override public void print(ArrayList<PrintRow> rows, boolean isShort) {
    _first.print(rows, true);
    StringBuilder sb = new StringBuilder();
    sb.append("1");
    if (_split != 1) { sb.append("-").append(_split); }
    rows.get(rows.size()-1)._split = sb.toString();
    rows.add(new PrintRow());
    _second.print(rows, true);
    sb = new StringBuilder();
    if (_split != 19) { sb.append(_split+1).append("-"); }
    sb.append("20");
    rows.get(rows.size()-1)._split = sb.toString();
  }
}
