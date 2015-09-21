package strat.client.model;

import strat.shared.BaseState;

public class GameState {
  public boolean _hold_runner = true;
  
  public Team _vis = new Team();
  public Team _home = new Team();
  public ParkInfo _park = null;
  public final FieldChart _fielding;
  public final RunExpChart _runexp;

  private int _inning = 1;
  private boolean _top = true;
  private boolean _leadoff = true;
  private BaseState _bases = new BaseState();
  
  public GameState(RunExpChart runexp, FieldChart fielding) { _runexp = runexp; _fielding = fielding; }
  
  public int inning() { return _inning; }
  public boolean isLeadoff() { return _leadoff; }
  
  public boolean gameOver() {
    if (_inning < 9 || _vis._runs == _home._runs) { return false; }
    if (!_top && _home._runs > _vis._runs) { return true; }
    return _inning > 9 && _top && _leadoff && _vis._runs != _home._runs;
  }
  public Team offense() { return _top ? _vis : _home; }
  public Team defense() { return _top ? _home : _vis; }
  
  public PlayLog canSteal(RandomBase rand, boolean onGoodLead) {
    RunLog rl = null;
    if (_bases._onbase[1] != null && _bases._onbase[2] == null) { // steal third
      rl = new RunLog(_bases, 1, RunLog.Cause.STEAL);
    } else if (_bases._onbase[0] != null && _bases._onbase[1] == null) { // steal second
      rl = new RunLog(_bases, 0, RunLog.Cause.STEAL);
    } else { return null; }
    rl._bases.set(_bases);
    Player catcher = defense()._fielders.get(Position.CATCH);
    Pitcher pitcher = defense()._pitcher;
    rl._safe_max = pitcher._onfield._hold + catcher._onfield._arm;
    if (rl._safe_max > 5) { rl._safe_max = 5; } else if (rl._safe_max < -5) { rl._safe_max = -5; }
    if (rl._base == 0 && onGoodLead) { // steal second with a good lead
      rl._safe_max += rl._runner._run._primary;
    } else { 
      rl._safe_max += rl._runner._run._second;
    }
    if (_hold_runner) { rl._safe_max -= rl._base == 0 ? 2 : 4; }
    if (rl._safe_max > 19 && _hold_runner) { rl._safe_max = 19; }
    else if (rl._safe_max < 1) { rl._safe_max = 1; }
    rl._err_max = Math.min(rl._safe_max, 3);
    int good_lead = 0, picked_off = 0, bad_lead = 0;
    for (int i = 0; i != rl._runner._run._lead.length; ++i) {
      int wt = i > 5 ? 11 - i : i + 1;
      switch (rl._runner._run._lead[i]) {
        case GOOD: good_lead += wt; break;
        case OUT: picked_off += wt; break;
        case BAD: bad_lead += wt; break;
      }
    }
    if (!onGoodLead) { good_lead += bad_lead; bad_lead = 0; }
    rl._out_pct = picked_off / 36.0 + good_lead / 36.0 * (1 - rl._safe_max / 20.0);
    rl._hold_pct = bad_lead / 36.0;
    rl._safe_pct = good_lead / 36.0 * rl._safe_max / 20.0;
    rl._extra_pct = good_lead / 36.0 * rl._err_max / 20.0 * catcher._onfield._throw / 20.0;
    
    PlayLog pl = create(rand);
    pl._runner = rl;
    pl._e_bases.set(pl._s_bases);
    pl._fielder = catcher;
    return pl;
  }
  
