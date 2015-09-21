package app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class BatOrder {
  public static Comparator<Integer> SORT = new Comparator<Integer>() {
    @Override public int compare(Integer arg0, Integer arg1) { return arg1 - arg0; }
  };
  
  public static void main(String[] args) throws Exception {
    TreeMap<String, ArrayList<Integer>> players = new TreeMap<>();
    for (String line : Files.readAllLines(Paths.get("C:/build/mlbstats/batorder1978.txt"), StandardCharsets.UTF_8)) {
      String[] cols = line.split("\t");
      if (cols.length != 2) { continue; }
      ArrayList<Integer> array = players.get(cols[0]);
      if (array == null) { players.put(cols[0], array = new ArrayList<>()); }
      array.add(Integer.parseInt(cols[1]));
    }
    for (Map.Entry<String, ArrayList<Integer>> entry : players.entrySet()) {
      ArrayList<Integer> array = entry.getValue();
      Collections.sort(array, SORT);
      int sum = 0, fac = 0;
      for (int i = 0; i != array.size(); ++i) {
        sum += array.get(i);
        fac += array.get(i) * (i+1);
      }
      System.out.format("%s\t%.2f\n", entry.getKey(), fac / (double)sum);
    }
  }
}
