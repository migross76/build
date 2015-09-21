package strat.driver;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import strat.client.model.Batter;
import strat.client.model.FieldChart;
import strat.client.model.Fielding;
import strat.client.model.NumberList;
import strat.client.model.Pitcher;
import strat.client.model.Play;
import strat.client.model.Player;
import strat.client.model.Position;
import strat.client.model.PrintRow;
import strat.client.model.Running;
import strat.client.model.TogglePlay;
import strat.client.model.TypeCollector;
import strat.client.model.ValueCollector;
import strat.server.Load;

/** Create the HTML pages to replicate the paper cards, plus calculations on each player... alphabetically, and grouped by the mypos.txt */
public class CardPages {
  private static final Path OUT_DIR = Load.BASE_DIR.resolve("html");
  
  private static String printNumberList(Running.Lead[] list, Running.Lead value) {
    ArrayList<Integer> vals = new ArrayList<>();
    for (int i_l = 0; i_l != list.length; ++i_l) {
      if (list[i_l] == value) { vals.add(i_l+2); }
    }
    return NumberList.print(vals);
  }
  
  private static void printHeader(PrintWriter pw, boolean isBatter) {
    pw.print("<tr><th>Player</th><th>L/R</th><th>Pos");
    for (Play.Type pt : EnumSet.allOf(Play.Type.class)) { if (pt.supported(isBatter)) { pw.format("</th><th>%c", pt.code()); } }
    for (TogglePlay.Type tt : EnumSet.allOf(TogglePlay.Type.class)) { if (tt.supported(isBatter)) { pw.format("</th><th>/%c", tt.code()); } }
    pw.println("</th></tr>");
  }
  
  private static int generatePrintRows(Player card, ArrayList<ArrayList<PrintRow>> lists) {
    int max_list = 0;
    for (int i = 0; i != card._asL.length; ++i) {
      ArrayList<PrintRow> list = new ArrayList<>();
      lists.add(list);
      card._asL[i].print(list, false);
      if (max_list < list.size()) { max_list = list.size(); }
    }
    for (int i = 0; i != card._asR.length; ++i) {
      ArrayList<PrintRow> list = new ArrayList<>();
      lists.add(list);
      card._asR[i].print(list, false);
      if (max_list < list.size()) { max_list = list.size(); }
    }
    return max_list;
  }
  
  private static void printCardHeader(PrintWriter pw) {
    pw.println("<html><head><meta charset=\"UTF-8\"><style>");
    pw.println("body { font-family: Arial; }");
    pw.println("div.header { position: absolute; top: 0; width: 750px; }");
    pw.println("div.header div { position: absolute; white-space: nowrap; }");
    pw.println("table.cols { position: absolute; top: 45px; border-collapse: collapse; border: 1px solid black; }");
    pw.println("table.cols th { font-size: 12px; border: 1px solid black; background-color: lightgray; }");
    pw.println("table.cols td { font-size: 12px; }");
    pw.println("table.cols td.first { width: 35px; text-align: right; }");
    pw.println("table.cols td.main  { width: 50px; white-space: nowrap; }");
    pw.println("table.cols td.combo { width: 85px; border-right: 1px solid black; }");
    pw.println("table.cols td.last  { width: 35px; border-right: 1px solid black; }");
    pw.println("span.die { font-weight: bold; }");
    pw.println("table.cols td.good { font-weight: bold; }");
    pw.println("table.stats { position: absolute; top: 400px; }");
    pw.println("table.stats th { padding: 0 15px; text-align: center; font-size: 14px; font-weight: bold; vertical-align: bottom; }");
    pw.println("table.stats td { padding: 0 15px; text-align: center; font-size: 14px; }");
  }
  
  private static void printColumns(PrintWriter pw, Player card, int first) {
    ArrayList<ArrayList<PrintRow>> lists = new ArrayList<>();
    int max_list = generatePrintRows(card, lists);
    pw.format("<tr><th colspan='3'>%1$s</th><th colspan='3'>%2$s</th><th colspan='3'>%3$s</th><th colspan='3'>%1$s</th><th colspan='3'>%2$s</th><th colspan='3'>%3$s</th></tr>", first, first+1, first+2);
    for (int row = 0; row != max_list; ++row) {
      pw.println("<tr>");
      for (ArrayList<PrintRow> list : lists) {
        if (list.size() > row) {
          pw.print(list.get(row).toHTMLString());
        } else {
          pw.print(PrintRow.emptyHTML());
        }
      }
      pw.println("</tr>");
    }
  }
  
