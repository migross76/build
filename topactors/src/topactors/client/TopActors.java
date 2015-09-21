package topactors.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;

public class TopActors implements EntryPoint {
  /**
   * This is the entry point method.
   */
  @Override public void onModuleLoad() {
    MainPage MP = new MainPage();
    RootPanel.get("mainpage").add(MP);
    MP.init();
    System.err.println("Started");
  }
}

/* 305 actors, 321 movies
30.59   Robert De Niro
22.88   James Stewart
18.88   Jack Nicholson
18.25   Morgan Freeman
18.11   Harrison Ford
18.08   Cary Grant
17.84   Brad Pitt
17.48   Toshir� Mifune
17.29   Tom Hanks
17.25   Al Pacino
16.26   Clint Eastwood
15.50   Marlon Brando
15.35   Samuel L. Jackson
15.12   Bruce Willis
15.02   Keith David
14.92   Dustin Hoffman
14.74   Kevin Spacey
14.64   Humphrey Bogart
14.61   Henry Fonda
14.60   Robert Duvall
14.56   Elijah Wood
13.92   Takashi Shimura
13.79   Natalie Portman
13.43   Ian McKellen
13.12   Max von Sydow
12.91   William Holden
12.89   Matt Damon
12.76   Tatsuya Nakadai
12.62   Johnny Depp
12.59   Michael Caine
12.44   Orson Welles
11.96   Sigourney Weaver
11.92   Liam Neeson
11.77   John Ratzenberger
11.66   Alec Guinness
11.40   John Hurt
11.38   Paul Newman
11.33   Steve Buscemi
11.20   Christian Bale
11.09   William Fichtner
10.91   Ward Bond
10.75   Joe Pesci
10.73   Claude Rains
10.71   Charlton Heston
10.65   Leonardo DiCaprio
10.53   Thomas Mitchell
10.53   Mark Hamill
10.25   Adam Baldwin
10.05   Bill Murray
10.03   Gary Oldman
*/