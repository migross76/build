package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import util.MyDatabase;
import util.WeightedWar;
import data.Appearances;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

// Select the best single seasons (batting by default) in history, based on:
// 1) wWAR
// 2) The amount the player's wWAR tops the other top 10 players (minus the top player's lead)
// Grab a list of the top seasons for each player (1 per player)
// Also grab a list of the top seasons by players that have better seasons
// - This was in response to ESPN's 32-player "Best Season Ever" tournament
public class BestSeasons {
  public static class Player implements Comparable<Player> {
    public WeightedWar _wwar = null;
    public double _above = 0;
    @Override
    public int compareTo(Player o) {
      if (_above != o._above) { return _above > o._above ? -1 : 1; }
      return _wwar.compareTo(o._wwar);
    }
    public Player(WeightedWar WW, double above) { _wwar = WW; _above = above; }
  }
  
  private static int FIRST_YEAR = 1901;
  private static int TOP_N_YEAR = 10;
  private static int TOP_N_PRINT = 100;
  
  public static void aboveYear() throws Exception {
    Appearances.Table AT = null;
    Master.Table MT = null;
    WeightedWar.Tally WWT = null;
    try (MyDatabase db = new MyDatabase()) {
      AT = new Appearances.Table(db);
      MT = new Master.Table(db, Sort.UNSORTED);
      WWT = new WeightedWar.Tally(new War.Table(db, Type.BAT));
    }
    WWT.adjustByPosition(AT);

    TreeMap<Integer, Set<WeightedWar>> map = new TreeMap<>();
    for (WeightedWar WW : WWT) {
      int yr = WW.yearID();
      if (yr < FIRST_YEAR) { continue; }
      Set<WeightedWar> set = map.get(yr);
      if (set == null) { map.put(yr, set = new TreeSet<>()); }
      set.add(WW);
    }
    
    ArrayList<Player> all = new ArrayList<>();
    ArrayList<Player> year = new ArrayList<>();
    for (Set<WeightedWar> set : map.values()) {
      int i = 0;
      for (WeightedWar WW : set) {
        for (Player P : year) { P._above += P._wwar.wwar() - WW.wwar(); }
        year.add(new Player(WW, year.isEmpty() ? 0 : -year.get(0)._above));
        if (++i == TOP_N_YEAR) { break; }
      }
      all.addAll(year);
      year.clear();
    }
    Collections.sort(all);

    
    ArrayList<Player> others = new ArrayList<>();
    System.out.println("Year\tName\tWAR\tPF\twWAR\tAbove");
    HashSet<String> ids = new HashSet<>();
    int i = 0;
    for (Player P : all) {
      WeightedWar WW = P._wwar;
      String id = WW.playerID();
      if (!ids.add(id)) { others.add(P); continue; }
      if (i > TOP_N_PRINT) { break; }
      Master M = MT.byID(WW.playerID());
      System.out.format("%d\t%s %s\t%.1f\t%.2f\t%.1f\t%.1f\n", WW.yearID(), M.nameFirst(), M.nameLast(), WW.war(), WW.factor(), WW.wwar(), P._above);
      if (++i >= TOP_N_PRINT && others.size() > TOP_N_PRINT) { break; }
    }
    
    System.out.println("\nYear\tName\tWAR\tPF\twWAR\tAbove");
    i = 0;
    for (Player P : others) {
      WeightedWar WW = P._wwar;
      Master M = MT.byID(WW.playerID());
      System.out.format("%d\t%s %s\t%.1f\t%.2f\t%.1f\t%.1f\n", WW.yearID(), M.nameFirst(), M.nameLast(), WW.war(), WW.factor(), WW.wwar(), P._above);
      if (++i == TOP_N_PRINT) { break; }
    }
  }

  public static void overall() throws Exception {
    War.Table WT = null;
    Appearances.Table AT = null;
    Master.Table MT = null;
    try (MyDatabase db = new MyDatabase()) {
      WT = new War.Table(db, Type.BAT);
      AT = new Appearances.Table(db);
      MT = new Master.Table(db, Sort.UNSORTED);
    }
    WeightedWar.Tally WWT = new WeightedWar.Tally(WT);
    WWT.adjustByPosition(AT);

    TreeSet<WeightedWar> set = new TreeSet<>();
    for (WeightedWar WW : WWT) { set.add(WW); }
    
    ArrayList<WeightedWar> others = new ArrayList<>();
    System.out.println("\nYear\tName\tWAR\tPF\twWAR");
    HashSet<String> ids = new HashSet<>();
    int i = 0;
    for (WeightedWar WW : set) {
      String id = WW.playerID();
      if (!ids.add(id)) { others.add(WW); continue; }
      if (i > TOP_N_PRINT) { break; }
      Master M = MT.byID(WW.playerID());
      System.out.format("%d\t%s %s\t%.1f\t%.2f\t%.1f\n", WW.yearID(), M.nameFirst(), M.nameLast(), WW.war(), WW.factor(), WW.wwar());
      if (++i >= TOP_N_PRINT && others.size() > TOP_N_PRINT) { break; }
    }
    
    System.out.println("Year\tName\tWAR\tPF\twWAR");
    i = 0;
    for (WeightedWar WW : others) {
      Master M = MT.byID(WW.playerID());
      System.out.format("%d\t%s %s\t%.1f\t%.2f\t%.1f\n", WW.yearID(), M.nameFirst(), M.nameLast(), WW.war(), WW.factor(), WW.wwar());
      if (++i == TOP_N_PRINT) { break; }
    }
  }

  public static void main(String[] args) throws Exception {
    aboveYear();
  }
}
