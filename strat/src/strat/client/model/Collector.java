package strat.client.model;



public interface Collector {
  public void collect(SimplePlay play, double weight, ToggleState toggles);
}
