package skin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import javax.imageio.ImageIO;
import util.MyDatabase;
import data.Master;
import data.Sort;
import data.Type;
import data.War;

/* Try to detect skin color based on portraits from Baseball Gauge
 * Inspired by http://thegamedesigner.blogspot.com/2013/02/the-skin-color-project.html
 * 
 * Steps:
 * - Detect skin pixels (high-precision, but hopefully not biased)
 *   - Red hue (not yellow or magenta)?  Not bright red
 *   - Ratio of red/green/blue?
 *   - Trim outside (0.25-0.75W, 0.4-0.8H)
 * - Combine into one skin color (mean/median/cluster, "value" of HSV)
 * - Order by color
 * - Create new image: original, skin detect, color
 *   - Debug: HSV, RGB, ratios?
 */
public class AnalyzePic {
  private static Master.Table _mt = null;
  private static final String _cache = "C:/build/mlbstats/pics/";
  
  private static class Player implements Comparable<Player> {
    public Player(Master master) { _master = master; }
    
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
    public void compute() {
      int w = _original.getWidth();
      int h = _original.getHeight();
      float[] hsv = new float[3];
      for (int i_w = (int)(w * 0.25); i_w != (int)(w*0.75); ++i_w) {
        for (int i_h = (int)(h*0.4); i_h != (int)(h*0.8); ++i_h) {
          int clr =  _original.getRGB(i_w, i_h); 
          int red   = (clr & 0x00ff0000) >> 16;
          int green = (clr & 0x0000ff00) >> 8;
          int blue  =  clr & 0x000000ff;
          Color.RGBtoHSB(red, green, blue, hsv);
          _red += red; _grn += green; _blu += blue; ++_tot;
          _hue += hsv[0]; _sat += hsv[1]; _val += hsv[2];
//          System.out.format("RGB[%2d,%3d] : %02X%02X%02X : rg=%.2f rb=%.2f gb=%.2f : h=%.2f s=%.2f v=%.2f\n", i_w, i_h, red, green, blue, red/(double)green, red/(double)blue, green/(double)blue, hsv[0], hsv[1], hsv[2]);
        }
      }
      _red /= _tot; _grn /= _tot; _blu /= _tot;
      _hue /= _tot; _sat /= _tot; _val /= _tot;
      //Color.RGBtoHSB(r_tot, g_tot, b_tot, hsv);
    }
    
    /*
     * r:g 
     * 256:0 == 384:128 == 3/1
     * 2:0 == 3:1
     * 128:128 == 256:256 == 1/1
     * 0:256 == 128:384 == 1/3
     */
    
    private static int grayscale(int val) {
      return (val * 256 + val) * 256 + val;
    }
    
    private static int ratio(int one, int two) {
      if (one + two == 0) { return 0; }
      return one * 256 / (one + two);
    }
    
    public void draw(BufferedImage img) {
      int w = _original.getWidth();
      int h = _original.getHeight();
      for (int b_w = (int)(w * 0.25), e_w = (int)(w*0.75), i_w = b_w; i_w != e_w; ++i_w) {
        for (int b_h = (int)(h*0.4), e_h = (int)(h*0.8), i_h = b_h; i_h != e_h; ++i_h) {
          int rgb = _original.getRGB(i_w, i_h);
          //int rgb2 = rgb;
          int red = (rgb & 0x00ff0000) >> 16;
          int green = (rgb & 0x0000ff00) >> 8;
          int blue  =  rgb & 0x000000ff;
          //int sat = (int)(hsv(rgb)[1] * 256);
          //int val = (int)(hsv(rgb)[2] * 256);
          int r2g = ratio(red, green);
          //if (r2g < 78 || r2g > 140) { rgb2 = 0x0000FF; }
          int r2b = ratio(red, blue);
          //if (r2b < 81 || r2b > 158) { rgb2 = 0; }
          int g2b = ratio(green, blue);
          //if (g2b < 67 || g2b > 100) { rgb2 = 0; }
          //if (val < 50 || val > 230) { rgb2 = 0xFF0000; }
          img.setRGB(i_w - b_w, i_h - b_h, rgb);
          img.setRGB(e_w - b_w + i_w - b_w, i_h - b_h, grayscale(r2g));
          img.setRGB(i_w - b_w, e_h - b_h + i_h - b_h, grayscale(r2b));
          img.setRGB(e_w - b_w + i_w - b_w, e_h - b_h + i_h - b_h, grayscale(g2b));
        }
      }
    }
    
    public void print() {
      System.out.format("%.0f\t%.0f\t%.0f\t%.2f\t%.2f\t%.2f\t%s\t%s %s\n", _red, _grn, _blu, _hue, _sat, _val, id(), _master.nameFirst(), _master.nameLast());
    }

    Master _master;
    BufferedImage _original;
    // BufferedImage _analyze;
    double _hue = 0;
    double _sat = 0;
    double _val = 0;
    double _red = 0;
    double _grn = 0;
    double _blu = 0;
    
    double _tot = 0;

    @Override public int compareTo(Player arg0) {
      return _val > arg0._val ? -1 : 1;
    }
  }
  
  public static void main(String[] args) throws Exception {
    War.Table wt = null;
    try (MyDatabase db = new MyDatabase()) {
      _mt = new Master.Table(db, Sort.UNSORTED);
      wt = new War.Table(db, Type.BAT);
    }
    Cache cache = new Cache(_cache, _mt, wt);
    ArrayList<Player> players = new ArrayList<>();
    for (War w : wt) {
      try {
        if (w.teamID().equals("BAL") && w.yearID() == 1997) {
          Player p = new Player(_mt.byID(w.playerID()));
          p._original = cache.fetch(p.id());
          p.compute();
          players.add(p);
        }
      } catch (Exception e) { System.err.println(e); }
    }
    System.out.println("R\tG\tB\tH\tS\tV\tID\tName");
    Collections.sort(players);
    for (Player p : players) { p.print(); }
    BufferedImage image = new BufferedImage(1000, 600, BufferedImage.TYPE_INT_RGB);
    int i = 0;
    for (Player p : players) {
      System.out.println((i % 10) * 100 + " : " + i / 4 * 150);
      p.draw(image.getSubimage((i % 10) * 100, (int)Math.floor(i / 10) * 150, 100, 150));
      ++i;
    }
    ImageIO.write(image, "jpg", new File(_cache, "total.jpg"));
  }
}
