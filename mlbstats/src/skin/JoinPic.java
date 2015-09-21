package skin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import javax.imageio.ImageIO;
import util.MyDatabase;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

/*
 * Weight pixel based on distance from center (1 - sqrt(dx^2 + dy^2)/z)
 * Loop through all the images
 * Sort by averaged Value of top-weighted color
 * Output HTML table : Name+id, Original jpg, Detected jpg, colors 1-10 (bg+rgb+h+s+v)
 * Any way to rule out blocks based on color? Worst case: Mathews=#3
 */
public class JoinPic {
  private static final String CACHE = "C:/build/mlbstats/pics";

  private static class Player implements Comparable<Player> {
    public Player(Master master, int splotch) { _master = master; _spl_id = splotch; }
    
    public String id() { return _master.playerID(); }
/*
    private static float[] hsv(int rgb) {
      float[] hsv = new float[3];
      int red   = (rgb & 0x00ff0000) >> 16;
      int green = (rgb & 0x0000ff00) >> 8;
      int blue  =  rgb & 0x000000ff;
      Color.RGBtoHSB(red, green, blue, hsv);
      return hsv;
    }
*/
    public void add(Splotch s) {
      if (_splotches.size() == _spl_id) {
        float[] hsv = new float[3];
        Color.RGBtoHSB((int)s._r, (int)s._g, (int)s._b, hsv);
//        _hue = hsv[0];
//        _sat = hsv[1];
        _val = hsv[2];
      }
      _splotches.add(s);
    }

    Master _master;
//    BufferedImage _image;
    int _spl_id = 0;
//    double _hue = 0;
//    double _sat = 0;
    double _val = 0;

    ArrayList<Splotch> _splotches = new ArrayList<>();
    
    @Override public int compareTo(Player arg0) {
      return _val > arg0._val ? -1 : 1;
    }
  }
    
/*
  private static void copyImage(BufferedImage from, BufferedImage to) {
    int e_w = from.getWidth();
    int e_h = from.getHeight();
    for (int i_w = 0; i_w != e_w; ++i_w) {
      for (int i_h = 0; i_h != e_h; ++i_h) {
        to.setRGB(i_w, i_h, from.getRGB(i_w, i_h));
      }
    }
  }
*/
  
  private static final int[] COLORS = { 0x0000FF, 0x00FFFF, 0x00FF00, 0xFFFF00, 0xFF0000, 0xFF00FF, 0x7F007F, 0x00007F, 0x007F7F, 0x007F00 };
  private static Master.Table _mt = null;
  
  public static void main(String[] args) throws Exception {
    War.Table wt = null;
    try (MyDatabase db = new MyDatabase()) {
      _mt = new Master.Table(db, Sort.UNSORTED);
      wt = new War.Table(db, Type.BAT);
    }
    Cache cache = new Cache(CACHE + "/orig", _mt, wt);
    ArrayList<Player> players = new ArrayList<>();
    for (War w : wt) {
      try {
        if (w.teamID().equals("BAL") && w.yearID() == 1997) {
          Player p = new Player(_mt.byID(w.playerID()), 0);
          BufferedImage image = cache.fetch(p.id());
          Splotch.Pic pic = new Splotch.Pic(image);
          System.out.format("%s (%s %s)\n", p.id(), p._master.nameFirst(), p._master.nameLast());
          for (int i_calc = 0; i_calc != 8; ++i_calc) {
            pic.merge(i_calc*5);
          }
          ArrayList<Splotch> results = pic.sorted();
          int i = 0;
          for (Splotch s : results) {
            if (s._r < s._g || s._g < s._b || s._r < 1.3 * s._b) { continue; }
            if (s._r < 60) { continue; }
            if (s._r - s._g > 90 && s._r > s._g * 2.0) { continue; }
            
            s.paint(image, COLORS[i]);
            p.add(s);
            if (++i == COLORS.length) { break; } 
          }
          ImageIO.write(image, "jpg", new File(CACHE + "/join", p.id() + ".jpg"));
          players.add(p);
        }
      } catch (Exception e) { System.err.println(e); }
    }
    Collections.sort(players);
    try (PrintWriter pw = new PrintWriter(new FileWriter(CACHE + "/main.html"))) {
      pw.println("<table>");
      for (int i = 0; i != players.size(); ++i) {
        if (i % 5 == 0) {
          pw.print("<tr><th>Player</th><th>Original</th><th>Joined</th>");
          for (int j = 0; j != COLORS.length; ++j) {
            String fg = (j == 1 || j == 3 || j == 5) ? "black" : "white";
            pw.format("<th style='color:%s; background-color:#%06X'>Color #%d</th>", fg, COLORS[j], j+1);
          }
          pw.println();
        }
        Player p = players.get(i);
        pw.format("<tr><td>%s %s<br>%s</td>", p._master.nameFirst(), p._master.nameLast(), p.id());
        pw.format("<td><img src='orig/%1$s.jpg'/></td><td><img src='join/%1$s.jpg'/></td>", p.id());
        float[] hsv = new float[3];
        for (Splotch s : p._splotches) { 
          int r = (int)s._r, g = (int)s._g, b = (int)s._b;
          String fg = (r + g + b > 384) ? "black" : "white";
          pw.format("<td style='color:%4$s; background-color:#%1$02x%2$02x%3$02x'>rgb&nbsp;%1$03d&nbsp;%2$03d&nbsp;%3$03d<br>", r, g, b, fg);
          Color.RGBtoHSB(r, g, b, hsv);
          pw.format("hsv&nbsp;%03d&nbsp;%03d&nbsp;%03d<br>", (int)(hsv[0] * 1000), (int)(hsv[1] * 1000), (int)(hsv[2] * 1000));
          pw.format("%d</td>", s._pix.size());
        }
        pw.println("</tr>");
      }
      pw.println("</table>");
      pw.flush();
    }
/*    
    File in_file = new File(CACHE, "mathete01.jpg");
    File out_file = new File(CACHE_JOIN, "mathete01.jpg");
    BufferedImage image = ImageIO.read(in_file);
    
    Splotch.Pic pic = new Splotch.Pic(image);
    System.out.println("I : " + pic.size());
    for (int i_calc = 0; i_calc != 12; ++i_calc) {
      pic.merge(i_calc*5);
    }
    ArrayList<Splotch> results = pic.sorted();
    for (int i = 0; i != COLORS.length; ++i) {
      results.get(i).paint(image, COLORS[i]);
    }
    ImageIO.write(image, "jpg", out_file);
*/
  }
}

