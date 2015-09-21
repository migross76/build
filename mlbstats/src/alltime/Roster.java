package alltime;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class Roster implements Iterable<Slot> {
  @Override public Iterator<Slot> iterator() { return _slots.iterator(); }
  
  public Slot get(int index) { return _slots.get(index); }
  
  public int size() { return _slots.size(); }
  
  public String getName() { return _name; }
  
  public Roster(BufferedReader in, int multiplier) throws IOException {
    String line = null;
    int id = 0;
    while ((line = in.readLine()) != null) {
      String[] opt = line.split("\t");
      double weight = 0, field = 0;
      if (opt.length > 1) { try { weight = Double.parseDouble(opt[1]); } catch (NumberFormatException e) { /* don't assign if not a number */ } }
      if (opt.length > 2) { try { field = Double.parseDouble(opt[2]); } catch (NumberFormatException e) { /* don't assign if not a number */ } }
      _slots.add(new Slot(id++, opt[0], multiplier, weight, field));
    }
  }
  
  public Roster(Roster R, int multiplier) {
    for (Slot S : R) {
      _slots.add(new Slot(S.getID(), S.getName(), multiplier, S.getWeight(), S.getFieldBonus()));
    }
  }
  
  public boolean finished() {
    for (Slot S : _slots) { if (S.available()) { return false; } }
    return true;
  }
  
  public double _total = 0;
  
  private String _name = null;
  private ArrayList<Slot> _slots = new ArrayList<>();
}
