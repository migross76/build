package topactors.server;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MovieParser implements RequestHandler {
  private static final Pattern FIND_ACTOR = Pattern.compile("<td class=\"name\">\\s*<a\\s+href=\"/name/(.*?)/\">(.*)</a>");
  private static final Pattern FIND_CHARACTER = Pattern.compile("<td class=\"character\">\\s*<div>\\s*(.*?)\\s*</div>", Pattern.MULTILINE | Pattern.DOTALL);
  private static final Pattern FIND_ROLE_ID = Pattern.compile("<a href=\"/character/(.*?)/\">");
  private static final Pattern FIND_STAR = Pattern.compile("<a .*?href=\"/name/(.*?)/\"\\s*>(.*?)</a>");
  private static final Pattern FIND_SCORE = Pattern.compile("<span class=\"rating-rating\">([0-9.]+)<span>/10</span></span>");
  private static final Pattern FIND_VOTES = Pattern.compile(">([0-9,]+) votes</a>");
  
 // private static final String IMDB_URL = "http://www.imdb.com";
  //private static final String TITLE_URL = IMDB_URL + "/title/";

  private static HashMap<String, String> getStars(StringBuilder sbPage) {
    HashMap<String, String> stars = new HashMap<String, String>();
    int start = sbPage.indexOf("<h4 class=\"inline\">Stars:</h4>");
    int end = sbPage.indexOf("</div>", start);
    Matcher M = FIND_STAR.matcher(sbPage.substring(start, end));
    while (M.find()) { stars.put(M.group(1), Normalize.unescape(M.group(2))); }
    return stars;
  }
  
  private static void updateScore(Movie M, StringBuilder sbPage) {
    Matcher mScore = FIND_SCORE.matcher(sbPage);
    if (!mScore.find()) { System.err.println(M._name + " : could not find score"); return; }
    double rating = Double.parseDouble(mScore.group(1));
    if (rating < 1) { System.err.println(M._name + " : could not find valid score"); return; }
    Matcher mVotes = FIND_VOTES.matcher(sbPage);
    if (!mVotes.find()) { System.err.println(M._name + " : could not find votes"); return; }
    int votes = Score.parseVotes(mVotes.group(1));
    if (votes == -1) { System.err.println(M._name + " : could not find valid votes"); return; }
    M._votes = votes;
    M._rating = rating;
    //M._score = Score.computeScore(rating, votes);
    //M._sat = M._score - 7.0;
    // System.out.format("  Score v(%d) r(%.1f) w(%.2f) s(%.2f)\n", M._votes, M._rscore, M._wscore, M._sat);
  }
  
  private static Data parse(Movie M, StringBuilder sbPage) {
    HashMap<String, String> stars = getStars(sbPage);
    if (stars.size() != 3) { System.err.println(M._name + " : missing a star"); }
    updateScore(M, sbPage);
    int start = sbPage.indexOf("<table class=\"cast_list\">");
    int end = sbPage.indexOf("</table>", start);
    int count = 0;
    Data D = new Data();
    D.add(M);
    while (start < end) {
      int mStart = sbPage.indexOf("<tr", start);
      if (mStart == -1) { break; }
      int mEnd = sbPage.indexOf("</tr>", mStart);
      if (mEnd == -1) { break; }
      Matcher M2 = FIND_ACTOR.matcher(sbPage.substring(mStart, mEnd));
      if (M2.find()) {
        Actor A = new Actor(M2.group(1), Normalize.unescape(M2.group(2)));
        D.add(A);
        ++count;
        Matcher M3 = FIND_CHARACTER.matcher(sbPage.substring(mStart, mEnd));
        Role R = null;
        if (M3.find()) {
          String role = M3.group(1);
          String role_id = null;
          Matcher M4 = FIND_ROLE_ID.matcher(role);
          if (M4.find()) { role_id = M4.group(1); }
          role = Normalize.unescape(role.replaceAll("<[^>]*>", ""));
          role = role.replaceAll("\\s*\\(as .*?\\)\\s*", " ");
          if (role_id == null) { role_id = role; }
          R = new Role(role_id, role, M, A);
          if (role.contains("(voice)")) { R._voice = true; }
        } else {
          System.err.format("%s : unable to find role for %s\n", M._name, A._name);
          R = new Role(null, "<unknown>", M, A);
        }
        R._level = stars.containsKey(A._id) ? Role.Level.STAR : Role.Level.APPEAR;
        D.add(R);
        stars.remove(A._id);
      }
      start = mEnd;
    }
    if (count != 15) { System.err.format("%s : Only found %d actors\n", M._name, count); }
    for (Map.Entry<String, String> star : stars.entrySet()) {
      System.err.format("%s : Did not find star %s in cast list\n", M._name, star.getValue());
      Actor A = new Actor(star.getKey(), star.getValue());
      D.add(A);
      Role R = new Role(null, "<unknown>", M, A);
      R._level = Role.Level.STAR;
      D.add(R);
    }
    M._processed = new Date();
    return D;
  }
  
  @Override
  public Data request(String movieID, String movieName) {
    Movie M = new Movie(movieID, movieName);
    try {
      return parse(M, _cache.fetch("movie_main", movieID, WebPage.MOVIE + movieID));
    } catch (IOException e) { throw new RuntimeException("Unable to parse movie " + movieName, e); }
  }
  
  public MovieParser(Cache cache) {
    _cache = cache;
  }

  private Cache _cache = null;

  public static void main(String[] args) throws IOException {
    StringBuilder SB = null;
    if (args.length == 1) {
      SB = new StringBuilder();
      try (FileReader FR = new FileReader(args[0])) {
        char[] buf = new char[1024];
        int size = 0;
        while ((size = FR.read(buf)) != -1) { SB.append(buf, 0, size); }
      }
    } else {
      System.err.println("Can only run against a file");
      // StringBuilder sbPage = getPage(IMDB_URL + "/title/" + movie._url
    }
    Movie M = new Movie(null, "Shawshank Redemption");
    Data D = MovieParser.parse(M, SB);
    ArrayList<Role> sorted = new ArrayList<Role>(D._roles);
    System.out.println("Actors fetched : " + sorted.size());
    for (Role R : sorted) {
      System.out.println(R._actor + " : " + R);
    }
  }

}
