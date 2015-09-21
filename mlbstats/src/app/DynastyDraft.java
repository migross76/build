package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import util.MyDatabase;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

/*
 * PA >= 100: WAR_bat>.007*PA
IP >= 50: WAR_pitch>.018*IP

Correlate 2013 dynasty draft to: age, playing time + nWAR + prospect rank + draft rank for 2010-2012 (prospect + draft are for 2011-2013).

Start with 400 draftees. Download prospect + draft list. Grab age + playing time + nWAR from database. Correlate by full name.
Consider positional strength, too, per team. e.g., Becquey may pass on Reyes and draft Bautista b/c he already has Tulo. Though Berry drafted Miggy(1B?) + Longo.
Play with weights, or better yet figure out how to do linear weights.

2013 draft with my weights.
1990 draft with my weights.
Compute player rankings (whose PA/IP/GS count more), positional assignments, to compute 1990 scores.
Figure out the "release" weight. Might be based on who is out there, but how to figure out who you'll get. Easiest impl is to have each team drop players first, (who become free agents), then draft openings.
Also, play with different weights for different teams.

Simpler system:
- Correlate age, WAR of previous 3/4 seasons to WAR for: year 1, years 2-4, years 5+, risk (stdev)? Hopefully normalize to evenly-balanced numbers.
- Correlate age, draft position to WAR breakdown as well
- Break it down by SP, RP, C?, Bat
- Calculate projections of all actives/prospects for the given year
- Do a draft, trying to balance three groups per position.
- For scoring, select highest WAR per slot.
- Which slots? Min 8 bat + 3 SP + 1 RP; Max 8+5 backup (C,CIF,MIF,OF,DH) + 5 SP + 3 RP. The other part of the 40 are on the roster.
- For next year, drop worst players at over-valued slots. Watch for position changes. Either a required drop amount, or drop all under a certain threshold. Use "next year's" projections to determine who to drop.
- Fill out the rest with a draft starting midway; worst score from previous year goes first. If unbalanced drops, extra ones are drafted at the end.

 */
public class DynastyDraft {
  
  private static class Player implements Comparable<Player> {
    public Player(Master m) { _master = m; }
    
    public String playerID() { return _master.playerID(); }
    
    public double nWAR() { return _season == 0 ? -10 : _war * Math.sqrt(1 / _season); }
    public final Master _master;
    public double _war = 0;
    public double _waa = 0;
    public double _season = 0;
    public double _norm = 0;
    
    public double _new_war = 0;
    public double _new_season = 0;

    @Override public int compareTo(Player arg0) {
      double cmp = 0;
      if ((cmp = _waa - arg0._waa) != 0) { return cmp > 0 ? -1 : 1; }
      if ((cmp = nWAR() - arg0.nWAR()) != 0) { return cmp > 0 ? -1 : 1; }
      if ((cmp = _war - arg0._war) != 0) { return cmp > 0 ? -1 : 1; }
      return playerID().compareTo(arg0.playerID());
    }
    private HashSet<Integer> _years = new HashSet<>();
  }
  
  private static Master.Table _mt = null;

  private static HashMap<String, Player> _players = null;
  
  private static void populate(MyDatabase db, Type type) throws SQLException {
    War.Table wt = new War.Table(db, type);
    HashMap<String, Player> players = new HashMap<>();
    for (War w : wt) {
      if (w.yearID() < 1987 || w.yearID() > 1990) { continue; }
      Player p = players.get(w.playerID());
      if (p == null) { players.put(w.playerID(), p = new Player(_mt.byID(w.playerID()))); }
      if (w.yearID() == 1990) {
        p._new_war += w.war();
        p._new_season += w.playtime() / (double)type.playtime();
        continue;
      }
      double norm = (w.yearID() - 1985) / 9.0; // 1987 = 2, 1989 = 4
      p._waa += w.waa() * norm;
      p._war += w.war() * norm;
      p._season += (w.playtime() / (double)type.playtime()) * norm;
      if (p._years.add(w.yearID())) { p._norm += norm; }
    }
    if (_players == null) { _players = players; }
    else {
      for (Player p : players.values()) {
        Player p2 = _players.get(p._master.playerID());
        if (p2 == null || p2._season < p._season) { _players.put(p._master.playerID(), p); } 
      }
    }
  }
  
  public static void main(String[] args) throws Exception {
    try (MyDatabase db = new MyDatabase()) {
      _mt = new Master.Table(db, Sort.UNSORTED);
      populate(db, Type.BAT);
      populate(db, Type.PITCH);
    }
    ArrayList<Player> players = new ArrayList<>(_players.values());
    Collections.sort(players);

    System.out.print("Name\tWAR\tWAA\tnWAR\tsWAR\tSeason\tNorm\tWAR*\tSeason*\n");
    for (int i = 0; i != 300; ++i) {
      Player p = players.get(i);
      System.out.format("%s %s\t%.1f\t%.2f\t%.1f\t%.1f\t%.2f\t%.2f\t%.1f\t%.2f\n", p._master.nameFirst(), p._master.nameLast(),
          p._war, p._waa, p.nWAR(), p._war / p._season, p._season, p._norm, p._new_war, p._new_season);
    }
  }
}
