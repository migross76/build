package strat.client.model;

public class LineupLog {
  public LineupLog(Batter batter, char facing) {
    _batter = batter; _facing = facing;
  }
  
  public final Batter _batter;
  public final char _facing;

  public boolean _primary = false;
  public boolean _resting = false;
  public boolean _asDH = false;
  public int _num = 0;
}
