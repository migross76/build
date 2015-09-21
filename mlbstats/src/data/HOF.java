package data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import util.MyDatabase;
import db.CachedStatement;

//hofID    | varchar(10)
//yearID   | smallint(4)
//votedBy  | varchar(64)
//ballots  | smallint(5)
//needed   | varchar(20)
//votes    | float(5,1)
//inducted | enum('Y','N')
//category | varchar(20)
public class HOF {
  
  public enum Selection { ELECTED, ALL }

  public String hofID()   { return _hofID; }
  public int    yearID()  { return _yearID; }
  public String votedBy() { return _votedBy; }
  public int    votes()   { return _votes; }
  public int    ballots() { return _ballots; }
  public double percent() { return _votes / (double)_ballots; }
  
  private String _hofID = null;
  private int    _yearID = 0;
  private String _votedBy = null; // TODO enum
  private int    _votes = 0;
  private int    _ballots = 0;
  
  private HOF() { }
  
  public static class Table {
    private void fetch(MyDatabase db, Selection selection) throws SQLException {
      CachedStatement PS = db.prepare("SELECT hofID, yearID, votedBy, inducted, category, votes, ballots FROM halloffame");
      try (ResultSet RS = PS.executeQuery()) {
        while (RS.next()) {
          if (!RS.getString(5).equals("Player")) { continue; }
          HOF R = new HOF();
          R._hofID = RS.getString(1);
          R._yearID = RS.getInt(2);
          R._votedBy = RS.getString(3);
          R._votes = RS.getInt(6);
          R._ballots = RS.getInt(7);

          if (selection == Selection.ALL || RS.getString(4).equals("Y")) {
            addID(R._hofID, R);
            addYear(R._yearID, R);
          }
          if (R._yearID == 2013) {
            double percent = RS.getInt(6) / (double) RS.getInt(7);
            if (percent >= 0.05 && percent < 0.75) {
              _onBallot.put(R._hofID, R);
            }
          }
        }
      }
    }
    
    private void addID(String id, HOF R) {
      List<HOF> list = _byID.get(id);
      if (list == null) { _byID.put(id, list = new ArrayList<>()); }
      list.add(R);
    }
    
    private void addYear(int year, HOF R) {
      List<HOF> list = _byYear.get(year);
      if (list == null) { _byYear.put(year, list = new ArrayList<>()); }
      list.add(R);
    }
    
    public Table(MyDatabase db, Sort yearSort, Selection selection) throws SQLException {
      _byID = new HashMap<>();
      _byYear = (yearSort == Sort.SORTED) ? new TreeMap<Integer, List<HOF>>() : new HashMap<Integer, List<HOF>>();
      fetch(db, selection);
    }
    
    public Map<Integer, List<HOF>> byYear() { return _byYear; }
    public List<HOF> year(int year) { return _byYear.get(year); }
    public Map<String, List<HOF>> byID() { return _byID; }
    public List<HOF> id(String ID) { return _byID.get(ID); }
    public HOF idFirst(String ID) { List<HOF> hof = _byID.get(ID); return hof == null || hof.isEmpty() ? null : hof.get(0); }
    public HOF onBallot(String ID) { return _onBallot.get(ID); }
    
    private Map<Integer, List<HOF>> _byYear = null;
    private Map<String, List<HOF>> _byID = null;
    
    private Map<String, HOF> _onBallot = new HashMap<>();
  }
  
  public static void main(String[] args) throws SQLException {
    HOF.Table T = null;
    try (MyDatabase db = new MyDatabase()) { T = new HOF.Table(db, Sort.SORTED, Selection.ELECTED); }
    for (Map.Entry<Integer, List<HOF>> E : T.byYear().entrySet()) {
      System.out.format("%d :", E.getKey());
      for (HOF R : E.getValue()) { System.out.format(" %s", R.hofID()); }
      System.out.println();
    }
    HOF R = T.idFirst("charlos99h");
    System.out.format("%s : %d [%s]\n", R.hofID(), R.yearID(), R.votedBy());
    R = T.idFirst("youngro01h");
    System.out.format("%s : %d [%s]\n", R.hofID(), R.yearID(), R.votedBy());
    R = T.idFirst("yountro01h");
    System.out.format("%s : %d [%s]\n", R.hofID(), R.yearID(), R.votedBy());
  }
}