  private static void printBatter(Batter card) throws IOException {
    try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(OUT_DIR.resolve(card._id + "b.html"), StandardCharsets.UTF_8))) {
System.out.println(card._id);
      printCardHeader(pw);
      pw.println("div.header .hand  { left: 0;    top: 5px; font-size: 18px; font-weight: bold; padding: 0 5px; border-right: 1px solid black; height: 40px; }");
      pw.println("div.header .name  { left: 30px; top: 5px; font-size: 18px; font-weight: bold; text-transform: uppercase; }"); // 18px
      pw.println("div.header .field { left: 40px; top: 25px; font-size: 14px; }"); // 14px
      pw.println("div.header .steal { left: 300px; top: 5px; }");
      pw.println("div.header .lead  { left: 400px; top: 5px; font-size: 14px; }");
      pw.println("div.header .bunt  { right: 100px; top: 5px; }");
      pw.println("div.header .handr { right: 0; top: 5px; }");
      pw.println("div.header .run   { right: 0; top: 25px; }");
      pw.println("div.header .label { font-size: 14px; }");
      pw.println("div.header .value { font-size: 14px; font-weight: bold; }");
      pw.println("table.cols .split { background-color: darkgray; }");
      pw.println("table.cols .perc { padding-left: 20px; font-size: 12px; font-weight: normal; float: left; }");
      pw.println("table.cols .power { padding-right: 10px; font-size: 12px; font-weight: bold; float: right; }");
      pw.println("</style></head><body><div class='header'>");
      pw.println("<div class='hand'>" + card._bats + "</div>");
      pw.println("<div class='name'>" + card._nameFirst + " " + card._nameLast + "</div>");
      pw.println("<div class='steal'><span class='label'>stealing- </span><span class='value'>(" + card._run._steal + ")</span></div>");
      pw.format("<div class='lead'>%s%s/%s (%s-%s)</div>\n", card._run._auto_good ? "*" : "", printNumberList(card._run._lead, Running.Lead.GOOD), printNumberList(card._run._lead, Running.Lead.OUT), card._run._primary == 0 ? "-" : "" + card._run._primary, card._run._second == 0 ? "-" : "" + card._run._second);
      pw.println("<div class='bunt'><span class='label'>bunting-</span><span class='value'>" + card._bunt + "</span></div>");
      pw.println("<div class='handr'><span class='label'>hit &amp; run-</span><span class='value'>" + card._hitandrun + "</span></div>");
      pw.println("<div class='run'><span class='label'>running </span><span class='value'>1-" + card._run._advance + "</span></div>");
      pw.print("<div class='field'>");
      boolean first = true, print_of_arm = true;
      for (Fielding f : card._fielding.values()) {
        if (first) { first = false; } else { pw.print("/ "); }
        pw.format("%s-%d", f._pos.code(), f._field);
        if ((f._pos.isOF() && print_of_arm) || f._pos == Position.CATCH) {
          pw.format("(%s)", (f._arm > 0 ? "+" : "") + f._arm);
        }
        if (f._pos.isOF()) { print_of_arm = false; }
        pw.format(" e%d", f._err);
        if (f._pos == Position.CATCH) { pw.format(", T-1-%d(pb-%d)", f._throw, f._pb); }
      }
      pw.println("</div>");
      pw.println("</div>"); // header
      pw.println("<table class='cols'>");
      pw.format("<tr><th colspan='9' class='split'><span class='perc'>%d%% AGAINST LEFT-HAND PITCHERS</span><span class='power'>Power-%c</span></th>", card._percL, card._weakL ? 'W' : 'N');
      pw.format("<th colspan='9' class='split'><span class='perc'>%d%% AGAINST RIGHT-HAND PITCHERS</span><span class='power'>Power-%c</span></th></tr>\n", 100 - card._percL, card._weakR ? 'W' : 'N');
      printColumns(pw, card, 1);
      pw.println("</table>");
      pw.println("<table class='stats'>");
      pw.println("<tr><th>AVG</th><th>AB</th><th>2B</th><th>3B</th><th>HR</th><th>RBI</th><th>BB</th><th>SO</th><th>SB</th><th>CS</th><th>SLG%</th><th>ON BASE%</th></tr>");
      pw.format("<tr><td>%.3f</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%.3f</td><td>%.3f</td></tr>\n",
                 card._avg, card._ab, card._d, card._t, card._hr, card._rbi, card._bb, card._so, card._sb, card._cs, card._slg, card._oba);
      pw.println("</table>");
      pw.println("</body></html>");
    }
  }
  
  // TODO add header with all the values in the right places
  // TODO move detailed stats to below this page
  private static void printPitcher(Pitcher card) throws IOException {
    try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(OUT_DIR.resolve(card._id + "p.html"), StandardCharsets.UTF_8))) {
      printCardHeader(pw);
      pw.println("div.header .hand { left: 0;    top: 5px; font-size: 18px; font-weight: bold; padding: 0 5px; border-right: 1px solid black; height: 40px; }");
      pw.println("div.header .name { left: 30px; top: 5px; font-size: 18px; font-weight: bold; text-transform: uppercase; }");
      pw.println("div.header .balk { left: 320px; top: 5px; font-size: 14px; }");
      pw.println("div.header .wp   { left: 370px; top: 5px; font-size: 14px; }");
      pw.println("div.header .err  { left: 450px; top: 5px; font-size: 14px; }");
      pw.println("div.header .bat  { left: 500px; top: 5px; font-size: 14px; }");
      pw.println("div.header .throws { left: 320px; top: 25px; font-size: 14px; font-weight: bold; }");
      pw.println("div.header .hold { left: 450px; top: 25px; font-size: 14px; }");
      pw.println("div.header .bunt { left: 510px; top: 25px; font-size: 14px; }");
      pw.println("div.header .role-main { right: 50px; top: 5px; font-size: 14px; }");
      pw.println("div.header .role-alt  { right: 50px; top: 25px; font-size: 14px; }");
      pw.println("table.cols .split { background-color: darkgray; }");
      pw.println("table.cols .perc { padding-left: 20px; font-size: 12px; font-weight: normal; }");
      pw.println("</style></head><body><div class='header'>");
      pw.println("<div class='hand'>" + card._pitches + "</div>");
      pw.println("<div class='name'>" + card._nameFirst + " " + card._nameLast + "</div>");
      pw.println("<div class='balk'>bk- " + card._bk + "</div>");
      pw.println("<div class='wp'>wp- " + card._wp + "</div>");
      Fielding f = card._fielding.get(Position.PITCH);
      pw.println("<div class='err'>e" + f._err + "</div>");
      pw.format("<div class='bat'>#%d%c%c</div>\n", card._bat, card._pow, card._bats);
      pw.format("<div class='hold'>hold %s%d</div>\n", f._hold > 0 ? "+" : "", f._hold);
      pw.format("<div class='bunt'>bunting-%c</div>\n", card._bunt);
      pw.format("<div class='throws'>throws %s</div>\n", card._pitches == 'R' ? "RIGHT" : "LEFT");
      if (card._starter > 0) {
        pw.format("<div class='role-main'>pitcher-%d starter(%d)</div>\n", f._field, card._starter);
        if (card._reliever > 0) { pw.format("<div class='role-alt'>relief(%d)/%d</div>\n", card._reliever, card._closer); }
      } else {
        pw.format("<div class='role-main'>pitcher-%d relief(%d)/%d</div>\n", f._field, card._reliever, card._closer);
      }
      pw.println("</div>"); // header
      pw.println("<table class='cols'>");
      pw.format("<tr><th colspan='9' class='split'><span class='perc'>%d%% AGAINST LEFT-HAND BATTERS</span></th>", card._percL);
      pw.format("<th colspan='9' class='split'><span class='perc'>%d%% AGAINST RIGHT-HAND BATTERS</span></th></tr>\n", 100 - card._percL);
      printColumns(pw, card, 4);
      pw.println("</table>");
      pw.println("<table class='stats'>");
      pw.println("<tr><th>W</th><th>L</th><th>ERA</th><th>STARTS</th><th>SAVES</th><th>IP</th><th>HITS<br/>ALLOWED</th><th>BB</th><th>SO</th><th>HOMERUNS<br/>ALLOWED</th></tr>");
      pw.format("<tr><td>%d</td><td>%d</td><td>%.2f</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td></tr>\n",
                 card._w, card._l, card._era, card._gs, card._sv, card._ip, card._h, card._bb, card._so, card._hr);
      pw.println("</table>");
      pw.println("</body></html>");
    }
  }
  
  // TODO summary stats : Hand, Value by pos, vsL, vsR, byPos, toggles, dp%, obp, iso, xb%, adv%(q s789), running
  // TODO how to weight Weak power - which takes away HR from pitchers
  private static void addValueRow(PrintWriter pw, Player card, FieldChart fc) {
    ValueCollector tcL = new ValueCollector(card._asL);
    ValueCollector tcR = new ValueCollector(card._asR);
    boolean isBatter = card instanceof Batter;
    if (isBatter) {
      Batter b = (Batter)card;
      if (b._weakL) { tcL._total -= 3; }
      if (b._weakR) { tcR._total -= 3; }
    }
    char code = isBatter ? 'b' : 'p';
    pw.format("<tr><td class='nowrap'><a href='" + card._id + code + ".html'>%s, %s</a></td><td>%c</td><td>", card._nameLast, card._nameFirst, card.handed());
    for (Fielding f : card._fielding.values()) {
      pw.format("%s<br/>", f._pos.code().toUpperCase());
    }
    pw.print("</td><td>");
    for (Fielding f : card._fielding.values()) {
      pw.format("%.3f<br/>", fc.woba(f));
    }
    if (isBatter) { pw.format("<td>%c/%c</td>", ((Batter)card)._weakL ? 'W' : '-', ((Batter)card)._weakR ? 'W' : '-'); }
    else if (((Pitcher)card)._reliever > 0) { pw.format("<td>(%d)/%d</td>", ((Pitcher)card)._reliever, ((Pitcher)card)._closer); }
    else { pw.print("<td>N/A</td>"); }
    pw.format("</td><td class='number'>%.3f</td><td class='number'>%.3f", tcL._total / tcL._ct, tcR._total / tcR._ct);
    double ct = tcL._ct = tcR._ct;
    for (TogglePlay.Type tt : EnumSet.allOf(TogglePlay.Type.class)) {
      if (tt.supported(isBatter)) {
        double[] ctL = tcL._toggles.get(tt);
        double[] ctR = tcR._toggles.get(tt);
        double tot = (ctL == null ? 0 : ctL[0]) + (ctR == null ? 0 : ctR[0]);
        pw.format("</td><td class='number'>%.3f", tot / ct);
      }
    }
    pw.println("</td></tr>");
  }
  
  private static void addTypeRow(PrintWriter pw, Player card) {
    TypeCollector tcL = new TypeCollector(card._asL);
    TypeCollector tcR = new TypeCollector(card._asR);
    boolean isBatter = card instanceof Batter;
    char code = isBatter ? 'b' : 'p';
    pw.format("<tr><td class='nowrap'><a href='" + card._id + code + ".html'>%s, %s</a></td><td>%c</td><td>", card._nameLast, card._nameFirst, card.handed());
    for (Fielding f : card._fielding.values()) {
      pw.format("%s&nbsp;", f._pos.code().toUpperCase());
    }
    for (Play.Type pt : EnumSet.allOf(Play.Type.class)) {
      double[] ctL = tcL._plays.get(pt);
      double[] ctR = tcR._plays.get(pt);
      double tot = (ctL == null ? 0 : ctL[0]) + (ctR == null ? 0 : ctR[0]);
      if (pt.supported(isBatter)) {
        pw.format("</td><td class='number'>%5.2f", tot);
      } else if (tot != 0) {
        System.err.format("Play type %c is not supported for %s and yet has non-zero value\n", pt.code(), card._id);
      }
    }
    for (TogglePlay.Type tt : EnumSet.allOf(TogglePlay.Type.class)) {
      double[] ctL = tcL._toggles.get(tt);
      double[] ctR = tcR._toggles.get(tt);
      double tot = (ctL == null ? 0 : ctL[0]) + (ctR == null ? 0 : ctR[0]);
      if (tt.supported(isBatter)) {
        pw.format("</td><td class='number'>%5.2f", tot);
      } else if (tot != 0) {
        System.err.format("Toggle type /%c is not supported for %s and yet has non-zero value\n", tt.code(), card._id);
      }
    }
    pw.println("</td></tr>");
  }
  
  private static void printPosition(PrintWriter pw_main, Load load, String pos, boolean isBatter) {
    pw_main.println("<div class='pos'>" + pos.toUpperCase() + "</div>");
    pw_main.println("<table>");
    pw_main.print("<tr><th>Player</th><th>L/R</th><th>Pos</th><th>Field</th><th>" + (isBatter ? "Pow" : "Relief") + "</th><th>vsL</th><th>vsR");
    for (TogglePlay.Type tt : EnumSet.allOf(TogglePlay.Type.class)) { if (tt.supported(isBatter)) { pw_main.format("</th><th>/%c", tt.code()); } }
    pw_main.println("</th></tr>");
    StringBuilder sb = new StringBuilder();
    for (String id : load._posMap.get(pos)) {
      Player p = load._cards.get(id);
      if (p == null) { if (sb.length() != 0) { sb.append(", "); } sb.append(id); }
      else { addValueRow(pw_main, p, load._fielding); }
    }
    pw_main.println("</table>");
    pw_main.println("<div class='extras'>" + sb.toString() + "</div>");
  }
  
  public static void main(String[] args) throws Exception {
    Load load = new Load();
    for (Batter b : load._batters) { printBatter(b); }
    for (Pitcher p : load._pitchers) { printPitcher(p); }
    
    try (PrintWriter pw_main = new PrintWriter(Files.newBufferedWriter(OUT_DIR.resolve("value.html"), StandardCharsets.UTF_8))) {
      pw_main.println("<html><head><meta charset=\"UTF-8\"><style>");
      pw_main.println("div.pos { font-family: Arial; font-size: 16px; font-weight: bold; margin-top: 15px; }");
      pw_main.println("div.extras { font-family: Arial; font-size: 11px; font-style: italic; }");
      pw_main.println("table { border-collapse: collapse; border: 1px solid black; }");
      pw_main.println("th { font-size: 12px; font-family: Arial; background-color: lightgray; }");
      pw_main.println("td { font-size: 12px; font-family: Arial; border-top: 1px solid darkgray; border-right: 1px solid lightgray; vertical-align: top; padding: 2px 5px; }");
      pw_main.println("td.nowrap { white-space: nowrap; }");
      pw_main.println("td.number { text-align: right; }");
      pw_main.println("</style></head><body>");
      for (Position pos : EnumSet.range(Position.CATCH, Position.RIGHT)) {
        printPosition(pw_main, load, pos.code(), true);
      }
      printPosition(pw_main, load, "sp", false);
      printPosition(pw_main, load, "rp", false);
      pw_main.println("</body></html>");
    }
    
    try (PrintWriter pw_main = new PrintWriter(Files.newBufferedWriter(OUT_DIR.resolve("index.html"), StandardCharsets.UTF_8))) {
      pw_main.println("<html><head><meta charset=\"UTF-8\"><style>");
      pw_main.println("div.pos { font-family: Arial; font-size: 16px; font-weight: bold; margin-top: 15px; }");
      pw_main.println("div.extras { font-family: Arial; font-size: 11px; font-style: italic; }");
      pw_main.println("table { border-collapse: collapse; border: 1px solid black; }");
      pw_main.println("th { font-size: 12px; font-family: Arial; background-color: lightgray; }");
      pw_main.println("td { font-size: 12px; font-family: Arial; border-top: 1px solid darkgray; border-right: 1px solid lightgray; vertical-align: top; padding: 2px 5px; }");
      pw_main.println("td.nowrap { white-space: nowrap; }");
      pw_main.println("td.number { text-align: right; }");
      pw_main.println("</style></head><body><table>");
      printHeader(pw_main, true);
      for (Player p : load._batters) { addTypeRow(pw_main, p); }
      pw_main.println("</table><p/><table>");
      printHeader(pw_main, false);
      for (Player p : load._pitchers) { addTypeRow(pw_main, p); }
      pw_main.println("</table></body></html>");
    }
  }
}
