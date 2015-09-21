package data;
import java.util.EnumSet;
import java.util.HashMap;

/* Average number of games played per season, per position
  SP :  35.8 |10.1
  C  :  99.4 | 8.0
  1B : 121.6 | 7.4
  2B : 120.9 | 7.2
  3B : 118.9 | 7.5
  SS : 120.6 | 7.6
  LF : 120.8 | 6.7
  CF : 120.0 | 6.8
  RF : 121.0 | 6.9
  DH : 103.4 | 2.0
  RP :  51.4 | 6.9
*/
public enum Position {
  CATCH("C", "2", 100), FIRST("1B", "3", 120), SECOND("2B", "4", 120), THIRD("3B", "5", 120), SHORT("SS", "6", 120),
  LEFT("LF", "7", 120), CENTER("CF", "8", 120), RIGHT("RF", "9", 120), DESIG("DH", "D", 120),
  STARTER("SP", "S", 35), MIDDLE("MR", "M", 50), CLOSER("CL", "C", 50);

  public static final int DEFAULT_G = 120;
  
  Position(String name, String shrt, int season_g) {
    _name = name;
    _short = shrt;
    _season_g = season_g;
  }

  public String getName() { return _name; }
  public String getShort() { return _short; }
  public int    getGamesPerSeason() { return _season_g; }

  private String _name = null;
  private String _short = null;
  private int    _season_g = 0;

  public static EnumSet<Position> parse(String pos) {
    EnumSet<Position> results = EnumSet.noneOf(Position.class);
    for (String S : pos.split("/")) { results.addAll(find(S)); }
    return results;
  }

  public static EnumSet<Position> find(String pos) {
    initialize();
    return EnumSet.copyOf(_lookup.get(pos));
  }

  public static String toString(EnumSet<Position> pos) {
    initialize();
    String str = _parseMap.get(pos);
    if (str != null) { return str; }
    StringBuilder SB = new StringBuilder();
    for (Position P : pos) {
      if (SB.length() != 0) { SB.append("/"); }
      SB.append(P.getName());
    }
    return SB.toString();
  }

  private static void initialize() {
    if (_lookup == null) {
      _lookup = new HashMap<>();
      _parseMap = new HashMap<>();
      EnumSet<Position> PITCHER = EnumSet.of(STARTER, MIDDLE, CLOSER);
      for (Position P : Position.values()) { register(P.getName(), EnumSet.of(P)); }
      register("CIF", EnumSet.of(FIRST, THIRD));
      register("MIF", EnumSet.of(SECOND, SHORT));
      register("IF", EnumSet.of(FIRST, SECOND, THIRD, SHORT));
      register("OF", EnumSet.of(LEFT, CENTER, RIGHT));
      register("COF", EnumSet.of(LEFT, RIGHT));
      register("RP", EnumSet.of(MIDDLE, CLOSER));
      register("P", PITCHER);
      register("Bat", EnumSet.complementOf(PITCHER));
      register("Any", EnumSet.allOf(Position.class));
      register("DH", EnumSet.noneOf(Position.class));
    }
  }
  
  private static void register(String name, EnumSet<Position> poses) {
    _lookup.put(name, poses);
    _parseMap.put(poses, name);
  }
  
  private static HashMap<String, EnumSet<Position>> _lookup = null;
  private static HashMap<EnumSet<Position>, String> _parseMap = null;
}
