package topactors.client;

import topactors.shared.BestActor;
import topactors.shared.NextInfo;
import topactors.shared.Update;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;

/* TODO Main list
 *
 * Fix data retrieval/accuracy
 * - How to remove roles that were initially added by Oscars, but don't meet the score threshold 
 * Example: Meryl Streep
 * | The French Lieutenant's Woman                | 0.207 |  0.000 |     0 | 0.000 | SELF N     | 6.80/4836
 * | Ironweed                                     | 0.207 |  0.000 |     0 | 0.000 | SELF N     | 6.71/3281
 * | A Cry in the Dark                            | 0.207 |  0.000 |     0 | 0.000 | SELF N     | 6.90/3431
 * | Postcards from the Edge                      | 0.207 |  0.000 |     0 | 0.000 | SELF N     | 6.50/6204
 * | One True Thing                               | 0.207 |  0.000 |     0 | 0.000 | SELF N     | 6.90/5786
 * | Music of the Heart                           | 0.207 |  0.000 |     0 | 0.000 | SELF N     | 6.50/5781
 * | The Devil Wears Prada                        | 0.207 |  0.000 |     0 | 0.000 | SELF N     | 6.70/75007
 * + Clean up Role names : replace newlines with space, remove descriptors (voice: Japanese version), etc.
 * 
 * Update Model Logic
 * 
 * Algorithm
 * - Consider reflecting star percentage in estimating the overall score (regressed, I think)
 * - Consider giving a small bonus to older movies
 * 
 * Reports
 * - Filter on Year ranges
 * - More (specialized) columns, such as top roles
 * - Export to Excel
 * 
 * Next Algorithm
 * - Pull in next actor/movie lists, when it starts to run dry - maybe only get these every N queries, from the database
 * - Grab only N at a time, don't update live, fetch more when down to M (display amount)
 * - Popup criteria for selecting next
 *   - Standard (score * occurrences + oscar bonus)
 *   - Simple (just the score)
 *   - Limit to movies for top N actors / Pro-rate for movies with high-scoring actors, actors with many unprocessed records
 * 
 * Improve display
 * - Add highlighting indicators
 *   - Best actor is unprocessed
 *   - Last processed is Actor or Movie
 *   - Best actor changes (new actor, updated score)
 * - Set hard column widths for movies (cut off?)
 * - Stop word-wrapping actor names
 * - Graphics - Oscar icon, Star icon, Movie icon
 * - Provide cleaner logging lines (including proper use of System.out vs. System.err)
 * - Link display for actor name (with cursor change) for popup
 * 
 * Content
 * - Do I treat TV series as movie series, with each season being equivalent to a movie?
 * 
 * Skill Set
 * - Consider Unit Testing
 * - Hook in Hibernate
 * 
 * Other
 * - Consider breaking data vs. controller into separate packages
 * - Pause retrieval while viewing details?
 */
public class MainPage extends Composite {

  private static MainPageUiBinder uiBinder = GWT.create(MainPageUiBinder.class);
  interface MainPageUiBinder extends UiBinder<Widget, MainPage> { /*binder*/ }

  @UiField ToggleButton _buttonFetch;
  @UiField BestActors   _bestActors;
  @UiField LastProcessedTable _last;
  @UiField NextTable    _nextActors;
  @UiField NextTable    _nextMovies;
  @UiField InlineLabel  _actorCount;
  @UiField InlineLabel  _movieCount;
  
  private int _movies = 0;
  private int _actors = 0;
  
  private static final int MOVIE_COUNT = 2;
  private static final int ACTOR_COUNT = 1;
  private int _rotation = 0;
  private String _lastID = null;

  private final ActorServiceAsync actorService = GWT.create(ActorService.class);

  private AsyncCallback<Update> _update = new AsyncCallback<Update>() {
    @Override
    public void onFailure(Throwable caught) { System.err.println("Error : " + caught); }

    @Override
    public void onSuccess(Update result) {
      if (_lastID != null) {
        if (_rotation < MOVIE_COUNT) {
          _nextMovies.remove(_lastID);
          _movieCount.setText("" + (++_movies));
        } else {
          _nextActors.remove(_lastID);
          _actorCount.setText("" + (++_actors));
        }
      } else {
        _actorCount.setText("" + (_actors = result._processed_actors));
        _movieCount.setText("" + (_movies = result._processed_movies));
      }
      for (NextInfo N : result._nextActors) { _nextActors.update(N); }
      for (NextInfo N : result._nextMovies) { _nextMovies.update(N); }
      for (BestActor A : result._bestActors) { _bestActors.add(A); }
      _last.insert(result._last);
      _bestActors.update();
      _nextActors.update(true);
      _nextMovies.update(true);
      _last.update();
      if (_buttonFetch.isDown()) { Timer T = new Timer() {
        @Override public void run() { fetch(); } };
        T.schedule(1000);
      }
    }
  };

  private void fetch() {
    _rotation = (_rotation + 1) % (ACTOR_COUNT + MOVIE_COUNT);
    if (_rotation < MOVIE_COUNT) {
      _lastID = _nextMovies.next()._id;
      actorService.fetchMovie(_lastID, _update);
    } else {
      _lastID = _nextActors.next()._id;
      actorService.fetchActor(_lastID, _update);
    }
  }
  
  /** @param e mark it as a ClickHandler */
  @UiHandler("_buttonFetch")
  /*package*/ void onClick(ClickEvent e) {
    if (_buttonFetch.isDown()) { fetch(); }
  }

  public void init() {
    actorService.init(_bestActors.getSize(), 500, _update);
  }
  
  public MainPage() {
    initWidget(uiBinder.createAndBindUi(this));
  }
}
