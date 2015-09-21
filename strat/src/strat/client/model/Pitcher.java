package strat.client.model;

import java.util.List;

public class Pitcher extends Player {
  public char _pitches;
  public int _bat;
  public char _pow;

  public int _bk;
  public int _wp;
  
  public boolean _four_man = false;
  public int _starter = 4;
  public int _reliever = 0;
  public int _closer = -1;
  
  public int _w;
  public int _l;
  public double _era;
  public int _gs;
  public int _sv;
  public int _ip;
  public int _h;
  public int _bb;
  public int _so;
  public int _hr;
  
  public double _woba = 0;
  public int _firstBatter = 0;
  public int _bfLimit = 0;
  
  @Override public char handed() { return _pitches; }
  
  // TODO more error checking; confirm right number of tokens, of characters, that everything is filled in
  public Pitcher(String id, String primary, List<String> lines, ParkInfo park) {
    super(id, primary);
    boolean has_ctrl = false;
    while (!lines.isEmpty()) {
      String line = lines.remove(0);
      String[] tokens = line.split(" ");
      if (tokens.length == 0 || tokens[0].isEmpty()) { break; }
      switch (useChar(tokens[0])) {
        case 'N':
          _nameFirst = line.substring(2);
          break;
        case 'L':
          _nameLast = line.substring(2);
          break;
        case 'P': // R 39
          check(line, tokens, 3);
          _pitches = useChar(tokens[1]);
          _percL = Integer.parseInt(tokens[2]);
          break;
        case 'B': // 3 N R B
          check(line, tokens, 5);
          _bat = Integer.parseInt(tokens[1]);
          _pow = useChar(tokens[2]);
          _bats = useChar(tokens[3]);
          _bunt = useChar(tokens[4]);
          break;
        case 'R': // 14 AA * 3-6 0 17 13
          _run = new Running(tokens);
          break;
        case 'C': // 0 1
          check(line, tokens, 3);
          _bk = Integer.parseInt(tokens[1]);
          _wp = Integer.parseInt(tokens[2]);
          has_ctrl = true;
          break;
        case 'F':
          _fielding.put(Position.PITCH, new Fielding(tokens, Position.PITCH));
          break;
        case 'E': //  * 9 r 4 3
          int index = 0;
          while (index != tokens.length - 1) {
            switch (useChar(tokens[++index])) {
              case '*':
                _four_man = true;
                //$FALL-THROUGH$
              case 's':
                _starter = Integer.parseInt(tokens[++index]);
                break;
              case 'r':
                _reliever = Integer.parseInt(tokens[++index]);
                _closer = Integer.parseInt(tokens[++index]);
            }
          }
          break;
        case 'S':
          check(line, tokens, 11);
          _w   = Integer.parseInt(tokens[1]);
          _l   = Integer.parseInt(tokens[2]);
          _era = Double.parseDouble(tokens[3]);
          _gs  = Integer.parseInt(tokens[4]);
          _sv  = Integer.parseInt(tokens[5]);
          _ip  = Integer.parseInt(tokens[6]);
          _h   = Integer.parseInt(tokens[7]);
          _bb  = Integer.parseInt(tokens[8]);
          _so  = Integer.parseInt(tokens[9]);
          _hr  = Integer.parseInt(tokens[10]);
          break;
      }
    }
    Play parkSingle = null, parkHomer = null;
    switch (_pitches) { // specifically against lefties
      case 'L' : parkSingle = park._sLeft; parkHomer = park._rLeft; break;
      case 'R' : parkSingle = park._sRight; parkHomer = park._rRight; break;
      default: throw new IllegalArgumentException("illegal pitches : " + _pitches);
    }
    for (int i = 0; i != 3; ++i) { _asL[i] = new ColumnPlay(lines, parkSingle, parkHomer); }
    for (int i = 0; i != 3; ++i) { _asR[i] = new ColumnPlay(lines, parkSingle, parkHomer); }
    check("first name", _nameFirst);
    check("last name", _nameLast);
    check("pitching info", _percL);
    check("pitching info", _bat);
    check("running info", _run);
    check("control info", has_ctrl ? 1 : 0);
    check("fielding info", _fielding.size());
    check("endurance", _starter + _reliever);
    check("stats", _ip);
  }
}
