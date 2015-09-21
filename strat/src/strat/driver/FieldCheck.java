package strat.driver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import strat.client.model.Batter;
import strat.client.model.ColumnPlay;
import strat.client.model.FieldCollector;
import strat.client.model.ParkInfo;
import strat.client.model.Pitcher;
import strat.client.model.Player;
import strat.client.model.Position;
import strat.client.model.ToggleState;

public class FieldCheck {
  private static final Path BAT_DIR = Paths.get("C:/build/strat/bat");
  private static final Path PITCH_DIR = Paths.get("C:/build/strat/pit");

  
  private static void collect(FieldCollector fc, ColumnPlay[] cols) {
    for (ColumnPlay cp : cols) { cp.collect(fc, 1, new ToggleState()); }
  }
  
  private static String getName(Path cardFile) {
    return cardFile.getFileName().toString().split("\\.")[0];
  }
  
  private static void printHeader() {
    System.out.format("Player");
    for (Position pt : EnumSet.allOf(Position.class)) { System.out.format("\tH(%s)", pt.code()); }
    for (Position pt : EnumSet.allOf(Position.class)) { System.out.format("\tO(%s)", pt.code()); }
    for (Position pt : EnumSet.allOf(Position.class)) { System.out.format("\tF(%s)", pt.code()); }
    System.out.println();
  }
  
  private static void process(Player card) {
    FieldCollector fc = new FieldCollector();
    collect(fc, card._asL);
    collect(fc, card._asR);
    System.out.format("%c %s", card._nameFirst.charAt(0), card._nameLast);
    for (Position pt : EnumSet.allOf(Position.class)) {
      double[] ct = fc._hits.get(pt);
      double tot = ct == null ? 0 : ct[0];
      System.out.format("\t%.1f", tot);
    }
    for (Position pt : EnumSet.allOf(Position.class)) {
      double[] ct = fc._outs.get(pt);
      double tot = ct == null ? 0 : ct[0];
      System.out.format("\t%.1f", tot);
    }
    for (Position pt : EnumSet.allOf(Position.class)) {
      double[] ct = fc._fields.get(pt);
      double tot = ct == null ? 0 : ct[0];
      System.out.format("\t%.1f", tot);
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
