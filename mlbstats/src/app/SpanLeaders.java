package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.Master;
import data.Position;
import data.Sort;
import data.Type;
import data.War;

/*
 * Best player by position for each position over an N-year span, for each starting year 
 */
public class SpanLeaders {
  private WeightedApp.ByYear byWA = new WeightedApp.ByYear();
  private Master.Table MT = null;
  
  private static final int YEARS = 5;
  private static final int START_YEAR = 1901;
  private static final int END_YEAR = 2013;
  
  private static class Player implements Comparable<Player> {
    @Override
    public int compareTo(Player o) { 
      double diff = score() - o.score();
      if (diff != 0) { return diff > 0 ? -1 : 1; }
      return playerID().compareTo(o.playerID());
    }
    
    public String playerID() { return _first.playerID(); }
    public double score() { return _total; }

    public Player(WeightedApp WA) { _first = WA; }
    
    public WeightedApp _first = null;
    public double _total = 0;
  }
  
  private static class Slot {
    public Slot(Position pos) { _name = pos.getName(); _pos = EnumSet.of(pos); }
    public Slot(String name, Position... poses) { _name = name; _pos = EnumSet.copyOf(Arrays.asList(poses)); }
    
    public void addIf(WeightedApp WA) {
      double total = 0;
      for (Position pos : _pos) { total += WA.games(pos); }
      if (total != 0) {
        Player P = _players.get(WA.playerID());
        if (P == null) { _players.put(WA.playerID(), P = new Player(WA)); }
        P._total += total;
      }
    }
    
    public void reset() { _players.clear(); }
    
    public boolean isEmpty() { return _players.isEmpty(); }
    
    public ArrayList<Player> ordered() {
      ArrayList<Player> order = new ArrayList<>(_players.values());
      Collections.sort(order);
      return order;
    }
    
    public String _name = null;
    public EnumSet<Position> _pos = null;
    public HashMap<String, Player> _players = new HashMap<>();
  }
  
  public void execute() {
    ArrayList<Slot> slots = new ArrayList<>();
    slots.add(new Slot(Position.CATCH));
    slots.add(new Slot(Position.FIRST));
    slots.add(new Slot(Position.SECOND));
    slots.add(new Slot(Position.THIRD));
    slots.add(new Slot(Position.SHORT));
    slots.add(new Slot(Position.LEFT));
    slots.add(new Slot(Position.CENTER));
    slots.add(new Slot(Position.RIGHT));
    slots.add(new Slot(Position.DESIG));
    slots.add(new Slot(Position.STARTER));
    slots.add(new Slot("RP", Position.MIDDLE, Position.CLOSER));
    slots.add(new Slot("IF", Position.CATCH, Position.FIRST, Position.SECOND, Position.THIRD, Position.SHORT));
    slots.add(new Slot("OF", Position.LEFT, Position.CENTER, Position.RIGHT));
    slots.add(new Slot("Bat", Position.CATCH, Position.FIRST, Position.SECOND, Position.THIRD, Position.SHORT, Position.LEFT, Position.CENTER, Position.RIGHT));
    slots.add(new Slot("Pit", Position.STARTER, Position.MIDDLE, Position.CLOSER));
    
    System.out.print("Start\tEnd");
    for (Slot S : slots) { System.out.format("\t%s\t%s", S._name, S._name); }
    System.out.println();

    for (int s_yr = START_YEAR; s_yr != END_YEAR - YEARS + 2; ++s_yr) {
      for (Slot S : slots) { S.reset(); }
      for (int i_yr = 0; i_yr != YEARS; ++i_yr) {
        for (WeightedApp WA : byWA.get(s_yr + i_yr)) {
          for (Slot S : slots) { S.addIf(WA); }
        }
      }
      System.out.format("%d\t%d", s_yr, s_yr + YEARS - 1);
      for (Slot S : slots) {
        if (S.isEmpty()) { System.out.print("\t\t"); continue; }
        ArrayList<Player> order = S.ordered();
        Player P = order.get(0);
        Master M = MT.byID(P.playerID());
        System.out.format("\t%s %s\t%.1f", M.nameFirst(), M.nameLast(), P.score());
      }
      System.out.println();
    }
  }
  
  private void assemble(MyDatabase db, Appearances.Table AT, Type type) throws SQLException {
    War.Table WT = new War.Table(db, type);
    WeightedWar.Tally WWT = new WeightedWar.Tally(WT); 
    WWT.adjustByPosition(AT);
    WeightedApp.Tally WA = new WeightedApp.Tally(WT, AT);
    byWA.addAll(WA);
  }
  
  public SpanLeaders() throws SQLException {
    try (MyDatabase db = new MyDatabase()) {
      Appearances.Table AT = new Appearances.Table(db);
      MT = new Master.Table(db, Sort.UNSORTED);
      assemble(db, AT, Type.BAT);
      assemble(db, AT, Type.PITCH);
    }
  }
  
  public static void main(String[] args) throws SQLException {
    new SpanLeaders().execute();
  }
}
