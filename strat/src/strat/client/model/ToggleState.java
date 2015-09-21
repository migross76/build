package strat.client.model;

import java.util.EnumSet;

public class ToggleState {
  public final EnumSet<TogglePlay.Type> _main = EnumSet.noneOf(TogglePlay.Type.class);
  public final EnumSet<TogglePlay.Type> _alt = EnumSet.noneOf(TogglePlay.Type.class);
}