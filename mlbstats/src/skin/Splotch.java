package skin;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

public class Splotch implements Comparable<Splotch> {

  private static class Pixel {
    public Pixel(int x, int y, int rgb) {
      _x = x; _y = y;
      _r = (rgb >> 16) & 0xFF;
      _g = (rgb >> 8) & 0xFF;
      _b = rgb & 0xFF;
    }
    public int _x;
    public int _y;
    public int _r;
    public int _g;
    public int _b;
  }
  
  public Splotch(int i, Pixel pix) { _i = i; _pix.add(pix); _r = pix._r; _g = pix._g; _b = pix._b; }
  
  public final int _i;
  public double _r;
  public double _g;
  public double _b;
  public ArrayList<Pixel> _pix = new ArrayList<>();
  public HashSet<Splotch> _next = new HashSet<>();
  
  public double diff(Splotch g) {
    return Math.abs(_r - g._r) + Math.abs(_g - g._g) + Math.abs(_b - g._b);
  }
  
  @Override public int compareTo(Splotch g) {
    int cmp = g._pix.size() - _pix.size();
    if (cmp != 0) { return cmp; }
    return g._i - _i;
  }
  
  public void paint(BufferedImage original, int rgb) {
    for (Pixel p : _pix) { original.setRGB(p._x, p._y, rgb); }
  }
  
  public void consume(Splotch s) {
    _r = (s._r * s._pix.size() + _r * _pix.size()) / (s._pix.size() + _pix.size());
    _g = (s._g * s._pix.size() + _g * _pix.size()) / (s._pix.size() + _pix.size());
    _b = (s._b * s._pix.size() + _b * _pix.size()) / (s._pix.size() + _pix.size());
    _pix.addAll(s._pix);
    for (Splotch n : s._next) {
      if (n._i != _i) { n._next.add(this); _next.add(n); n._next.remove(s); }
    }
  }
  
  public static class Pic implements Iterable<Splotch> {
  
    public ArrayList<Splotch> sorted() {
      ArrayList<Splotch> results = new ArrayList<>(_all);
      Collections.sort(results);
      return results;
    }
    
    public void merge(double within) { // FIXME why does changing this from double to int change the results dramatically?
      for (Iterator<Splotch> i_g = _all.iterator(); i_g.hasNext(); ) {
        Splotch g = i_g.next();
        for (Splotch n : g._next) { 
          if (g._i > n._i) { continue; } // only do it where g is before n
          if (g.diff(n) < within) {
            n.consume(g);
            i_g.remove(); // everything belongs to i_n now
            break;
          }
        }
      }
    }
    
    @Override public Iterator<Splotch> iterator() { return _all.iterator(); }
    
    public int size() { return _all.size(); }
    
    public Pic(BufferedImage original) {
      _wd = original.getWidth();
      _ht = original.getHeight();
      Splotch[][] array = new Splotch[_wd][_ht];
      for (int i_w = 0; i_w != _wd; ++i_w) {
        for (int i_h = 0; i_h != _ht; ++i_h) {
          array[i_w][i_h] = new Splotch(i_w * _ht + i_h, new Pixel(i_w, i_h, original.getRGB(i_w, i_h)));
        }
      }
      for (int i_w = 0; i_w != _wd; ++i_w) {
        for (int i_h = 0; i_h != _ht; ++i_h) {
          if (i_w != 0) { array[i_w-1][i_h]._next.add(array[i_w][i_h]); array[i_w][i_h]._next.add(array[i_w-1][i_h]); }
          if (i_h != 0) { array[i_w][i_h-1]._next.add(array[i_w][i_h]); array[i_w][i_h]._next.add(array[i_w][i_h-1]); }
          _all.add(array[i_w][i_h]);
        }
      }
      
    }
    
    private final int _wd;
    private final int _ht;
    private ArrayList<Splotch> _all = new ArrayList<>();
  }
}
