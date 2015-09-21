package strat.driver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import strat.server.CardData;
import strat.shared.CardLegacy;

/*
 * TODO Basic gameplay
 * Lineout into as many outs as possible
 * 
 * TODO Substitutions
 * Select reliever for 8th, 9th based on current scores - what about extra innings in this case? Use #5 and skip turn in order?
 * Replace with new player on INJ
 * 
 * TODO Choices
 * Take extra base
 * Steal base
 * Pitcher replacements
 * Pinch hitter/runner (need to know if fielding substitution is necessary/available)
 * Sacrifice hit
 * Intentional walk
 * 
 * TODO Assign fielding
 * TODO Consider 3x3 play
 */
public class Simulator {
  private static final Path ROSTER1      = Paths
                                             .get("C:/build/strat/team1.txt");
  private static final Path ROSTER2      = Paths
                                             .get("C:/build/strat/team2.txt");

  private static final int  SEASONS      = 1;
  private static final int  GAMES        = 162;
  private static boolean    PLAY_BY_PLAY = false;

  public abstract static class Player {
    public CardLegacy _card;
    public int  _pa   = 0;
    public int  _runs = 0;
    public int  _h_1b = 0;
    public int  _h_2b = 0;
    public int  _h_3b = 0;
    public int  _h_hr = 0;
    public int  _bb   = 0;
    public int  _on_e = 0;
    public int  _sf   = 0;
    public int  _so   = 0;
    public int  _gidp = 0;

    public int ab() {
      return _pa - _sf - _bb;
    }

    public int hits() {
      return _h_1b + _h_2b + _h_3b + _h_hr;
    }

    public int tb() {
      return _h_1b + _h_2b * 2 + _h_3b * 3 + _h_hr * 4;
    }

    public double avg() {
      return hits() / (double)ab();
    }

    public double slg() {
      return tb() / (double)ab();
    }

    public double obp() {
      return (hits() + _bb + _on_e) / (double)_pa;
    }

    public Player(CardLegacy card) {
      _card = card;
    }
  }

  public static class Batter extends Player {
    public int _rbi = 0;
    public int _fc  = 0;

    public Batter(CardLegacy card) {
      super(card);
    }

    public static String header() {
      return "Batter\tPA\tAB\tR\tH" + "\t2B\t3B\tHR\tRBI\tSF"
          + "\tBB\tSO\tOnE\tGIDP\tAVG" + "\tISO\tOBP\tSLG\tOPS";
    }

    @Override public String toString() {
      return String.format("%s\t%d\t%d\t%d\t%d" + "\t%d\t%d\t%d\t%d\t%d"
          + "\t%d\t%d\t%d\t%d\t%.03f" + "\t%.03f\t%.03f\t%.03f\t%.03f",
          _card._name, _pa / SEASONS, ab() / SEASONS, _runs / SEASONS, hits()
              / SEASONS, _h_2b / SEASONS, _h_3b / SEASONS, _h_hr / SEASONS,
          _rbi / SEASONS, _sf / SEASONS, _bb / SEASONS, _so / SEASONS, _on_e
              / SEASONS, _gidp / SEASONS, avg(), slg() - avg(), obp(), slg(),
          obp() + slg());
    }
  }

  public static class Pitcher extends Player {
    public int _ip3 = 0;
    public int _w   = 0;
    public int _l   = 0;

    public double ra9() {
      return _runs * 27.0 / _ip3;
    }

    public double whip() {
      return (_bb + _on_e + hits()) * 3.0 / _ip3;
    }

    public String ip() {
      return _ip3 / SEASONS / 3 + "." + _ip3 / SEASONS % 3;
    }

    public Pitcher(CardLegacy card) {
      super(card);
    }

    public static String header() {
      return "Pitcher\tW\tL\tRA9\tIP" + "\tBF\tH\tR\tHR\tBB"
          + "\tSO\tWHIP\toAVG\toOPS";
    }

    @Override public String toString() {
      return String.format("%s\t%d\t%d\t%.02f\t%s" + "\t%d\t%d\t%d\t%d\t%d"
          + "\t%d\t%.02f\t%.03f\t%.03f", _card._name, _w / SEASONS, _l
          / SEASONS, ra9(), ip(), _pa / SEASONS, hits() / SEASONS, _runs
          / SEASONS, _h_hr / SEASONS, _bb / SEASONS, _so / SEASONS, whip(),
          avg(), obp() + slg());
    }
  }

  public static class Team {
    public Batter[]  _lineup   = new Batter[9];
    public Pitcher[] _rotation = new Pitcher[5];

    public int       _atbat    = 0;
    public int       _runs     = 0;
    public Pitcher   _pitcher  = null;

