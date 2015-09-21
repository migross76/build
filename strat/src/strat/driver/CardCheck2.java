package strat.driver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import strat.client.model.Batter;
import strat.client.model.Fielding;
import strat.client.model.ParkInfo;
import strat.client.model.Pitcher;
import strat.client.model.Play;
import strat.client.model.Player;
import strat.client.model.TogglePlay;
import strat.client.model.TypeCollector;

public class CardCheck2 {
  private static final Path BAT_DIR = Paths.get("C:/build/strat/bat");
  private static final Path PITCH_DIR = Paths.get("C:/build/strat/pit");

  private static String getName(Path cardFile) {
    return cardFile.getFileName().toString().split("\\.")[0];
  }
  
  private static void printHeader() {
    System.out.format("Player\tL/R\tPos\tField\twOBA\tLwOBA\tRwOBA");
    for (Play.Type pt : EnumSet.allOf(Play.Type.class)) { System.out.format("\t%c", pt.code()); }
    for (TogglePlay.Type tt : EnumSet.allOf(TogglePlay.Type.class)) { System.out.format("\t/%c", tt.code()); }
    System.out.println();
  }
  
  private static void process(Player card) {
    TypeCollector tcL = new TypeCollector(card._asL);
    TypeCollector tcR = new TypeCollector(card._asR);
    System.out.format("%c %s\t%c\t%s\t", card._nameFirst.charAt(0), card._nameLast, card.handed(), card._fielding.get(0)._pos.code());
    for (Fielding f : card._fielding.values()) {
      System.out.format("%s(%de%d) ", f._pos.code(), f._field, f._err);
    }
    System.out.format("\t%.3f\t%.3f\t%.3f", (tcL.woba() + tcR.woba() * 2) / 3, tcL.woba(), tcR.woba());
    for (Play.Type pt : EnumSet.allOf(Play.Type.class)) {
      double[] ctL = tcL._plays.get(pt);
      double[] ctR = tcR._plays.get(pt);
      double tot = (ctL == null ? 0 : ctL[0]) + (ctR == null ? 0 : ctR[0]);
      System.out.format("\t%5.2f", tot);
    }
    for (TogglePlay.Type tt : EnumSet.allOf(TogglePlay.Type.class)) {
      double[] ctL = tcL._toggles.get(tt);
      double[] ctR = tcR._toggles.get(tt);
      double tot = (ctL == null ? 0 : ctL[0]) + (ctR == null ? 0 : ctR[0]);
      System.out.format("\t%5.2f", tot);
    }
    System.out.println();
  }
  
  public static void main(String[] args) throws Exception {
    printHeader();
    for (Path cardFile : Files.newDirectoryStream(BAT_DIR, "*.txt")) {
      process(new Batter(getName(cardFile), null, Files.readAllLines(cardFile, StandardCharsets.UTF_8), ParkInfo.AVERAGE));
    }
    System.out.println();
    printHeader();
    for (Path cardFile : Files.newDirectoryStream(PITCH_DIR, "*.txt")) {
      process(new Pitcher(getName(cardFile), null, Files.readAllLines(cardFile, StandardCharsets.UTF_8), ParkInfo.AVERAGE));
    }
  }
}
