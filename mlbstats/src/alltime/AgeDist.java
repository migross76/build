package alltime;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import util.MyDatabase;
import data.Master;
import data.Sort;

public class AgeDist {
  public static final int AGE_START = 20;
  public static final int AGE_END = 42;
  
  private static class Block {
    public Block(String name) { _name = name; }
    public String _name = null;
    public int _count = 0;
    public int _playingTime = 0;
    public double _war = 0;
    public double _warNorm = 0;
    public double _warPerc = 0;
  }
  
  private static int getOffset(int age) {
    int off = age;
    if (off < AGE_START) { off = AGE_START; } else if (off > AGE_END) { off = AGE_END; }
    return off - AGE_START;
  }
  
  public void add(Season S) {
    Block bAge = _byAge[getOffset(S._age)];
    ++bAge._count;
    bAge._war += S._war;
    bAge._playingTime += S._countStat;
    
    ++_all._count;
    _all._war += S._war;
    _all._playingTime += S._countStat;
    
    _ids.add(S._career._id);
  }
  
  public void finish() {
    for (Block B : _byAge) {
      B._warNorm = B._war / B._playingTime;
      B._warPerc = B._war / _all._war;
    }
    _all._warNorm = _all._war / _all._playingTime;
  }

  public double careerFactor(Career C) {
    double war = 0;
    for (Season S : C._seasons) {
      war += S._countStat * _byAge[getOffset(S._age)]._warNorm;
    }
//    if (C._war > 100) { 
//      System.out.println(C._id + " : " + C._war + " : " + war + " : " + C._countStat + " : " + _all._war + " : " + _all._countStat);
//      System.out.println(war / C._countStat + " : " + _all._war / _all._countStat);
//    }
    return (war / C._countStat) / (_all._war / _all._playingTime);
  }
  
  public double careerMissing(Career C) {
    double wt = 1;
    boolean before = false, after = false;
    for (Season S : C._seasons) {
      if (S._age <= AGE_START) {
        if (before) { continue; }
        before = true;
      } else if (S._age >= AGE_END) {
        if (after) { continue; }
        after = true;
      }
      Block bAge = _byAge[getOffset(S._age)];
      wt -= bAge._warPerc;
    }
    return wt;
  }
  
  public void print() {
    for (Block B : _byAge) {
      System.out.format("%4s\t%4d\t%2d\t%6.1f\t%6d\t%5.2f\n", B._name, _ids.size(), B._count, B._war, B._playingTime, B._war * 650 / B._playingTime);
    }
  }
  
  public AgeDist() {
    _byAge = new Block[AGE_END - AGE_START + 1];
    _byAge[0] = new Block(AGE_START + "-");
    for (int i = AGE_START + 1, e = AGE_END; i != e; ++i) {
      _byAge[i - AGE_START] = new Block("" + i);
    }
    _byAge[AGE_END - AGE_START] = new Block(AGE_END + "+");
  }
  
  private HashSet<String> _ids = new HashSet<>();
  private Block[] _byAge = null;
  private Block _all = new Block("All");
  
  public static void analyze(Collection<Career> careers, int factor) {
    AgeDist total = new AgeDist();
    TreeMap<Integer, AgeDist> decades = new TreeMap<>();
    for (Career C : careers) {
      for (Season S : C._seasons) {
        int decYr = (S._year - 1) / 10;
        AgeDist AD = decades.get(decYr);
        if (AD == null) { decades.put(decYr, AD = new AgeDist()); }
        AD.add(S);
        total.add(S);
      }
    }
    System.out.print("Decade");
    for (int i = AGE_START; i <= AGE_END; ++i) { System.out.format("\t%d", i); }
    System.out.println("\tAll");
    for (Map.Entry<Integer, AgeDist> E : decades.entrySet()) {
      System.out.print(E.getKey() * 10 + 1);
      for (Block B : E.getValue()._byAge) { System.out.format("\t%6.2f", B._war * factor / B._playingTime); }
      System.out.format("\t%6.2f\n", E.getValue()._all._war * factor / E.getValue()._all._playingTime);
    }
    System.out.print("All");
    for (Block B : total._byAge) { System.out.format("\t%6.2f", B._war * factor / B._playingTime); }
    System.out.format("\t%6.2f\n", total._all._war * factor / total._all._playingTime);

    for (Map.Entry<Integer, AgeDist> E : decades.entrySet()) {
      System.out.print(E.getKey() * 10 + 1);
      for (Block B : E.getValue()._byAge) { System.out.format("\t%d", B._playingTime); }
      System.out.format("\t%d\n", E.getValue()._all._playingTime);
    }
    System.out.print("All");
    for (Block B : total._byAge) { System.out.format("\t%d", B._playingTime); }
    System.out.format("\t%d\n", total._all._playingTime);
  }
  
  public static void main(String[] args) throws Exception {
    try (MyDatabase db = new MyDatabase()) {
      Master.Table mt = new Master.Table(db, Sort.UNSORTED);
      Fetch fetch = new Fetch(mt);
      analyze(fetch.getBatters(db, 2, 10), 650);
      System.out.println();
      analyze(fetch.getPitchers(db, 2, 10), 760);
    }
  }
}