    public void setPitcher(int id) {
      _pitcher = _rotation[id];
      _atbat = 0;
      _runs = 0;
    }

    public Team(Path roster, HashMap<String, CardLegacy> cards) throws IOException {
      List<String> lines = Files.readAllLines(roster, StandardCharsets.UTF_8);
      int line = 0;
      for (int i = 0; i != _lineup.length; ++i) {
        CardLegacy card = null;
        while ((card = cards.get(lines.get(line++))) == null) { /*no-op*/}
        _lineup[i] = new Batter(card);
      }
      for (int i = 0; i != _rotation.length; ++i) {
        CardLegacy card = null;
        while ((card = cards.get(lines.get(line++))) == null) { /*no-op*/}
        _rotation[i] = new Pitcher(card);
      }
    }
  }

  public int      _inning = 1;
  public boolean  _top    = true;

  public int      _outs   = 0;
  public Batter[] _onbase = new Batter[3];

  public Team     _home;
  public Team     _vis;

  private static CardLegacy.Stat roll(CardLegacy batter, CardLegacy pitcher) {
    int die1 = rollDie(6), die2 = rollDie(6), die3 = rollDie(6);
    int column = die1;
    int row = die2 + die3;
    CardLegacy.Slot slot = column < 3 ? batter._slots[column][row]
        : pitcher._slots[column - 3][row];
    int die4 = -1;
    CardLegacy.Stat stat = slot._split == 1
        || slot._split * 20 >= (die4 = rollDie(20)) ? slot._first
        : slot._second;
    if (PLAY_BY_PLAY) {
      System.out.format("%d %d+%d%s\t", die1 + 1, die2 + 1, die3 + 1,
          die4 == -1 ? "" : " (" + (die4 + 1) + ")");
    }
    return stat;
  }

  public void next() {
    if (!_top) {
      ++_inning;
    }
    _top = !_top;
    _outs = 0;
    for (int i = 0; i != _onbase.length; ++i) {
      _onbase[i] = null;
    }
  }

  public int advance(int bases, Pitcher pitch, Batter bat) {
    int runs = 0;
    for (int i = 2; i != -1; --i) {
      if (_onbase[i] != null) {
        if (i + bases > 2) {
          ++runs;
          ++_onbase[i]._runs;
        } else {
          _onbase[i + bases] = _onbase[i];
        }
        _onbase[i] = null;
      }
    }
    if (pitch != null) {
      pitch._runs += runs;
    }
    if (bat != null) {
      bat._rbi += runs;
    }
    return runs;
  }

  public int advanceForced() {
    Batter move = null;
    for (int i = 0; i != 3; ++i) {
      Batter newMove = _onbase[i];
      _onbase[i] = move;
      move = newMove;
      if (move == null) {
        return 0;
      }
    }
    if (move == null) {
      return 0;
    }
    ++move._runs;
    return 1;
  }

  public void printState() {
    System.out.format("%c%d\t%d-%d\t%d\t", _top ? 'T' : 'B', _inning,
        _vis._runs, _home._runs, _outs);
    for (int i = 0; i != _onbase.length; ++i) {
      System.out.print(_onbase[i] == null ? "-" : Integer.toString(i + 1));
    }
  }

