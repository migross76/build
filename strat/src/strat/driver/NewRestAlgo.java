package strat.driver;

import java.util.Random;

public class NewRestAlgo {
  public static void main(String[] args) throws Exception {
    Random r = new Random();
    for (int rest_min = 0; rest_min != 22; ++rest_min) {
      boolean played = true;
      int gs = 0;
      for (int i = 0; i != 16200; ++i) {
        if (played && r.nextInt(6) < 4 && r.nextInt(20) < rest_min) { played = false; continue; }
        played = true;
        ++gs;
      }
      System.out.format("%d : %.2f : %d\n", rest_min, gs * 0.01, gs / 25);
    }
  }
}
