package topactors.server;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import topactors.shared.Gender;

public class ActorParser implements RequestHandler {
  private static final Pattern FIND_SECTION = Pattern.compile("<h5><a name=\"(.*?)\">(.*?)</a></h5>");
  private static final Pattern FIND_VOTES = Pattern.compile("<div>(.*?)</div><a href=\"/title/(.*?)/\">(.*?)</a> \\((.*?)\\).*?(\\(.*?\\))?<br/>");
  private static final Pattern FIND_RATING = Pattern.compile("<li>\\((.*?)\\) - <a href=\"/title/(.*?)/\">(.*?)</a> \\((.*?)\\).*?(\\(.*?\\))?\\s*<", Pattern.DOTALL);
  private static final Pattern FIND_ROLE = Pattern.compile("<a .*?href=\"/title/(.*?)/\".*?>(.*?)</a>(.*?)</li>"); 
  private static final Pattern FIND_ROLE_ID = Pattern.compile(" href=\"/character/(.*?)/\">(.*?)</a>"); 
  private static final Pattern FIND_ROLE_NOID = Pattern.compile("\\.\\.\\.\\. (.*?)     "); 
  
  private static final HashSet<String> VALID_SECTIONS = new HashSet<String>(Arrays.asList("actor", "actress", "self", "actor_main", "actress_main", "self_main"));
  
 // private static final String IMDB_URL = "http://www.imdb.com";
  //private static final String TITLE_URL = IMDB_URL + "/title/";
  
  private static class Roles {
    public Roles(Actor A) { _actor = A; }
    public Actor _actor = null;
    public ArrayList<Role> _list = new ArrayList<Role>();
    public HashMap<String, Role> _map = new HashMap<String, Role>();
    
    public Role fetch(String movie_id, String movie_name) {
      Role R = _map.get(movie_id);
      if (R == null) {
        if (movie_name.startsWith("\"") && movie_name.contains("(#")) { return null; } // TV episode
        Movie M = new Movie(movie_id, movie_name);
        _map.put(movie_id, R = new Role());
        R._actor = _actor;
        R._movie = M;
        _list.add(R);
      }
      return R;
    }
  }
  
  private static void setYear(Movie M, String year) {
    int endYear = year.indexOf('/');
    if (endYear != -1) { year = year.substring(0, endYear); }
    try {
      M._year = Integer.parseInt(year);
    } catch (NumberFormatException e) { System.err.println("Movie [" + M._name + "] has unidentifiable year : " + year); }
  }
  
  private static void setGender(Actor A, String section) {
    if (A._gender != Gender.Unknown) { return; }
    if (section.contains("actress")) { A._gender = Gender.Female; }
    if (section.contains("actor")) { A._gender = Gender.Male; }
  }

  private static void parseVotes(StringBuilder SB, Roles roles) {
    Matcher mSection = FIND_SECTION.matcher(SB.toString());
    int end = SB.indexOf("<div id=\"footer\" class=\"ft\">");
    while (mSection.find()) {
      String section_name = mSection.group(1);
      if (!VALID_SECTIONS.contains(section_name)) { continue; }
//System.err.println("[V]Section = " + section_name);
      setGender(roles._actor, section_name);
      int eSection = SB.indexOf("<div class=\"filmo\">", mSection.start());
      if (eSection == -1) { eSection = end; }
      Matcher mMovie = FIND_VOTES.matcher(SB.substring(mSection.start(), eSection));
      while (mMovie.find()) {
        String movie_id = mMovie.group(2);
        Role R = roles.fetch(movie_id, Normalize.unescape(mMovie.group(3)));
        if (R == null) { continue; }
        if (section_name.startsWith("self") && "(TV)".equals(mMovie.group(5))) { continue; } // skip TV shows as self
        if ("(VG)".equals(mMovie.group(5))) { continue; } // skip all video games
//System.err.print("[V]Movie id = " + movie_id);
//System.err.print(" G[" + mMovie.group(5) + "] ");
//String field = mMovie.group(0).replaceAll("\n", "|");
//if (field.length() > 100) { field = field.substring(0, 97) + "..."; }
//System.err.print(" [[" + field + "]] ");
        // R._role_type = section_name;
        R._movie._votes = Score.parseVotes(mMovie.group(1));
        setYear(R._movie, mMovie.group(4));
//System.err.println(" : " + R._movie._name);
      }
    }
  }

