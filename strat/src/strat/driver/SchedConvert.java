package strat.driver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class SchedConvert {
  public static final String filename = "C:/build/strat/strat16-backup-sched.txt";

  TreeMap<String, ArrayList<String>> teams = new TreeMap<>();
  
  public void input(String tm, char bench, char minor) {
    ArrayList<String> sched = teams.get(tm);
    if (sched == null) { teams.put(tm, sched = new ArrayList<>()); }
    sched.add("" + bench + minor);
  }
  
  public SchedConvert() throws Throwable {
    try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
      String line = null;
      while ((line = br.readLine()) != null) {
        if (line.isEmpty()) { continue; }
        String[] vals = line.split("\t");
        if (vals.length != 24) { System.err.println("Warning : wrong # of columns : " + line); continue; }
        for (int i = 0; i != 8; ++i) {
          String vis = vals[i*3];
          String home = vals[i*3+1];
          String code = vals[i*3+2];
          boolean isHomeCummings = code.charAt(2) == 'A';
          input(vis, code.charAt(0), isHomeCummings ? 'F' : 'C');
          input(home, code.charAt(1), isHomeCummings ? 'C' : 'F');
        }
      }
    }
    for (Map.Entry<String, ArrayList<String>> entry : teams.entrySet()) {
      System.out.print(entry.getKey());
      for (String s : entry.getValue()) { System.out.format("\t%s", s); }
      System.out.println();
    }
  }
  
  public static void main(String[] args) throws Throwable {
    new SchedConvert();
  }
}
