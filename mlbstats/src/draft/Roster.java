package draft;

import java.util.ArrayList;
import java.util.Iterator;

public class Roster implements Iterable<Slot> {
  @Override public Iterator<Slot> iterator() { return _slots.iterator(); }
  
  public int size() { return _slots.size(); }
  
  public String getName() { return _name; }
  
  public Roster(String name, String... poses) {
    _name = name;
    _slots = new ArrayList<>(poses.length);
    for (String pos : poses) { _slots.add(new Slot(pos)); }
  }
  
  private String _name = null;
  private ArrayList<Slot> _slots = null;
  public String _primaryTeam = null;
}
