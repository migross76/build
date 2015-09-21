package topactors.server;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import topactors.shared.Gender;
import topactors.shared.Oscar;

/*

<div id="main">

<h2>Best Motion Picture of the Year</h2>
<h2>Best Performance by an Actor in a Leading Role</h2>
<h2>Best Performance by an Actress in a Leading Role</h2>
<h2>Best Performance by an Actor in a Supporting Role</h2>
<h2>Best Performance by an Actress in a Supporting Role</h2>
<blockquote>
<h3>WINNER</h3>
<div class='alt'>

<div style="float:left;height:32px;width:25px;"><a href="/name/nm0000142/"><img src="http://ia.media-imdb.com/images/M/MV5BMTk4NDcyNTEzOF5BMl5BanBnXkFtZTYwMjMzNDQz._V1._SX21_SY32_.jpg" border='0' /></a></div>
<strong>
<a  href="/title/tt0405159/">Million Dollar Baby</a>: </strong><a  href="/name/nm0000142/">Clint Eastwood</a>, <a  href="/name/nm0748665/">Albert S. Ruddy</a>, <a  href="/name/nm0742347/">Tom Rosenberg</a></div><h3>NOMINEES</h3>
<div class='alt'>

<div style="float:left;height:32px;width:25px;"><a href="/name/nm0000520/"><img src="http://ia.media-imdb.com/images/M/MV5BMTU2MjMzODY3Ml5BMl5BanBnXkFtZTYwNjE0OTMz._V1._SX21_SY32_.jpg" border='0' /></a></div>
<strong>
<a  href="/title/tt0338751/">The Aviator</a>: </strong><a  href="/name/nm0000520/">Michael Mann</a>, <a  href="/name/nm0454752/">Graham King</a></div><hr style="margin:2px;padding:0px"><div class='alt2'>


<div style="float:left;height:32px;width:25px;"><a href="/name/nm0004937/"><img src="http://ia.media-imdb.com/images/M/MV5BMjAyMDMwNzkxMV5BMl5BanBnXkFtZTcwNzg4Nzg4Mg@@._V1._SX22_SY31_.jpg" border='0' /></a></div>
<strong>
<a  href="/title/tt0350258/">Ray</a>: </strong><a  href="/name/nm0004937/">Jamie Foxx</a></div><h3>NOMINEES</h3>
<div class='alt'>

<div style="float:left;height:32px;width:25px;"><a href="/name/nm0000138/"><img src="http://ia.media-imdb.com/images/M/MV5BMTM3ODY5MTA0MV5BMl5BanBnXkFtZTcwMTc0OTA1Mg@@._V1._SX22_SY30_.jpg" border='0' /></a></div>
<strong>
<a  href="/title/tt0338751/">The Aviator</a>: </strong><a  href="/name/nm0000138/">Leonardo DiCaprio</a></div><hr style="margin:2px;padding:0px"><div class='alt2'>


 */
public class OscarParser implements InitHandler {
  private static final String[] MOVIE_AWARDS = {
    "Best Motion Picture of the Year",
    "Best Picture",
    "Best Picture, Production",
  };
  
  private static final String[] MALE_ROLE_AWARDS = {
    "Best Performance by an Actor in a Leading Role",
    "Best Performance by an Actor in a Supporting Role",
    "Best Actor in a Leading Role",
    "Best Actor in a Supporting Role",
  };
  
  private static final String[] FEMALE_ROLE_AWARDS = {
    "Best Performance by an Actress in a Leading Role",
    "Best Performance by an Actress in a Supporting Role",
    "Best Actress in a Leading Role",
    "Best Actress in a Supporting Role",
  };
  
  // <a  href="/title/tt0405159/">Million Dollar Baby</a>: </strong><a  href="/name/nm0000142/">Clint Eastwood</a>
  // <a  href="/title/tt0338751/">The Aviator</a>: </strong><a  href="/name/nm0000138/">Leonardo DiCaprio</a>
  
  private static final Pattern FIND_MOVIE = Pattern.compile("<a  href=\"/title/(.*?)/\">(.*?)</a>: ");
  private static final Pattern FIND_ROLE = Pattern.compile("<a  href=\"/title/(.*?)/\">(.*?)</a>: </strong><a  href=\"/name/(.*?)/\">(.*?)</a>");
  
 // private static final String IMDB_URL = "http://www.imdb.com";
  //private static final String TITLE_URL = IMDB_URL + "/title/";
  
  private static boolean parseAward(StringBuilder sbPage, Data D, String award, Gender gender) {
    int start = sbPage.indexOf("<div id=\"main\">");
    // System.out.println("\n" + award);
    int cat_start = sbPage.indexOf("<h2>" + award + "</h2>", start);
    if (cat_start == -1) { return false; }
    int cat_end = sbPage.indexOf("</blockquote>", cat_start);
    if (cat_end == -1) { System.err.println("Could not find end to " + award); return false; }
    // int win_start = sbPage.indexOf("WINNER", cat_start);
    int nom_start = sbPage.indexOf("NOMINEE", cat_start);
    if (nom_start == -1) { System.err.println("Could not find nom section to " + award); return false; }
    Matcher M = (gender != null ? FIND_ROLE : FIND_MOVIE).matcher(sbPage.substring(cat_start, cat_end));
    while (M.find()) {
      Movie movie = new Movie(M.group(1), Normalize.unescape(M.group(2)));
      D.add(movie);
      boolean nominee = M.start() + cat_start > nom_start;
      //System.err.println((M.start() + cat_start) + " : " + nom_start);
      if (gender == null) {
        movie._oscar = nominee ? Oscar.NOMINATED : Oscar.WINNER;
      } else {
        Actor A = new Actor(M.group(3), Normalize.unescape(M.group(4)));
        A._gender = gender;
        D.add(A);
        Role R = new Role(null, null, movie, A);
        R._oscar = nominee ? Oscar.NOMINATED : Oscar.WINNER;
        D.add(R);
      }
    }
    return true;
  }
  
  public static void parse(StringBuilder sbPage, Data D) {
    for (String award : MOVIE_AWARDS) { parseAward(sbPage, D, award, null); }
    for (String award : FEMALE_ROLE_AWARDS) { parseAward(sbPage, D, award, Gender.Female); }
    for (String award : MALE_ROLE_AWARDS) { parseAward(sbPage, D, award, Gender.Male); }
  }
  
  @Override
  public Data init() {
    Data D = new Data();
    try {
      for (int year = 1929; year != 2012; ++year) {
        String suffix = year == 1930 ? "-2" : "";
        parse(_cache.fetch("oscar", "" + year, "http://www.imdb.com/event/ev0000003/" + year + suffix), D);
      }
    } catch (IOException e) {
      throw new RuntimeException("Trouble fetching Oscars", e);
    }
    return D;
  }

  public OscarParser(Cache cache) {
    _cache = cache;
  }
    
  private Cache _cache = null;

/*
  // C:\build\imdbparsers\sample\oscars2005.htm
  public static void main(String[] args) throws IOException {
    Cache cache = new Cache("C:/build/imdbparsers/cache");
    int year = 2011;
    OscarParser OP = new OscarParser();
    while (year > 1928) {
      System.out.print(year + "\t");
      String suffix = year == 1930 ? "-2" : "";
      OP.parse(cache.fetch("oscars" + year + ".htm", "http://www.imdb.com/event/ev0000003/" + year + suffix));
      --year;
      System.out.println();
    }
  }
*/

}
