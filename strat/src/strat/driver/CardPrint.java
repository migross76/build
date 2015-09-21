package strat.driver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import strat.client.model.Batter;
import strat.client.model.ParkInfo;
import strat.client.model.Player;
import strat.client.model.PrintRow;
import strat.server.PrintRowPlain;

public class CardPrint {
  private static final String ID = "aaronha01";
  private static final Path CARD_FILE = Paths.get("C:/build/strat/bat/" + ID + ".txt");
  
  public static void main(String[] args) throws Exception {
    List<String> lines = Files.readAllLines(CARD_FILE, StandardCharsets.UTF_8);
    Player card = new Batter(ID, null, lines, ParkInfo.AVERAGE);
    
    ArrayList<ArrayList<PrintRow>> lists = new ArrayList<>();
    int max_list = 0;
    for (int i = 0; i != card._asL.length; ++i) {
      ArrayList<PrintRow> list = new ArrayList<>();
      lists.add(list);
      card._asL[i].print(list, false);
      if (max_list < list.size()) { max_list = list.size(); }
    }
    for (int i = 0; i != card._asR.length; ++i) {
      ArrayList<PrintRow> list = new ArrayList<>();
      lists.add(list);
      card._asR[i].print(list, false);
      if (max_list < list.size()) { max_list = list.size(); }
    }
    
    for (int row = 0; row != max_list; ++row) {
      for (ArrayList<PrintRow> list : lists) {
        if (list.size() > row) {
          System.out.print(PrintRowPlain.toPlainString(list.get(row)));
        } else {
          System.out.print(PrintRowPlain.empty());
        }
        System.out.print("\t");
      }
      System.out.println();
    }
  }
}
