package draft;

import java.util.List;

public interface Elector {
  public List<Roster> elect(Candidates C);
}
