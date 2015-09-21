package strat.server;

import java.util.Random;
import strat.client.model.RandomBase;

public class RandomServer implements RandomBase {
  @Override public int nextInt(int upperBound) {
    return _rand.nextInt(upperBound);
  }

  public RandomServer() { this(new Random()); }
  public RandomServer(int seed) { this(new Random(seed)); }
  public RandomServer(Random rand) { _rand = rand; }

  private Random _rand;
}
