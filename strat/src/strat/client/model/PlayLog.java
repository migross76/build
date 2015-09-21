package strat.client.model;

import java.util.ArrayList;
import java.util.EnumSet;
import strat.shared.BaseState;

public class PlayLog {
  public enum Special { DP, FC, IBB, SF, WP, PB, BK, SB, CS, PICK }
  
  public Batter _batter;
  public Position _bat_pos;
  public Pitcher _pitcher;
  public Player _fielder;

  public int _inning;
  public boolean _top;
  public int _atBat;
  public boolean _leadoff;

  public Dice _dice;
  public EnumSet<TogglePlay.Type> _toggles = EnumSet.noneOf(TogglePlay.Type.class);
  public boolean _is_main_pitcher_card; // is the main play from the pitcher or batter card
  public SimplePlay _main_play;
  public FieldChart.RangePlay _range_play;
  public int _error_play;
  public Special _special;
  
  public BaseState _s_bases = new BaseState();
  public BaseState _e_bases = new BaseState();
  
  public ArrayList<Batter> _scored = new ArrayList<>();

  public RunLog _runner = null;
}
