package strat.shared;

import java.util.ArrayList;

public class Roster implements java.io.Serializable {
  private static final long serialVersionUID = 363561234572171616L;

  public String _name = null;
  public ArrayList<CardLegacy> _players = new ArrayList<>();
}
