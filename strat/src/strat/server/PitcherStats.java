package strat.server;

public class PitcherStats extends BasicStats {
  public PitcherStats(String id) { super(id); }
  
  public int _w = 0;
  public int _l = 0;
  public int _g = 0;
  public int _gs = 0;

  public int _bf_lt = 0;
  public int _bf_rt = 0;

  public int _ip3 = 0;
  public int _h = 0;
  public int _r = 0;
  public int _hr = 0;
  public int _bb = 0;
  public int _so = 0;  
  public int _hb = 0;
  public int _wp = 0;
  public int _bk = 0;
  
  public double _re_lt = 0;
  public double _re_rt = 0;

  public static void printHeader() {
    System.out.println("Pitching\tW\tL\tG\tGS\tBF-r\tBF-l\tIP\tR\tH\tHR\tBB\tSO\tHB\tWP\tBK\tRA9\tWHIP\tRE+lt\tRE+rt\tRE+r\tRE+f\tRE+\tRE1k+lt\tRE1k+rt\tRE1k+r\tRE1k+f\tRE1k+");
  }
  
  public void print(int seasons, double posREperG) {
    double posREVal = posREperG * _gs;
    double re_tot = _re_lt + _re_rt + _re_run + _re_fld - posREVal;
    int bf = _bf_lt + _bf_rt;
    System.out.format("%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d.%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%.2f\t%.2f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n",
        _id, _w / seasons, _l / seasons, _g / seasons, _gs / seasons, _bf_rt / seasons, _bf_lt / seasons, _ip3 / seasons / 3, _ip3 / seasons % 3,
        _r / seasons, _h / seasons, _hr / seasons, _bb / seasons, _so / seasons, _hb / seasons, _wp / seasons, _bk / seasons,
        _r * 27.0 / _ip3, (_h + _bb) * 3.0 / _ip3,
        _re_lt / seasons, _re_rt / seasons, _re_run / seasons, (_re_fld - posREVal) / seasons, re_tot / seasons,
        _re_lt * 1000 / _bf_lt, _re_rt * 1000 / _bf_rt, _re_run * 1000 / bf, (_re_fld - posREVal) * 1000 / bf, re_tot * 1000 / bf  /*seasons cancel out*/);
  }
}
