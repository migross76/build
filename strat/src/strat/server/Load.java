package strat.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import strat.client.model.Batter;
import strat.client.model.FieldChart;
import strat.client.model.ParkInfo;
import strat.client.model.Pitcher;
import strat.client.model.Player;

public class Load {
  public static boolean STATUS = true;
  
  public static final Path BASE_DIR = Paths.get("C:/build/strat");

  private static final Path BAT_DIR = BASE_DIR.resolve("bat");
  private static final Path PITCH_DIR = BASE_DIR.resolve("pit");
  private static final Path OTHER_DIR = BASE_DIR.resolve("other");

  private static String getName(Path cardFile) {
    return cardFile.getFileName().toString().split("\\.")[0];
  }
  
  public Load() throws IOException {
    for (String line : Files.readAllLines(OTHER_DIR.resolve("mypos.txt"), StandardCharsets.UTF_8)) {
      if (line.isEmpty()) { continue; }
      String[] tokens = line.split("\t");
      ArrayList<String> ids = _posMap.get(tokens[1]);
      if (ids == null) { _posMap.put(tokens[1], ids = new ArrayList<>()); }
      ids.add(tokens[0]);
    }
    for (Path cardFile : Files.newDirectoryStream(BAT_DIR, "*.txt")) {
      String id = getName(cardFile);
      if (STATUS) { System.out.println(id); }
      Batter b = new Batter(id, null, Files.readAllLines(cardFile, StandardCharsets.UTF_8), ParkInfo.AVERAGE);
      _cards.put(id + "b", b);
      _batters.add(b);
    }
    for (Path cardFile : Files.newDirectoryStream(PITCH_DIR, "*.txt")) {
      String id = getName(cardFile);
      if (STATUS) { System.out.println(id); }
      Pitcher p = new Pitcher(id, null, Files.readAllLines(cardFile, StandardCharsets.UTF_8), ParkInfo.AVERAGE);
      _cards.put(id + "p", p);
      _pitchers.add(p);
    }
    _fielding.load(DataStore.getFieldInfo());
  }
  
  public final HashMap<String, Player> _cards = new HashMap<>();
  public final HashMap<String, ArrayList<String>> _posMap = new HashMap<>();
  public final ArrayList<Batter>  _batters  = new ArrayList<>();
  public final ArrayList<Pitcher> _pitchers = new ArrayList<>();
  public final FieldChart         _fielding = new FieldChart();
}