  /** return true if tried to advance; false if didn't obtain good lead */ 
  public boolean advance(PlayLog log, boolean needsLead) {
    RunLog run = log._runner;
    if (run._cause == RunLog.Cause.STEAL) {
      int leadVal = log._dice.roll(6, 6);
      Running.Lead lead = run._runner._run._lead[leadVal-2];
      switch (lead) {
        case OUT:
          run._type = RunLog.Type.OUT;
          _bases.out(run._base);
          log._special = PlayLog.Special.PICK;
          _runexp.setRE(_bases);
          run._bases.set(_bases);
          checkEndOfInning();
          return true;
        case BAD: if (needsLead) { return false; } break;
        case GOOD: break;
      }
    }
    int roll = log._dice.roll(20);
    if (run._safe_max >= roll) {
      run._type = RunLog.Type.SAFE;
      int runs = _bases.advanceForced(run._base, null);
      if (runs != 0) {
        offense()._runs += runs;
        run._scored = true;
        if (run._cause == RunLog.Cause.OUT) { log._special = PlayLog.Special.SF; }
      }
      if (run._cause == RunLog.Cause.STEAL) { log._special = PlayLog.Special.SB; }
    } else if (run._hold_max < roll) {
      run._type = RunLog.Type.OUT;
      _bases.out(run._base);
      if (run._cause == RunLog.Cause.STEAL) { log._special = PlayLog.Special.CS; }
    }
    _runexp.setRE(_bases);
    run._bases.set(_bases);
    if (defense()._fatigue != null) { defense()._fatigue.collect(log); }
    checkEndOfInning();
/*
    if (run._cause == RunLog.Cause.STEAL) {
      System.err.format("%s %s (%d/%d)\t", run._runner._nameFirst, run._runner._nameLast, run._runner._run._primary, run._runner._run._second);
      System.err.format("%s %s (%d T-%d)\t", log._fielder._nameFirst, log._fielder._nameLast, log._fielder._onfield._arm, log._fielder._onfield._throw);
      System.err.format("%s %s (%d)\t", log._pitcher._nameFirst, log._pitcher._nameLast, log._pitcher._onfield._hold);
      System.err.format("base %d\t%d/20 %c\t", run._base+1, run._safe_max, needsLead ? '+' : '-');
      System.err.format("S=%.2f\tE=%.2f\tO=%.2f\t%s\t%.3f\n", run._safe_pct, run._extra_pct, run._out_pct, run._type, run._bases._runexp - log._e_bases._runexp + (run._scored ? 1 : 0));
    }
*/
    return true;
  }
  
  private PlayLog create(RandomBase rand) {
    PlayLog log = new PlayLog();
    Team off = offense();
    Team def = defense();
    log._pitcher = def._pitcher;
    log._batter = off._lineup[off._atBat % 9];
    if (log._batter.isWeak(log._pitcher._pitches)) { log._toggles.add(TogglePlay.Type.NHR); }
    log._bat_pos = log._batter._onfield == null ? Position.DH : log._batter._onfield._pos;
    log._inning = _inning;
    log._top = _top;
    log._atBat = off._atBat;
    log._leadoff = _leadoff;
    _runexp.setRE(_bases);
    log._s_bases.set(_bases);
    log._dice = new Dice(rand);
    return log;
  }

