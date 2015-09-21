package app;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;
import util.ByPlayer;
import util.Excel;
import util.MyDatabase;
import util.WeightedApp;
import util.WeightedWar;
import data.Appearances;
import data.HOF;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

// Rebuild the Hall of Fame
// - Based on wWAR
// - Ordered by Year of Birth
// - Allow for different number of electees per year, to support inclusion of players (e.g., integration)
// - Outputs various slices to Excel format
public class HallReboot {
  private static final int CURRENT_YEAR = 2013;
  
  private static final int AGE_MIN = 46; // age that a player becomes eligible
  private static final int PRINT_KEEP = 5; // the top carryovers to the next ballot
  private static final int KEEP_YEARS = 10; // number of years a player stays on the ballot
  private static final int QUAL_YEARS = 5; // the number of years a player must play to qualify
  
  private static final int MISS_COUNT = 500; // 150; // number of top players to print that missed entry into this HOF

  private static class Range {
    public int _year_start = 0;
    public int _year_end = 0;
    public double _per_year = 0.0;
    public Range ( int start, int end, double per ) {
      _year_start = start;
      _year_end = end;
      _per_year = per;
    }
  }
  
  private static class Player implements Comparable<Player> {
    public Master _master = null;
    public WeightedWar _wwar = null;
    public WeightedApp _app = null;
    public HOF _hof = null;
    public int _year_elected = 0;
    public int _year_last = 0;
    public Player _worst = null;
    public int _years = 0;
    
    public String id() { return _master.playerID(); }
    public String name() { return String.format("%s %s", _master.nameFirst(), _master.nameLast()); }
    public int yearBirth() { return _master.yearBirth(); }
    public String primary() { return _app == null ? "??" : _app.primary().pos().getName(); }
    
    public void setWorst(Player P) {
      if (_worst == null || _worst._wwar.wwar() > P._wwar.wwar()) { _worst = P; }
    }
    
    public int eligibleYear(int currYear) {
      return currYear - AGE_MIN - _master.yearBirth() + 1;
    }
    
    public int eligibleElected() {
      return eligibleYear(_year_elected);
    }
    
    public Player(Master M, ByPlayer<WeightedWar> WW, WeightedApp WA, HOF hof) {
      _master = M;
      _wwar = WW.total();
      _year_last = WW.last().yearID();
      _years = WW.size();
      _app = WA;
      _hof = hof;
    }
    
    public String hof() {
      if (_year_last + 5 > CURRENT_YEAR) { return "N/A"; }
      if (_hof == null) { return ""; }
      return Integer.toString(_hof.yearID());
    }
    
    @Override
    public int compareTo(Player arg0) {
      if (_wwar != arg0._wwar) { return _wwar.wwar() > arg0._wwar.wwar() ? -1 : 1; }
      return _master.playerID().compareTo(arg0._master.playerID());
    }
  }
    
  private static Master.Table _mt = null;
  private static HOF.Table _ht = null;
  private static WeightedWar.ByID _wwBy = null;
  private static WeightedApp.ByID _waBy = null;
  
  private static void assemble(MyDatabase db, Appearances.Table AT, Type type, int playtime) throws SQLException {
    WeightedWar.Tally WWT = new WeightedWar.Tally(new War.Table(db, type), playtime); 
    WWT.adjustByPosition(AT);
    _wwBy.addAll(WWT);
    _waBy.addAll(new WeightedApp.Tally(WWT, AT));
  }
  
  public static void main(String[] args) throws Exception {
    // table initialization
    System.out.println("Loading...");
    _wwBy = new WeightedWar.ByID();
    _waBy = new WeightedApp.ByID();
    try (MyDatabase db = new MyDatabase()) {
      _mt = new Master.Table(db, Sort.SORTED);
      _ht = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED);
      Appearances.Table AT = new Appearances.Table(db);
      assemble(db, AT, Type.BAT, 650);
      assemble(db, AT, Type.PITCH, 800);
    }

    
    System.out.println("Processing...");
    Range[] ranges = { new Range(1901, 1977, 1.0), new Range(1977, 2012, 2.0), new Range(2012, 2037, 2.0) }; // 2021 for "finished" players, 2037 for current players
//    Range[] ranges = { new Range(2012, 2036, 2.0) }; // 2036 for current players
    
    HashMap<String, Player> players = new HashMap<>();

    for (int i = _mt.yearFirst(); i != ranges[0]._year_start - AGE_MIN; ++i) {
      for (Master M : _mt.year(i)) {
        ByPlayer<WeightedWar> wwBy = _wwBy.get(M.playerID());
        if (wwBy == null) { continue; }
        ByPlayer<WeightedApp> waBy = _waBy.get(M.playerID());
        WeightedApp WA = waBy == null ? null : waBy.total();
        players.put(M.playerID(), new Player(M, wwBy, WA, _ht.idFirst(M.hofID())));
      }
    }
    
    int max_num = 0;
    for (Range R : ranges) {
      int max = (int)Math.ceil(R._per_year);
      if (max_num < max) { max_num = max; }
    }
    double elect_num = 0;
    TreeSet<Player> dq = new TreeSet<>();
    TreeSet<Player> drop = new TreeSet<>();
    TreeSet<Player> hof = new TreeSet<>();
    TreeSet<Player> actual = new TreeSet<>();
    
