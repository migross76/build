package alltime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import util.ByPlayer;
import util.MyDatabase;
import data.Appearances;
import data.Master;
import data.Sort;

/*
 * TODO Flatter RP warWeight (300IPOuts, not 900)
 * TODO Fill by position (8 starters, 1 Bat-only, C/CIF/MIF/CF/OF Backups, UT (Field+Pos >= 0)
 * TODO Allow backup weights (e.g., backups are 30%)
 * TODO Allow DH classification for all players
 * TODO Movement #1: DH and non-DH
 * TODO Movement #2: Secondary position (% games played compared to primary position?)
 * TODO Movement #3: Secondary season (e.g., replace ARod-SS with best ARod-3B season)
 * TODO Finally, sort each roster slot by seasonal warWeight (best estimate of standard performance)
 */

/*
Next Step (any one of the following):
A. Calculate per-position scores (incl. DH)
B. Sort/normalize per-position and do the draft
C. Improve the display, either with HTML output or GWT front-end

PA & IPOuts == Playing Time. Consider this renaming.

1. Grab all players with 10+ career WAR and at least 1 2+ WAR season.
  - Read them in by player, so it can group all of a player's seasons together.
  - Flag as pitcher/batter, so future fetches (i.e. position) will be fetched correctly.
  - Consider Batter and Pitcher sub-classes, with fetching logic self-contained
2. Calculate global age distribution
  a. Every season (PA & WAR) added to an age bracket.
  b. Separate by pitcher and batter, unless there's some way to combine PA/IPouts correctly
  c. Calculate what percentage of WAR was accumulated in each year
  d. Calculate the WAR per PA/IPouts of each age
3. For every player, calculate Career Weighted WAR
  - Career WAR * half the fraction of seasons they missed
  - Helps offset WWII (Williams/DiMaggio), Imports (Robinson/Suzuki), and present day (Pujols/Longoria)
4. For every player, calculate Career Normalized WAR
  - Begins with WAR * 650.0 / PA (batters) ; WAR * 900.0 / IPOuts (pitchers)
  - Calculate playing time distribution by age, and how it compares to default.
  - Offsets missed prime years (Williams/Robinson), players not yet hitting downturn (Pujols/Longoria)
5. For every player season, calculate Seasonal Normalized WAR
  - (WAR + Career Norm WAR) * DEFAULT PLAYING TIME / (playing time + DEFAULT PLAYING TIME)
  - Adjusts for short seasons (Murray 81, Bagwell 94, 1800s) and huge playing time discrepancies (1800s starters)
6. For every player season, calculate Seasonal Weighted WAR
  - Seasonal Normalized WAR * 4 + Career Weighted WAR
  - Produces a nice balance emphasizing career performance, but still selecting superior seasons from comparable careers.
7. For every batter season, calculate Composite Fielding WAR
  - (WAR / RAR) * (Pos RAR + Field RAR - PA * DH PENALTY) * 650.0 / PA
  - DH PENALTY = 17 RAR / 700 PA
  - Assesses fielding impact of player; used for projecting the DH spot, or a fielding-major spot.
  - Should this be weighted by career fielding performance? PRO: Seasonal fielding is unstable; CON: Fielding degrades over the years.
8. For every player, select best Seasonal Weighted WAR
  - This is the way to ensure a player is not encountered (and therefore more fetches/computations occur) until his first opportunity to be used. This way, Babe Ruth will be first; Julio Lugo will "never" show up.
  - For negative fielding values, subtract that (giving a higher score), as this player will be the "best" when playing DH.
9. Go through the player list by Seasonal Weighted WAR
  - Fetch positional information, and calculate best WAR for each position (batters include DH)
  - Secondary positions are weighted based on how playing time compares to the primary position (80G @ LF, 60G @ RF means RF is 75% of LF score). Is this too much of a penalty? 1.5 * playing time % (90 vs. 60+ = 100%, 90 vs. 45 = 75%, 90 vs. 30 = 50%, 90 vs. 9 = 15%). Not bad, but ideally something that scales (minimally) at the minimal differences.
  - Actually, do it by Slot, so: can handle multi-position slots, DH vs. fielder; bonus of precomputing playing percentage drops
10. Positional reconfiguring
  - Goal: If primary slot for a player is full, but a player in that slot can be shifted to another slot (recursive) for minimal penalty, do that to make room for the player.
  - Consideration: Taking a penalty (by shifting selected player and/or slotted player) means the selected player is less valuable, and may drop him below another player that.
  - Player going into a 100% slot opening can drop in immediately.
  - Penalty players must go onto a queue, where players start coming off once the best remaining score is lower than theirs.
  - How to handle when only partial playing time positions (i.e. backups) remain? Lots of players will queue up waiting to get to the 30-50% WAR of the backup level. Maybe track the minimum penalty level for whatever player shows up next, and evaluate queued players that exceed primary * penalty level.
  - When a queued player is pulled off the queue, his penalty must be re-evaluated, as the initial  placement plan may have disappeared (no empty slot, or player-to-swap has already swapped). If the penalty is greater, then he goes back on the queue (may even come off again before next player evaluated).
  - Handle swaps by tracking minimal penalty needed to place a player in a slot. If the slot has availability, then that value is 0. Otherwise, record the player to swap, the position to swap to, and the overall penalty (including penalty for the swapped position). When a slot is modified, re-evaluate it [there may be short-circuits] and any dependent slots.
11. Reorder in position by Seasonal Normalized WAR (to pick the best seasons)
12. Normalize by subtracting lowest Seasonal Normalized WAR (by slot), such that each score represents how much is gained over taking the final pick.
13. THE DRAFT
  - Draft by greatest Seasonal Scaled WAR for available positions.
  - No positional reconfiguring necessary, as each player is in his unique slot.
  - Team with lowest combined SSWAR drafts next (even if they just drafted) to ensure most balanced teams.
14. Output the teams, ideally with all the WAR calculations, plus some baseline stats.
  - HTML for multipage hyperlinked goodness?
 */

