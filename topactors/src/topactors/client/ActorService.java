package topactors.client;

import java.util.ArrayList;
import topactors.shared.ActorDetail;
import topactors.shared.BestActor;
import topactors.shared.NextInfo;
import topactors.shared.Update;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("actor")
public interface ActorService extends RemoteService {
  Update init(int bestSize, int nextSize);

  Update fetchActor(String actorID);
  
  Update fetchMovie(String movieID);

  ArrayList<BestActor> getBestActors(int start, int end);

  ArrayList<NextInfo> getNextActors(int start, int end);

  ArrayList<NextInfo> getNextMovies(int start, int end);
  
  ArrayList<ActorDetail> getActorDetails(String actorID);
}
