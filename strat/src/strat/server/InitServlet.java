package strat.server;

import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

public class InitServlet extends BaseServlet {
  private static final long serialVersionUID = -5797965560308870424L;

  @Override protected Object getJSON(StringBuffer url, String[] paths) throws IOException, JSONException {
    if (paths == null) {
      JSONObject js = new JSONObject();
      js.put("fielding", DataStore.getFieldInfo());
      js.put("runexp", DataStore.getRunExpInfo());
      return js;
    } else if (paths[1].equals("fielding")) {
      return DataStore.getFieldInfo();
    } else if (paths[1].equals("runexp")) {
      return DataStore.getRunExpInfo();
    }
    return null;
  }
}
