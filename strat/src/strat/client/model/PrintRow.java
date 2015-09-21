package strat.client.model;

public class PrintRow {
  public String _toggle = "";
  public String _die = "";
  public String _main = "";
  public String _extra = "";
  public String _split = "";
  public boolean _good = false;
  
  public static String emptyHTML() {
    return "<td></td><td></td><td class='last'>&nbsp;</td>";
  }
  
  public String toHTMLString() {
    StringBuilder sb = new StringBuilder();
    String type = _good ? "good" : "bad";
    sb.append("<td class='first'>").append(_toggle).append("<span class='die'>").append(_die).append(_die.isEmpty() ? "" : "-").append("</span></td>");
    if (_split.isEmpty()) {
      sb.append("<td class='combo " + type + "' colspan='2'>").append(_main).append(_extra).append("</td>");
    } else {
      sb.append("<td class='main " + type + "'>").append(_main).append(_extra).append("</td>");
      sb.append("<td class='last " + type + "'>").append(_split).append("</td>");
    }
    return sb.toString();
  }
}
