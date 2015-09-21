package draft;

import util.MyDatabase;
import data.HOF;
import data.Sort;

public class Main {
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    final int SPAN = 5;
    
    System.out.println("Loading...");
    Publisher P = new HtmlPublisher(args[0]);
    AllStarElector E = new AllStarElector(
        "C", "1B", "2B", "SS", "3B", "OF", "OF", "OF", "Bat",
//        "SP", "SP", "SP", "SP", "SP", "MR", "CL", "CL");
        "SP", "SP", "SP", "CL", "RP");
    Nominator N = new RangeNominator(1876, 2012, SPAN);
    Candidates C = null;
    System.out.println("Processing...");
    while ((C = N.nominate()) != null) {
      for (Roster R : E.elect(C)) { P.print(R); }
    }
    P.publish();
    HOF.Table ht = null;
    try (MyDatabase db = new MyDatabase()) { ht = new HOF.Table(db, Sort.UNSORTED, HOF.Selection.ELECTED); }
    for (AllStarElector.Elected e : E.elected()) {
      HOF hof = ht.idFirst(e._master.hofID());
      System.out.format("%s %s\t%.1f\t%d\t", e._master.nameFirst(), e._master.nameLast(), e._times / SPAN, e._master.yearBirth());
      if (hof != null) { System.out.print(hof.yearID()); }
      System.out.println();
    }
    System.out.println("Finished");
  }

}
