package strat.client.model;

import java.util.List;

public class Batter extends Player {
  public char _hitandrun;
  public boolean _weakL;
  public boolean _weakR;
  
  public boolean isWeak(char opposing) { return opposing == 'L' ? _weakL : _weakR; }

  public int pa() { return _ab + _bb; }
  
  public double _avg;
  public int _ab;
  public int _d;
  public int _t;
  public int _hr;
  public int _rbi;
  public int _bb;
  public int _so;
  public int _sb;
  public int _cs;
  public double _slg;
  public double _oba;
  
  public double _bwoba = 0; // batting woba (relative to fielding woba)
  public double _fwoba = 0; // fielding woba (relative to batting woba)

  public char selectHanded(char opposing) {
    if (handed() == 'S') { return opposing == 'R' ? 'L' : 'R'; }
    return handed();
  }

  @Override public char handed() { return _bats; }
  
  // TODO more error checking; confirm right number of tokens, of characters, that everything is filled in
  public Batter(String id, String primary, List<String> lines, ParkInfo park) {
    super(id, primary);
    int of_arm = -100;
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
        case 'B': // R A B N N 25
          check(line, tokens, 7);
          _bats = useChar(tokens[1]);
          _bunt = useChar(tokens[2]);
          _hitandrun = useChar(tokens[3]);
          _weakL = useBool(tokens[4], 'W', 'N');
          _weakR = useBool(tokens[5], 'W', 'N');
          _percL = Integer.parseInt(tokens[6]);
          break;
        case 'R': // 14 AA * 3-6 0 17 13
          _run = new Running(tokens); // dynamic; checked by class
          break;
        case 'F':
          Fielding f = new Fielding(tokens); // dynamic; checked by class
          if (f._pos.isOF()) {
            if (of_arm == -100) { of_arm = f._arm; } else { f._arm = of_arm; }
          }
          _fielding.put(f._pos, f);
          break;
        case 'S':
          check(line, tokens, 13);
          _avg = Double.parseDouble(tokens[1]);
          _ab  = Integer.parseInt(tokens[2]);
          _d   = Integer.parseInt(tokens[3]);
          _t   = Integer.parseInt(tokens[4]);
          _hr  = Integer.parseInt(tokens[5]);
          _rbi = Integer.parseInt(tokens[6]);
          _bb  = Integer.parseInt(tokens[7]);
          _so  = Integer.parseInt(tokens[8]);
          _sb  = Integer.parseInt(tokens[9]);
          _cs  = Integer.parseInt(tokens[10]);
          _slg = Double.parseDouble(tokens[11]);
          _oba = Double.parseDouble(tokens[12]);
          break;
      }
    }
    Play parkSingle = null, parkHomer = null;
    switch (_bats) { // specifically against lefties
      case 'L' : parkSingle = park._sLeft; parkHomer = _weakL ? park._wLeft : park._rLeft; break;
      case 'S' : case 'R' : parkSingle = park._sRight; parkHomer = _weakR ? park._wRight : park._rRight; break;
    }
    for (int i = 0; i != 3; ++i) { _asL[i] = new ColumnPlay(lines, parkSingle, parkHomer); }
    if (_bats == 'S') { parkSingle = park._sLeft; parkHomer = park._rLeft; } // swap for righties
    for (int i = 0; i != 3; ++i) { _asR[i] = new ColumnPlay(lines, parkSingle, parkHomer); }
    check("first name", _nameFirst);
    check("last name", _nameLast);
    check("batting info", _percL);
    check("running info", _run);
    check("fielding info", _fielding.size());
    check("stats", _ab);
  }
}
