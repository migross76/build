package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import data.Appearances;
import data.Position;
import data.Type;
import data.War;

public class DH {
  private static class Player {
    public final War _war;
    public final Appearances _app;
    public final double _drar;
    
    public static double drar(War w) {
      return (w.rPos() + w.rField()) * 700 / w.playtime();
    }
    
    public Player(War w, Appearances a) {
      _war = w;
      _app = a;
      _drar = drar(w);
    }
    
    @Override public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(String.format("%s\t%d\t%.1f\t%d\t%.0f\t%.0f\t%.1f\t",
          _war.playerID(), _war.yearID(), _war.war(), _war.playtime(), _war.rPos(), _war.rField(), _drar));
      for (Appearances.Use u : _app) {
        if (u.games() > 81) { sb.append("*"); }
        else if (u.games() < 10) { sb.append("/"); }
        sb.append(u.pos().getShort());
      }
      return sb.toString();
    }
  }
  
  private static Appearances findApp(Appearances.ByID at, War w) {
    ByPlayer<Appearances> byA = at.get(w.playerID());
    if (byA != null) {
      for (Appearances a : byA) {
        if (a.primary() == null) { continue; }
        if (a.yearID() == w.yearID()) { return a; }
      }
    }
    return null;
  }
  
  private static void evalDHs(War.Table wt, Appearances.ByID at) throws Exception {
    ArrayList<Player> dhs = new ArrayList<>();
    for (War w : wt) {
      if (w.war() < 2) { continue; }
      Appearances a = findApp(at, w);
      if (a != null && a.primary().pos() == Position.DESIG) {
        dhs.add(new Player(w, a));
      }
    }
    Collections.sort(dhs, new Comparator<Player>() {
      @Override public int compare(Player w0, Player w1) {
        int cmp = w1._war.yearID() - w0._war.yearID();
        if (cmp != 0) { return cmp; }
        double dcmp = w1._war.war() - w0._war.war();
        if (dcmp != 0) { return dcmp < 0 ? -1 : 1; }
        return w0._war.playerID().compareTo(w1._war.playerID());
      }
    });
    for (Player p : dhs) { System.out.println(p); }
  }
  
  private static void findDHs(War.Table wt, Appearances.ByID at) throws Exception {
    ArrayList<Player> dhs = new ArrayList<>();
    for (War w : wt) {
      if (w.war() < 4 || Player.drar(w) > -15) { continue; }
      Appearances a = findApp(at, w);
      if (a != null) { dhs.add(new Player(w, a)); }
    }
    Collections.sort(dhs, new Comparator<Player>() {
      @Override public int compare(Player w0, Player w1) {
        double dcmp = w1._war.war() - w0._war.war();
        if (dcmp != 0) { return dcmp < 0 ? -1 : 1; }
        return w0._war.playerID().compareTo(w1._war.playerID());
      }
    });
    for (Player p : dhs) { System.out.println(p); }
  }
  
  public static void main(String[] args) throws Exception {
    War.Table wt = null;
    Appearances.ByID byA = new Appearances.ByID();
    try (MyDatabase db = new MyDatabase()) {
      wt = new War.Table(db, Type.BAT);
      byA.addAll(new Appearances.Table(db));
    }
    evalDHs(wt, byA);
    System.out.println();
    findDHs(wt, byA);
  }
}
