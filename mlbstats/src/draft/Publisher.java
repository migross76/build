package draft;

import java.io.IOException;
import java.sql.SQLException;

public interface Publisher {
  public void print(Roster roster) throws IOException, SQLException;
  public void publish() throws IOException;
}
