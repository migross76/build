package strat.driver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import strat.client.model.Batter;
import strat.client.model.FieldChart;
import strat.client.model.Fielding;
import strat.client.model.ParkInfo;
import strat.client.model.Pitcher;
import strat.client.model.Play.Type;
import strat.client.model.Player;
import strat.client.model.Position;
import strat.client.model.REPlayChart;
import strat.client.model.REValueCollector;
import strat.client.model.SimplePlay;
import strat.server.DataStore;
import strat.server.PlayerInfo;

public class RECardValues {
  
  public static class Data {
    public Data(Player p) {
      _player = p;
    }
    
    public void print(double ltPerc, double repl, int neg) {
      REValueCollector vcL = new REValueCollector(_re_chart, _player._asL);
      REValueCollector vcR = new REValueCollector(_re_chart, _player._asR);
      
      double vsL = (vcL._total * 500 / vcL._ct + repl) * neg;
      double vsR = (vcR._total * 500 / vcR._ct + repl) * neg;
      System.out.format("%s\t%s\t%c", _player._id, _player._primary.toUpperCase(), _player.handed());
      if (_player instanceof Batter) {
        Batter b = (Batter)_player;
        System.out.format("\t%d\t%c/%c", b.pa(), b._weakL ? 'W' : '-', b._weakR ? 'W' : '-');
      } else { System.out.print("\t\t"); }
      System.out.format("\t%.0f", vsL * ltPerc + vsR * (1 - ltPerc));
      System.out.format("\t%.0f\t%.0f\t%.0f", vsL, vcL._ob, vcL._xb);
      System.out.format("\t%.0f\t%.0f\t%.0f", vsR, vcR._ob, vcR._xb);
      for (Fielding f : _player._fielding.values()) {
        REValueCollector vcF = new REValueCollector(_re_chart);
        _f_chart.collect(f, vcF);
        String baseline = _field_plays.get(f._pos);
        double freq = _re_chart.frequency(baseline);
        double score = _re_chart.getRE(baseline);
        System.out.format("\t%s\t%de%d", f._pos.code().toUpperCase(), f._field, f._err);
        if (f._pos.isOF()) {
          System.out.format(" %s%d", f._arm > 0 ? "+" : "", f._arm);
        } else if (f._pos == Position.CATCH) {
          System.out.format(" %s%d\tC*\tT-%d pb-%d", f._arm > 0 ? "+" : "", f._arm, f._throw, f._pb);
        } else if (f._pos == Position.PITCH) {
          System.out.format(" H %s%d", f._hold > 0 ? "+" : "", f._hold);
        }
        //System.out.format("\t%s\t%.1f", f._pos.code().toUpperCase(), score - vcF._total / vcF._ct * freq * 1000);
      }
      System.out.println();
    }
    
    public final Player _player;
  }
  
  private static REPlayChart _re_chart;
  private static FieldChart _f_chart = new FieldChart();
  private static HashMap<Position, String> _field_plays = new HashMap<>();
  
  public static void main(String[] args) throws Exception {
    DataStore ds = new DataStore();
    _re_chart = new REPlayChart(DataStore.getREPlayChart());
    _f_chart.load(DataStore.getFieldInfo());
    for (Position pos : EnumSet.range(Position.PITCH, Position.RIGHT)) {
      _field_plays.put(pos, new SimplePlay(Type.FIELD, pos).createShortLine());
    }
    for (String teamName : DataStore.teamNames("default")) {
      System.out.format("\n%s\tPos\tL/R\tPA\tPow\tRE+T\tRE+L\tobL\txbL\tRE+R\tobR\txbR\tPos...\n", teamName.toUpperCase());
      for (PlayerInfo player :  ds.team("default", teamName)) {
        ArrayList<String> lines = new ArrayList<>(Arrays.asList(player._cardData.split("\n")));
        if (player._isPitcher) {
          Pitcher p = new Pitcher(player._id, player._mainPos, lines, ParkInfo.AVERAGE);
          Data d = new Data(p);
          d.print(0.5, 12.5, -1);
        } else {
          Batter b = new Batter(player._id, player._mainPos, lines, ParkInfo.AVERAGE);
          Data d = new Data(b);
          d.print(0.33, 12.5, 1);
        }
      }
    }
  }
}
