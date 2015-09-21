package strat.driver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import strat.client.model.Batter;
import strat.client.model.ParkInfo;
import strat.client.model.Play;
import strat.client.model.TogglePlay;
import strat.client.model.TypeCollector;

public class CardCheck {
  private static final String ID = "aaronha01";
  private static final Path CARD_FILE = Paths.get("C:/build/strat/bat/" + ID + ".txt");
  
  private static void print(String name, TypeCollector tc) {
    System.out.print("Against " + name);
    for (Play.Type pt : EnumSet.allOf(Play.Type.class)) {
      double[] ct = tc._plays.get(pt);
      if (ct == null) { System.out.print("\t-"); }
      else { System.out.format("\t%5.2f", ct[0]); }
    }
    System.out.print("\t");
    for (TogglePlay.Type tt : EnumSet.allOf(TogglePlay.Type.class)) {
      double[] ct = tc._toggles.get(tt);
      if (ct == null) { System.out.print("\t-"); }
      else { System.out.format("\t%5.2f", ct[0]); }
    }
    System.out.println();
    
  }
  
  public static void main(String[] args) throws Exception {
    List<String> lines = Files.readAllLines(CARD_FILE, StandardCharsets.UTF_8);
    Batter card = new Batter(ID, null, lines, ParkInfo.AVERAGE);
    TypeCollector tcL = new TypeCollector(card._asL);
    TypeCollector tcR = new TypeCollector(card._asR);
    System.out.print(card._nameFirst.charAt(0) + " " + card._nameLast);
    for (Play.Type pt : EnumSet.allOf(Play.Type.class)) {
      System.out.format("\t%c", pt.code());
    }
    System.out.print("\t");
    for (TogglePlay.Type tt : EnumSet.allOf(TogglePlay.Type.class)) {
      System.out.format("\t%c", tt.code());
    }
    System.out.println();
    print("LHP", tcL);
    print("RHP", tcR);
  }
}
