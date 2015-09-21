package strat.client.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

public class TogglePlay extends Play {
  public enum Type {
    PARK_SINGLE('s', '▼', null, "PARK_S"),
    PARK_HOMERUN('r', '◆', null, "PARK_R"),
    WEATHER('w', '△', null, null),
    CLUTCH('c', 'Ω', null, "CLUTCH", true, false),
    FATIGUE('f', '●', null, "s=", false, true),
    INFIELD('i', '+', null, "s=", true, false),
    NHR('n', 'n', "r", "s=", false, true);

    public char code() { return _code; }
    public char unicode() { return _unicode; }
    public String play1() { return _play1; }
    public String play2() { return _play2; }
    public boolean supported(boolean isBatter) { return isBatter ? _for_batter : _for_pitcher; }
    
    Type(char code, char unicode, String play1, String play2, boolean forBatter, boolean forPitcher) {
      _code = code; _unicode = unicode;
      _play1 = play1; _play2 = play2;
      _for_batter = forBatter; _for_pitcher = forPitcher;
    }
    Type(char code, char unicode, String play1, String play2) { this(code, unicode, play1, play2, true, true); }
    private final char _code;
    private final char _unicode;
    private final String _play1;
    private final String _play2;
    private final boolean _for_batter;
    private final boolean _for_pitcher;
    
    public static Type map(char code) {
      Type en = tMap.get(code);
      if (en == null) { throw new IllegalArgumentException("unknown Toggle Type : " + code); }
      return en;
    }
    private static HashMap<Character, Type> tMap = new HashMap<>();
    static { for (Type t : Type.values()) { tMap.put(t._code, t); } }
  }
  
  public final Type _tType;
  public final Play _main;
  public final Play _alternate;
  
  @Override public SimplePlay calculate(Dice dice, EnumSet<TogglePlay.Type> toggles) {
    if (toggles.contains(_tType)) {
      return _alternate.calculate(dice, toggles);
    }
    return _main.calculate(dice, toggles);
  }

  @Override public void collect(Collector c, double weight, ToggleState toggles) {
    toggles._main.add(_tType);
    _main.collect(c, weight, toggles);
    toggles._main.remove(_tType);
    toggles._alt.add(_tType);
    _alternate.collect(c, weight, toggles);
    toggles._alt.remove(_tType);
  }
  
  public TogglePlay(List<String> tokens, Play parkSingle, Play parkHomer) {
    String token = tokens.remove(0);
    _tType = Type.map(token.charAt(1));
    if (_tType == null) { throw new IllegalArgumentException("illegal toggle type : " + token); }
    _main = _tType._play1 == null ? Play.parse(tokens, parkSingle, parkHomer) : new SimplePlay(_tType._play1);
    if (_tType._play2 == null) {
      if (tokens.isEmpty()) { throw new IllegalArgumentException("missing expected play2 for : /" + _tType.code()); }
      _alternate = Play.parse(tokens, parkSingle, parkHomer);
    } else {
      switch (_tType._play2) {
        case "PARK_S" : _alternate = parkSingle; break;
        case "PARK_R" : _alternate = parkHomer; break;
        case "CLUTCH" :
          if (_main instanceof SimplePlay) {
            SimplePlay sp = (SimplePlay)_main;
            _alternate = new SimplePlay(sp._type.woba_wt() == 0 ? "s=" : "p3");
          } else { _alternate = null; }
          break;
        default: _alternate = new SimplePlay(_tType._play2);
      }
    }
    if (_alternate == null) { throw new IllegalArgumentException("missing alternate for : " + _tType.code()); }
  }

  @Override public void print(ArrayList<PrintRow> rows, boolean isShort) {
    switch(_tType) {
      case PARK_SINGLE: case PARK_HOMERUN: case WEATHER: case CLUTCH:
        rows.get(rows.size()-1)._toggle = _tType.unicode() + "";
        break;
      case FATIGUE: case INFIELD:
        rows.get(rows.size()-1)._extra = _tType.unicode() + "";
        break;
      case NHR:
        rows.get(rows.size()-1)._main = "N-HR";
        rows.get(rows.size()-1)._good = true;
    }
    if (_tType._play1 == null) { _main.print(rows, isShort); if (_tType._play2 == null) { rows.add(new PrintRow()); } }
    
    if (_tType._play2 == null) { _alternate.print(rows, isShort); }
  }
}
