package draft;

import java.util.List;

// FIXME implement
public class DraftElector implements Elector {

  @Override
  public List<Roster> elect(Candidates C) {
    return null;
  }

}
/*
import Best;
import Player;
import Roster;
import Team;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Set;

public class DraftElector implements Elector {

  public boolean findRosterSpot(Player P, Team T, Best B) {
    double score = P._rar;
    if (score <= B._score) { return false; } // done with this player
    String newTeam = null;
    if (T._primaryTeam != null && !P._teams.contains(T._primaryTeam)) {
      score *= _teamweight;
    } else if (T._primaryTeam == null) {
      for (String team : P._teams) {
        if (!_teams_claimed.contains(team)) { newTeam = team; break; }
      }
      if (newTeam == null) { score *= _teamweight; }
    }
    if (score <= B._score) { return true; } // done with this player
    if (findRosterSpot(P, T, new ArrayList<Roster>(), score, B)) {
      B._team = newTeam;
      B._player = P;
    }
    return true;
  }

  public boolean findRosterSpot(Player P, Team T, ArrayList<Roster> rosters, double score, Best B) {
    ++B._attempts;
    boolean found = false;
    for (Roster R : T._roster) {
      if (!R.contains(P._pos)) { continue; }
      if (rosters.contains(R)) { continue; }
      // don't move an existing player to a spot that has more positional coverage
      // than his current position; it's ALWAYS better to put the new guy there
      if (!rosters.isEmpty() && R.containsAll(rosters.get(rosters.size() - 1))) { continue; }
      double rScore = score;
      if (rosters.isEmpty()) { // first player
        rScore *= R._spot._playingtime;
      } else {
        rScore += P._rar * (R._spot._playingtime - rosters.get(rosters.size() - 1)._spot._playingtime);
      }
      if (rScore < B._score) { continue; } // don't replace with a lower score
      if (rScore == B._score && !B._roster.isEmpty() && B._roster.size() <= rosters.size() + 1) { continue; }
      rosters.add(R);
      if (R._player == null) { // free spot
        B._roster.clear();
        B._roster.addAll(rosters);
        B._score = rScore;
        found = true;
      } else {
        if (findRosterSpot(R._player, T, rosters, rScore, B)) { found = true; }
      }
      rosters.remove(rosters.size() - 1);
    }
    return found;
  }

  public void draft(int num_teams, PrintWriter orderWriter) throws Exception {
    for (int i = 0; i != num_teams; ++i) {
      _teams.add(new Team(_spots));
    }
    for (int round = 0; round != _spots.size(); ++round) {
      for (int t = 0; t != num_teams; ++t) {
        Team T = _teams.get(round % 2 == 0 ? t : num_teams - t - 1);
        Best B = new Best();
        for (Player P : _players) { if (!findRosterSpot(P, T, B)) { break; } }
        if (B._player != null) {
          for (int i = B._roster.size() - 1; i != 0; --i) {
            B._roster.get(i)._player = B._roster.get(i-1)._player;
          }
          B._roster.get(0)._player = B._player;
          orderWriter.format("%d\t%d\t%d\t%s\t%s %s\t%.1f", round * num_teams + t + 1, round + 1, round % 2 == 0 ? t + 1 : num_teams - t, B._roster.get(0)._spot._pos, B._player._firstname, B._player._lastname, B._player._rar);
          for (int i = 1; i != B._roster.size(); ++i) {
            orderWriter.format("%s%s[%s]", i == 1 ? "\t" : " : ", B._roster.get(i)._player._lastname, B._roster.get(i)._spot._pos);
          }
          orderWriter.println();
          _players.remove(B._player);
          if (T._primaryTeam == null) { T._primaryTeam = B._team; _teams_claimed.add(B._team); }
          if (T._primaryTeam != null && B._player._teams.contains(T._primaryTeam)) { ++T._onteam; }
        }
      }
    }
  }
  
  @Override
  public ArrayList<Roster> elect(Candidates C) {
    Set<Player> players = C.toSortedPlayers();
    int teams = _rosters.size();
    for (int round = 0; round != _rounds; ++round) {
      for (int team = 0; team != teams; ++team) {
        // snake through the rosters
        Roster R = _rosters.get(round % 2 == 0 ? team : teams - team - 1);
        for (Player P : players) {
          if (!findRosterSpot(P, R, B)) { break; }
        }
        if (B._player != null) {
          B.assign();
          players.remove(B._player);
          if (R._primaryTeam == null) { R._primaryTeam = B._team; _teams_claimed.add(B._team); }
          if (R._primaryTeam != null && B._player._teams.contains(R._primaryTeam)) { ++T._onteam; }
        }
      }
    }
    
    return _rosters;
  }
  
  
  public DraftElector(int teams, String... pos) {
    _rosters = new ArrayList<Roster>(teams);
    for (int i = 0; i != teams; ++i) {
      _rosters.add(new Roster("#" + i, pos));
    }
    _rounds = pos.length;
  }

  private ArrayList<Roster> _rosters = null;
  private int _rounds = 0;
}
*/