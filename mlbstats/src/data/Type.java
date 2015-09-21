package data;

public enum Type {
  BAT("B", 650), PITCH("P", 900);

  public String code() { return _code; }
  public int playtime() { return _playtime; }
  
  private Type(String code, int playtime) { _code = code; _playtime = playtime; }
  
  private String _code = null;
  private int _playtime = 0;
}