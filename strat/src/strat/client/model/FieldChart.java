package strat.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import strat.client.model.Play.Flag;

public class FieldChart {
  
  public static int[] PROBS18 = { 1, 3, 6, 10, 15, 21, 25, 27, 27, 25, 21, 15, 10, 6, 3, 1 };
  
  public static class RangePlay {
    public RangePlay(String name, SimplePlay play) { _name = name; _play = play; }
    
    public final String _name;
    public final SimplePlay _play;
  }
  
  public static class RangeChart {
    // OF : TR3 DO3 DO2 SI2 F1 F2 F3
    //    : t   d=  d-  s=  y?a y?b[+2nd] y?c
    // IF : SI2 SI1 G3 G2 G1 #-#
    //      s=  s-  g?c[right] g?b[adv] g?a[+++]
    public RangeChart(List<String> lines) {
      String line = null;
      while (!(line = lines.remove(0)).isEmpty()) {
//System.out.println("RNG : " + line);
        String[] tokens = line.split(" ");
        String name = tokens[0];
        if (name.equals("#")) { // TODO add support for extra base
        } else {
          RangePlay rp = new RangePlay(tokens[0], new SimplePlay(tokens[1]));
          for (int i = 0; i != 5; ++i) {
            int start = Integer.parseInt(tokens[i+2]);
            if (start < 1 || start > 20) { continue; }
            for (; start != 21; ++start) {
              _ranges[i][start-1] = rp;
            }
          }
        }
      }
/*
      for (RangePlay[] rarr : _ranges) {
        for (RangePlay rp : rarr) {
          System.out.format("%s ", rp._name);
        }
        System.out.println();
      }
*/
    }
    
    private RangePlay[][] _ranges = new RangePlay[5][20];
  }
  
  public static class ErrorChart {
    public static final int RARE_PLAY = -1;

    public ErrorChart(List<String> lines) {
      String line = null;
      while (!(line = lines.remove(0)).isEmpty()) {
        String[] tokens = line.split(" ");
        int[] err = new int[16];
        _map.put(Integer.parseInt(tokens[0]), err);
        err[2] = RARE_PLAY;
        for (int i = 1; i != tokens.length; ++i) {
          for (int n : new NumberList(tokens[i])) { err[n-3] = i; }
        }
      }
    }
    
    private HashMap<Integer, int[]> _map = new HashMap<>();
  }
  
  private void add(List<String> lines) {
    while (!lines.isEmpty()) {
      String line = lines.remove(0);
      String[] tokens = line.split(" ");
      if (tokens[0].charAt(0) == 'R') {
        RangeChart rc = new RangeChart(lines);
        for (int i = 1; i != tokens.length; ++i) {
          _ranges.put(Position.map(tokens[i]), rc);
        }
      } else if (tokens[0].charAt(0) == 'E') {
        ErrorChart ec = new ErrorChart(lines);
        for (int i = 1; i != tokens.length; ++i) {
          _errors.put(Position.map(tokens[i]), ec);
        }
      }
    }
  }

  private RangePlay[] selectRange(Fielding fielding) {
    return _ranges.get(fielding._pos)._ranges[fielding._field-1];
  }
  
  private int[] selectError(Fielding fielding) {
    int errRate = fielding._err;
    int[] err = null;
    while (err == null) {
      err = _errors.get(fielding._pos)._map.get(errRate); if (err == null) { --errRate; }
    }
    return err;
  }
  
  public RangePlay calcRange(Fielding fielding, Dice dice) {
    return selectRange(fielding)[dice.roll(20)-1];
  }
  
  public int calcError(Fielding fielding, Dice dice) {
    return selectError(fielding)[dice.roll(6,6,6)-3];
  }
  
  public void collect(Fielding fielding, Collector collector) {
    RangePlay[] rp = selectRange(fielding);
    int[] err = selectError(fielding);
    for (int i_e = 0, e_e = err.length; i_e != e_e; ++i_e) {
      int e = err[i_e];
      int wt = PROBS18[i_e];
      int ct = 0;
      for (RangePlay r : rp) {
        ++ct;
        if (r == null) { System.err.println("Missing range play : " + ct); continue; }
        Play.Type type = r._play._type;
        int bases = e;
        if (bases == -1) { /* TODO do rare play instead */ bases = 0; }
        switch (type) {
          case SINGLE: case WALK: case HBP: ++bases; break;
          case DOUBLE: bases += 2; break;
          case TRIPLE: bases += 3; break;
          default: break;
        }
        if (bases < 0) { bases = 0; }
        if (bases > 4) { bases = 4; }
        Flag flag = null;
        switch (bases) {
          case 1: type = Play.Type.SINGLE; flag = Flag.ONE_BASE; break;
          case 2: type = Play.Type.DOUBLE; flag = Flag.TWO_BASE; break;
          case 3: type = Play.Type.TRIPLE; break;
          case 4: type = Play.Type.HOMERUN; break;
          default: break; // keep the original play type
        }
        Position pos = bases == 0 ? fielding._pos : null;
        SimplePlay sp = flag == null ? new SimplePlay(type, pos) : new SimplePlay(type, pos, flag);
        collector.collect(sp, wt, new ToggleState());
      }
    }
  }
  
  public double woba(Fielding fielding) {
    RangePlay[] rp = selectRange(fielding);
    int[] err = selectError(fielding);
    
//    for (RangePlay r : rp) { System.out.format("%s ", r._play._type.code()); } System.out.println();
//    for (int e : err) { System.out.format("%d ", e); } System.out.println();

    double woba = 0;
    double total = 0;
    for (int i_e = 0, e_e = err.length; i_e != e_e; ++i_e) {
      int e = err[i_e];
      int wt = PROBS18[i_e];
      int ct = 0;
      for (RangePlay r : rp) {
        ++ct;
        if (r == null) { System.err.println("Missing range play : " + ct); continue; }
        Play.Type type = r._play._type;
        int bases = e;
        if (bases == -1) { /* TODO do rare play instead */ bases = 0; }
        switch (type) {
          case SINGLE: case WALK: case HBP: ++bases; break;
          case DOUBLE: bases += 2; break;
          case TRIPLE: bases += 3; break;
          default: break;
        }
        if (bases < 0) { bases = 0; }
        if (bases > 4) { bases = 4; }
        switch (bases) {
          case 1: type = Play.Type.SINGLE; break;
          case 2: type = Play.Type.DOUBLE; break;
          case 3: type = Play.Type.TRIPLE; break;
          case 4: type = Play.Type.HOMERUN; break;
          default: type = Play.Type.POP; break;
        }
        
        woba += wt * type.woba_wt();
        total += wt;
      }
    }
    return woba / total;
  }
  
  public void load(String data) {
    ArrayList<String> fieldLines = new ArrayList<>(Arrays.asList(data.split("\n")));
    fieldLines.add("");
    add(fieldLines);
  }
/*  
  public static void main(String[] args) throws Exception {
    List<String> lines = Files.readAllLines(Paths.get("C:/build/strat/other/fielding.txt"), StandardCharsets.UTF_8);
    FieldChart fc = new FieldChart();
    fc.add(lines);
    
    for (int r = 2; r != 4; ++r) {
      for (int e = 16; e != 27; ++e) {
        System.out.format("p %de%d %.3f\n", r, e, fc.woba(Position.PITCH, r, e));
      }
    }
    
  }
*/  
  private HashMap<Position, RangeChart> _ranges = new HashMap<>();
  private HashMap<Position, ErrorChart> _errors = new HashMap<>();
}
