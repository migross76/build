package strat.client.model;

import java.util.ArrayList;
import java.util.EnumSet;

public class SimplePlay extends Play {
  
  public final Type _type;
  public final Position _fielder;
  public final EnumSet<Flag> _flags = EnumSet.noneOf(Flag.class);
  
  public SimplePlay(Type type, Position fielder, Flag... flags) {
    _type = type;
    _fielder = fielder;
    for (Flag f : flags) { _flags.add(f); }
  }
  
  public SimplePlay(String codes) {
    _type = Type.map(codes.charAt(0));
    if (_type == null) { throw new IllegalArgumentException("illegal type : " + codes); }
    if (codes.length() > 1) {
      char num = codes.charAt(1);
      _fielder = Character.isDigit(num) ? Position.values()[num - '0'] : null;
    } else { _fielder = null; }
    for (int i = _fielder == null ? 1 : 2; i < codes.length(); ++i) {
      Flag flag = Flag.map(codes.charAt(i));
      if (flag == null) { throw new IllegalArgumentException("illegal flag : " + codes); }
      _flags.add(flag);
    }
  }

  @Override public SimplePlay calculate(Dice dice, EnumSet<TogglePlay.Type> toggles) { return this; }

  @Override public void collect(Collector c, double weight, ToggleState toggles) {
    c.collect(this, weight, toggles);
  }
  
  public String createShortLine() {
    StringBuilder sb = new StringBuilder();
    if (_type == Type.FIELD) {
      if (_fielder == Position.CATCH) { sb.append("CATCH-"); }
      else if (_fielder.isOF()) { sb.append("FLY"); }
      else { sb.append("GB"); }
    } else if (_type == Type.OUT_LONG || _type == Type.OUT_MEDIUM || _type == Type.OUT_MEDSHORT || _type == Type.OUT_SHORT) {
      sb.append(_fielder.isOF() ? "fly" : "gb");
    } else {
      sb.append(_type.print_short());
    }
    if (_fielder != null && (_type != Type.FIELD || _fielder != Position.CATCH)) {
      sb.append(" (").append(_fielder.code()).append(") ");
    }
    if (_type == Type.FIELD || _type == Type.OUT_LONG || _type == Type.OUT_MEDIUM || _type == Type.OUT_MEDSHORT || _type == Type.OUT_SHORT) { sb.append(_type.print_short()); }
    for (Flag flag : _flags) {
      if (flag == Flag.INJURY) { continue; }
      sb.append(flag.print());
    }
    return sb.toString();
  }

  @Override public void print(ArrayList<PrintRow> rows, boolean isShort) {
    PrintRow pr = rows.get(rows.size()-1);
    try {
      StringBuilder sb = new StringBuilder();
      if (_type == Type.FIELD) {
        if (_fielder == Position.CATCH) { sb.append("CATCH-"); }
        else if (_fielder.isOF()) { sb.append("FLY"); }
        else { sb.append("GB"); }
      } else if (_type == Type.OUT_LONG || _type == Type.OUT_MEDIUM || _type == Type.OUT_MEDSHORT || _type == Type.OUT_SHORT) {
        sb.append(_fielder.isOF() ? "fly" : "gb");
      } else {
        sb.append(isShort || _flags.contains(Flag.MAXOUT) ? _type.print_short() : _type.print_long());
      }
      if (_fielder != null && (_type != Type.FIELD || _fielder != Position.CATCH)) {
        sb.append(" (").append(_fielder.code()).append(") ");
      }
      if (_type == Type.FIELD || _type == Type.OUT_LONG || _type == Type.OUT_MEDIUM || _type == Type.OUT_MEDSHORT || _type == Type.OUT_SHORT) { sb.append(_type.print_short()); }
      if (_type.woba_wt() > 0) { pr._good = true; }
      for (Flag flag : _flags) {
        if (flag == Flag.INJURY) { continue; }
        sb.append(flag.print());
      }
      pr._main = sb.toString();
      if (_flags.contains(Flag.INJURY)) {
        rows.add(pr = new PrintRow()); sb = new StringBuilder();
        pr._main = Flag.INJURY.print();
      }
    } catch (RuntimeException e) {     System.out.println(pr.toHTMLString()); throw e; }
  }
}
