package app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import util.ByPlayer;
import util.MyDatabase;
import data.Master;
import data.Sort;
import data.Teams;
import data.Type;
import data.War;

// Calculates two different measures:
// 1) The most valuable players to their SECOND most valuable franchise (e.g., Bonds on PIT, Ruth on BOS, Maddux on CHC)
//    Inspired by Pujols's trade to LAA (how many players produced 20+ WAR on 2+ teams?)
// 2) The players making the greatest contribution to one team OVER all the other teams they played on
//    - Any players that play on only one team count all the WAR for that team
//    - Any players on multiple teams count the team's WAR minus all other WAR from that.
//      Bonds SFG[121.6] - PIT[50.2] = 71.4
//      Molitor MIL[60.8] - TOR[9.3] - MIN[4.9] = 46.4
public class OneTwoPunch {
  public static final double MIN_SECONDARY = 20; // Minimal score for secondary team, (double this for single team contributions)
  
  public static class Player implements Comparable<Player> {
    public Player ( War total, FValue one, FValue two, double score ) { _total = total; _one = one; _two = two; _score = score; }
    
    public War _total = null;
    public FValue _one = null;
    public FValue _two = null;
    
    public double _score = 0;
    
    @Override
    public int compareTo(Player arg0) {
      if (_score != arg0._score) { return _score > arg0._score ? -1 : 1; }
      return _total.playerID().compareTo(arg0._total.playerID());
    }
  }
  
  public static class FValue implements Comparable<FValue> {
    public FValue(String franchID) { _franchID = franchID; }
    
    public String _franchID = null;
    public double _value = 0;
    @Override public int compareTo(FValue arg0) {
      if (_value != arg0._value) { return _value > arg0._value ? -1 : 1; }
      return _franchID.compareTo(arg0._franchID);
    }
  }
  
  public static void main(String[] args) throws Exception {
    Master.Table MT = null;
    Teams.Table TT = null;
    War.ByID wBy = new War.ByID();
    wBy.addFilter(War.filterPositive);
    try (MyDatabase db = new MyDatabase()) {
      MT = new Master.Table(db, Sort.UNSORTED);
      TT = new Teams.Table(db);
      wBy.addAll(new War.Table(db, Type.BAT));
      wBy.addAll(new War.Table(db, Type.PITCH));
    }
    
    ArrayList<Player> main = new ArrayList<>();
    ArrayList<Player> next = new ArrayList<>();
    for (ByPlayer<War> WW : wBy) {
      War pos = WW.filter(War.filterPositive);
      if (pos == null || pos.war() < MIN_SECONDARY * 2) { continue; }
      HashMap<String, FValue> map = new HashMap<>();
      for (War W : WW) {
        String franchID = TT.getFranchiseID(W.teamID());
        if (franchID == null) { continue; }
        FValue FV = map.get(franchID);
        if (FV == null) { map.put(franchID, FV = new FValue(franchID)); }
        FV._value += W.war();
      }
      ArrayList<FValue> vals = new ArrayList<>(map.values());
      Collections.sort(vals);
      if (vals.size() > 1 && vals.get(1)._value >= MIN_SECONDARY) {
        next.add(new Player(WW.total(), vals.get(0), vals.get(1), vals.get(1)._value));
      }
      double val = vals.get(0)._value * 2 - WW.total().war();
      if (val > WW.total().war()) { val = WW.total().war(); }
      if (val >= MIN_SECONDARY * 2) {
        main.add(new Player(WW.total(), vals.get(0), vals.get(0), val));
      }
    }
    
    Collections.sort(main);
    Collections.sort(next);
    
    for (Player P : main) {
      Master M = MT.byID(P._total.playerID());
      System.out.format("%s %s\t%.1f\t%s\t%.1f\t%.1f\n", M.nameFirst(), M.nameLast(), P._total.war(), P._one._franchID, P._one._value, P._score);
    }
    System.out.println();
    for (Player P : next) {
      Master M = MT.byID(P._total.playerID());
      System.out.format("%s %s\t%.1f\t%s\t%.1f\t%s\t%.1f\n", M.nameFirst(), M.nameLast(), P._total.war(), P._one._franchID, P._one._value, P._two._franchID, P._two._value);
    }
  }
}
