package crawl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import util.MyDatabase;

public class LoadSchema {
  public static void load(File file) throws Exception {
    try (BufferedReader br = new BufferedReader(new FileReader(file)); MyDatabase db = new MyDatabase()) {
      StringBuilder sb = new StringBuilder();
      String line = null;
      while ((line = br.readLine()) != null) {
        if (line.startsWith("--")) { continue; }
        sb.append(line);
        if (line.endsWith(";")) {
          db.prepare(sb.toString()).execute();
          sb.setLength(0);
        }
      }
    }
  }
}
