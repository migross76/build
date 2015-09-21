package alltime;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import util.ByPlayer;
import util.MyDatabase;
import data.Master;
import data.Type;
import data.War;

public class Fetch {
  private static final double DH_RATE = 16.0 / 700.0;
  private static final double PA_ADJUST = 650.0;
  private static final double IP3_ADJUST = 900.0;
  
  public ArrayList<Career> getBatters(MyDatabase db, double min_season, double min_career) throws SQLException {
    War.ByID wid = new War.ByID();
    wid.addAll(new War.Table(db, Type.BAT));
    HashMap<String, Career> cMap = new HashMap<>();

    for (ByPlayer<War> byW : wid) {
      War wTot = byW.total();
      if (wTot.war() < min_career && byW.best().war() < min_season) { continue; }
      Career C = cMap.get(wTot.playerID());
      Master m = _mt.byID(wTot.playerID());
      if (C == null) {
        cMap.put(wTot.playerID(), C = new Career(m));
        C._isBatter = true;
        C._countStat = wTot.playtime();
        C._war = wTot.war();
        C._warNorm = C._war * PA_ADJUST / C._countStat;
      }
      for (War w : byW) {
        Batter S = new Batter(C);
        S._year = w.yearID();
        S._age = m.age(S._year);
        S._team = w.teamID();
        S._war = w.war();
        S._countStat = w.playtime();
        double dhPenalty = S._countStat * DH_RATE;
        S._warField = S._war * (w.rField() + w.rPos() - dhPenalty) / w.rar();
        S._warNorm = (S._war + C._warNorm) * PA_ADJUST / (S._countStat + PA_ADJUST);
        S._warWeight = S._warNorm * 4 + C._war;
        C._seasons.add(S);
      }
    }
    return new ArrayList<>(cMap.values());
  }
  
  // sqrt(y.rWAR * (s.sumrwar - y.rWAR) / 6) as totalWar
  // s.sumrwar + y.rWAR * 2 as totalWar
  public ArrayList<Career> getPitchers(MyDatabase db, double min_season, double min_career) throws SQLException {
    War.ByID wid = new War.ByID();
    wid.addAll(new War.Table(db, Type.PITCH));
    HashMap<String, Career> cMap = new HashMap<>();
    
    for (ByPlayer<War> byW : wid) {
      War wTot = byW.total();
      if (wTot.war() < min_career && byW.best().war() < min_season) { continue; }
      Career C = cMap.get(wTot.playerID());
      Master m = _mt.byID(wTot.playerID());
      if (C == null) {
        cMap.put(wTot.playerID(), C = new Career(m));
        C._isBatter = false;
        C._countStat = wTot.playtime();
        C._war = wTot.war();
        C._warNorm = C._war * IP3_ADJUST / C._countStat;
      }
      for (War w : byW) {
        Season S = new Pitcher(C);
        S._year = w.yearID();
        S._age = m.age(S._year);
        S._team = w.teamID();
        S._war = w.war();
        S._countStat = w.playtime();
        S._warNorm = (S._war + C._warNorm) * IP3_ADJUST / (S._countStat + IP3_ADJUST);
        S._warWeight = S._warNorm * 4 + C._war;
        C._seasons.add(S);
      }
    }
    return new ArrayList<>(cMap.values());
  }
  
  public Fetch(Master.Table mt) { _mt = mt; }
  
  private final Master.Table _mt;
}