  private static void parseRatings(StringBuilder SB, Roles roles) {
    Matcher mSection = FIND_SECTION.matcher(SB.toString());
    int end = SB.indexOf("<div id=\"footer\" class=\"ft\">");
//int count = 0;
    while (mSection.find()) {
      String section_name = mSection.group(1);
      if (!VALID_SECTIONS.contains(section_name)) { continue; }
      setGender(roles._actor, section_name);
      int eSection = SB.indexOf("<div class=\"filmo\">", mSection.start());
      if (eSection == -1) { eSection = end; }
      Matcher mMovie = FIND_RATING.matcher(SB.substring(mSection.start(), eSection));
      while (mMovie.find()) {
        String movie_id = mMovie.group(2);
        Role R = roles.fetch(movie_id, Normalize.unescape(mMovie.group(3)));
        if (R == null) { continue; }
        if (section_name.startsWith("self") && "(TV)".equals(mMovie.group(5))) { continue; } // skip TV shows as self
        if ("(VG)".equals(mMovie.group(5))) { continue; } // skip all video games
//++count;
//System.err.print(" G[" + mMovie.group(5) + "] ");
//System.err.println("[S] [[" + mMovie.group(0) + "]] ");
        // R._role_type = section_name;
        R._movie._rating = Double.parseDouble(mMovie.group(1));
        setYear(R._movie, mMovie.group(4));
      }
    }
//System.out.println("COUNT = " + count);
  }

  private static void parseRoles(StringBuilder SB, Roles roles) {
    Matcher mSection = FIND_SECTION.matcher(SB.toString());
    int end = SB.indexOf("<div id=\"footer\" class=\"ft\">");
    while (mSection.find()) {
      String section_name = mSection.group(1);
      if (!VALID_SECTIONS.contains(section_name)) { continue; }
//System.err.println("[R]Section = " + section_name);
      setGender(roles._actor, section_name);
      int eSection = SB.indexOf("<div class=\"filmo\">", mSection.start());
      if (eSection == -1) { eSection = end; }
      Matcher mMovie = FIND_ROLE.matcher(SB.substring(mSection.start(), eSection));
      while (mMovie.find()) {
        String movie_id = mMovie.group(1);
        Role R = roles.fetch(movie_id, Normalize.unescape(mMovie.group(2)));
        if (R == null) { continue; }
//System.err.print("[R]Movie id = " + movie_id);
        if (mMovie.group(3).indexOf("(voice") != -1) { R._voice = true; }
        R._type = section_name.startsWith("self") ? Role.Type.SELF : Role.Type.ACTOR;
//System.err.print(" [[" + mMovie.group(3) + "]] ");
        Matcher mRole = FIND_ROLE_ID.matcher(mMovie.group(3));
        if (mRole.find()) {
          R._id = mRole.group(1);
          R._name = Normalize.unescape(mRole.group(2));
//System.err.print(" : Role name = " + R._name + " [" + R._id + "]");
        } else {
          mRole = FIND_ROLE_NOID.matcher(mMovie.group(3));
          if (mRole.find()) {
            R._name = Normalize.unescape(mRole.group(1));
//System.err.print(" : Role name = " + R._name);
          }
        }
        if (R._name != null) { R._name = R._name.replaceAll("\\s*\\(as .*?\\)\\s*", " "); }
//System.err.println(" : " + R._movie._name);
      }
    }
  }
  
  public static void parse(StringBuilder sbVote, StringBuilder sbRate, StringBuilder sbChar, Roles roles) {
    parseVotes(sbVote, roles);
    parseRatings(sbRate, roles);
    parseRoles(sbChar, roles);
  }

  @Override
  public Data request(String actorID, String actorName) {
    Actor A = new Actor(actorID, actorName);
    Data D = new Data();
    D.add(A);
    try {
      StringBuilder sbVote = _cache.fetch("actor_vote", actorID, WebPage.ACTOR_BASE + actorID + "/filmovote");
      StringBuilder sbRate = _cache.fetch("actor_rate", actorID, WebPage.ACTOR_BASE + actorID + "/filmorate");
      StringBuilder sbChar = _cache.fetch("actor_type", actorID, WebPage.ACTOR_BASE + actorID + "/filmotype");
      Roles roles = new Roles(A);
      parse(sbVote, sbRate, sbChar, roles);
      for (Role R : roles._list) { D.add(R); D.add(R._movie); }
      A._processed = new Date();
      return D;
    } catch (IOException e) { throw new RuntimeException("Unable to parse movie " + actorName, e); }
  }
  
  public ActorParser(Cache cache) {
    _cache = cache;
  }

  private Cache _cache = null;

  private static final String ACTOR_ID = "nm0202966";
  private static final String ACTOR_NAME = "Keith David";
//  private static final String ACTOR_ID = "nm0000704";
//  private static final String ACTOR_NAME = "Elijah Wood";

  public static void main(String[] args) throws SQLException {
    try (MyDatabase db = new MyDatabase()) {
      Repository cache = new Repository(db);
      ActorParser P = new ActorParser(cache);
      Data D = P.request(ACTOR_ID, ACTOR_NAME);
      Model model = new Model();
      D = model.update(D);
      Actor main = model._actors.get(ACTOR_ID);
      ArrayList<Role> roles = new ArrayList<Role>(model._roles.get(ACTOR_ID));
      Collections.sort(roles);
      for (Role R : roles) {
        System.out.format("%s\t[%s]\t%s\t[%s]\t#%d\t%s\t%.2f\t%.2f\n", R._movie._name, R._movie._id, R._name, R._id, R._series, R._voice ? "V":"", R._movie._score, R._sat_series);
      }
      System.out.format("%s[%s:%s] : %.2f\n", main._name, main._gender, main._id, main._sat);
    }
  }
}
