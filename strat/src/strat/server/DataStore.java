package strat.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import strat.client.model.GameLog;

/*
 * This is the object that is in charge of retrieving all the file data for the clients.
 * 
 * To JSON or not to JSON
 * Pros: This app becomes a learning experience, not just a game/distraction/time sink. I can think about and learn lessons about REST and JSON APIs.
 *       I can do similar things with GWT ideas, such as Layouts, Animations, and Code Splitting
 *       Oh, and I can actually read the serialized messages in the browser console, instead of GWT's crazy method.
 * Cons: It's going to be slower going... it's quicker to do RPC and skip the JSON serialization.
 * 
 * To do REST + JSON, implement servlets that make calls to this, then transform the data to JSON.
 * 
 * Resources:
 * GWT: http://www.gwtproject.org/javadoc/latest/index.html?com/google/gwt/json/client/JSONParser.html
 *   Can also create a JSON object by creating JSONObject, etc. and then calling toString()
 * Java: https://code.google.com/p/google-gson/
 *    convert Java Objects into their JSON representation. It can also be used to convert a JSON string to an equivalent Java object.
 *    Gson can work with arbitrary Java objects including pre-existing objects that you do not have source-code of. 
 * http://stackoverflow.com/questions/6568436/need-to-json-output
 * http://stackoverflow.com/questions/683123/json-java-serialization-that-works-with-gwt
 * 
 */
public class DataStore {
  public static final Path BASE_DIR = Paths.get("C:/build/strat");

  private static final Path TEAM_DIR = BASE_DIR.resolve("teams");
  private static final Path BAT_DIR = BASE_DIR.resolve("bat");
  private static final Path PITCH_DIR = BASE_DIR.resolve("pit");
  private static final Path OTHER_DIR = BASE_DIR.resolve("other");
/*
  private static String getName(Path cardFile) {
    return cardFile.getFileName().toString().split("\\.")[0];
  }
*/
  private void loadPositionMap() throws IOException {
    if (_posMap != null) { return; }
    _posMap = new HashMap<>();
    for (String line : Files.readAllLines(OTHER_DIR.resolve("mypos.txt"), StandardCharsets.UTF_8)) {
      try {
        String[] tokens = line.split("\t");
        if (tokens.length < 2) { continue; }
        ArrayList<String> ids = _posMap.get(tokens[1]);
        if (ids == null) { _posMap.put(tokens[1], ids = new ArrayList<>()); }
        ids.add(tokens[0]);
      } catch (RuntimeException e) { System.err.println(line); throw e; } 
    }
  }
  
  public static String getFieldInfo() throws IOException {
    return new String(Files.readAllBytes(OTHER_DIR.resolve("fielding.txt")), StandardCharsets.UTF_8);
  }
  
  public static String getPositionalAdjustment() throws IOException {
    return new String(Files.readAllBytes(OTHER_DIR.resolve("pos_adjust.txt")), StandardCharsets.UTF_8);
  }
  
  public static String getEligibility() throws IOException {
    return new String(Files.readAllBytes(OTHER_DIR.resolve("eligible.txt")), StandardCharsets.UTF_8);
  }
  
  public static String getREPlayChart() throws IOException {
    return new String(Files.readAllBytes(OTHER_DIR.resolve("playRE.txt")), StandardCharsets.UTF_8);
  }
  
  public List<PlayerInfo> team(String leagueName, String teamName) throws IOException {
    if (!leagueName.equals("default")) { throw new IllegalArgumentException("unknown league name : " + leagueName); }
    ArrayList<PlayerInfo> roster = new ArrayList<>();
    for (String player : Files.readAllLines(TEAM_DIR.resolve(teamName + ".txt"), StandardCharsets.UTF_8)) {
      String[] split = player.split("\t");
      roster.add(fetchPlayer(split[0] + (split[1].endsWith("P") ? 'p' : 'b'), split[1].toLowerCase()));
    }
    return roster;
  }
  
  public static ArrayList<String> teamNames(String leagueName) {
    if (!leagueName.equals("default")) { throw new IllegalArgumentException("unknown league name : " + leagueName); }
    ArrayList<String> teams = new ArrayList<>();
    for (String team : TEAM_DIR.toFile().list()) {
      teams.add(team.replace(".txt", ""));
    }
    return teams;
  }
  
  private PlayerInfo fetchPlayer(String id, String position) throws IOException {
    PlayerInfo pi = _playerCache.get(id);
    if (pi == null) {
      boolean isPitcher = id.endsWith("p");
      Path dir = isPitcher ? PITCH_DIR : BAT_DIR;
      id = id.substring(0, id.length() - 1);
      String file = new String(Files.readAllBytes(dir.resolve(id + ".txt")), StandardCharsets.UTF_8);
      _playerCache.put(id, pi = new PlayerInfo(id, position, file, isPitcher));
    }
    return pi;
  }
  
  private void pickCards(String[] poses, Set<String> claimed, List<PlayerInfo> players) throws IOException {
    for (String position : poses) {
      ArrayList<String> ids = _posMap.get(position);
      String id = null;
      while (!claimed.add(id = ids.get(_random.nextInt(ids.size())))) {/*no-op*/}
      players.add(fetchPlayer(id, position));
    }
  }

  public static String getRunExpInfo() throws IOException {
    return new String(Files.readAllBytes(OTHER_DIR.resolve("runexp.txt")), StandardCharsets.UTF_8);
  }
  
  public void saveStats(GameLog log) {
    _stats.addLog(log);
  }
  
  public void printStats(int seasons) {
    System.out.println();
    _stats.printBatters(seasons);
    System.out.println();
    _stats.printPitchers(seasons);
    System.out.println();
    _stats.printFieldRunExp();
    System.out.println();
    _stats.printPlayRunExp();
    System.out.println();
    _stats.printRunExp();
  }
  
  public List<PlayerInfo> allPlayers() throws IOException {
    loadPositionMap();
    ArrayList<PlayerInfo> players = new ArrayList<>();
    for (Map.Entry<String, ArrayList<String>> entry : _posMap.entrySet()) {
      String pos = entry.getKey();
      for (String id : entry.getValue()) {
        players.add(fetchPlayer(id, pos));
      }
    }
    return players;
  }
  
  private static final String[] GEN_TEAM_PITCH = { "sp", "sp", "sp", "sp", "sp", "rp", "rp", "rp" };
  private static final String[] GEN_TEAM_BAT = { "c", "1b", "2b", "3b", "ss", "lf", "cf", "rf" };
  public TeamInfo generateTeam(Set<String> claimed) throws IOException {
    loadPositionMap();
    TeamInfo info = new TeamInfo();
    pickCards(GEN_TEAM_PITCH, claimed, info._pitchers);
    pickCards(GEN_TEAM_BAT, claimed, info._batters);
    pickCards(GEN_TEAM_BAT, claimed, info._batters);
    return info;
  }

  private SeasonStats _stats = new SeasonStats();
  private HashMap<String, PlayerInfo> _playerCache = new HashMap<>();
  private Random _random = new Random(12345);
  private HashMap<String, ArrayList<String>> _posMap = null;
}
