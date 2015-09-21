package strat.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootLayoutPanel;

public class Strat implements EntryPoint {
  @SuppressWarnings("unused") private GameController _gc = null;
  
  @Override public void onModuleLoad() {
    RootLayoutPanel.get().add(new MainPanel());
    _gc = new GameController();
  }
}