    Excel xVote = new Excel("Vote");
    xVote.add("Year", "Cand");
    for (int num = 0; num != max_num; ++num) {
      xVote.add("Elect #" + (num+1), "Yr", "WAR", "wWAR");
    }
    xVote.add("Elim").add("WAR").add("wWAR");
    for (int num = 0; num != PRINT_KEEP; ++num) {
      xVote.add("Next #" + (num+1), "Yr", "WAR", "wWAR");
    }
    for (Range R : ranges) {
      for (int yr = R._year_start; yr != R._year_end; ++yr) {
        for (Master M : _mt.year(yr - AGE_MIN)) {
          ByPlayer<WeightedWar> wwBy = _wwBy.get(M.playerID());
          if (wwBy == null) { continue; }
          ByPlayer<WeightedApp> waBy = _waBy.get(M.playerID());
          WeightedApp WA = waBy == null ? null : waBy.total();
          players.put(M.playerID(), new Player(M, wwBy, WA, _ht.idFirst(M.hofID())));
        }
        for (Player P : new ArrayList<>(players.values())) {
          if (P._years < QUAL_YEARS) { players.remove(P.id()); dq.add(P); }
        }
        ArrayList<Player> list = new ArrayList<>(players.values());
        Collections.sort(list);
        elect_num += R._per_year;
        xVote.row().add(yr).add(list.size());
        int num = 0;
        Player last_in = null;
        for (; num < elect_num; ++num) {
          Player P = list.get(num);
          P._year_elected = yr;
          xVote.add(P.name()).add(P.eligibleElected()).add(P._wwar.war(), 1).add(P._wwar.wwar(), 1);
          players.remove(P.id());
          hof.add(P);
          last_in = P;
        }
        elect_num -= num;
        for (int i = num; i < max_num; ++i) {
          xVote.skip(4);
        }
        Player best_dropped = null;
        ArrayList<Player> keepers = new ArrayList<>();

        keepers.add(list.get(num++)); // keep the top remaining player, no matter what
        for (; num < list.size(); ++num) { // don't drop the best player, EVER!
          Player P = list.get(num);
          P.setWorst(last_in);
          if (P.yearBirth() + AGE_MIN + KEEP_YEARS - 1 <= yr) {
            players.remove(P.id());
            if (best_dropped == null) { best_dropped = P; }
            drop.add(P);
            if (P._hof != null) { actual.add(P); }
          } else if (keepers.size() < PRINT_KEEP) {
            keepers.add(P);
          }
        }
        if (best_dropped == null) {
          xVote.add("[none]").skip(2);
        } else {
          xVote.add(best_dropped.name()).add(best_dropped._wwar.war(), 1).add(best_dropped._wwar.wwar(), 1);
        }
        for (Player P : keepers) {
          xVote.add(P.name()).add(P.eligibleYear(yr)).add(P._wwar.war(), 1).add(P._wwar.wwar(), 1);
        }
      }
    }
    xVote.setWidth();

    // TODO Do I want the real WAR or the adjusted one?
    System.out.println("Outputting...");
    Excel xHOF = new Excel("HOF");
    xHOF.add("Pos", "Name", "YOB", "Elect", "Yrs", "Retire", "WAR", "PF", "wWAR", "HOF");
    for (Player P : hof) {
      xHOF.row().add(P.primary()).add(P.name()).add(P.yearBirth()).add(P._year_elected).add(P.eligibleElected()).add(P._year_last).add(P._wwar.war(), 1).add(P._wwar.factor(), 2).add(P._wwar.wwar(), 1).add(P.hof());
    }
    xHOF.setWidth();
    int i = 0;
    Excel xInelig = new Excel("Ineligible");
    xInelig.add("Pos", "Name", "YOB", "WAR", "PF", "wWAR");
    i = 0;
    for (Player P : dq) {
      xInelig.row().add(P.primary()).add(P.name()).add(P.yearBirth()).add(P._wwar.war(), 1).add(P._wwar.factor(), 2).add(P._wwar.wwar(), 1);
      if (++i == 20) { break; }
    }
    xInelig.setWidth();
    
    Excel xDrop = new Excel("Dropped HOF");
    xDrop.add("Pos", "Name", "YOB", "First", "Last", "WAR", "PF", "wWAR", "HOF");
    for (Player P : actual) {
      xDrop.row().add(P.primary()).add(P.name())
           .add(P.yearBirth()).add(P.yearBirth() + AGE_MIN).add(P.yearBirth() + AGE_MIN + KEEP_YEARS - 1)
           .add(P._wwar.war(), 1).add(P._wwar.factor(), 2).add(P._wwar.wwar(), 1).add(P.hof());
    }
    xDrop.setWidth();
    Excel xMiss = new Excel("Missed");
    xMiss.add("Pos", "Name", "YOB", "First", "Last", "WAR", "PF", "wWAR", "HOF", "Elected", "Year", "wWAR");
    i = 0;
    for (Player P : drop) {
      xMiss.row().add(P.primary()).add(P.name())
           .add(P.yearBirth()).add(P.yearBirth() + AGE_MIN).add(P.yearBirth() + AGE_MIN + KEEP_YEARS - 1)
           .add(P._wwar.war(), 1).add(P._wwar.factor(), 2).add(P._wwar.wwar(), 1).add(P.hof())
           .add(P._worst.name()).add(P._worst._year_elected).add(P._worst._wwar.wwar(), 1);
      if (++i == MISS_COUNT) { break; }
    }
    xMiss.setWidth();
    Excel.save("C:/build/mlbstats/out/hall-reboot-wwar.xls");
  }

}

