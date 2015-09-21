package strat.client.model;

import java.util.EnumSet;

public class LineupRest {
  public static EnumSet<Position> selectPositions(Dice dice) {
    switch (dice.roll(6)) {
      case 1: return EnumSet.of(Position.CENTER, Position.THIRD, Position.DH);
      case 2: return EnumSet.of(Position.SHORT, Position.RIGHT, Position.FIRST);
      case 3: return EnumSet.of(Position.SECOND, Position.CATCH, Position.LEFT);
      case 4: return EnumSet.of(Position.SHORT, Position.LEFT, Position.FIRST);
      case 5: return EnumSet.of(Position.SECOND, Position.RIGHT, Position.DH);
      case 6: return EnumSet.of(Position.CENTER, Position.CATCH, Position.THIRD);
    }
    return null;
  }
  
  public static boolean restPlayer(int pa, Dice dice) {
    int roll = dice.roll(20);
    switch (roll) {
      case 1: return pa < 600;
      case 2: case 3: return pa < 575;
      case 4: case 5: case 6: return pa < 550;
      case 7: case 8: return pa < 525;
      case 9: case 10: return pa < 500;
      case 11: case 12: case 13: return pa < 475;
      case 14: case 15: return pa < 450;
      case 16: case 17: case 18: return pa < 425;
      case 19: return pa < 400;
      case 20: return pa < 375;
    }
    return false;
  }
}
