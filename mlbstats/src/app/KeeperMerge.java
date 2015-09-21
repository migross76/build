package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class KeeperMerge {
  private static final String KEEPER_DIR = "C:/build/mlbstats/keepers/";
  
  public static class YearInfo {
    public YearInfo(int rank, String pos, int age, String team) { _rank = rank; _pos = pos; _age = age; _team = team; }
    public int _rank;
    public String _pos;
    public int _age;
    public String _team;
  }
  
  public static class Player {
    public Player(String firstname, String lastname) { _firstname = firstname; _lastname = lastname; }
    public final String _firstname;
    public final String _lastname;
    public String _teamFirst = "???";
    public String _posFirst = "??";
    public TreeSet<String> _otherfirsts = new TreeSet<>();
    public HashMap<String, YearInfo> _infos = new HashMap<>();
  }
  
  public static void main(String[] args) throws Exception {
    File dir = new File(KEEPER_DIR);
    File[] filesList = dir.listFiles(new FileFilter() {
      @Override public boolean accept(File f) {
        return (f.isFile() && f.getName().endsWith(".tsv"));
      }
    });
    ArrayList<Player> players = new ArrayList<>();
    HashMap<String, Player> byRank = new HashMap<>();
    TreeSet<String> years = new TreeSet<>();
    
    for (int f = filesList.length - 1; f >= 0; --f) {
      File file = filesList[f];
      if (file.isDirectory()) { continue; }
      String year = file.getName().substring(0, file.getName().length() - 4);
      years.add(year);
      System.out.println("Processing " + year);
      try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        String[] headers = br.readLine().split("\t");
        String line = null;
        while ((line = br.readLine()) != null) {
          String[] vals = line.split("\t");
          if (vals.length < headers.length) { continue; }
          int age = -1;
          String name = null;
          String pos = null;
          String team = null;
          TreeMap<String, Integer> ranks = new TreeMap<>();
          for (int i = 0; i != headers.length; ++i) {
            String val = vals[i].trim();
            if (headers[i].equals("Player")) { name = val; }
            else if (headers[i].equals("Pos")) { pos = val; }
            else if (headers[i].equals("Age")) { age = Integer.parseInt(val); }
            else if (headers[i].equals("Team")) { team = val; }
            else if (headers[i].startsWith("201") && !val.equals("--")) { ranks.put(headers[i], Integer.parseInt(val)); }
          }
          if (name == null) { System.err.println("No name? : " + line); continue; }
          int lastSpace = name.lastIndexOf(' ');
          String firstName = name.substring(0, lastSpace);
          String lastName = name.substring(lastSpace + 1);
          Player p = null;
          for (Map.Entry<String, Integer> entry : ranks.entrySet()) {
            Player tp = byRank.get(entry.getKey() + ":" + entry.getValue());
            if (tp != null && tp._lastname.equalsIgnoreCase(lastName)) { p = tp; break; }
            if (tp != null) { System.err.format("Different names for same value [%s:%d] : %s vs. %s\n", entry.getKey(), entry.getValue(), tp._lastname, lastName); }
          }
          if (p == null) { players.add(p = new Player(firstName, lastName)); }
          else if (!p._firstname.equals(firstName)) { p._otherfirsts.add(firstName); }
          if (pos != null) { p._posFirst = pos; }
          if (team != null && team.length() > 0) { p._teamFirst = team; }
          int rank = ranks.get(year);
          YearInfo yi = new YearInfo(rank, pos, age, team);
          p._infos.put(year, yi);
          for (Map.Entry<String, Integer> entry : ranks.entrySet()) {
            byRank.put(entry.getKey() + ":" + entry.getValue(), p);
          }
        }
      }
    }
    try (PrintWriter pw = new PrintWriter(new FileWriter(KEEPER_DIR + "output.txt"))) {
      pw.print("Name\tAKA\tPos\tTeam");
//      for (String year : years) { pw.print("\t" + year + "\tPos\tAge"); }
      for (String year : years) { pw.print("\t" + year + "\tinfo"); }
      pw.println();
      for (Player p : players) {
        pw.format("%s %s\t", p._firstname, p._lastname);
        for (String fn : p._otherfirsts) { pw.format("%s ", fn); }
        pw.format("\t%s\t%s", p._posFirst, p._teamFirst);
        String lastPos = p._posFirst;
        String lastTeam = p._teamFirst;
        for (String year : years) {
          YearInfo yi = p._infos.get(year);
//          if (yi == null) { pw.print("\t--\t--\t--"); }
//          else { pw.format("\t%d\t%s\t%d", yi._rank, yi._pos, yi._age); }
          if (yi == null) { pw.print("\t--\t"); }
          else {
            pw.format("\t%d\t", yi._rank);
            if (yi._pos != null && yi._pos.length() > 0 && !yi._pos.equals(lastPos)) { pw.format("%s ", yi._pos); lastPos = yi._pos; }
            if (yi._team != null && yi._team.length() > 0 && !yi._team.equals(lastTeam)) { pw.format("%s ", yi._team); lastTeam = yi._team; }
          }
        }
        pw.println();
      }
    }
  }
}
