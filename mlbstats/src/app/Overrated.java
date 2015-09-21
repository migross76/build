package app;

import java.util.ArrayList;
import java.util.Collections;
import util.ByPlayer;
import util.MyDatabase;
import data.ELO;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

// Compare ELO ratings against WAR to determine who is overrated or underrated
// - Or, of course, who is likely not adequately represented by WAR (i.e. Jackie Robinson, Barry Bonds)
// - Split by batter/pitcher, active/"retired"
// - Comparison done by best fit equations that map ELO to WAR, as calculated by Excel
public class Overrated {
  private static final int ACTIVE_YEAR = 2011;
  
  public static class Player implements Comparable<Player> {
    public Master _master = null;
    public double _war = 0;
    public ELO _elo = null;
    public boolean _active = false;
    
    public void addWAR(ByPlayer<War> ag) {
      if (ag == null) { return; }
      _war += ag.total().war();
      if (ag.last().yearID() == ACTIVE_YEAR) { _active = true; }
    }
    
    public double diff() { return _war - _elo.norm(); }
    
    @Override public int compareTo(Player arg0) {
      if (diff() != arg0.diff()) { return diff() > arg0.diff() ? -1 : 1; }
      if (_war != arg0._war) { return _war > arg0._war ? -1 : 1; }
      return _master.playerID().compareTo(arg0._master.playerID());
    }
    
    @Override public String toString() {
      return String.format("%s %s\t%d\t%d\t%.1f\t%.1f\t%.1f", _master.nameFirst(), _master.nameLast(), _master.yearBirth(), _elo.rating(), _elo.norm(), _war, diff());
    }
    
    public static final String HEADER = "Name\tBorn\tRating\txWAR\tWAR\tdiff";
  }

  public static void print(ArrayList<Player> players, int num) {
    Collections.sort(players);
    
    System.out.println(Player.HEADER);
    for (int i = 0; i != num; ++i) {
      System.out.println(players.get(i));
    }
    System.out.println();
    System.out.println(Player.HEADER);
    for (int i = players.size(); i != players.size() - num; --i) {
      System.out.println(players.get(i-1));
    }
    System.out.println();
  }
  
  public static void main(String[] args) throws Exception {
    Master.Table MT = null;
    War.Table BT = null;
    War.Table PT = null;
    ELO.Table ET = null;
    try (MyDatabase db = new MyDatabase()) {
      MT = new Master.Table(db, Sort.UNSORTED);
      BT = new War.Table(db, Type.BAT);
      PT = new War.Table(db, Type.PITCH);
      ET = new ELO.Table(db);
    }
    
    War.ByID BG = new War.ByID();
    BG.addAll(BT);
    War.ByID PG = new War.ByID();
    PG.addAll(PT);
    
    ArrayList<Player> activeP = new ArrayList<>();
    ArrayList<Player> retiredP = new ArrayList<>();
    ArrayList<Player> activeB = new ArrayList<>();
    ArrayList<Player> retiredB = new ArrayList<>();
    for (ELO E : ET.all()) {
      Player P = new Player();
      P._elo = E;
      P._master = MT.byID(E.playerID());
      P._active = (ACTIVE_YEAR - P._master.yearBirth() < 35);
      P.addWAR(BG.get(E.playerID()));
      P.addWAR(PG.get(E.playerID()));
      if (P._active) { if (P._elo.type() == Type.PITCH) { activeP.add(P); } else { activeB.add(P); } }
      else { if (P._elo.type() == Type.PITCH) { retiredP.add(P); } else { retiredB.add(P); } }
    }
    print(retiredP, 50);
    print(retiredB, 50);
    print(activeP, 20);
    print(activeB, 20);
  }
}
