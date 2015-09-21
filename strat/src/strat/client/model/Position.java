package strat.client.model;

import java.util.HashMap;

public enum Position {
  DH("dh", 0, false),
  PITCH("p", 2, false),
  CATCH("c", 3, false),
  FIRST("1b", 2, false),
  SECOND("2b", 6, false),
  THIRD("3b", 3, false),
  SHORT("ss", 7, false),
  LEFT("lf", 2, true),
  CENTER("cf", 3, true),
  RIGHT("rf", 2, true);
  
  public String code() { return _code; }
  public int weight() { return _weight; }
  public boolean isOF() { return _isOF; }
  
  Position(String code, int weight, boolean isOF) { _code = code; _weight = weight; _isOF = isOF; }
  
  private final String _code;
  private final int _weight;
  private final boolean _isOF;
  
  public static Position map(String code) {
    Position en = tMap.get(code.toLowerCase());
    if (en == null) { throw new IllegalArgumentException("unknown Position : " + code); }
    return en;
  }
  private static HashMap<String, Position> tMap = new HashMap<>();
  static { for (Position t : Position.values()) { tMap.put(t._code, t); } }
}
