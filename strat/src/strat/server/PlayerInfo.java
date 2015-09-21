package strat.server;

public class PlayerInfo {
  public PlayerInfo(String id, String pos, String data, boolean isPitcher) {
    _id = id; _mainPos = pos; _cardData = data; _isPitcher = isPitcher;
  }
  
  public final String _id;
  public final String _mainPos;
  public final String _cardData;
  public final boolean _isPitcher;
}
