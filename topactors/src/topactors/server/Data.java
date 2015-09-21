package topactors.server;

import java.util.ArrayList;

public class Data {
  public ArrayList<Movie> _movies = new ArrayList<Movie>();
  public ArrayList<Actor> _actors = new ArrayList<Actor>();
  public ArrayList<Role>  _roles = new ArrayList<Role>();

  public void add(Actor A) { if (A != null) { _actors.add(A); } }

  public void add(Role R) { if (R != null) { _roles.add(R); } }

  public void add(Movie M) { if (M != null) { _movies.add(M); } }
}
