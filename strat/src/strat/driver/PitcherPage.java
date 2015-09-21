package strat.driver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import strat.client.model.NumberList;
import strat.client.model.ParkInfo;
import strat.client.model.Pitcher;
import strat.client.model.Running;

public class PitcherPage {
  private static final Path BASE_DIR = Paths.get("C:/build/strat");
  private static final Path PITCH_DIR = BASE_DIR.resolve("pit");

  private static String getName(Path cardFile) {
    return cardFile.getFileName().toString().split("\\.")[0];
  }
  
  private static String printNumberList(Running.Lead[] list, Running.Lead value) {
    ArrayList<Integer> vals = new ArrayList<>();
    for (int i_l = 0; i_l != list.length; ++i_l) {
      if (list[i_l] == value) { vals.add(i_l+2); }
    }
    return NumberList.print(vals);
  }
  
  public static void main(String[] args) throws Exception {
    for (Path cardFile : Files.newDirectoryStream(PITCH_DIR, "*.txt")) {
      String id = getName(cardFile);
      Pitcher p = new Pitcher(id, null, Files.readAllLines(cardFile, StandardCharsets.UTF_8), ParkInfo.AVERAGE);
      System.out.format("%s, %s\t%s\t1-%d\t%s\t%s\t%s\t%s\t(%s-%s)\n", p._nameLast, p._nameFirst, p._four_man ? "*" : "", p._run._advance, p._run._steal, p._run._auto_good ? "*" : "", printNumberList(p._run._lead, Running.Lead.GOOD), printNumberList(p._run._lead, Running.Lead.OUT), p._run._primary == 0 ? "-" : "" + p._run._primary, p._run._second == 0 ? "-" : "" + p._run._second);
    }
  }
}