public class CareerSeasons {
  
  private static final double MIN_SEASON_WAR = 2.0;
  private static final double MIN_CAREER_WAR = 10.0;
  
  public static final Comparator<Season> _byWarWeight = new Comparator<Season>() {
    @Override
    public int compare(Season s0, Season s1) {
      int cmp = Double.compare(s0._warWeight, s1._warWeight);
      if (cmp != 0) { return -cmp; }
      return s0.compareTo(s1);
    }    
  };
  
  public static final Comparator<Season> _byWarNorm = new Comparator<Season>() {
    @Override
    public int compare(Season s0, Season s1) {
      int cmp = Double.compare(s0._warNorm, s1._warNorm);
      if (cmp != 0) { return -cmp; }
      return s0.compareTo(s1);
    }    
  };
  
  public static final Comparator<Season> _byWarReplace = new Comparator<Season>() {
    @Override
    public int compare(Season s0, Season s1) {
      int cmp = Double.compare(s0._warReplace, s1._warReplace);
      if (cmp != 0) { return -cmp; }
      return s0.compareTo(s1);
    }    
  };
  
  public static final Comparator<Career> _byBestSeason = new Comparator<Career>() {
    @Override
    public int compare(Career c0, Career c1) {
      int cmp = Double.compare(c0._seasons.get(0)._warWeight, c1._seasons.get(0)._warWeight);
      if (cmp != 0) { return -cmp; }
      return c0.compareTo(c1);
    }    
  };
  
  public static final Comparator<Roster> _byRosterTotal = new Comparator<Roster>() {
    @Override
    public int compare(Roster r0, Roster r1) {
      return Double.compare(r0._total, r1._total);
    }    
  };
  
