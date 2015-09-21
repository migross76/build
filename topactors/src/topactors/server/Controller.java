package topactors.server;

import java.util.ArrayList;
import java.util.Collections;

public class Controller {
  private static Controller _instance = new Controller();
  public static Controller instance() { return _instance; }
  
  public Model getModel() { return _model; }
  
  public void load() {
    if (_data_store != null) { update(_data_store.init(), false); } // don't store the results in the store, obviously
    for (InitHandler handler : _inits) { update(handler.init()); }
  }
  
  public void fillUnfinishedMovies(Data D) {
    for (Movie M : _model._movies.values()) {
      if (M._processed == null) { D.add(M); }
    }
    Collections.sort(D._movies);
  }
  
  public Data requestMovie(String movieID) {
    if (_movie_request == null) { return new Data(); }
    Movie M = _model._movies.get(movieID);
    String name = M == null ? "TODO" : M._name;
    if (M != null) { System.out.println("Request : " + M._name + "[" + M._id + "]"); }
    return update(_movie_request.request(movieID, name));
  }
  
  public Data requestActor(String actorID) {
    if (_actor_request == null) { return new Data(); }
    Actor A = _model._actors.get(actorID);
    String name = A == null ? "TODO" : A._name;
    if (A != null) { System.out.println("Request : " + A._name + "[" + A._id + "]"); }
    return update(_actor_request.request(actorID, name));
  }
  
  public void addInitHandler(InitHandler handler) {
    _inits.add(handler);
  }
  
  public void setMovieRequestHandler(RequestHandler handler) {
    _movie_request = handler;
  }
  
  public void setActorRequestHandler(RequestHandler handler) {
    _actor_request = handler;
  }
  
  public void setDataStore(DataStore store) {
    _data_store = store;
  }
  
  private Data update(Data D) {
    return update(D, true);
  }
  
  private Data update(Data D, boolean store) {
    D = _model.update(D);
    if (store && _data_store != null) { _data_store.update(D); }
    return D;
  }
  
  private Controller() { }

  private ArrayList<InitHandler> _inits = new ArrayList<InitHandler>();
  private RequestHandler _actor_request = null;
  private RequestHandler _movie_request = null;
  private DataStore      _data_store = null;
  private Model _model = new Model();
}
