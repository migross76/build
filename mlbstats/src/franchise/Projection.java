package franchise;

import data.War;

public class Projection {
  private static final int HIST = 3;
  private static final int START = 4;
  
  // PT*=5.4*POWER(A2-16,-0.7)
  // WAR%*=-0.01*(A2-16) + 0.92
  // TODO adjust so it's based on projected year
  public static void compute(Player p, int year) {
    p.reset();
    double[] war = new double[HIST];
    double[] play = new double[HIST];
    for (War w : p._allwar) {
      if (year + 1 == w.yearID()) { p._wNow += w.war(); p._pNow += w.playtime(); continue; }
      if (year < w.yearID() || year - HIST >= w.yearID()) { continue; }
      int slot = year - w.yearID();
      war[slot] += w.war();
      play[slot] += w.playtime();
    }
    double total = 0;
    for (int i = 0; i != HIST; ++i) {
      total += START - i;
      p._wProj += war[i] * (START - i);
      p._pProj += play[i] * (START - i);
    }
    p._wProj /= total;
    p._pProj /= total;
    p._pProj /= p._season;
    p._pNow /= p._season;
    p._wProjRate = p._pProj == 0 ? 0 : p._wProj / p._pProj;
    int age = p._master.age(year);
    p._pAge = 5.4*Math.pow(age - 16, -0.7);
    p._wAgeRate = 0.92-0.01*(age - 16);
  }
}
