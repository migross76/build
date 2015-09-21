package strat.server;

import strat.client.model.PrintRow;

public class PrintRowPlain {
  private static final int COL_SIZE = 20;
  
  public static String toPlainString(PrintRow pr) {
    StringBuilder sb = new StringBuilder();
    if (pr._die.length() == 1) { sb.append(" "); }
    sb.append(pr._toggle.isEmpty() ? " " : pr._toggle);
    sb.append(pr._die.isEmpty() ? "   " : (pr._die + "-"));
    sb.append(pr._main).append(pr._extra);
    int spaces = COL_SIZE - sb.length() - pr._split.length();
    sb.append(String.format("%" + spaces + "s", ""));
    sb.append(pr._split);
    return sb.toString();
  }
  
  public static String empty() {
    return String.format("%" + COL_SIZE + "s", "");
  }
}