  public PlayLog bat(RandomBase rand) {
    boolean atbat = true; // was the at-bat completed?
    Team off = offense();
    Team def = defense();
    PlayLog log = create(rand);

    if (_bases.onbase()) {
      int val = log._dice.roll(20);
      if (val == 1) { // Wild pitch?
        if (log._pitcher._wp > log._dice.roll(20)) { log._special = PlayLog.Special.WP; atbat = false; } 
      } else if (val == 2) { // Balk/PB?
        val = log._dice.roll(6);
        if (val < 3) { // Balk?
          if (log._pitcher._bk > log._dice.roll(20)) { log._special = PlayLog.Special.BK; atbat = false; } 
        } else { // Passed Ball?
          Player p = def._fielders.get(Position.CATCH);
          if (p._onfield._pb > log._dice.roll(20)) { log._special = PlayLog.Special.PB; log._fielder = p; atbat = false; } 
        }
      }
    }
    if (log._special != null) {
      _bases.advance(1, log._scored);
    } else {
      Batter b = off._lineup[off._atBat % 9];
      Pitcher p = def._pitcher;
      int col = log._dice.roll(6) - 1;
      log._is_main_pitcher_card = col > 2;
      ColumnPlay cp = log._is_main_pitcher_card ? p.selectColumns(b.handed())[col-3] : b.selectColumns(p.handed())[col];
      log._main_play = cp.calculate(log._dice, log._toggles);
      if (log._main_play._fielder != null) {
        log._fielder = def._fielders.get(log._main_play._fielder);
      }
      Position fieldPos = log._main_play._fielder;
      if (log._main_play._type == Play.Type.FIELD) {
        Fielding fld = log._fielder._onfield;
        log._range_play = _fielding.calcRange(fld, log._dice);
        log._error_play = _fielding.calcError(fld, log._dice);
        fieldPos = fld._pos;
        if (_bases.onbase()) {
          switch (log._range_play._name) {
            case "W/S" : case "W/G" : log._special = PlayLog.Special.WP; break;
            case "P/F" : case "P/P" : if (log._pitcher._wp > log._dice.roll(20)) { log._special = PlayLog.Special.PB; atbat = false; } break; // misplayed a "wild" pitcher
          }
        }
      }
      SimplePlay play = log._range_play == null ? log._main_play : log._range_play._play;
      if (log._special != null) {
        _bases.advance(1, log._scored);
      } else {
        switch (play._type) {
          case WALK: case HBP:
            _bases.advanceForced(0, log._scored); _bases._onbase[0] = log._batter; break;
          case SINGLE:
            int bases = play._flags.contains(Play.Flag.TWO_BASE) ? 2 : 1;
            _bases.advance(bases, log._scored); _bases._onbase[0] = log._batter;
            int lead = _bases.lead();
            if (log._fielder != null && lead > 0) {
              Batter runner = _bases._onbase[lead];
              int chance = runner._run._advance + log._fielder._onfield._arm - 1;
              if (_bases._outs == 2) { chance += 2; }
              if (lead == 1) { // second, going to third
                if (play._fielder == Position.LEFT) { chance -= 2; }
                else if (play._fielder == Position.RIGHT) { chance += 2; }
              }
              if (chance < 1) { chance = 1; } else if (chance > 19) { chance = 19; }
              log._runner = new RunLog(_bases, lead, RunLog.Cause.HIT);
              log._runner._safe_max = chance;
              log._runner._safe_pct = chance / 20.0;
              log._runner._out_pct = 1 - log._runner._safe_pct;
            }
            break;
          case DOUBLE:
            _bases.advance(2, log._scored); _bases._onbase[1] = log._batter; break;
          case TRIPLE:
            _bases.advance(3, log._scored); _bases._onbase[2] = log._batter; break;
          case HOMERUN:
            _bases.advance(4, log._scored); log._scored.add(log._batter); break;
          case OUT_LONG:
            if (log._error_play > 0) { break; }
            if (fieldPos.isOF()) { if (_bases._outs < 2) { _bases.advance(1, log._scored); if (!log._scored.isEmpty()) { log._special = PlayLog.Special.SF; } } }
            else if (_bases._onbase[0] != null && _bases._outs < 2) {
              _bases._onbase[0] = null; ++_bases._outs; log._special = PlayLog.Special.DP; if (_bases._outs < 2) { _bases.advance(1, log._scored); }
            }
            break;
          case OUT_MEDIUM:
            if (log._error_play > 0) { break; }
            if (fieldPos.isOF()) {
              if (_bases._outs < 2) {
                if (_bases._onbase[2] != null) { log._scored.add(_bases._onbase[2]); _bases._onbase[2] = null; log._special = PlayLog.Special.SF; }
                if (_bases._onbase[1] != null && fieldPos == Position.RIGHT && log._range_play == null) {
                  int chance = _bases._onbase[1]._run._advance + log._fielder._onfield._arm + 2;
                  if (chance < 1) { chance = 1; } else if (chance > 19) { chance = 19; }
                  log._runner = new RunLog(_bases, 1, RunLog.Cause.OUT);
                  log._runner._safe_max = chance; log._runner._hold_max = 19;
                  log._runner._safe_pct = chance / 20.0; log._runner._hold_pct = (19 - chance) / 20.0; log._runner._out_pct = 0.05;
                }
              }
            } else if (_bases._onbase[0] != null && _bases._outs < 2) {
              _bases._onbase[0] = null; _bases.advance(1, log._scored); _bases._onbase[0] = log._batter; log._special = PlayLog.Special.FC;
            }
            break;
          case OUT_MEDSHORT:
            if (log._error_play > 0) { break; }
            if (_bases._onbase[2] != null && _bases._outs < 2) {
              int chance = _bases._onbase[2]._run._advance + log._fielder._onfield._arm + 2;
              if (chance < 1) { chance = 1; } else if (chance > 19) { chance = 19; }
              log._runner = new RunLog(_bases, 2, RunLog.Cause.OUT);
              log._runner._safe_max = chance;
              log._runner._safe_pct = chance / 20.0;
              log._runner._out_pct = 1 - log._runner._safe_pct;
            }
            break;
          case OUT_SHORT:
            if (log._error_play > 0) { break; }
            if (!fieldPos.isOF() && _bases._outs < 2) { _bases.advance(1, log._scored); }
            break;
          default:
            break;
        }
        if (log._error_play > 0) {
          _bases.advance(log._error_play, log._scored);
          if (play._type.woba_wt() == 0) { // supposed to be an out, so batter hasn't yet gotten on base
            _bases._onbase[log._error_play - 1] = log._batter;
          }
        } else if (play._type.woba_wt() == 0) { ++_bases._outs; } // a real out
      }
      off._runs += log._scored.size();
    }
    if (_bases._outs > 3) { _bases._outs = 3; }
    _runexp.setRE(_bases);
    log._e_bases.set(_bases);
    
    _leadoff = false;
    if (defense()._fatigue != null) { defense()._fatigue.collect(log); }
    checkEndOfInning();
    if (atbat) { ++off._atBat; }
    return log;
  }
  
  private void checkEndOfInning() {
    if (_bases._outs >= 3) {
      _bases.reset();
      _top = !_top;
      if (_top) { ++_inning; }
      _leadoff = true;
    }
  }
}