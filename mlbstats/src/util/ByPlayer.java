package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import data.Filter;
import data.Groupable;

// Aggregate a data statistic, such that:
// 1) It's grouped together by a key (e.g., player id, year)
// 2) Each group contains the best, first, last, and total
// 3) Best is determined by a Comparable<> (optional)
// 4) First & last are determined by a Comparable<>, determining the order of sub-sections
// 5) One or more filters can be applied, to get a subset total (e.g., positive-only values)
public class ByPlayer<Value extends Groupable<Value>> implements Iterable<Value> {
  public static interface GroupKey<Key extends Comparable<Key>, Value> {
    public Key groupBy(Value V);
  }
  
  public Value best()     { return _best; }
  public Value get(int i) { return _seasons.get(i); }
  public Value first()    { return _seasons.get(0); }
  public Value last()     { return _seasons.get(_seasons.size() - 1); }
  public int   size()     { return _seasons.size(); }
  public Value total()    { return _total; }
  public Value filter(Filter<Value> F) { return _filtered.get(F); }
  
  @Override public Iterator<Value> iterator() { return _seasons.iterator(); }
  
  private ByPlayer(Value T) {
    _best = null;
    _total = T.create();
  }

  private Value _best = null;
  private Value _total = null;
  private List<Value> _seasons = new ArrayList<>();
  private Map<Filter<Value>, Value> _filtered = new HashMap<>();
  
  public static class Group<Key extends Comparable<Key>, Value extends Groupable<Value>> implements Iterable<ByPlayer<Value>> {
    public ByPlayer<Value> get(Key id) { return _map.get(id); }

    public void copy(Collection<ByPlayer<Value>> C) { C.addAll(_map.values()); } 
    
    @Override public Iterator<ByPlayer<Value>> iterator() { return _map.values().iterator(); } 
    
    public void addAll(Iterable<Value> iter) {
      for (Value V : iter) {
        Key K = _groupKey.groupBy(V);
        ByPlayer<Value> ag = _map.get(K);
        if (ag == null) { _map.put(K, ag = new ByPlayer<>(V)); }

        if (_compGroup == null) {
          ag._seasons.add(V);
        } else {
          int i = 0;
          for (; i != ag._seasons.size(); ++i) {
            int cmp = _compGroup.compare(V, ag._seasons.get(i));
            if (cmp == 0) { ag._seasons.get(i).add(V); break; } // FIXME don't just combine them blindly
            if (cmp > 0) { ag._seasons.add(i, V); break; }
          }
          if (i == ag._seasons.size()) { ag._seasons.add(V); }
        }

        ag._total.add(V);
        if (_compBest != null) {
          if (ag._best == null || _compBest.compare(ag._best, V) > 0) { ag._best = V; }
        }
        for (Filter<Value> F : _filters) {
          if (F.satisfied(V)) {
            Value FT = ag._filtered.get(F);
            if (FT == null) { ag._filtered.put(F, FT = V.create()); }
            FT.add(V);
          }
        }
      }
    }
    
    public void addFilter(Filter<Value> filter) { _filters.add(filter); }
    public void setGroup(Comparator<Value> comp) { _compGroup = comp; }
    public void setBest(Comparator<Value> comp) { _compBest = comp; }
    
    public Group(GroupKey<Key, Value> groupKey) { _groupKey = groupKey; }
    public Group(GroupKey<Key, Value> groupKey, Comparator<Value> compBest, Comparator<Value> compGroup) {
      _groupKey = groupKey;
      setBest(compBest);
      setGroup(compGroup);
    }
    
    private GroupKey<Key, Value> _groupKey = null;
    private TreeMap<Key, ByPlayer<Value>> _map = new TreeMap<>();
    private Comparator<Value> _compBest = null;
    private Comparator<Value> _compGroup = null;
    private ArrayList<Filter<Value>> _filters = new ArrayList<>();
  }
}
