package strat.sim;

import strat.client.model.GameState;
import strat.client.model.PlayLog;
import strat.client.model.RandomBase;
import strat.client.model.RunExpChart;
import strat.client.model.RunLog;
import strat.server.RandomServer;
import strat.shared.BaseState;

public class Simulator {
  private boolean takeExtraBase(RunLog rl) {
    _re.setRE(rl._bases); double re_hold = rl._bases._runexp;    
    
    BaseState bases_safe = new BaseState(rl._bases); int runs = bases_safe.advanceForced(rl._base, null); _re.setRE(bases_safe);
    double re_safe = bases_safe._runexp + runs - re_hold;

    BaseState bases_out = new BaseState(rl._bases); bases_out.out(rl._base); _re.setRE(bases_out);
    double re_out = bases_out._runexp - re_hold;

    BaseState bases_extra = new BaseState(bases_safe); runs += bases_extra.advance(1, null); _re.setRE(bases_extra);
    double re_extra = bases_extra._runexp + runs - re_safe - re_hold;
    
    double re_tot = (re_safe * rl._safe_pct + re_extra * rl._extra_pct + re_out * rl._out_pct) / (rl._safe_pct + rl._out_pct);
    return re_tot > 0; 
  }

  public void playGame(GameState gs, Scorer sc, Manager mgr) {
    sc.rosters(gs._vis, gs._home);
    while (!gs.gameOver()) {
      if (mgr.replacePitcher(gs)) {
        sc.relieve(gs, gs.defense()._pitcher);
      }
      PlayLog log = gs.canSteal(_rand, true); // on good lead
      boolean runner_went = false;
      if (log != null && takeExtraBase(log._runner)) {
        if (gs.advance(log, true)) { // needs lead
          runner_went = true;
        } else if (log._runner._base == 0) {
          log = gs.canSteal(_rand, false); // on bad lead
          if (log != null && takeExtraBase(log._runner)) {
            runner_went = gs.advance(log, false); // doesn't need lead on second try
          }
        }
      }
      if (!runner_went || log == null) {
        log = gs.bat(_rand);
        if (log._runner != null && takeExtraBase(log._runner)) { gs.advance(log, false); } // no lead necessary
      }
      sc.play(gs, log);
    }
  }
  
  public Simulator(RunExpChart re) {
    _re = re;
  }
  
  private final RandomBase _rand = new RandomServer(12345);
  private final RunExpChart _re;
}
