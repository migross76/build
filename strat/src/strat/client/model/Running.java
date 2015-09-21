package strat.client.model;


public class Running {
  public enum Lead { GOOD, BAD, OUT }
  
  public int _advance;
  public String _steal; // base steal rating; not used for super-advanced
  public Lead[] _lead = new Lead[11]; // 2 to 12
  public int _primary; // a good lead from first
  public int _second; // a bad lead, or leads from second or third
  public boolean _auto_good; // always achieves good lead if not being held
  
  // TODO more standardized format, for easier parsing
  // Was: *2-5,11/7,12 (15-6); 2-5/- (3-1); -/- (---)
  // Adj: * 2-5,11 7,12 15 6; 2-5 0 3 1; 0 0 0 0
  // First token: possible asterisk
  // A : GOOD leads
  // B : OUT leads
  // C : Primary success
  // D : Second success
  public Running(String[] tokens) {
    // R 14 AA * 3-6 0 17 13
    int index = 0;
    _advance = Integer.parseInt(tokens[++index]);
    _steal = tokens[++index];
    if (tokens[index+1].charAt(0) == '*') { ++index; _auto_good = true; }
    for (int id = 0; id != _lead.length; ++id) { _lead[id] = Lead.BAD; }
    for (int id : new NumberList(tokens[++index])) { _lead[id-2] = Lead.GOOD; }
    for (int id : new NumberList(tokens[++index])) { _lead[id-2] = Lead.OUT; }
    _primary = Integer.parseInt(tokens[++index]);
    _second = Integer.parseInt(tokens[++index]);
    if (index != tokens.length - 1) {
      throw new IllegalArgumentException("didn't parse all running tokens");
    }
  }
}
