package topactors.server;

import java.util.Collection;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public abstract class DataEvent<T extends EventHandler> extends GwtEvent<T> {
  
  public Collection<Actor> _actors = null;
  public Collection<Role>  _roles = null;
  public Collection<Movie> _movies = null;

}
