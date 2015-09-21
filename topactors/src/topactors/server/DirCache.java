package topactors.server;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;

public class DirCache implements Cache {
  
  @Override public StringBuilder fetch(String type, String name, String url) throws IOException {
    File F = name == null ? null : new File(_dir, name);
    if (F != null && F.exists()) {
      StringBuilder SB = new StringBuilder();
      try (FileReader FR = new FileReader(F)) {
        char[] buf = new char[1024];
        int size = 0;
        while ((size = FR.read(buf)) != -1) { SB.append(buf, 0, size); }
      }
      return SB;
    } // else; new page
    long current = Calendar.getInstance().getTimeInMillis();
    long wait = _lastFetch + _fetchDelay - current;
    _lastFetch = current;
    try {
      if (wait > 0) { Thread.sleep(wait); }
    } catch (InterruptedException e) { e.printStackTrace(); }
    StringBuilder page = Fetcher.getPage(url);
/*
    if (F != null) {
      FileWriter FW = new FileWriter(F);
      FW.write(page.toString());
      FW.close();
    }
*/
    return page;
  }
  
  public DirCache(String directory) {
    _dir = new File(directory);
    _dir.mkdirs();
  }
  
  public DirCache(String directory, long fetchDelay) {
    this(directory);
    _fetchDelay = fetchDelay;
  }
  
  private long _lastFetch = 0;
  private long _fetchDelay = 5000;
  
  private File _dir = null;
}
