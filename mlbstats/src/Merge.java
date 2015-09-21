import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import data.Position;

public class Merge extends DefaultHandler {

  private static Pattern NAME_PATTERN = Pattern.compile("playerid=([0-9]+).*?>(.+)</a>");

  private void addByName(Player P) {
    String name = (P._lastname + ", " + P._firstname).toLowerCase();
    ArrayList<Player> list = _byName.get(name);
    if (list == null) { _byName.put(name, list = new ArrayList<>()); }
    list.add(P);
  }

  private void collectBatter(String[] line, String pos) {
    if (line.length < 8) { return; }
    Matcher M = NAME_PATTERN.matcher(line[0]);
    if (!M.find()) { return; }
    int id = Integer.parseInt(M.group(1));
    Player P = _players.get(id);
    if (P == null) {
      _players.put(id, P = new Player());
      P._id = id;
      P.setName(M.group(2));
      addByName(P);
      if (!line[1].equals("- - -")) { P._teams.add(line[1]); }
      P._rar = Float.parseFloat(line[6]);
      if (line[3].length() != 0) { P._posrar += Float.parseFloat(line[3]); }
      if (line[5].length() != 0) { P._posrar += Float.parseFloat(line[5]); }
      P._repl = Float.parseFloat(line[4]);
    }
    P._pos.addAll(Position.parse(pos));
  }

  private void collectPitcher(String[] line) {
    if (line.length < 8) { return; }
    Matcher M = NAME_PATTERN.matcher(line[0]);
    if (!M.find()) { return; }
    int id = Integer.parseInt(M.group(1));
    Player P = _players.get(id);
    if (P == null) {
      float sinn = line[3].length() == 0 ? 0 : Float.parseFloat(line[3]);
      float rinn = line[5].length() == 0 ? 0 : Float.parseFloat(line[5]);
      _players.put(id, P = new Player());
      P._id = id;
      P.setName(M.group(2));
      addByName(P);
      if (!line[1].equals("- - -")) { P._teams.add(line[1]); }
      P._rar = Float.parseFloat(line[6]);
      P._repl = sinn + rinn;
      float diff = sinn - rinn;
      if (Math.abs(diff) <= 10) { P._pos.addAll(Position.find("P")); }
      else if (diff < 0) { P._pos.add(Position.CLOSER); }
      else { P._pos.add(Position.STARTER); }
    }
  }

  private void appendTeam(String[] line) {
    if (line.length < 4) { return; }
    Matcher M = NAME_PATTERN.matcher(line[0]);
    if (!M.find()) { return; }
    int id = Integer.parseInt(M.group(1));
    Player P = _players.get(id);
    if (P == null) { return; }
    if (!line[1].equals("- - -")) { P._teams.add(line[1]); }
  }

  private void appendSalary(String[] line) {
    if (line.length < 6) { return; }
    if (line[2].isEmpty() && line[3].isEmpty()) { return; }
    String name = line[1].trim();
    ArrayList<Player> list = _byName.get(name.toLowerCase());
    if (list == null) {
      System.err.format("Unable to find player '%s'\n", name);
      return;
    } else if (!_salaryNames.add(name)) {
      System.err.format("Note: duplicate name in salary list '%s'\n", name);
    }
    String team = _teams.getTeam(line[2]);
    String salary = line[5];
    if (salary.length() == 0) { return; }
    salary = salary.substring(1).replaceAll(",", "");
    for (Player P : list) {
      if (team != null && P._teams.contains(team)) {
        P._salary += Integer.parseInt(salary) / 1000;
        return;
      }
    }
    System.err.format("Cannot find player '%s' on team '%s'.  Options are :", name, team);
    for (Player P : list) {
      for (String T : P._teams) { System.err.format(" %s", T); }
    }
    System.err.println();
  }

  @Override public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    String file = atts.getValue("file");
    if (file == null) { return; }
    String pos = atts.getValue("position");
    try {
      if ("payroll".equals(qName)) {
        try (BufferedReader R = new BufferedReader(new FileReader(new File(_file.getParent(), file)))) {
          String line = null;
          while ((line = R.readLine()) != null) {
            appendSalary(line.split("\t"));
          }
        }
      } else {
        try (CSVReader R = new CSVReader(new FileReader(new File(_file.getParent(), file)))) {
          String[] line = null;
          while ((line = R.readLine()) != null) {
            try {
              if ("war".equals(qName) && "P".equals(pos)) {
                collectPitcher(line);
              } else if ("war".equals(qName)) { // batters
                collectBatter(line, pos);
              } else if ("team".equals(qName)) {
                appendTeam(line);
              }
            } catch (NumberFormatException e) { System.out.println("Bad line : " + line[0]); }
          }
        }
      }
    }
    catch (IOException e) { throw new SAXException(e); }
  }

  public void load() throws Exception {
    SAXParserFactory.newInstance().newSAXParser().parse(_file, this);
  }

  public Merge(File file) {
    _file = file;
  }

  public HashMap<Integer, Player> _players = new HashMap<>();
  public HashMap<String, ArrayList<Player>> _byName = new HashMap<>();
  private File _file = null;
  public HashSet<String> _salaryNames = new HashSet<>();
  private Teams _teams = new Teams();

  public static void usage(String msg) {
    if (msg != null) { System.err.println("ERROR : " + msg); }
    System.err.println("Parameters :\n" +
       "-h :: help\n" +
       "-c <config file>\n" +
       "-o <output file>\n");
    System.exit(msg == null ? 0 : 1);
  }

  public static void main(String[] args) throws Exception {
    File config = null;
    String output_file = null;
    for (int i = 0, e = args.length; i != e; ++i) {
      String opt = args[i];
      if ("-h".equals(opt)) { usage(null); }
      if (i+1 == e) { usage("missing value for " + opt); }
      String val = args[++i];
      if ("-c".equals(opt)) { config = new File(val); continue; }
      if ("-o".equals(opt)) { output_file = val; continue; }
      usage("unknown parameter : " + opt);
    }
    if (config == null) { usage("missing -c option"); }
    if (output_file == null) { usage("missing -o option"); }
    Merge merge = new Merge(config);
    merge.load();
    ArrayList<Player> sort = new ArrayList<>(merge._players.values());
    Collections.sort(sort, new Comparator<Player>() {
      @Override public int compare(Player one, Player two) {
        float diff = one._rar - two._rar;
        if (diff != 0) { return diff > 0 ? -1 : 1; }
        return one._id - two._id;
      }
    });
    try (PrintWriter oWriter = new PrintWriter(new FileWriter(output_file))) {
      for (Player P : sort) { oWriter.println(P.format()); }
      oWriter.flush();
    }
  }
}