package topactors.server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Fetcher {
  private static final int MAX_ATTEMPTS = 5;

  public static StringBuilder getPage(String url) throws IOException {
    Exception caused = null;
    for (int i = 0; i != MAX_ATTEMPTS; ++i) {
      caused = null;
      try {
        HttpURLConnection HUC = (HttpURLConnection)new URL(url).openConnection();
        HUC.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.1.3) Gecko/20090824 Firefox/3.5.3");
        HUC.connect();
        if (HUC.getResponseCode() != HttpURLConnection.HTTP_OK) {
          throw new Exception(HUC.getResponseCode() + " : " + HUC.getResponseMessage());
        }
        Reader in = new InputStreamReader(HUC.getInputStream());
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int read = 0;
        while ((read = in.read(buf)) != -1) { sb.append(buf, 0, read); }
        return sb;
      } catch (Exception e) {
        caused = e;
      }
    }
    System.err.println(caused);
    throw new IOException("Unable to get URL : " + url);
  }

}
