package topactors.server;

import java.io.IOException;

public interface Cache {
  public StringBuilder fetch(String type, String id, String url) throws IOException;
}
