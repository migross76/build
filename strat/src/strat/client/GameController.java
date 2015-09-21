package strat.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import strat.client.event.AdvanceRequest;
import strat.client.event.GameUpdate;
import strat.client.event.InitResponse;
import strat.client.event.LeagueInfoRequest;
import strat.client.event.LeagueInfoResponse;
import strat.client.event.LeagueRequest;
import strat.client.event.LeagueResponse;
import strat.client.event.PlayRequest;
import strat.client.event.TeamLoaded;
import strat.client.event.TeamRequest;
import strat.client.event.TeamResponse;
import strat.client.model.Batter;
import strat.client.model.FieldChart;
import strat.client.model.GameState;
import strat.client.model.ParkInfo;
import strat.client.model.Pitcher;
import strat.client.model.PlayLog;
import strat.client.model.RandomBase;
import strat.client.model.RunExpChart;
import strat.client.model.TeamLocation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.web.bindery.event.shared.binder.EventBinder;
import com.google.web.bindery.event.shared.binder.EventHandler;

public class GameController {
  private final MyEventBinder _bindEvent = GWT.create(MyEventBinder.class);
  interface MyEventBinder extends EventBinder<GameController> {/*binder*/}

  private Map<String, String> _leagueURL = new HashMap<>();
  private Map<String, String> _teamURL = new HashMap<>();
  private RunExpChart _re;
  private FieldChart _fc = new FieldChart();
  private GameState _model = null;
  private RandomBase _rand = new RandomClient();
  
  @EventHandler public void onTeamSelected(TeamLoaded event) {
    if (event._location == TeamLocation.HOME) {
      _model._home = event._team;
      _model._park = ParkInfo.AVERAGE; // TODO allow for team-specific park
    } else {
      _model._vis = event._team;
    }
  }

  @EventHandler public void onAdvanceRequested(AdvanceRequest event) {
    /*boolean tried = */_model.advance(event._log, event._needsLead);
    Static.fire(new GameUpdate(event._log, _model._vis._runs, _model._home._runs, _model.offense() == _model._vis ? TeamLocation.VISITOR : TeamLocation.HOME, _model.gameOver()));
  }
  
  /** @param event indicates what type of handler needed */
  @EventHandler public void onPlayRequested(PlayRequest event) {
    PlayLog pl = _model.bat(_rand);
    Static.fire(new GameUpdate(pl, _model._vis._runs, _model._home._runs, _model.offense() == _model._vis ? TeamLocation.VISITOR : TeamLocation.HOME, _model.gameOver()));
  }

  @EventHandler public void onTeam(final TeamRequest event) {
    String url = _teamURL.get(event._league + "\n" + event._team);
    if (url == null) { throw new IllegalArgumentException("unknown league/team name : " + event._league + " : " + event._team); }
    makeCall(url, new BaseCallback() {
      @Override protected void parseJSON(JSONValue js) {
        ArrayList<Batter> batters = new ArrayList<>();
        ArrayList<Pitcher> pitchers = new ArrayList<>();
        JSONObject main = js.isObject();
        JSONArray players = main.get("roster").isArray();
        for (int i_play = 0; i_play != players.size(); ++i_play) {
          JSONObject player = players.get(i_play).isObject();
          String id = player.get("id").isString().stringValue();
          String primary = player.get("primary").isString().stringValue();
          boolean isPitcher = player.get("isPitcher").isBoolean().booleanValue();
          String card = player.get("card").isString().stringValue();
          ArrayList<String> lines = new ArrayList<>(Arrays.asList(card.split("\n")));
          if (isPitcher) {
            pitchers.add(new Pitcher(id, primary, lines, ParkInfo.AVERAGE));
          } else {
            batters.add(new Batter(id, primary, lines, ParkInfo.AVERAGE));
          }
        }
        Static.fire(new TeamResponse(batters, pitchers, event._location));
      }
    });
  }
  
  @EventHandler public void onLeagueInfo(final LeagueInfoRequest event) {
    String url = _leagueURL.get(event._name);
    if (url == null) { throw new IllegalArgumentException("unknown league name : " + event._name); }
    makeCall(url, new BaseCallback() {
      @Override protected void parseJSON(JSONValue js) {
        ArrayList<String> names = new ArrayList<>();
        JSONObject main = js.isObject();
        JSONArray teams = main.get("teams").isArray();
        for (int i_tm = 0; i_tm != teams.size(); ++i_tm) {
          JSONObject team = teams.get(i_tm).isObject();
          String name = team.get("name").isString().stringValue();
          String teamUrl = team.get("url").isString().stringValue();
          _teamURL.put(event._name + "\n" + name, teamUrl);
          names.add(name);
        }
        Static.fire(new LeagueInfoResponse(names));
      }
    });
  }
  
  /** @param event indicates what type of handler needed */
  @EventHandler public void onLeague(LeagueRequest event) {
    makeCall("/leagues", new BaseCallback() {
      @Override protected void parseJSON(JSONValue js) {
        ArrayList<String> names = new ArrayList<>();
        JSONArray main = js.isArray();
        for (int i_leag = 0; i_leag != main.size(); ++i_leag) {
          JSONObject league = main.get(i_leag).isObject();
          String name = league.get("name").isString().stringValue();
          String url = league.get("url").isString().stringValue();
          _leagueURL.put(name, url);
          names.add(name);
        }
        Static.fire(new LeagueResponse(names));
      }
    });
  }
  
  private void load() {
    makeCall("/guts", new BaseCallback() {
      @Override protected void parseJSON(JSONValue js) {
        JSONObject main = (JSONObject)js;
        String fielding = main.get("fielding").isString().stringValue();
        _re = new RunExpChart(main.get("runexp").isString().stringValue());
        _fc.load(fielding);
        _model = new GameState(_re, _fc);
        Static.fire(new InitResponse(_re, _fc));
      }
    });
  }
  
  public GameController() {
    _bindEvent.bindEventHandlers(this, Static.eventBus());
    load();
  }
  
  private static void makeCall(String url, BaseCallback cb) {
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));

    try {
      /*Request request = */builder.sendRequest(null, cb);
    } catch (RequestException e) {
      // Couldn't connect to server
    }  
  }
}
