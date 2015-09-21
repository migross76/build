package strat.driver;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import strat.server.CardData;
import strat.server.MyDatabase;
import strat.shared.CardLegacy;
import db.CachedStatement;

public class YearStats {
  private enum Bat {
    B_PA, B_AB, B_H, B_1B, B_AVG, B_2B, B_3B, B_HR, B_RBI, B_BB, B_SO
  }

  /* playerID | yearID | stint | teamID | lgID | G | G_batting | AB | R | H | 2B | 3B | HR | RBI | SB | CS | BB | SO | IBB | HBP | SH | SF | GIDP | G_old */
  public static void fetchBat(MyDatabase db, CardLegacy card) throws SQLException {
    CachedStatement ps = db
        .prepare("SELECT sum(pa), sum(ab), sum(h), sum(2b), sum(3b), sum(hr), sum(rbi), sum(bb), sum(so), sum(ibb), b.yearID FROM batting b, rbwar r WHERE b.playerID = r.playerID AND b.yearID = r.yearID AND b.stint = r.stintID AND b.playerID = ? AND b.yearID >= ? AND b.yearID <= ? AND pa > 0 GROUP BY b.yearID, r.yearID, b.playerID, r.playerID");
    ps.setString(1, card._id);
    ps.setInt(2, 2008);
    ps.setInt(3, 2010);
    HashSet<Integer> years = new HashSet<>();
    double[] tots = new double[Bat.values().length];
    double[] avgs = new double[Bat.values().length];
    int ct = 0;
    try (ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        ++ct;
        years.add(rs.getInt(11));
        double[] tot = new double[Bat.values().length];
        tot[Bat.B_PA.ordinal()] = rs.getInt(1) - rs.getInt(10); // PA
        tot[Bat.B_AB.ordinal()] = rs.getInt(2); // AB
        tot[Bat.B_H.ordinal()] = rs.getInt(3); // H
        tot[Bat.B_AVG.ordinal()] = tot[Bat.B_H.ordinal()]
            / tot[Bat.B_AB.ordinal()] * 1000;
        tot[Bat.B_2B.ordinal()] = rs.getInt(4); // 2B
        tot[Bat.B_3B.ordinal()] = rs.getInt(5); // 3B
        tot[Bat.B_HR.ordinal()] = rs.getInt(6); // HR
        tot[Bat.B_1B.ordinal()] = tot[Bat.B_H.ordinal()]
            - tot[Bat.B_2B.ordinal()] - tot[Bat.B_3B.ordinal()]
            - tot[Bat.B_HR.ordinal()]; // 1B
        tot[Bat.B_RBI.ordinal()] = rs.getInt(7); // RBI
        tot[Bat.B_BB.ordinal()] = (rs.getInt(8) - rs.getInt(10)); // uBB
        tot[Bat.B_SO.ordinal()] = rs.getInt(9); // SO
        for (int i = 0; i != Bat.values().length; ++i) {
          tots[i] += tot[i];
          avgs[i] += tot[i] / tot[Bat.B_PA.ordinal()] * 216;
        }
      }
    }
    System.out.format("%s\t%d", card._name, ct);
    for (double tot : tots) {
      System.out.format("\t%.0f", tot / years.size());
    }
    System.out.format("\n%s\t%d", card._name, ct);
    for (double avg : avgs) {
      System.out.format("\t%.2f", (int)(avg / years.size() * 20) / 20.0);
    }
    System.out.println();
  }

  public static void main(String[] args) throws Exception {
    Iterable<CardLegacy> cards = new CardData().cards();
    try (MyDatabase db = new MyDatabase()) {
      System.out.print("Batter\tYr");
      for (Bat bat : Bat.values()) {
        System.out.format("\t%s", bat.toString().substring(2));
      }
      System.out.println();
      for (CardLegacy card : cards) {
        if (!card.isPitcher()) {
          fetchBat(db, card);
        }
      }
    }
  }

}
