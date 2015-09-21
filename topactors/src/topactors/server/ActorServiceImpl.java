package topactors.server;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import topactors.client.ActorService;
import topactors.server.Role.Level;
import topactors.shared.ActorDetail;
import topactors.shared.BestActor;
import topactors.shared.LastProcessed;
import topactors.shared.NextInfo;
import topactors.shared.Oscar;
import topactors.shared.Update;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class ActorServiceImpl extends RemoteServiceServlet implements ActorService {
  @Override
  public void init() {
    System.out.println("Initializing");
    try { _database = new MyDatabase(); } catch (SQLException e) { throw new RuntimeException("could not connect to database : " + e); }
    Repository cache = new Repository(_database);
    cache.setDelay(2000);
    // DirCache cache = new DirCache("C:/build/imdbparsers/cache");
    _controller.addInitHandler(new Top250Parser(cache));
    _controller.addInitHandler(new OscarParser(cache));
    _controller.setMovieRequestHandler(new MovieParser(cache));
    _controller.setActorRequestHandler(new ActorParser(cache));
    _controller.setDataStore(new DataStore(_database));
    
    _controller.load();
    System.out.format("%d actors, %d movies in initial set\n", _controller.getModel()._actors.size(), _controller.getModel()._movies.size());
  }
  
  @Override
  public ArrayList<BestActor> getBestActors(int start, int end) {
    ArrayList<Actor> actors = new ArrayList<Actor>(_controller.getModel()._actors.values());
    Collections.sort(actors);
    ArrayList<BestActor> best = new ArrayList<BestActor>();
    for (int i = start; i != end; ++i) {
      best.add(createBestActor(actors.get(i)));
    }
    return best;
  }
  
  private int getRoleCount(String id) {
    ArrayList<Role> roles = _controller.getModel()._roles.get(id);
    return roles == null ? 0 : roles.size();
  }
  
  @Override
  public ArrayList<NextInfo> getNextActors(int start, int end) {
    ArrayList<Actor> raw = new ArrayList<Actor>(_controller.getModel()._unprocessed_actors);
    Collections.sort(raw);
    ArrayList<NextInfo> list = new ArrayList<NextInfo>();
    for (int i = start; i != end; ++i) {
      Actor next = raw.get(i);
      list.add(new NextInfo(next._id, next._name, next._sat, getRoleCount(next._id), 0));
    }
    return list;
  }

  @Override
  public ArrayList<NextInfo> getNextMovies(int start, int end) {
    ArrayList<Movie> raw = new ArrayList<Movie>(_controller.getModel()._unprocessed_movies);
    Collections.sort(raw);
    ArrayList<NextInfo> list = new ArrayList<NextInfo>();
    for (int i = start; i != end; ++i) {
      Movie next = raw.get(i);
      list.add(new NextInfo(next._id, next._name, next._sat, getRoleCount(next._id), next._oscar.getScore()));
    }
    //debugNextMovies(list, "init");
    return list;
  }

  @Override
  public Update init(int bestSize, int nextSize) {
    Update U = new Update();
    U._bestActors.addAll(getBestActors(0, bestSize));
    U._nextActors.addAll(getNextActors(0, nextSize));
    U._nextMovies.addAll(getNextMovies(0, nextSize));
    Model M = _controller.getModel();
    U._processed_actors = M._actors.size() - M._unprocessed_actors.size();
    U._processed_movies = M._movies.size() - M._unprocessed_movies.size();
    return U;
  }
    
  
  private static LastProcessed getLastProcessed(Data dModel) {
    LastProcessed LP = null;
    for (Role R : dModel._roles) {
      if (LP == null) {
        LP = new LastProcessed(R._actor._name, R._movie._name, R._actor._sat, R._sat_series);
      } else if (LP._actorSAT < R._actor._sat ||
                 (LP._actorSAT == R._actor._sat && LP._roleSAT < R._sat_series)) {
        LP.set(R._actor._name, R._movie._name, R._actor._sat, R._sat_series);
      }
    }
    return LP;
  }
  
  private BestActor createBestActor(Actor A) {
    BestActor BA = new BestActor();
    BA._id = A._id;
    BA._name = A._name;
    BA._processed = (A._processed != null);
    BA._sat = A._sat;
    BA._gender = A._gender;
    Model model = _controller.getModel();
    ArrayList<Role> roles = model._roles.get(A._id);
    if (roles != null) {
      for (Role R : model._roles.get(A._id)) {
        Movie M = R._movie;
        ++BA._movies;
        if (M._oscar == Oscar.WINNER) { ++BA._oscars; } 
        if (R._oscar == Oscar.WINNER) { ++BA._oscars; } 
        if (M._oscar == Oscar.NOMINATED) { ++BA._nominations; } 
        if (R._oscar == Oscar.NOMINATED) { ++BA._nominations; }
        if (M._processed == null) { ++BA._movies_unprocessed; }
        if (R._level == Level.STAR) { ++BA._stars; }
      }
    }
    return BA;
  }
  
  @Override
  public Update fetchActor(String actorID) {
    Data dModel = _controller.requestActor(actorID);
    Update U = new Update();
    for (Movie M : dModel._movies) {
      if (M._processed == null) { U._nextMovies.add(new NextInfo(M._id, M._name, M._sat, getRoleCount(M._id), M._oscar.getScore())); }
    }
    for (Actor A : dModel._actors) { U._bestActors.add(createBestActor(A)); }
    U._last = getLastProcessed(dModel);
    //debugNextMovies(U._nextMovies, _controller.getModel()._actors.get(actorID)._name);
    //debug(dModel, U);
    return U;
  }

  @Override
  public Update fetchMovie(String movieID) {
    Data dModel = _controller.requestMovie(movieID);
    Update U = new Update();
    for (Actor A : dModel._actors) {
      if (A._processed == null) { U._nextActors.add(new NextInfo(A._id, A._name, A._sat, getRoleCount(A._id), 0)); }
      U._bestActors.add(createBestActor(A));
    }
    U._last = getLastProcessed(dModel);
    //debug(dModel, U);
    return U;
  }
  
  @Override public ArrayList<ActorDetail> getActorDetails(String actorID) {
    ArrayList<ActorDetail> details = new ArrayList<ActorDetail>();
    ArrayList<Role> roles = _controller.getModel()._roles.get(actorID);
    if (roles != null) {
      for (Role R : roles) {
        ActorDetail AD = new ActorDetail();
        AD._name = R._movie._name;
        AD._processed = R._movie._processed != null;
        AD._star = R._level == Level.STAR;
        AD._self = R._type == Role.Type.SELF;
        AD._score = R._movie._score;
        AD._sat = R._sat_series;
        AD._voice = R._voice;
        AD._series = R._series;
        AD._oscar_role = R._oscar;
        AD._oscar_movie = R._movie._oscar;
        details.add(AD);
      }
      Collections.sort(details);
    }
    return details;
  }
  
  /*  
  private void debugNextMovies(Collection<NextInfo> list, String name) {
    ArrayList<NextInfo> sorted = new ArrayList<NextInfo>(list);
    Collections.sort(sorted);
    System.err.print("  Next Movies[" + name + "] :");
    int i = 0;
    for (NextInfo N : sorted) {
      System.err.format(" %s[%d]", N._name.length() > 20 ? N._name.substring(0, 17) + "..." : N._name, N._occurrences);
      if (++i == 10) { break; }
    }
    System.err.println();
  }
  

  private void debug(Data D, Update U) {
    System.err.print("  Known Actors[" + D._actors.size() + "]");
    System.err.println();
    System.err.print("  Best Actors[" + U._bestActors.size() + "] :");
    for (BestActor A : U._bestActors) {
      System.err.format(" %s[%s]:%.2f", A._name, A._id, A._sat);
    }
    System.err.println();
  }
*/
  @Override public void destroy() {
    if (_database != null) { _database.close(); }
  }
  
  private Controller _controller = Controller.instance();
  private MyDatabase _database = null;
}
