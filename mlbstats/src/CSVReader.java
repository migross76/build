import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

/* TODO: Check for errors in formatting, Refactor logic (it's kind of a mess), Quoted block should end with a comma or EOL */

public class CSVReader implements Closeable {

  public CSVReader(Reader R) {
    if (R instanceof BufferedReader) {
      _in = (BufferedReader)R;
    } else {
      _in = new BufferedReader(R);
    }
  }

  public String[] readLine() throws IOException {
    ArrayList<String> res = new ArrayList<>();
    String line = _in.readLine();
    int pos = 0; int end = line == null ? 0 : line.length();
    while (line != null && pos != end) {
      int quot = line.indexOf('"', pos);
      int comma = line.indexOf(',', pos);
      if (quot == -1 && comma == -1) {
        res.add(line.substring(pos));
        break; // finished with this CSV line
      }
      if (comma != -1 && comma < quot) {
        res.add(line.substring(pos, comma));
        pos = comma + 1;
        continue;
      }
      // else quote is first
      StringBuilder SB = new StringBuilder();
      while (true) {
        int nextquote = line.indexOf('"', quot+1);
        int escquote = line.indexOf("\"\"", quot);
        if (nextquote == -1) {
          SB.append(line.substring(quot+1)).append("\n");
          line = _in.readLine();
          pos = 0; end = line.length();
          quot = -1;
          // check error
          continue;
        }
        if (escquote != nextquote) {
          SB.append(line.substring(quot+1, nextquote));
          pos = nextquote+1;
          break;
        }
        // escaped quote
        SB.append(line.substring(quot+1, nextquote)).append('"');
        quot = escquote + 1;
      }
      if (pos != end && line.charAt(pos) == ',') { ++pos; }
      res.add(SB.toString());
    }
    return line == null ? null : res.toArray(_cast);
  }

  @Override public void close() throws IOException {
    _in.close();
  }

  private static String[] _cast = new String[0];
  private BufferedReader _in = null;

  public static void main(String[] args) throws Exception {
    try (CSVReader R = new CSVReader(new FileReader(args[0]))) {
      String[] line = null;
      while ((line = R.readLine()) != null) {
        boolean first = true;
        for (String S : line) {
          if (!first) { System.out.print("\t"); } else { first = false; }
          System.out.print(S);
        }
        System.out.println();
      }
    }
  }

}
