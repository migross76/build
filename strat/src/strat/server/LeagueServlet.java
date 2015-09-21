package strat.server;

import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LeagueServlet extends BaseServlet {
  private static final long serialVersionUID = 8087485061156189577L;

  private Object getRoster(String leagueName, String teamName) throws IOException, JSONException {
    JSONArray arr = new JSONArray();
    for (PlayerInfo pi : _ds.team(leagueName, teamName)) {
      JSONObject js = new JSONObject();
      js.put("id", pi._id);
      js.put("primary", pi._mainPos);
      js.put("isPitcher", pi._isPitcher);
      js.put("card", pi._cardData);
      arr.put(js);
    }
    return arr;
  }
  
  private static Object getTeamNames(StringBuffer url, String leagueName) throws JSONException {
    JSONArray arr = new JSONArray();
    if (!url.toString().endsWith("/teams")) { url.append("/teams"); }
    for (String teamName : DataStore.teamNames(leagueName)) {
      JSONObject js = new JSONObject();
      js.put("name", teamName);
      js.put("url", url.toString() + "/" + teamName);
      arr.put(js);
    }
    return arr;
  }
  
  private static Object getLeagueInfo(StringBuffer url, String leagueName) throws JSONException {
    JSONObject js = new JSONObject();
    js.put("name", leagueName);
    js.put("teams", getTeamNames(url, leagueName));
    return js;
  }
  
  private static Object getLeagueNames(StringBuffer url) throws JSONException {
    JSONArray arr = new JSONArray();
    JSONObject js = new JSONObject();
    js.put("name", "default");
    js.put("url", url.append("/default"));
    arr.put(js);
    return arr;
  }
  
  @Override protected Object getJSON(StringBuffer url, String[] paths) throws IOException, JSONException {
    if (paths == null) { return getLeagueNames(url); }
    if (paths.length == 2) { return getLeagueInfo(url, paths[1]); }
    if (paths.length == 3) {
      if (paths[2].equals("teams")) { return getTeamNames(url, paths[1]); }
      throw new IllegalArgumentException("Unsupported path : " + url);
    }
    if (paths.length == 4) {
      JSONObject js = new JSONObject();
      js.put("name", paths[3]);
      js.put("roster", getRoster(paths[1], paths[3]));
      return js;
    }
    if (paths.length == 5) {
      if (paths[4].equals("roster")) { return getRoster(paths[1], paths[3]); }
      throw new IllegalArgumentException("Unsupported path : " + url);
    }
    throw new IllegalArgumentException("Unsupported path : " + url);
  }
}
