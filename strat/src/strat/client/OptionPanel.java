package strat.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class OptionPanel extends Composite {

  private static OptionPanelUiBinder uiBinder = GWT
                                                  .create(OptionPanelUiBinder.class);

  interface OptionPanelUiBinder extends UiBinder<Widget, OptionPanel> {}

  public OptionPanel() {
    initWidget(uiBinder.createAndBindUi(this));
  }

}
