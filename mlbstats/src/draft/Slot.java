package draft;

import data.Position;
import java.util.EnumSet;

public class Slot {
  public Slot(String pos) {
    _player = null;
    _pos = Position.parse(pos);
  }
  
  public boolean available() { return _player == null; }
  
  public boolean supports(Position pos) { return _pos.contains(pos); }
  
  public void setPlayer(Player P) { _player = P; }
  public Player getPlayer() { return _player; }
  public EnumSet<Position> getPosition() { return _pos; }
  
  private Player _player = null;
  private EnumSet<Position> _pos = null;
}