  public void playAtBat(Team bat, Pitcher pitcher) {
    if (PLAY_BY_PLAY) {
      printState();
    }
    Batter batter = bat._lineup[bat._atbat];
    if (PLAY_BY_PLAY) {
      System.out.format("\t%s\t%s\t", pitcher._card._id, batter._card._id);
    }
    CardLegacy.Stat stat = roll(batter._card, pitcher._card);
    if (stat.isOut()) {
      String extra = "";
      ++_outs;
      ++pitcher._ip3;
      if (stat._event.equals("gb")) {
        switch (stat._mode) {
          case 'A':
            if (_onbase[0] != null) {
              if (_outs == 3) {
                if (stat._fielder.equals("3b") || stat._fielder.equals("ss")) {
                  extra = "FC";
                  ++batter._fc;
                }
              } else {
                ++_outs;
                ++pitcher._ip3;
                _onbase[0] = null;
                extra = "DP";
                ++batter._gidp;
                ++pitcher._gidp;
                if (_outs != 3) {
                  bat._runs += advance(1, pitcher, null);
                }
              }
            }
            break;
          case 'B':
            if (_onbase[0] != null) {
              extra = "FC";
              ++batter._fc;
              if (_outs != 3) {
                _onbase[0] = null;
                bat._runs += advance(1, pitcher, batter);
                _onbase[0] = batter;
              }
            }
            break;
          case 'C':
            if (_outs != 3) {
              bat._runs += advance(1, pitcher, batter);
            }
            break;
        }
      } else if (stat._event.equals("fb") && _outs != 3) {
        switch (stat._mode) {
          case 'A':
            if (advance(1, pitcher, batter) == 1) {
              extra = "SF";
              ++batter._sf;
              ++pitcher._sf;
              ++bat._runs;
            }
            break;
          case 'B':
            if (_onbase[2] != null) {
              extra = "SF";
              ++batter._sf;
              ++pitcher._sf;
              ++batter._runs;
              ++pitcher._runs;
              ++bat._runs;
              ++_onbase[2]._runs;
              _onbase[2] = null;
            }
            break;
          case 'C':
            break;
        }
      } else if (stat._event.equals("so")) {
        ++batter._so;
        ++pitcher._so;
      }
      if (PLAY_BY_PLAY) {
        System.out.format("%s\t%s\t%s\t%s\n", stat._event,
            stat._fielder == null ? "" : stat._fielder, stat._mode, extra);
      }
    } else {
      switch (stat._event) {
        case "BB":
          bat._runs += advanceForced();
          _onbase[0] = batter;
          ++batter._bb;
          ++pitcher._bb;
          break;
        case "1E":
          bat._runs += advance(1, pitcher, null);
          _onbase[0] = batter;
          ++batter._on_e;
          ++pitcher._on_e;
          break;
        case "1B":
          bat._runs += advance(
              1 + (stat._advance == CardLegacy.Advance.MOVE ? 1 : 0), pitcher, batter);
          ++batter._h_1b;
          ++pitcher._h_1b;
          _onbase[0] = batter;
          break;
        case "2E":
          bat._runs += advance(2, pitcher, null);
          _onbase[1] = batter;
          ++batter._on_e;
          ++pitcher._on_e;
          break;
        case "2B":
          bat._runs += advance(
              2 + (stat._advance == CardLegacy.Advance.MOVE ? 1 : 0), pitcher, batter);
          ++batter._h_2b;
          ++pitcher._h_2b;
          _onbase[1] = batter;
          break;
        case "3B":
          bat._runs += advance(3, pitcher, batter);
          _onbase[2] = batter;
          ++batter._h_3b;
          ++pitcher._h_3b;
          break;
        case "HR":
          bat._runs += advance(4, pitcher, batter) + 1;
          ++batter._h_hr;
          ++pitcher._h_hr;
          ++batter._runs;
          ++batter._rbi;
          ++pitcher._runs;
          break;
      }
      if (PLAY_BY_PLAY) {
        System.out.format("%s%s\n", stat._event, stat._advance.code());
      }
    }
    ++batter._pa;
    ++pitcher._pa;

    if (++bat._atbat == 9) {
      bat._atbat = 0;
    }
  }

  public void playGame() {
    while (_inning < 10 || _home._runs == _vis._runs) {
      while (_outs != 3) {
        playAtBat(_vis, _home._pitcher);
      }
      if (PLAY_BY_PLAY) {
        printState();
        System.out.println();
      }
      next();
      while (_outs != 3 && (_inning < 9 || _home._runs <= _vis._runs)) {
        playAtBat(_home, _vis._pitcher);
      }
      if (PLAY_BY_PLAY) {
        printState();
        System.out.println();
      }
      next();
    }
    if (_home._runs > _vis._runs) {
      ++_home._pitcher._w;
      ++_vis._pitcher._l;
    } else {
      ++_vis._pitcher._w;
      ++_home._pitcher._l;
    }
  }

  public Simulator(Team vis, Team home) {
    _vis = vis;
    _home = home;
  }

  public static int rollDie(int size) {
    return (int)Math.floor(Math.random() * size);
  }

  public static void main(String[] args) throws Exception {
    CardData cd = new CardData();
    
    HashMap<String, CardLegacy> cards = new HashMap<>();
    for (CardLegacy c : cd.cards()) {
      cards.put(c._id, c);
    }
    Team team1 = new Team(ROSTER1, cards);
    Team team2 = new Team(ROSTER2, cards);

    for (int i = 0; i != SEASONS * GAMES; ++i) {
      boolean t1home = (i / 3) % 2 == 0;
      Simulator sim = new Simulator(t1home ? team2 : team1, t1home ? team1
          : team2);
      team1.setPitcher(i % 5);
      team2.setPitcher((i + i / 5) % 5);
      sim.playGame();
    }

    System.out.println();
    System.out.println(Batter.header());
    for (Batter b : team1._lineup) {
      System.out.println(b);
    }
    for (Batter b : team2._lineup) {
      System.out.println(b);
    }
    System.out.println();
    System.out.println(Pitcher.header());
    for (Pitcher p : team1._rotation) {
      System.out.println(p);
    }
    for (Pitcher p : team2._rotation) {
      System.out.println(p);
    }
  }

}
