import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.TreeSet;
import data.Position;

/*package*/ class Player {
  public EnumSet<Position> _pos = EnumSet.noneOf(Position.class);
  public TreeSet<String> _teams = new TreeSet<>();

  private static HashSet<String> TWO_PART = new HashSet<>(Arrays.asList("Chan Ho Park", "Daniel Ray Herrera"));
  public void setName(String fullname) {
    int space = fullname.indexOf(' ');
    if (TWO_PART.contains(fullname)) {
      space = fullname.indexOf(' ', space + 1);
    }
    if (space != -1) { _firstname = fullname.substring(0, space); }
    _lastname = fullname.substring(space + 1);
  }

  private static CharSequence printArray(Iterable<String> list) {
    StringBuilder SB = new StringBuilder();
    for (String S : list) {
      if (SB.length() != 0) { SB.append("/"); }
      SB.append(S);
    }
    return SB.toString();
  }


  public String format() {
    return String.format("%d\t%s\t%s\t%s\t%s\t%.1f\t%.1f\t%.1f\t%d\n",
             _id, Position.toString(_pos), _firstname, _lastname, printArray(_teams),
             _repl, _posrar, _rar, _salary - 400 < 0 ? 0 : _salary - 400);
  }

  public static Player parse(String line) {
    String[] tabs = line.split("\t");
    if (tabs.length < 7) { return null; }
    Player P = new Player();
    P._id = Integer.parseInt(tabs[0]);
    P._pos.addAll(Position.parse(tabs[1]));
    P._firstname = tabs[2];
    P._lastname = tabs[3];
    if (!tabs[4].equals("- - -")) {
      P._teams.addAll(Arrays.asList(tabs[4].split("/")));
    }
    P._repl = Float.parseFloat(tabs[5]);
    P._posrar = Float.parseFloat(tabs[6]);
    P._rar = Float.parseFloat(tabs[7]);
    P._salary = Integer.parseInt(tabs[8]);
    return P;
  }

  public String _firstname = null;
  public String _lastname = null;
  public int _id = -1;
  public float _rar = 0;
  public float _posrar = 0;
  public float _repl = 0;
  public int _salary = 0;
}