  public static void computeSeasons(ArrayList<Career> careers, double adjust, int startYear, int endYear) {
    AgeDist AD = new AgeDist();
    ArrayList<Season> seasons = new ArrayList<>();
    for (Career C : careers) { seasons.addAll(C._seasons); }
    for (Season S : seasons) { AD.add(S); }
    AD.finish();
    for (Iterator<Career> i_C = careers.iterator(); i_C.hasNext(); ) {
      Career C = i_C.next();
//if (C._war > 80) { System.out.println(C._id + " : " + C._war + " : " + adjust + " : " + C._countStat + " : " + AD.careerFactor(C)); }
      C._warNorm = C._war * adjust / C._countStat / AD.careerFactor(C);
      C._warWeight = C._war * (1 + AD.careerMissing(C));
      for (Iterator<Season> i_S = C._seasons.iterator(); i_S.hasNext(); ) {
        Season S = i_S.next();
        if (S._year < startYear || S._year > endYear) { i_S.remove(); }
        else { S.compute(adjust); }
      }
      Collections.sort(C._seasons, _byWarWeight);
      if (C._seasons.isEmpty()) { i_C.remove(); }
    }
  }
  
  private static int ROSTERS = 30;
  private static int START_YEAR = 1700;
  private static int END_YEAR = 2100;
  
