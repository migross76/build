package topactors.server;

import com.google.gwt.event.shared.EventHandler;

public class RefreshEvent extends DataEvent<RefreshEvent.Handler> {
  public interface Handler extends EventHandler { void onEvent(RefreshEvent event); }
  public static final Type<Handler> TYPE = new Type<Handler>();
  @Override public Type<Handler> getAssociatedType() { return TYPE; }
  @Override protected void dispatch(Handler handler) { handler.onEvent(this); }
  
  public RefreshEvent() { }
}
