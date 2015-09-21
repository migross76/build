package draft;

import data.Position;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

public class HtmlPublisher implements Publisher {

  @Override public void publish() throws IOException {
    _out.println("</table>");
    _out.flush();
    _out.close();
  }
  
  @Override public void print(Roster roster) throws IOException, SQLException {
    if (_isFirst) {
      _isFirst = false;
      _out.println("<table border='1'>");
      _out.print("<tr><th>Name</th>");
      for (Slot S : roster) { _out.format("<th>%s</th>", Position.toString(S.getPosition())); }
      _out.println("</tr>");
    }
    _out.format("<tr><td>%s</td>", roster.getName());
    for (Slot S : roster) {
      Player P = S.getPlayer();
      if (P == null) { _out.print("<td>&nbsp;</td>"); }
      else {
        _out.format("<td title='%.1f' style='white-space: nowrap'>%s %s</td>", P._war, P._master.nameFirst().charAt(0), P._master.nameLast());
      }
    }
    _out.println("</tr>");
  }
  
  public HtmlPublisher(String outputFile) throws IOException {
    _out = new PrintWriter(new FileWriter(outputFile));
  }
  
  private PrintWriter _out = null;
  private boolean _isFirst = true;
}
