package strat.server;

import strat.client.model.Position;

public class BatterStats extends BasicStats {
  public BatterStats(String id) { super(id); }
  
  public Position _pos = null;
  
  public int _gs_l = 0;
  public int _dh_l = 0;
  public int _gs_r = 0;
  public int _dh_r = 0;
  public int _pa_l = 0;
  public int _pa_r = 0;

  public int _h = 0;
  public int _si = 0;
  public int _do = 0;
  public int _tr = 0;
  public int _hr = 0;
  public int _bb = 0;
  public int _so = 0;
  public int _hb = 0;
  public int _sf = 0;
  public int _fc = 0;
  public int _sb = 0;
  public int _cs = 0;
  public int _dp = 0;
  public int _onE = 0;
  
  public int _r = 0;
  public int _bi = 0;

  public int _pb = 0;
  
  public double _re_lt = 0;
  public double _re_rt = 0;

  public static void printRawHeader() {
    System.out.println("Batting\tPos\tGS-L\tDH-L\tGS-R\tDH-R\tPA-L\tPA-R\tAB\tR\tH\t2B\t3B\tHR\tRBI\tBB\tSO\tHBP\tOnE\tSB\tCS\tSF\tFC\tDP\tA\tE\tPB\tAVG\tOBP\tSLG");
  }
  
  public static void printREHeader() {
    System.out.println("Batting\tPos\tGS-L\tDH-L\tPA-L\tTL\tOL\tBL\tTL650\tOL650\tBL650\tGS-R\tDH-R\tPA-R\tTR\tOR\tBR\tTR650\tOR650\tBR650\tOBP\tSLG\tF162\tPA");
  }
  
  public static void printHeader() {
    System.out.println("Batting\tPos\tGS-L\tDH-L\tGS-R\tDH-R\tPA-L\tPA-R\tAB\tR\tH\t2B\t3B\tHR\tRBI\tBB\tSO\tHBP\tOnE\tSB\tCS\tSF\tFC\tDP\tA\tE\tPB\tAVG\tOBP\tSLG\tRE+lt\tRE+rt\tRE+r\tRE+f\tRE+\tRE650+lt\tRE650+rt\tRE650+r\tRE162+f\tRE650+");
  }
  
  private void printREStats(int seasons, double bat, double run, double field, int pa, int gs, int dh, double posREperG) {
    double pa_perc = pa / (double)(_pa_l + _pa_r);
    double posREVal = gs == dh ? 0 : posREperG * (gs - dh);
    bat = bat + pa * 25.0 / 650;
    run *= pa_perc;
    field = (field * pa_perc) - posREVal;
    System.out.format("\t%d\t%d\t%d\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f\t%.1f",
                      gs, dh, pa,
                      (bat + run + field) / seasons, (bat + run) / seasons, bat / seasons,
                      (bat + run) * 650 / pa + field * 162 / (gs - dh), (bat + run) * 650 / pa, bat * 650 / pa);
  }
  
  public void printRE(int seasons, double posREperG) {
    System.out.format("%s\t%s", _id, _pos == null ? "DH" : _pos.code().toUpperCase());
    printREStats(seasons, _re_lt, _re_run, _re_fld, _pa_l, _gs_l, _dh_l, posREperG);
    printREStats(seasons, _re_rt, _re_run, _re_fld, _pa_r, _gs_r, _dh_r, posREperG);
    
    int ab = (_pa_l + _pa_r - _bb - _sf);
    int gs = _gs_l + _gs_r; int pa = _pa_l + _pa_r; int dh = _dh_l + _dh_r;
    double posREVal = gs == dh ? 0 : posREperG * (gs - dh);
    System.out.format("\t%.3f\t%.3f\t%.1f%d\n",
        _h / (double)ab, (_h + _bb + _hb) / (double)pa, (_si + _do*2 + _tr*3 + _hr*4) / (double)ab,
        (_re_fld - posREVal) * 162 / (gs - dh), pa);
  }
  
  public void print(int seasons, double posREperG) {
    int ab = (_pa_l + _pa_r - _bb - _sf);
    int gs = _gs_l + _gs_r; int pa = _pa_l + _pa_r; int dh = _dh_l + _dh_r;
    double posREVal = gs == dh ? 0 : posREperG * (gs - dh);
    double re_tot = _re_lt + _re_rt + _re_run + _re_fld - posREVal;
    System.out.format("%s\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n",
        _id, _pos == null ? "DH" : _pos.code().toUpperCase(), _gs_l / seasons, _dh_l / seasons, _gs_r / seasons, _dh_r / seasons, _pa_l / seasons, _pa_r / seasons, ab / seasons,
        _r / seasons, _h / seasons, _do / seasons, _tr / seasons, _hr / seasons, _bi / seasons,
        _bb / seasons, _so / seasons, _hb / seasons, _onE / seasons, _sb / seasons, _cs / seasons, _sf / seasons, _fc / seasons, _dp / seasons,
        _assist / seasons, _e / seasons, _pb / seasons,
        _h / (double)ab, (_h + _bb + _hb) / (double)pa, (_si + _do*2 + _tr*3 + _hr*4) / (double)ab,
        _re_lt / seasons, _re_rt / seasons, _re_run / seasons, (_re_fld - posREVal) / seasons, re_tot / seasons,
        _re_lt * 650 / _pa_l, _re_rt * 650 / _pa_r, _re_run * 650 / pa, (_re_fld - posREVal) * 162 / (gs - dh), re_tot * 650 / pa /*seasons cancel out*/);
  }
}
