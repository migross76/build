package strat.client;

import com.google.gwt.user.client.Random;
import strat.client.model.RandomBase;

public class RandomClient implements RandomBase {
  @Override public int nextInt(int upperBound) {
    return Random.nextInt(upperBound);
  }
}
