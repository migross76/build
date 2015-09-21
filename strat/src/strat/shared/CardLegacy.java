package strat.shared;

public class CardLegacy implements java.io.Serializable {
  private static final long serialVersionUID = 8029587035561024311L;

  public enum Advance {
    STAY("-"), CHOICE(""), MOVE("+");
    Advance(String code) {
      _code = code;
    }

    public String code() {
      return _code;
    }

    private final String _code;
  }

  public static class Stat implements java.io.Serializable {
    private static final long serialVersionUID = 8621888753442827543L;

    public String  _event   = null;
    public String  _fielder = null;
    public Advance _advance = null;
    public char    _mode    = ' ';
    public String  _special = null;

    public boolean isOut() {
      return _event.equals(_event.toLowerCase());
    }
    
    public Stat() { }

    public Stat(String[] values) {
      _event = values[1];
      if (isOut()) {
        if (values.length > 2) {
          _fielder = values[2];
        }
        if (values.length > 3) {
          if (values[3].length() == 1) {
            _mode = values[3].charAt(0);
          } else {
            _special = values[3];
          }
        }
      } else {
        _advance = Advance.CHOICE;
        if (values.length > 2) {
          switch (values[2]) {
            case "+":
              _advance = Advance.MOVE;
              break;
            case "-":
              _advance = Advance.STAY;
              break;
          }
        }
      }
    }
  }

  public static class Slot implements java.io.Serializable {
    private static final long serialVersionUID = -3544870040467848649L;

    public double _split  = 1;
    public Stat   _first  = null;
    public Stat   _second = null;

    @Override public String toString() {
      StringBuilder sb = new StringBuilder();
//      sb.append(String.format("%.02f", _split)).append('\t').append(_first);
    sb.append(_split).append('\t').append(_first);
      if (_second != null) {
        sb.append('\t').append(_second);
      }
      return sb.toString();
    }
  }

  public String           _id;
  public String           _name;
  public String[]         _pos;
  public char             _stealing = 'X';
  public int              _running  = 0;

  public static final int ROWS      = 3;
  public static final int COLS      = 11;

  public Slot[][]         _slots    = new Slot[ROWS][COLS];

  public boolean isPitcher() {
    return _pos[0].equals("SP") || _pos[0].equals("RP");
  }

  @Override public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(_id).append('\t').append(_name).append('\t');
    for (String pos : _pos) {
      sb.append('[').append(pos).append(']');
    }
    sb.append('\t').append(_stealing).append('\t').append(_running);
    int die = 2;
    for (Slot[] row : _slots) {
      sb.append(die);
      for (Slot slot : row) {
        sb.append('\t').append(slot);
      }
      sb.append('\n');
    }
    return sb.toString();
  }

}
