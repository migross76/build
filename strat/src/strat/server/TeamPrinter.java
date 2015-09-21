package strat.server;

import strat.client.model.Batter;
import strat.client.model.Pitcher;
import strat.client.model.Team;

public class TeamPrinter {
  private static void printLineup(Batter b, int i) {
    System.out.format("%d\t%s %s (%s)\t%.3f", i+1, b._nameFirst, b._nameLast, b._onfield == null ? "DH" : b._onfield._pos.code().toUpperCase(), b._bwoba + b._fwoba);
  }
  
  private static void printBench(Batter b) {
    System.out.format("\t%s %s (%s)\t%.3f", b._nameFirst, b._nameLast, b._primary, b._bwoba + b._fwoba);
  }
  
  private static void printPitcher(Pitcher p) {
    System.out.format("\t%s %s\t%.3f", p._nameFirst, p._nameLast, p._woba);
  }
  
  public static void print(Team vis, Team home) {
    System.out.println("\tVISITORS\t\t\tHOME\t");
    System.out.println("\tLINEUP\t\t\tLINEUP\t");
    for (int i = 0; i != 9; ++i) {
      printLineup(vis._lineup[i], i);
      System.out.print("\t");
      printLineup(home._lineup[i], i);
      System.out.println();
    }
    System.out.println("\tSTARTER\t\t\tSTARTER\t");
    printPitcher(vis._pitcher);
    System.out.print("\t");
    printPitcher(home._pitcher);
    System.out.println();
    System.out.println("\tBENCH\t\t\tBENCH\t");
    for (int i = 0, e = Math.max(vis._bench.size(), home._bench.size()); i != e; ++i) {
      if (i < vis._bench.size()) { printBench(vis._bench.get(i)); } else { System.out.print("\t\t"); }
      System.out.print("\t");
      if (i < home._bench.size()) { printBench(home._bench.get(i)); } else { System.out.print("\t\t"); }
      System.out.println();
    }
    System.out.println("\tROTATION\t\t\tROTATION\t");
    for (int i = 0, e = Math.max(vis._rotation.size(), home._rotation.size()); i != e; ++i) {
      if (i < vis._rotation.size()) { printPitcher(vis._rotation.get(i)); } else { System.out.print("\t\t"); }
      System.out.print("\t");
      if (i < home._rotation.size()) { printPitcher(home._rotation.get(i)); } else { System.out.print("\t\t"); }
      System.out.println();
    }
    System.out.println("\tBULLPEN\t\t\tBULLPEN\t");
    for (int i = 0, e = Math.max(vis._bullpen.size(), home._bullpen.size()); i != e; ++i) {
      if (i < vis._bullpen.size()) { printPitcher(vis._bullpen.get(i)); } else { System.out.print("\t\t"); }
      System.out.print("\t");
      if (i < home._bullpen.size()) { printPitcher(home._bullpen.get(i)); } else { System.out.print("\t\t"); }
      System.out.println();
    }
  }
  
  public static void print(Team t) {
    System.out.println("LINEUP");
    for (int i = 0; i != 9; ++i) { Batter b = t._lineup[i]; System.out.format("%d\t%s %s (%s)\t%.3f\n", i+1, b._nameFirst, b._nameLast, b._onfield == null ? "DH" : b._onfield._pos.code().toUpperCase(), b._bwoba + b._fwoba); }
    System.out.println("STARTER");
    System.out.format("\t%c %s\n", t._pitcher._nameFirst.charAt(0), t._pitcher._nameLast);
    System.out.println("BENCH");
    for (int i = 0; i != t._bench.size(); ++i) { Batter b = t._bench.get(i); System.out.format("\t%s %s (%s)\t%.3f\n", b._nameFirst, b._nameLast, b._primary, b._bwoba + b._fwoba); }
    System.out.println("BULLPEN");
    for (int i = 0; i != t._bullpen.size(); ++i) { Pitcher p = t._bullpen.get(i); System.out.format("\t%c %s\n", p._nameFirst.charAt(0), p._nameLast); }
  }
}
