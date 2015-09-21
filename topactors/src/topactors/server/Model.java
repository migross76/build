package topactors.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import topactors.shared.Gender;
import topactors.shared.Oscar;
/*
    Merge all the movies
    Add to movie set
    Update movie scores
    - If changed, put roles into role set
    
    Merge all the actors
    Add to actor set
    
    Merge all the roles
    Add to role set
    
    For each role in role set
    - Calculate (non-series) SAT
    - If SAT < 0, flag as deleted, and register as such
    - Add actor to verification set
    
    For each actor in verification set
    - Get roles for the actor, sort by SAT
    - Determine series weights (keep at 0 if no other in series), calculate series SAT (add updates to role set)
    - Sum up actor weight, apply gender factor
    * Consider tracking roles inside of actor
    * Consider tracking counts, for easy creation of BestActor
    * Any way to optimize calculation?
*/

/*
 * Movie calculation
 * - IMDB regression algorithm (3000 votes, 6.9 rating) : return (votes / (votes+MIN_VOTES)) * score + (MIN_VOTES / (votes+MIN_VOTES)) * MEAN_SCORE;
 * - Add in Oscar factor : sqrt(score^2+oscar^2)
 * Role calculation
 * - Movie score
 * - Voice Penalty (0.75)
 * - Factor in Level (2x : Star, 1x : Regular, 0.5x Cameo)
 * - Factor in Type (1x : Normal, 0.5 : Self)
 * - Add in Oscar factor : sqrt(score^2+oscar^2)
 * Series calculation
 * - Order series based on role score (so highest score gets biggest boost)
 * - Divide by the series (3rd rank is 1/3 role score)
 * Actor calculation
 * - Sum of all role series numbers
 * - Female actors get bonus (2x) to even out scores
 */
public class Model {
  public HashMap<String, Actor> _actors = new HashMap<String, Actor>();

  public HashMap<String, Movie> _movies = new HashMap<String, Movie>();
  
  public HashMap<String, ArrayList<Role>> _roles = new HashMap<String, ArrayList<Role>>();
  
  // String is actor_id|movie_id
  public HashMap<String, Role> _exact_roles = new HashMap<String, Role>();
  
  public HashSet<Actor> _unprocessed_actors = new HashSet<Actor>();
  public HashSet<Movie> _unprocessed_movies = new HashSet<Movie>();

  private Movie merge(Movie obj) {
    Movie mod = _movies.get(obj._id);
    if (mod == null) { _movies.put(obj._id, mod = obj); } else { mod.update(obj); }
    return mod;
  }

  private Actor merge(Actor obj) {
    Actor mod = _actors.get(obj._id);
    if (mod == null) { _actors.put(obj._id, mod = obj); } else { mod.update(obj); }
    return mod;
  }


  private void invalidate(Movie M) {
    _movies.remove(M._id);
    _unprocessed_movies.remove(M);
  }
  
  public Data update2(Data in) {
    Data out = new Data();
    for (Movie M : in._movies) {
      M = merge(M);
      double oldsat = M._sat;
      M.calculate();
      ArrayList<Role> roles = _roles.get(M._id);
      if (M._sat < 0 && (roles == null || roles.isEmpty())) {
        invalidate(M); return null;
      }
      if (M._processed == null) { _unprocessed_movies.add(M); }
      else { _unprocessed_movies.remove(M); }
      // trickle this up to roles and actors
      if (oldsat != M._sat && roles != null) { out._roles.addAll(roles); }
      out.add(M);
    }
    for (Actor A : in._actors) {
      A = merge(A);
      if (A._processed == null) { _unprocessed_actors.add(A); }
      else { _unprocessed_actors.remove(A); }
      out.add(A);
    }

    return out;
  }
  
  public Data update(Data D) {
    Data newD = new Data();
    for (Movie M : D._movies) { newD.add(add(M)); }
    for (Actor A : D._actors) { newD.add(add(A)); }
    for (Role  R : D._roles)  { newD.add(add(R)); }
    return newD;
  }

