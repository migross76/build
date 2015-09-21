package topactors.server;

import java.text.DecimalFormat;
import java.text.ParseException;

public class Score {
  private static final DecimalFormat VOTE_FORMAT = new DecimalFormat("###,###,##0");

  public static int parseVotes(String votes) {
    try {
      return Score.VOTE_FORMAT.parse(votes).intValue();
    } catch (ParseException e) { e.printStackTrace(); return -1; }
  }

  
  private static final double MIN_VOTES = 3000;
  private static final double MEAN_SCORE = 6.9;
  public static double computeWeighted(double score, int votes) {
    return (votes / (votes+MIN_VOTES)) * score + (MIN_VOTES / (votes+MIN_VOTES)) * MEAN_SCORE;
  }
 
}
