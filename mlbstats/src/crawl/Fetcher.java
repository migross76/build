package crawl;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class Fetcher {
  public static final int MAX_ATTEMPTS = 5;
  public static final int DELAY_TIME = 30000; // minimum 30 seconds between fetches

  private static StringBuilder grabData(Reader R) throws IOException {
    StringBuilder sb = new StringBuilder();
    char[] buf = new char[1024];
    int read = 0;
    while ((read = R.read(buf)) != -1) { sb.append(buf, 0, read); }
    return sb;
  }
  
  public StringBuilder getWebPage(String url) throws IOException {
    Exception caused = null;
    long wait = _nextFetch - Calendar.getInstance().getTimeInMillis();
    if (wait > 0) { try { Thread.sleep(wait); } catch (InterruptedException e) { /* shouldn't happen */ } }
    for (int i = 0; i != MAX_ATTEMPTS; ++i) {
      caused = null;
      try {
        HttpURLConnection HUC = (HttpURLConnection)new URL(url).openConnection();
        HUC.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.1.3) Gecko/20090824 Firefox/3.5.3");
        HUC.connect();
        if (HUC.getResponseCode() != HttpURLConnection.HTTP_OK) {
          throw new Exception(HUC.getResponseCode() + " : " + HUC.getResponseMessage());
        }
        StringBuilder sb = grabData(new InputStreamReader(HUC.getInputStream()));
        _nextFetch = Calendar.getInstance().getTimeInMillis() + DELAY_TIME;
        return sb;
      } catch (Exception e) {
        caused = e;
      }
    }
    System.err.println(caused);
    throw new IOException("Unable to get URL : " + url);
  }

  public boolean exists(Object... args) {
    return new File(String.format(_fileFormat, args)).exists();
  }
  
  public StringBuilder getPage(Object... args) throws IOException {
    String filename = String.format(_fileFormat, args);
    File F = new File(filename);
    if (F.exists()) {
      try (FileReader fr = new FileReader(F)) { return grabData(fr); }
    }
    // else fetch page and save
    StringBuilder sb = getWebPage(String.format(_urlFormat, args));
    F.getParentFile().mkdirs();
    try (Writer out = new FileWriter(F)) { out.append(sb).flush(); }
    return sb;
  }

  public Fetcher(String urlFormat, String fileFormat) {
    _urlFormat = urlFormat;
    _fileFormat = fileFormat;
  }
  
  private String _urlFormat = null;
  private String _fileFormat = null;
  private long   _nextFetch = 0;
}
