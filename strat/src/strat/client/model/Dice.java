package strat.client.model;

import java.util.ArrayList;

public class Dice {
  public static class Roll {
    public Roll(int result, int[] sides) { _result = result; _sides = sides; }
    public final int _result;
    public final int[] _sides;
  }
  
  public int roll(int... sides) {
    int result = 0;
    for (int die : sides) { result += _rand.nextInt(die) + 1; }
    _rolls.add(new Roll(result, sides));
    return result;
  }
  
  public Iterable<Roll> rolls() { return _rolls; }
  
  public Dice(RandomBase rand) { _rand = rand; }
  
  public void clear() { _rolls.clear(); }
  
  private ArrayList<Roll> _rolls = new ArrayList<>();
  private final RandomBase _rand;
}
