package util;

import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

public class AgeDist {
  private static final int AGE_START = 20;
  private static final int AGE_END = 42;

  public AgeDist(String name) { _name = name; }
  public String _name = null;
  public int _count = 0;
  public int _playingTime = 0;
  public double _war = 0;
  public double _warNorm = 0;
  public double _warPerc = 0;
  
  public double perSeason(double playtime) {
    return _war * playtime / _playingTime;
  }
  
  public static class Ag {
    private static int getOffset(int age) {
      int off = age;
      if (off < AGE_START) { off = AGE_START; } else if (off > AGE_END) { off = AGE_END; }
      return off - AGE_START;
    }
    
    private void add(Master m, War W) {
      AgeDist bAge = _byAge[getOffset(m.age(W.yearID()))];
      ++bAge._count;
      bAge._war += W.war();
      bAge._playingTime += W.playtime();
      
      ++_all._count;
      _all._war += W.war();
      _all._playingTime += W.playtime();
      
      _ids.add(W.playerID());
    }
    
    public void finish() {
      for (AgeDist B : _byAge) {
        B._warNorm = B._war / B._playingTime;
        B._warPerc = B._war / _all._war;
      }
      _all._warNorm = _all._war / _all._playingTime;
    }

    public double careerFactor(Master m, ByPlayer<War> A) {
      double war = 0;
      for (War W : A) {
        war += W.playtime() * _byAge[getOffset(m.age(W.yearID()))]._warNorm;
      }
//      if (C._war > 100) { 
//        System.out.println(C._id + " : " + C._war + " : " + war + " : " + C._countStat + " : " + _all._war + " : " + _all._countStat);
//        System.out.println(war / C._countStat + " : " + _all._war / _all._countStat);
//      }
      return (war / A.total().playtime()) / (_all._war / _all._playingTime);
    }
    
    public double careerMissing(Master m, ByPlayer<War> A) {
      double wt = 1;
      boolean before = false, after = false;
      for (War W : A) {
        int age = m.age(W.yearID());
        if (age <= AGE_START) {
          if (before) { continue; }
          before = true;
        } else if (age >= AGE_END) {
          if (after) { continue; }
          after = true;
        }
        AgeDist bAge = _byAge[getOffset(age)];
        wt -= bAge._warPerc;
      }
      return wt;
    }
    
    public Ag() {
      _byAge = new AgeDist[AGE_END - AGE_START + 1];
      _byAge[0] = new AgeDist(AGE_START + "-");
      for (int i = AGE_START + 1, e = AGE_END; i != e; ++i) {
        _byAge[i - AGE_START] = new AgeDist("" + i);
      }
      _byAge[AGE_END - AGE_START] = new AgeDist(AGE_END + "+");
    }
    
    private HashSet<String> _ids = new HashSet<>();
    private AgeDist[] _byAge = null;
    private AgeDist _all = new AgeDist("All");
  }
  
  public static class Tally {
    private static final int MIN_CAREER = 10;
    private static final int MIN_SEASON = 2;

    
    private void print() {
      System.out.print("Decade");
      for (int i = AGE_START; i <= AGE_END; ++i) { System.out.format("\t%d", i); }
      System.out.println("\tAll");
      for (Map.Entry<Integer, Ag> E : _decades.entrySet()) {
        System.out.print(E.getKey() * 10 + 1);
        for (AgeDist B : E.getValue()._byAge) { System.out.format("\t%6.2f", B._war * _playtime / B._playingTime); }
        System.out.format("\t%6.2f\n", E.getValue()._all.perSeason(_playtime));
      }
      System.out.print("All");
      for (AgeDist B : _total._byAge) { System.out.format("\t%6.2f", B._war * _playtime / B._playingTime); }
      System.out.format("\t%6.2f\n\n", _total._all.perSeason(_playtime));

      for (Map.Entry<Integer, Ag> E : _decades.entrySet()) {
        System.out.print(E.getKey() * 10 + 1);
        for (AgeDist B : E.getValue()._byAge) { System.out.format("\t%d", B._playingTime); }
        System.out.format("\t%d\n", E.getValue()._all._playingTime);
      }
      System.out.print("All");
      for (AgeDist B : _total._byAge) { System.out.format("\t%d", B._playingTime); }
      System.out.format("\t%d\n\n", _total._all._playingTime);
    }    
    
    public Tally(Master.Table mt, War.Table WT, int playtime) {
      _playtime = playtime;
      War.ByID BP = new War.ByID();
      BP.addFilter(War.filterPositive);
      BP.addAll(WT);
      for (ByPlayer<War> A : BP) {
        Master m = mt.byID(A.total().playerID());
        War wplus = A.filter(War.filterPositive);
        if (wplus == null || wplus.war() < MIN_CAREER || A.best().war() < MIN_SEASON) { continue; }
        for (War W : A) {
          int decYr = (W.yearID() - 1) / 10;
          Ag AD = _decades.get(decYr);
          if (AD == null) { _decades.put(decYr, AD = new Ag()); }
          AD.add(m, W);
          _total.add(m, W);
        }
      }
    }
    
    private int _playtime = 0;
    private Ag _total = new Ag();
    private TreeMap<Integer, Ag> _decades = new TreeMap<>();
  }
  
  public static void main(String[] args) throws Exception {
    Master.Table master = null;
    War.Table bat = null;
    War.Table pit = null;
    try (MyDatabase db = new MyDatabase()) {
      master = new Master.Table(db, Sort.UNSORTED);
      bat = new War.Table(db, Type.BAT);
      pit = new War.Table(db, Type.PITCH);
    }
    
    AgeDist.Tally BT = new AgeDist.Tally(master, bat, 650);
    AgeDist.Tally PT = new AgeDist.Tally(master, pit, 760);
    System.out.println("Batting");
    BT.print();
    System.out.println("Pitching");
    PT.print();
  }
}