  public static void main(String[] args) throws SQLException, IOException {
    File out_dir = new File(args[1]);
    Roster roster = null;
    try (BufferedReader BR = new BufferedReader(new FileReader(args[0]))) {
      roster = new Roster(BR, ROSTERS);
    }

    Fetch fetch = null;
    Appearances.ByID at = new Appearances.ByID();
    ArrayList<Career> players = null;
    ArrayList<Career> pitchers = null;
    try (MyDatabase db = new MyDatabase()) {
      fetch = new Fetch(new Master.Table(db, Sort.UNSORTED));
      at.addAll(new Appearances.Table(db));
      players = fetch.getBatters(db, MIN_SEASON_WAR, MIN_CAREER_WAR);
      pitchers = fetch.getPitchers(db, MIN_SEASON_WAR, MIN_CAREER_WAR);
    }
    computeSeasons(players, Batter.PA_STD, START_YEAR, END_YEAR);
    computeSeasons(pitchers, Pitcher.IP3_STD, START_YEAR, END_YEAR);
    players.addAll(pitchers);
    Collections.sort(players, _byBestSeason);
    for (Career C : players) {
      ByPlayer<Appearances> ab = at.get(C._id);
      Season S = C._seasons.get(0);
      S._app = new Appearances(C._id, S._team, S._year);
      for (Appearances a : ab) {
        if (a.yearID() == S._year) { S._app.add(a); }
      }
      if (S._app.primary() == null) { continue; }
      for (Slot slot : roster) {
        if (slot.available() && slot.supports(S._app.primary().pos())) { slot.addPlayer(S); S._slotID = slot.getID(); break; }
      }
      if (roster.finished()) { break; }
    }
    ArrayList<Season> draftPool = new ArrayList<>();
    for (Slot slot : roster) {
      ArrayList<Season> seasons = slot.getSeasons();
      Collections.sort(seasons, _byWarNorm);
      double replace = seasons.get(seasons.size() - 1)._warNorm;
      for (Season S : seasons) { S._warReplace = (S._warNorm - replace) * slot.getWeight(); }
      draftPool.addAll(seasons);
    }
    try (PrintWriter sumPW = new PrintWriter(new FileWriter(new File(out_dir, "index.html")))) {
      sumPW.println("Organized by Slot<table border='1'>");
      for (Slot slot : roster) {
        File slotFile = new File(out_dir, "slot" + slot.getID() + ".html");
        try (PrintWriter slotPW = new PrintWriter(new FileWriter(slotFile))) {
          sumPW.println("<tr><td><a href='file:/" + slotFile.getAbsolutePath() + "'>" + slot.getName() + "</td>");
          slotPW.println("<table border='1'><tr><th>" + slot.getName() + "</th><th>Name</th><th>Career</th><th>nCareer</th><th>wCareer</th><th>Year</th><th>Age</th><th>PA</th><th>Team</th><th>Season</th><th>nSeason</th><th>Weight</th><th>Replace</th></tr>");
    //      System.out.println(slot.getName() + "\tName\tCareer\tnCareer\twCareer\tYear\tAge\tPA\tTeam\tSeason\tnSeason\tWeight\tReplace");
          for (Season S : slot.getSeasons()) {
            sumPW.format("<td title='%.2f'>%c&nbsp;%s</td>", S._warWeight, S._career._first.charAt(0), S._career._last);
            slotPW.format("<tr><td>%s</td><td>%s %s</td><td>%.1f</td><td>%.1f</td><td>%.1f</td><td>%d</td><td>%d</td><td>%d</td><td>%s</td><td>%.1f</td><td>%.1f</td><td>%.2f</td><td>%.2f</td></tr>", S._app.primary().pos().getName(), S._career._first, S._career._last, S._career._war, S._career._warNorm, S._career._warWeight, S._year, S._age, S._countStat, S._team, S._war, S._warNorm, S._warWeight, S._warReplace);
          }
          sumPW.println("</tr>");
          slotPW.println("</table>");
          slotPW.flush();
        }
  //      System.out.println();
      }
      sumPW.println("</table>Organized by Team<table border='1'><tr><th>Team</th>");
      for (Slot slot : roster) {
        sumPW.print("<th>" + slot.getName() + "</th>");
      }
      sumPW.println("</tr>");

      Collections.sort(draftPool, _byWarReplace);
      Roster[] rosters = new Roster[ROSTERS];
      for (int i = 0; i != rosters.length; ++i) {
        rosters[i] = new Roster(roster, 1);
      }
      for (Season S : draftPool) {
        for (Roster R : rosters) {
          Slot slot = R.get(S._slotID);
          if (slot.available()) { slot.addPlayer(S); R._total += S._warReplace; break; }
        }
        Arrays.sort(rosters, _byRosterTotal);
      }
      int i = 0;
      for (Roster R : rosters) {
        File rosterFile = new File(out_dir, "roster" + ++i + ".html");
        try (PrintWriter rosterPW = new PrintWriter(new FileWriter(rosterFile))) {
          double nSeason = 0, tReplace = 0;
          sumPW.println("<tr><td><a href='file:/" + rosterFile.getAbsolutePath() + "'>Roster&nbsp;" + i + "</a></td>");
          rosterPW.println("<table border='1'><tr><th>Slot</th><th>Pos</th><th>Name</th><th>Career</th><th>nCareer</th><th>wCareer</th><th>Year</th><th>Age</th><th>PA</th><th>Team</th><th>Season</th><th>nSeason</th><th>wSeason</th><th>Weight</th><th>Replace</th></tr>");
          for (Slot slot : R) {
            for (Season S : slot.getSeasons()) {
              nSeason += S._warNorm * slot.getWeight();
              tReplace += S._warReplace;
              sumPW.format("<td title='%.2f'>%c&nbsp;%s</td>", S._warNorm * slot.getWeight(), S._career._first.charAt(0), S._career._last);
              rosterPW.format("<tr><td>%s</td><td>%s</td><td>%s&nbsp;%s</td><td>%.1f</td><td>%.1f</td><td>%.1f</td><td>%d</td><td>%d</td><td>%d</td><td>%s</td><td>%.1f</td><td>%.1f</td><td>%.2f</td><td>%.2f</td><td>%.2f</td></tr>", slot.getName(), S._app.primary().pos().getName(), S._career._first, S._career._last, S._career._war, S._career._warNorm, S._career._warWeight, S._year, S._age, S._countStat, S._team, S._war, S._warNorm, S._warNorm * slot.getWeight(), S._warWeight, S._warReplace);
            }
          }
          rosterPW.println("</table>");
          sumPW.format("<td>%.2f</td><td>%.2f</td></tr>", nSeason, tReplace);
          rosterPW.format("Combined Season : %.2f<br/>Replacement Points : %.2f", nSeason, tReplace);
          rosterPW.flush();
        }
        //System.out.format("\t\t\t\t\t\t\t\t\t\t\t\t%.2f\t\t%.2f\n\n", nSeason, tReplace);
      }
      sumPW.println("</table>");
      sumPW.flush();
    }
    System.out.println("Finished");
  }
}
