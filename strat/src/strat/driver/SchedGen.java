package strat.driver;

import java.util.Random;

public class SchedGen {
  private static String[][] TEAMS = {
    { "ATL","CHC","CIN","LAD","PHI","PIT","SFG","STL" },
    { "BAL","BOS","CHW","CLE","DET","MIN","NYY","OAK" }
  };
  private static int[][] MATCHUPS = {
    { 0,1, 2,3, 4,5, 6,7 },
    { 0,2, 1,3, 4,6, 5,7 },
    { 0,3, 1,2, 4,7, 5,6 },
    { 0,4, 1,5, 2,6, 3,7 },
    { 0,5, 1,6, 2,7, 3,4 },
    { 0,6, 1,7, 2,4, 3,5 },
    { 0,7, 1,4, 2,5, 3,6 }
  };
  private static int[][] DAY_ONLY = {
    { 0,1,0,0,0,1,1,0 },
    { 0,1,1,0,1,1,1,1 }
  };
  private static final int G_PER_SERIES = 4;
  private static final int SERIES_AT_HOME = 3;
  
  public static int day = 1;
  public static Random rnd = new Random();
  
  public static void generateDay(int[] matchups, boolean swap) {
    boolean day_only = rnd.nextInt(7) == 0;
    for (int lg = 0; lg != TEAMS.length; ++lg) {
      String[] league = TEAMS[lg];
      for (int m = 0; m != matchups.length; m += 2) {
        int h = swap ? m+1 : m;
        int v = swap ? m : m+1;
        System.out.format("%d,%s,%s,%c\n",day,league[matchups[v]],league[matchups[h]],(day_only || DAY_ONLY[lg][h] == 1) ? 'D' : 'N');
      }
    }
    ++day;
  }
  
  public static void main(String[] args) throws Throwable {
    int[][] matchup_usage = new int[2][7];
    for (int i_s = 0, e_s = SERIES_AT_HOME * 2 * 7; i_s != e_s; ++i_s) {
      int home_away = 0;
      int matchup = 0;
      do {
        home_away = rnd.nextInt(2);
        matchup = rnd.nextInt(7);
      } while (matchup_usage[home_away][matchup] >= SERIES_AT_HOME);
      ++matchup_usage[home_away][matchup];
      for (int i = 0; i != G_PER_SERIES; ++i) {
        generateDay(MATCHUPS[matchup], home_away == 1);
      }
    }
  }
}
