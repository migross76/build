package strat.client.model;

public class Fielding {
  public Position _pos;
  public int _field;
  public int _arm;
  public int _err;

  public int _throw;
  public int _pb;
  public int _hold;
  
  public Fielding(String[] tokens, Position pos) {
    this(tokens, pos, 0);
  }
  
  public Fielding(String[] tokens) {
    this(tokens, Position.map(tokens[1]), 1);
  }
  
  private Fielding(String[] tokens, Position pos, int index) {
    _pos = pos;
    _field = Integer.parseInt(tokens[++index]);
    switch (_pos) {
      case CATCH:
        _arm = Integer.parseInt(tokens[++index]);
        _err = Integer.parseInt(tokens[++index]);
        _throw = Integer.parseInt(tokens[++index]);
        _pb = Integer.parseInt(tokens[++index]);
        break;
      case LEFT: case CENTER: case RIGHT:
        if (tokens.length == 5) { _arm = Integer.parseInt(tokens[++index]); }
        _err = Integer.parseInt(tokens[++index]);
        break;
      case PITCH:
        _err = Integer.parseInt(tokens[++index]);
        _hold = Integer.parseInt(tokens[++index]);
        break;
      default:
        _err = Integer.parseInt(tokens[++index]);
    }
    if (index != tokens.length - 1) {
      throw new IllegalArgumentException("more tokens for fielding parse");
    }
  }
}
