package strat.server;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.simple.JSONValue;

public abstract class BaseServlet extends HttpServlet {
  private static final long serialVersionUID = 5423146378552830762L;

  protected abstract Object getJSON(StringBuffer url, String[] paths) throws JSONException, IOException;
  
  @Override protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String path_info = req.getPathInfo();
    String[] paths = path_info == null ? null : path_info.split("/");
    System.err.print(req.getRequestURL() + " :");
    if (paths == null) { System.err.println(" [null]"); } else { for (String path : paths) { System.err.print(" " + path); } }
    System.err.println();
    try {
      Object js = getJSON(req.getRequestURL(), paths);
      PrintWriter pw = new PrintWriter(resp.getWriter());
      pw.println(JSONValue.toJSONString(js));
    } catch (JSONException e) { throw new IOException(e); }
  }
  
  @Override public void init(ServletConfig config) {
    _ds = (DataStore)config.getServletContext().getAttribute("data-store");
    if (_ds == null) { config.getServletContext().setAttribute("data-store", _ds = new DataStore()); }
  }

  protected DataStore _ds;
}
