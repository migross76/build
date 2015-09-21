package strat.client.model;

import java.util.LinkedHashMap;

public abstract class Player {
  public String _id;
  public String _primary;
  public String _nameFirst;
  public String _nameLast;
  public Running _run;
  public int _percL;
  public char _bats;
  public char _bunt;
  public LinkedHashMap<Position, Fielding> _fielding = new LinkedHashMap<>();
  
  public Fielding _onfield = null;
 
  public ColumnPlay[] _asL = new ColumnPlay[3];
  public ColumnPlay[] _asR = new ColumnPlay[3];

  public abstract char handed();
  
  public String name() { return _nameFirst + " " + _nameLast; }
  
  public ColumnPlay[] selectColumns(char opposing) {
    switch (opposing) {
      case 'R' : return _asR;
      case 'L' : return _asL;
      case 'S' : return handed() == 'R' ? _asL : _asR;
    }
    throw new IllegalArgumentException("unexpected opposing handedness : " + opposing);
  }
  
  protected static void check(String line, String[] tokens, int len) {
    if (tokens.length != len) {
      throw new IllegalArgumentException("expected " + len + " tokens for: " + line);
    }
  }
  
  protected static boolean useBool(String token, char tVal, char fVal) {
    char val = useChar(token);
    if (val == tVal) { return true; }
    if (val == fVal) { return false; }
    throw new IllegalArgumentException("token is neither " + tVal + " nor " + fVal);
  }
  
  protected static char useChar(String token) {
    if (token.length() != 1) { throw new IllegalArgumentException("token is not one character: " + token); }
    return token.charAt(0);
  }
  
  protected static void check(String name, int val) {
    if (val == 0) { throw new IllegalArgumentException("missing " + name); }
  }
  
  protected static void check(String name, Object val) {
    if (val == null) { throw new IllegalArgumentException("missing " + name); }
  }
  
  protected Player(String id, String primary) {
    _id = id; _primary = primary;
  }
}
