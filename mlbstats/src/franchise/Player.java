package franchise;

import util.ByPlayer;
import data.Appearances;
import data.Master;
import data.War;

public class Player {
  public Player(Master master, ByPlayer<War> allwar, ByPlayer<Appearances> allapp, int season) {
    _master = master; _allwar = allwar; _allapp = allapp; _season = season;
  }
  
  public void reset() { _wProj = _pProj = _wProjRate = _wNow = _pNow = _wAgeRate = _pAge = 0; }
  
  public double ptProject() { double val = _pProj * _pAge; return val > 1 ? 1 : val; }
  
  public double nowScore() {
    return _pProj == 0 ? 0 : _wProjRate * _wAgeRate * Math.sqrt(ptProject());
  }
  
  public double accum() {
    return _wProjRate * _wAgeRate;
  }
  
  public final Master _master;
  public final ByPlayer<War> _allwar;
  public final ByPlayer<Appearances> _allapp;
  public final int _season;
  
  public double _wProjRate = 0;
  public double _wProj = 0;
  public double _pProj = 0;
  public double _wAgeRate = 0;
  public double _pAge = 0;
  public double _wNow = 0;
  public double _pNow = 0;
}
