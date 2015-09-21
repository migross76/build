package strat.client.event;

import strat.client.model.FieldChart;
import strat.client.model.RunExpChart;
import com.google.web.bindery.event.shared.binder.GenericEvent;

public class InitResponse extends GenericEvent {
  public InitResponse(RunExpChart runexp, FieldChart field) { _runexp = runexp; _field = field; }
  
  public final RunExpChart _runexp;
  public final FieldChart _field;
}
