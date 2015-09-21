package strat.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import strat.shared.CardLegacy;
import strat.shared.Roster;

public class CardData {
  private static final Path CARD_DIR = Paths.get("C:/build/strat/cards");
  private static final Path TEAM_DIR = Paths.get("C:/build/strat/teams");

  private static String getName(Path cardFile) {
    return cardFile.getFileName().toString().split("\\.")[0];
  }
  
  private static CardLegacy loadCard(Path cardFile) throws IOException {
//System.out.println(cardFile);
    List<String> lines = Files.readAllLines(cardFile, StandardCharsets.UTF_8);
    CardLegacy card = new CardLegacy();
    card._id = getName(cardFile);
    card._name = lines.get(0);
    card._pos = lines.get(1).split(" ");
    int line = 2;
    if (!card.isPitcher()) {
      line = 4;
      card._stealing = lines.get(2).charAt(0);
      card._running = Integer.parseInt(lines.get(3));
    }
    int rowno = 0;
    int slotno = 0;
    for (; line != lines.size(); ++line) {
      //System.out.println(line + " " + rowno + " " + slotno + " : " + lines.get(line));
      String[] split = lines.get(line).split(" ");
      if (split.length < 2) {
        continue;
      }
      CardLegacy.Slot slot = new CardLegacy.Slot();
      card._slots[rowno][slotno] = slot;
      slot._split = Double.parseDouble(split[0]);
      slot._first = new CardLegacy.Stat(split);
      if (slot._split != 1) {
        slot._second = new CardLegacy.Stat(lines.get(++line).split(" "));
      }
      if (++slotno == CardLegacy.COLS) {
        slotno = 0;
        ++rowno;
      }
    }
    return card;
  }
  
  private Roster loadRoster(Path teamFile) throws IOException {
    List<String> lines = Files.readAllLines(teamFile, StandardCharsets.UTF_8);
    Roster roster = new Roster();
    roster._name = getName(teamFile);
    for (String line : lines) {
      CardLegacy card = _cards.get(line);
      if (card != null) { roster._players.add(_cards.get(line)); }
      else if (!line.isEmpty()) { System.err.println("Could not find card for " + line); }
    }
    return roster;
  }
  
  public Iterable<CardLegacy> cards() { return _cards.values(); }
  public Set<String> teams() { return _rosters.keySet(); }
  public Roster roster(String name) { return _rosters.get(name); }
  
  public CardData() throws IOException {
    for (Path cardFile : Files.newDirectoryStream(CARD_DIR, "*.txt")) {
      CardLegacy card = loadCard(cardFile);
      _cards.put(card._id, card);
    }
    for (Path teamFile : Files.newDirectoryStream(TEAM_DIR, "*.txt")) {
      Roster roster = loadRoster(teamFile);
      _rosters.put(roster._name, roster);
    }
  }
  
  private final HashMap<String, CardLegacy> _cards = new HashMap<>();
  private final HashMap<String, Roster> _rosters = new HashMap<>();
}
