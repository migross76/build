import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import data.Position;

class Roster {
  public Spot _spot = null;
  public EnumSet<Position> _pos = null;
  public Player _player = null;
  public Roster(Spot spot) {
    _spot = spot;
    _pos = Position.parse(_spot._pos);
  }

  public boolean contains(EnumSet<Position> pos) {
    EnumSet<Position> mypos = _pos;
    if (mypos.isEmpty()) {
      if (pos.isEmpty()) { return true; }
      mypos = Position.find("Bat");
    }
    for (Position P : pos) { if (mypos.contains(P)) { return true; } }
    return false;
  }

  public boolean containsAll(Roster R) {
    EnumSet<Position> mypos = _pos;
    EnumSet<Position> yourpos = R._pos;
    if (mypos.isEmpty()) { mypos = Position.find("Bat"); }
    if (yourpos.isEmpty()) { yourpos = Position.find("Bat"); }
    return mypos.containsAll(yourpos);
  }
}

class Team {
  public Team(Iterable<Spot> spots) {
    for (Spot spot : spots) { _roster.add(new Roster(spot)); }
  }
  public ArrayList<Roster> _roster = new ArrayList<>();
  public String _primaryTeam = null;
  public float _rar = 0;
  public int _onteam = 0;
}

class Best {
  ArrayList<Roster> _roster = new ArrayList<>();
  Player _player = null;
  double _score = -100.0;
  String _team = null;
  int _attempts = 0;
}

public class Draft extends DefaultHandler {

