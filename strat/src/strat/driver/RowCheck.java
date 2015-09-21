package strat.driver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class RowCheck implements Comparable<RowCheck> {
  private static final Path CARD_DIR = Paths.get("C:/build/strat/pit");

  public RowCheck(String line) { _line = line; }
  
  public final String _line;
  public int _count = 0;
  public HashSet<String> _ids = new HashSet<>();

  @Override public int compareTo(RowCheck arg0) {
//    int cmp = 0;
//    if ((cmp = _count - arg0._count) != 0) { return cmp; }
//    if ((cmp = _ids.size() - arg0._ids.size()) != 0) { return cmp; }
    return _line.compareTo(arg0._line);
  }
  
  private static String getName(Path cardFile) {
    return cardFile.getFileName().toString().split("\\.")[0];
  }
  
  
  public static void main(String[] args) throws Exception {
    
    HashMap<String, RowCheck> map = new HashMap<>();
    ArrayList<RowCheck> rows = new ArrayList<>();
    for (Path cardFile : Files.newDirectoryStream(CARD_DIR, "*.txt")) {
      String id = getName(cardFile);
      for (String line : Files.readAllLines(cardFile, StandardCharsets.UTF_8)) {
        String[] tokens = line.split(" ");
        if (tokens.length == 0 || tokens[0].length() == 0 || (tokens[0].charAt(0) >= 'A' && tokens[0].charAt(0) <= 'Z')) { continue; }
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
          try { Integer.parseInt(token); } catch (NumberFormatException e) { sb.append(token).append(" "); }
        }
        String key = sb.toString();
        RowCheck row = map.get(key);
        if (row == null) { map.put(key, row = new RowCheck(key)); rows.add(row); }
        ++row._count;
        row._ids.add(id);
      }
    }
    Collections.sort(rows);
    for (RowCheck row : rows) {
      System.out.format("%s\t%d\t", row._line, row._count);
      for (String id : row._ids) { System.out.format("%s ", id); }
      System.out.println();
    }
  }
}