  private Actor add(Actor upA) {
    Actor modA = _actors.get(upA._id);
    if (modA == null) { // add a new actor
      _actors.put(upA._id, modA = upA);
    } else if (upA._processed != null) { // update an existing actor with the official info
      modA._processed = upA._processed;
      if (modA._gender == Gender.Unknown) { modA._gender = upA._gender; modA._sat *= modA._gender.getWeight(); }
    } else {
      return modA; // no changes for this Actor
    }
    _unprocessed_actors.remove(modA);
    if (modA._processed == null) { _unprocessed_actors.add(modA); }
    return modA;
  }
  
  private Movie add(Movie upM) {
    Movie modM = _movies.get(upM._id);
    boolean is_new = false;
    if (modM == null) { // add a new movie
      _movies.put(upM._id, modM = upM); is_new = true;
    } else if (modM._processed == null) { // update an existing movie with the official info
      modM.update(upM);
    } else if (upM._oscar != Oscar.NONE) { // update Oscar info, if available
      modM._oscar = upM._oscar;
    } else {
      return modM; // no changes for this Movie
    }
    modM.calculate();
    if (is_new && modM._sat < 0) { // don't ever put a non-scoring movie in, b/c Oscars are added first, and always positive
      _movies.remove(modM._id);
      // System.err.println("Ignoring movie : " + modM._name + "[" + modM._sat + "]");
      return null;
    }
    _unprocessed_movies.remove(modM);
    if (modM._processed == null) { _unprocessed_movies.add(modM); }
    // trickle this up to roles and actors
    ArrayList<Role> roles = _roles.get(modM._id);
    if (roles != null) {
      for (Role R : _roles.get(modM._id)) { R.calculate(); }
    }
    return modM;
  }
  
  private Role add(Role upR) {
    // Map the actor and movie to the cached version
    upR._actor = _actors.get(upR._actor._id);
    upR._movie = _movies.get(upR._movie._id);
    if (upR._actor == null || upR._movie == null) { return null; } // trimmed b/c it's worthless
    // Find the matching role (actor id/movie id), inserting if new
    String unique_id = upR.getUniqueID();
    Role modR = _exact_roles.get(unique_id);
    // Insert if new; else update Oscar/Level/ID/Name
    String oldid = null;
    if (modR == null) { _exact_roles.put(unique_id, modR = upR); }
    else { oldid = modR.update(upR); } // TODO: Remove old registered role
    // Calculate base score
    modR.calculate();
    // Find matching movie/role pair, ensuring it is present
    ArrayList<Role> movie_roles = _roles.get(modR._movie._id);
    if (movie_roles == null) { _roles.put(modR._movie._id, movie_roles = new ArrayList<Role>()); }
    if (!movie_roles.contains(modR)) { movie_roles.add(modR); }
    
    // Find matching actor/role pairs, and adjust series numbers
    ArrayList<Role> actor_roles = _roles.get(modR._actor._id);
    if (modR._sat <= 0) {
      _exact_roles.remove(unique_id);
      if (actor_roles != null) { actor_roles.remove(modR); }
      return null;
    }
    if (actor_roles == null) { _roles.put(modR._actor._id, actor_roles = new ArrayList<Role>()); }
    else { actor_roles.remove(modR); modR._actor._sat += modR.adjustSeries(actor_roles, oldid == null); }
//System.err.print(upR._movie._name + " : " + upR._movie._sat);
//System.err.print(" :: " + modR._name + " : " + modR._sat_series);
    actor_roles.add(modR);
    // Calculate series score, pushing the adjustment up to the Actor
    modR._actor._sat += (modR.calculateSeries() * modR._actor._gender.getWeight());
//System.err.println(" :: " + modR._actor._name + " : " + modR._actor._sat);
    return modR;
  }
}
