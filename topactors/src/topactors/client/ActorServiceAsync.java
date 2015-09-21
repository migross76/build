package topactors.client;

import java.util.ArrayList;
import topactors.shared.ActorDetail;
import topactors.shared.BestActor;
import topactors.shared.NextInfo;
import topactors.shared.Update;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface ActorServiceAsync {
  void init(int bestSize, int nextSize, AsyncCallback<Update> callback);
  
  void fetchActor(String actorID, AsyncCallback<Update> callback);
  
  void fetchMovie(String movieID, AsyncCallback<Update> callback);
  
  void getBestActors(int start, int end, AsyncCallback<ArrayList<BestActor>> callback);
  
  void getNextActors(int start, int end, AsyncCallback<ArrayList<NextInfo>> callback);
  
  void getNextMovies(int start, int end, AsyncCallback<ArrayList<NextInfo>> callback);

  void getActorDetails(String actorID, AsyncCallback<ArrayList<ActorDetail>> callback);
}
