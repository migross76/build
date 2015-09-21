package topactors.server;

public abstract class WebPage {
  private WebPage() { }
  
  private static final String IMDB = "http://www.imdb.com";

  public static final String TOP250 = IMDB + "/chart/top";
  public static final String MOVIE = IMDB + "/title/";
  public static final String OSCAR = IMDB + "/event/ev0000003/";
  public static final String ACTOR_BASE = IMDB + "/name/";
}
