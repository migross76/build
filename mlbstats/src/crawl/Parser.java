package crawl;

import java.util.ArrayList;

public class Parser {

  private boolean pastEnd(int pos) {
    return pos == -1 || pos > _end;
  }
  
  // All but the last one is to find the beginning position
  // Last one finds the end
  public boolean filter(String... regex) {
    int end = _start;
    for (String R : regex) {
      _start = end;
      end = _SB.indexOf(R, _start);
      if (pastEnd(end)) { return false; }
    }
    _end = end;
    return true;
  }

  public boolean filterStart(String... regex) {
    for (String R : regex) {
      _start = _SB.indexOf(R, _start);
      if (pastEnd(_start)) { return false; }
    }
    return true;
  }

  public boolean filterEnd(String... regex) {
    int end = _start;
    for (String R : regex) {
      end = _SB.indexOf(R, end);
      if (pastEnd(end)) { return false; }
    }
    _end = end;
    return true;
  }
  
  // All but the last one is to find the beginning position
  // Last one finds the end
  public ArrayList<String> getColumns(String... regex) {
    ArrayList<String> cols = new ArrayList<>();
    int colStart = _start, colEnd = _start;
    while (true) {
      for (String R : regex) {
        colStart = colEnd;
        colEnd = _SB.indexOf(R, colStart);
        if (pastEnd(colEnd)) { break; }
      }
      if (pastEnd(colEnd)) { break; }
      cols.add(_SB.substring(colStart + 1, colEnd));
    }
    return cols;
  }
  
  public int getStart() { return _start; }
  public int getEnd() { return _end; }
  
  public String getCurrentString() {
    return _SB.substring(_start, _end);
  }
  
  public void setRange(int start, int end) {
    _start = start;
    _end = end;
  }
  
  public void reset() { _start = 0; _end = _SB.length(); }
  
  public Parser(StringBuilder SB) {
    _SB = SB;
    _start = 0;
    _end = _SB.length();
  }
  
  private StringBuilder _SB = null;
  private int _start = -1;
  private int _end = -1;
}
