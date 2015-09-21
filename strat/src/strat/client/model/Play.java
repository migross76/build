package strat.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

public abstract class Play {
  public enum Type {
    SINGLE('s', "SINGLE", "SI", 0.892),
    DOUBLE('d', "DOUBLE", "DO", 1.278),
    TRIPLE('t', "TRIPLE", "TR", 1.626),
    HOMERUN('r', "HOMERUN", "HR", 2.117),
//    N_HOMERUN('n', "N-HOMERUN", "N-HR", 2.117),
    WALK('w', "WALK", "WK", 0.692),
    HBP('h', "HBP", "HBP", 0.724, true, false),
    OUT_LONG('a', "A", "A", 0),
    OUT_MEDIUM('b', "B", "B", 0),
    OUT_MEDSHORT('q', "B?", "B?", 0, true, false),
    OUT_SHORT('c', "C", "C", 0),
//    FLY('y', "fly", "fly", 0),
//    GROUND('g', "gb", "gb", 0),
    LINE('l', "lineout", "lo", 0),
    POP('p', "popout", "po", 0),
    FOUL('f', "foulout", "fo", 0, true, false),
    STRIKEOUT('k', "strikeout", "so", 0),
    FIELD('x', "X", "X", 0, false, true);

    public char code() { return _code; }
    public double woba_wt() { return _woba_wt; }
    public String print_short() { return _print_short; }
    public String print_long() { return _print_long; }
    
    public boolean isHit() {
      return this == SINGLE || this == DOUBLE || this == TRIPLE || this == HOMERUN;
    }
    
    public boolean supported(boolean isBatter) { return isBatter ? _for_batter : _for_pitcher; }
    
    Type(char code, String print_ln, String print_sh, double woba_wt, boolean forBatter, boolean forPitcher) {
      _code = code; _woba_wt = woba_wt;
      _print_long = print_ln; _print_short = print_sh;
      _for_batter = forBatter; _for_pitcher = forPitcher;
    }

    Type(char code, String print_ln, String print_sh, double woba_wt) {
      this(code, print_ln, print_sh, woba_wt, true, true);
    }
    private final char _code;
    private final double _woba_wt;
    private final String _print_long;
    private final String _print_short;
    private final boolean _for_batter;
    private final boolean _for_pitcher;
    
    public static Type map(char code) {
      Type en = tMap.get(code);
      if (en == null) { throw new IllegalArgumentException("unknown Type : " + code); }
      return en;
    }
    private static HashMap<Character, Type> tMap = new HashMap<>();
    static { for (Type t : Type.values()) { tMap.put(t._code, t); } }
  }
  
  public enum Flag {
//    MODE_A('a', "A"),
//    MODE_B('b', "B"),
//    MODE_C('c', "C"),
//    MODE_Q('?', "B?"),
    ONE_BASE('-', "*"),
    TWO_BASE('=', "**"),
    INJURY('i', "plus injury"),
    MAXOUT('t', "max");
    
    public char code() { return _code; }
    public String print() { return _print; }
    
    Flag(char code, String print) { _code = code; _print = print; }
    private final char _code;
    private final String _print;

    public static Flag map(char code) {
      Flag en = fMap.get(code);
      if (en == null) { throw new IllegalArgumentException("unknown Flag : " + code); }
      return en;
    }
    public static HashMap<Character, Flag> fMap = new HashMap<>();
    static { for (Flag t : Flag.values()) { fMap.put(t._code, t); } }
  }
  
  public static Play parse(String line, Play parkSingle, Play parkHomer) {
    try {
      String[] tokens = line.split(" ");
      if (tokens.length == 0 || tokens[0].length() == 0) { return null; }
      ArrayList<String> list = new ArrayList<>(Arrays.asList(tokens));
      Play p = Play.parse(list, parkSingle, parkHomer);
      if (list.size() != 0) { throw new IllegalArgumentException("too many tokens detected"); }
      return p;
    } catch (RuntimeException e) { throw new RuntimeException("Problem with line : " + line, e); }
  }
  
  public static Play parse(List<String> tokens, Play parkSingle, Play parkHomer) {
    char code = tokens.get(0).charAt(0);
    if (code == '/') { return new TogglePlay(tokens, parkSingle, parkHomer); }
    if (Character.isDigit(code)) { return new SplitPlay(tokens, parkSingle, parkHomer); }
    return new SimplePlay(tokens.remove(0));
  }
  
  public abstract void collect(Collector c, double weight, ToggleState toggles);
  
  public abstract SimplePlay calculate(Dice dice, EnumSet<TogglePlay.Type> toggles);
  
  public abstract void print(ArrayList<PrintRow> rows, boolean isShort);
}
