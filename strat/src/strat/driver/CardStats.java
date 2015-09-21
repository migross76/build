package strat.driver;

import java.util.Map;
import java.util.TreeMap;
import strat.server.CardData;
import strat.shared.CardLegacy;

// TODO factor this out into a Deck object, and a DeckParser object
// TODO calculate the occurrences of each event, and output that
// TODO find the same player in the database, and calculate the breakdown for real stats
public class CardStats {

  public static void addWeight(TreeMap<String, double[]> stats, CardLegacy.Stat stat,
      double weight) {
    double[] wt = stats.get(stat._event);
    if (wt == null) {
      stats.put(stat._event, wt = new double[1]);
    }
    wt[0] += weight;
  }

  public static void main(String[] args) throws Exception {
    Iterable<CardLegacy> cards = new CardData().cards();
    for (CardLegacy card : cards) {
      //System.out.println(card._name);
      TreeMap<String, double[]> stats = new TreeMap<>();
      for (int r = 0; r != CardLegacy.ROWS; ++r) {
        for (int c = 0; c != CardLegacy.COLS; ++c) {
          CardLegacy.Slot slot = card._slots[r][c];
          //System.out.println(" " + r + " : " + c);
          double weight = c < 6 ? c + 1 : CardLegacy.COLS - c;
          addWeight(stats, slot._first, weight * slot._split);
          if (slot._split != 1) {
            addWeight(stats, slot._second, weight * (1 - slot._split));
          }
        }
      }
      System.out.println(card._name);
      for (Map.Entry<String, double[]> e : stats.entrySet()) {
        System.out.format("%s\t%5.2f\t%4.1f%%\n", e.getKey(), e.getValue()[0],
            e.getValue()[0] * 100.0 / 108.0);
      }
      System.out.println();
    }
  }
}