  @Override public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    if ("position".equals(qName)) {
      Spot S = new Spot();
      S._pos = atts.getValue("name");
      String temp = atts.getValue("playing-time");
      if (temp != null) { S._playingtime = Double.parseDouble(temp); }
      _spots.add(S);
    } else if ("team-weight".equals(qName)) {
      _teamweight = Double.parseDouble(atts.getValue("value"));
    }
  }

  public void loadConfig(String config_file) throws Exception {
    SAXParserFactory.newInstance().newSAXParser().parse(new File(config_file), this);
  }

  public void loadPlayers(BufferedReader reader) throws Exception {
    String line = null;
    while ((line = reader.readLine()) != null) {
      Player P = Player.parse(line);
      if (P != null) { _players.add(P); }
    }
  }

  public void draft(int num_teams, PrintWriter orderWriter) throws Exception {
    for (int i = 0; i != num_teams; ++i) {
      _teams.add(new Team(_spots));
    }
    for (int round = 0; round != _spots.size(); ++round) {
      for (int t = 0; t != num_teams; ++t) {
        Team T = _teams.get(round % 2 == 0 ? t : num_teams - t - 1);
        Best B = new Best();
        for (Player P : _players) { if (!findRosterSpot(P, T, B)) { break; } }
        if (B._player != null) {
          for (int i = B._roster.size() - 1; i != 0; --i) {
            B._roster.get(i)._player = B._roster.get(i-1)._player;
          }
          B._roster.get(0)._player = B._player;
          orderWriter.format("%d\t%d\t%d\t%s\t%s %s\t%.1f", round * num_teams + t + 1, round + 1, round % 2 == 0 ? t + 1 : num_teams - t, B._roster.get(0)._spot._pos, B._player._firstname, B._player._lastname, B._player._rar);
          for (int i = 1; i != B._roster.size(); ++i) {
            orderWriter.format("%s%s[%s]", i == 1 ? "\t" : " : ", B._roster.get(i)._player._lastname, B._roster.get(i)._spot._pos);
          }
          orderWriter.println();
          _players.remove(B._player);
          if (T._primaryTeam == null) { T._primaryTeam = B._team; _teams_claimed.add(B._team); }
          if (T._primaryTeam != null && B._player._teams.contains(T._primaryTeam)) { ++T._onteam; }
        }
      }
    }
  }

  public void print(PrintWriter writer) throws Exception {
    for (int round = 0; round != _spots.size(); ++round) {
      writer.print(_spots.get(round)._pos);
      for (Team T : _teams) {
        Roster R = T._roster.get(round);
        if (R._player == null) { writer.print("\t\t"); }
        else {
          writer.format("\t%s\t%s%s %s\t%.1f", Position.toString(R._player._pos), R._player._teams.contains(T._primaryTeam) ? "*" : "", R._player._firstname.charAt(0), R._player._lastname, R._player._rar);
          T._rar += R._player._rar * R._spot._playingtime;
        }
      }
      writer.println();
    }
    writer.print("Total");
    for (Team T : _teams) { writer.format("\t%d\t%s\t%.1f", T._onteam, T._primaryTeam, T._rar); }
    writer.println();
  }

  public ArrayList<Spot> _spots = new ArrayList<>();
  public double _teamweight = 1.0;
  public ArrayList<Player> _players = new ArrayList<>();
  public ArrayList<Team> _teams = new ArrayList<>();
  public HashSet<String> _teams_claimed = new HashSet<>();

  public static void usage(String msg) {
    if (msg != null) { System.err.println("ERROR : " + msg); }
    System.err.println("Parameters :\n" +
       "-h :: help\n" +
       "-i <war file>\n" +
       "-c <cfg file>\n" +
       "-t <teams>\n" +
       "-d <draft order file>\n" +
       "-o <output file>\n");
    System.exit(msg == null ? 0 : 1);
  }

  public boolean findRosterSpot(Player P, Team T, Best B) {
    double score = P._rar;
    if (score <= B._score) { return false; } // done with this player
    String newTeam = null;
    if (T._primaryTeam != null && !P._teams.contains(T._primaryTeam)) {
      score *= _teamweight;
    } else if (T._primaryTeam == null) {
      for (String team : P._teams) {
        if (!_teams_claimed.contains(team)) { newTeam = team; break; }
      }
      if (newTeam == null) { score *= _teamweight; }
    }
    if (score <= B._score) { return true; } // done with this player
    if (findRosterSpot(P, T, new ArrayList<Roster>(), score, B)) {
      B._team = newTeam;
      B._player = P;
    }
    return true;
  }

  public boolean findRosterSpot(Player P, Team T, ArrayList<Roster> rosters, double score, Best B) {
    ++B._attempts;
    boolean found = false;
    for (Roster R : T._roster) {
      if (!R.contains(P._pos)) { continue; }
      if (rosters.contains(R)) { continue; }
      // don't move an existing player to a spot that has more positional coverage
      // than his current position; it's ALWAYS better to put the new guy there
      if (!rosters.isEmpty() && R.containsAll(rosters.get(rosters.size() - 1))) { continue; }
      double rScore = score;
      if (rosters.isEmpty()) { // first player
        rScore *= R._spot._playingtime;
      } else {
        rScore += P._rar * (R._spot._playingtime - rosters.get(rosters.size() - 1)._spot._playingtime);
      }
      if (rScore < B._score) { continue; } // don't replace with a lower score
      if (rScore == B._score && !B._roster.isEmpty() && B._roster.size() <= rosters.size() + 1) { continue; }
      rosters.add(R);
      if (R._player == null) { // free spot
        B._roster.clear();
        B._roster.addAll(rosters);
        B._score = rScore;
        found = true;
      } else {
        if (findRosterSpot(R._player, T, rosters, rScore, B)) { found = true; }
      }
      rosters.remove(rosters.size() - 1);
    }
    return found;
  }

  public static void main(String[] args) throws Exception {
    String config_file = null, reader_file = null, writer_file = null, orderWriter_file = null;
    int num_teams = 0;
    for (int i = 0, e = args.length; i != e; ++i) {
      String opt = args[i];
      if ("-h".equals(opt)) { usage(null); }
      if (i+1 == e) { usage("missing value for " + opt); }
      String val = args[++i];
      if ("-c".equals(opt)) { config_file = val; continue; }
      if ("-i".equals(opt)) { reader_file = val; continue; }
      if ("-t".equals(opt)) { num_teams = Integer.parseInt(val); continue; }
      if ("-o".equals(opt)) { writer_file = val; continue; }
      if ("-d".equals(opt)) { orderWriter_file = val; continue; }
      usage("unknown parameter : " + opt);
    }
    if (config_file == null) { usage("missing -c option"); }
    if (reader_file == null) { usage("missing -i option"); }
    if (writer_file == null) { usage("missing -o option"); }
    if (orderWriter_file == null) { usage("missing -d option"); }
    if (num_teams <= 0) { usage("invalid -t option"); }
    
    Draft draft = new Draft();
    draft.loadConfig(config_file);
    try (BufferedReader reader = new BufferedReader(new FileReader(reader_file))) {
      draft.loadPlayers(reader);
    }
    try (PrintWriter orderWriter = new PrintWriter(new FileWriter(orderWriter_file))) {
      draft.draft(num_teams, orderWriter);
      orderWriter.flush();
    }
    try (PrintWriter writer = new PrintWriter(new FileWriter(writer_file))) {
      draft.print(writer);
      writer.flush();
    }
  }

}