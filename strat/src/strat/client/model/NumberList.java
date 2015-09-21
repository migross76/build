package strat.client.model;

import java.util.Iterator;
import java.util.List;

public class NumberList implements Iterable<Integer>, Iterator<Integer> {
  
  public static String print(List<Integer> list) {
    if (list.isEmpty()) { return "-"; }
    int first = -1, last = -2;
    StringBuilder sb = new StringBuilder();
    for (int i : list) {
      if (i == last + 1) { last = i; }
      else {
        if (first == last) { sb.append(","); }
        if (first + 1 == last) { sb.append(",").append(last).append(","); }
        else if (first + 1 < last) { sb.append("-").append(last).append(","); }
        first = last = i;
        sb.append(first);
      }
    }
    if (first + 1 == last) { sb.append(",").append(last); }
    else if (first + 1 < last) { sb.append("-").append(last); }
    return sb.toString();
  }

  @Override public boolean hasNext() {
    if (_nums.length == 1 && _nums[0].equals("0")) { return false; }
    return _index != _nums.length || _i_span != _e_span;
  }

  @Override public Integer next() {
    if (_i_span != _e_span) { return ++_i_span; }
    String num = _nums[_index++];
    int hyphen = num.indexOf('-');
    if (hyphen == -1) { return Integer.parseInt(num); }
    _i_span = Integer.parseInt(num.substring(0, hyphen));
    _e_span = Integer.parseInt(num.substring(hyphen+1));
    return _i_span;
  }

  @Override public void remove() {
    throw new UnsupportedOperationException("cannot remove from list");
  }
  
  @Override public Iterator<Integer> iterator() {
    return this;
  }

  public NumberList(String nums) {
    _nums = nums.split(",");
  }

  private final String[] _nums;
  private int _index = 0;
  
  private int _i_span = -1;
  private int _e_span = -1;
}
