package app;
/*
import util.ByPlayer;
import util.MyDatabase;
import data.ELO;
import data.Master;
import data.Sort;
import data.Type;
import data.War;
*/
// Start of a program to compute equation of ELO to WAR - 1) to put it on the same scale, and 2) to put pitchers and batters on the same scale
public class EloStats {
/*
  private static void compute(MyDatabase db, ELO.Table elo, Type type) throws Exception {
    War.ByID war = new War.ByID();
    war.addAll(new War.Table(db, type));
    for (ELO e : elo.all()) {
      if (e.type() != type) { continue; }
      ByPlayer<War> w = war.get(e.playerID());
    }
    
  }
  
  private static void main(String[] args) throws Exception {
    Master.Table master = null;
    ELO.Table elo = null;
    War.Table bat = null;
    War.Table pit = null;
    try (MyDatabase db = new MyDatabase()) {
      master = new Master.Table(db, Sort.SORTED);
      elo = new ELO.Table(db);
      bat = new War.Table(db, Type.BAT);
      pit = new War.Table(db, Type.PITCH);
    }
      
    for (ELO e : elo.all()) {
      if (e.type() == Type.BAT) { continue; }
      
    }
  }
*/
}
