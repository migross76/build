package app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/*
 * Combine multiple "Top 112" (BBWAA HOF size) lists to get a consensus HOF
 */
public class Top112Rating {
  private static final String FILE = "C:/build/mlbstats/top112.txt";
  
  private static class Player implements Comparable<Player> {
    public final String _name;
    public int _ranked = 0;
    public int _rankTotal = 0;
    public final int _best;
    public int _worst = 0;
    public String _bestCat;
    
    public Player(String name, int best, String bestCat) {
      _name = name;
      _best = best;
      _bestCat = bestCat;
    }
    
    public void add(int score, String cat) {
      ++_ranked;
      _rankTotal += score;
      _worst = score;
      if (score == _best && !cat.equals(_bestCat)) { _bestCat = _bestCat + ", " + cat; }
    }
    
    public double avg_10() { 
      int total = _rankTotal - _best;
      if (_ranked == 12) {
        total -= _worst;
      } else {
        total += (11 - _ranked) * 120;
      }
      return total / 10.0;
    }

    public double avg_tot() { return (avg() + avg_rk()) / 2; }

    public double avg() { return (_rankTotal + (12 - _ranked) * 120) / 12.0; }

    public double avg_rk() { return _rankTotal / (double)_ranked; }

    @Override public int compareTo(Player arg0) {
      double dcmp = avg_tot() - arg0.avg_tot();
      if (dcmp != 0) { return dcmp > 0 ? 1 : -1; }
      dcmp = avg() - arg0.avg();
      if (dcmp != 0) { return dcmp > 0 ? 1 : -1; }
      dcmp = avg_rk() - arg0.avg_rk();
      if (dcmp != 0) { return dcmp > 0 ? 1 : -1; }
      int icmp = _ranked - arg0._ranked;
      if (icmp != 0) { return icmp > 0 ? 1 : -1; }
      icmp = _best - arg0._best;
      if (icmp != 0) { return icmp > 0 ? 1 : -1; }
      return _name.compareTo(arg0._name);
    }
  }
  
  public static void main(String[] args) throws Exception {
    HashMap<String, Player> map = new HashMap<>();
    try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
      String line = br.readLine();
      String[] cats = line.split("\t");
      int ct = 0;
      while ((line = br.readLine()) != null) {
        if (ct != 112) { ++ct; }
        String[] names = line.split("\t");
        for (int j = 0; j != names.length; ++j) {
          String name = names[j];
          if (name.trim().isEmpty()) { continue; }
          Player p = map.get(name);
          if (p == null) { map.put(name, p = new Player(name, ct, cats[j])); }
          p.add(ct, cats[j]);
        }
      }
    }
    ArrayList<Player> players = new ArrayList<>(map.values());
    Collections.sort(players);
    System.out.println("Name\tAvg Tot\tAvg\tAvg Rk\t#\tBest\tCat");
    for (Player p : players) {
      System.out.format("%s\t%.1f\t%.1f\t%.1f\t%.1f\t%d\t%d\t%s\n", p._name, p.avg_tot(), p.avg_10(), p.avg(), p.avg_rk(), p._ranked, p._best, p._bestCat);
    }
  }
}
