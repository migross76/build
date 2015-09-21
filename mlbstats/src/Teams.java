import java.util.HashMap;


public class Teams {

  public Teams() {
    insert("Angels","LAA");
    insert("Astros","HOU");
    insert("Athletics","OAK");
    insert("Blue Jays","TOR");
    insert("Braves","ATL");
    insert("Brewers","MIL");
    insert("Cardinals","STL");
    insert("Cubs","CHC");
    insert("Diamondbacks","ARZ");
    insert("Dodgers","LAD");
    insert("Giants","SF");
    insert("Indians","CLE");
    insert("Mariners","SEA");
    insert("Marlins","FLA");
    insert("Mets","NYM");
    insert("Nationals","WAS");
    insert("Orioles","BAL");
    insert("Padres","SD");
    insert("Phillies","PHI");
    insert("Pirates","PIT");
    insert("Rangers","TEX");
    insert("Rays","TB");
    insert("Red Sox","BOS");
    insert("Reds","CIN");
    insert("Rockies","COL");
    insert("Royals","KCA");
    insert("Tigers","DET");
    insert("Twins","MIN");
    insert("White Sox","CWS");
    insert("Yankees","NYY");
  }

  public String getCity(String team) {
    return _team2city.get(team);
  }

  public String getTeam(String city) {
    return _city2team.get(city);
  }
  
  private void insert(String team, String city) {
    _team2city.put(team, city);
    _city2team.put(city, team);
  }

  private HashMap<String, String> _team2city = new HashMap<>();
  private HashMap<String, String> _city2team = new HashMap<>();
}
