package topactors.server;

import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Top250Parser implements InitHandler {
  private static final Pattern FIND_DATA = Pattern.compile("<td[^>]*>\\s*<font[^>]+>(.*?)</font>");
  private static final Pattern FIND_TOP_INFO = Pattern.compile("<b>[0-9]+\\.</b>.*<a href=\"/title/(.*?)/\">(.*?)</a>\\s+\\(([0-9]+)/?I*\\)");
  
  private static Data parse(StringBuilder sbPage) {
    Data D = new Data();
    int start = sbPage.indexOf("<div id=\"main\">");
    start = sbPage.indexOf("<table ", start);
    start = sbPage.indexOf("</tr>", start);
    int end = sbPage.indexOf("</table>", start);
    while (start < end) {
      int mStart = sbPage.indexOf("<tr", start);
      if (mStart == -1) { break; }
      int mEnd = sbPage.indexOf("</tr>", mStart);
      if (mEnd == -1) { break; }
      Matcher M2 = FIND_DATA.matcher(sbPage.substring(mStart, mEnd));
      if (!M2.find()) { start = mEnd; continue; } // rank (skip)
      if (!M2.find()) { throw new RuntimeException("Missing score"); }
      double score = Double.parseDouble(M2.group(1));
      if (!M2.find()) { throw new RuntimeException("Missing name"); }
      Matcher M = FIND_TOP_INFO.matcher(sbPage.substring(mStart, mEnd));
      if (!M.find()) { throw new RuntimeException("Missing name components"); }
      Movie movie = new Movie(M.group(1), Normalize.unescape(M.group(2)));
      movie._year = Integer.parseInt(M.group(3));
      movie._score = score;
      D.add(movie);
      if (!M2.find()) { throw new RuntimeException("Missing votes"); }
      movie._votes = Score.parseVotes(M2.group(1));
      start = mEnd;
    }
    return D;
  }
  
  @Override
  public Data init() {
    try {
      return parse(_cache.fetch("index", "top250", WebPage.TOP250));
    } catch (IOException e) {
      throw new RuntimeException("Trouble fetching Top 250", e);
    }
  }
  
  public Top250Parser(Cache cache) {
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
      SB = Fetcher.getPage(WebPage.TOP250);
    }
    Data D = Top250Parser.parse(SB);
    if (D._movies.size() != 250) { System.err.format("Only found %d out of 250 movies\n", D._movies.size()); }
  }

}
