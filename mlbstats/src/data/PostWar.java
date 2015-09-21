package data;

/**
 * wWAR* = (war + pwar) & (playtime + pplaytime)
 * - League wOBA/FIP = 90% for postseason
 * Batters:
 * - rBat = wRAA = ((wOBA – league wOBA) / wOBA scale) * PA
 * - rPos = C=+12.5, 1B=-12.5, 2B/3B/CF=+2.5, SS=+7.5, LF/RF=-7.5, DH=-17.5 (per 162 defensive G)
 * - rRepl = 20/600*PA
 * - Runs->Wins = 10
 * - rBase/rField/Context/Park Factors = N/A
 * Pitchers:
 * - FIP
 * - Replacement = 0.38 Starter, 0.47 Reliever
 * - http://www.fangraphs.com/blogs/index.php/pitcher-win-values-explained-part-seven/
 * I need:
 * - Fangraph Guts : http://www.fangraphs.com/guts.aspx?type=cn
 * - BR wOBA : http://www.baseball-reference.com/about/war_explained_wraa.shtml
 * - BR Repl : http://www.baseball-reference.com/about/war_explained_position.shtml
 * 
 * - League wOBA/FIP - either calculate or download
 * 
 * Catcher Interference - the missing piece of AB to PA:

Rk  Player  Date    Series  Gm#     Tm  Opp     Rslt    PA  AB  R   H   2B  3B  HR  RBI     BB  IBB     SO  HBP     SH  SF  ROE     XI  GDP     SB  CS  WPA     RE24    aLI     BOP     Pos Summary
1   Lance Berkman   2011-10-07  NLDS    5   STL     PHI     W 1-0   4   3   0   0   0   0   0   0   0   0   1   0   0   0   0   1   0   0   0   -0.086  -0.880  1.192   4   RF
2   Jacoby Ellsbury     2009-10-08  ALDS    1   BOS     LAA     L 0-5   4   3   0   0   0   0   0   0   0   0   0   0   0   0   0   1   0   0   0   -0.027  -0.584  .695    1   CF
3   Mike Scioscia   1985-10-14  NLCS    5   LAD     STL     L 2-3   4   2   0   1   0   0   0   0   1   0   0   0   0   0   0   1   0   0   0   0.075   0.719   1.268   6   C
4   George Hendrick     1982-10-15  WS  3   STL     MIL     W 6-2   4   2   1   1   0   0   0   0   1   0   0   0   0   0   0   1   0   0   1   0.025   0.556   .556    4   RF
5   Richie Hebner   1974-10-08  NLCS    3   PIT     LAD     W 7-0   4   3   1   2   0   0   1   3   0   0   0   0   0   0   0   1   0   0   0   0.148   2.847   .230    7   3B
6   Pete Rose   1970-10-10  WS  1   CIN     BAL     L 3-4   5   3   0   0   0   0   0   0   1   0   0   0   0   0   0   1   0   0   0   -0.004  -0.305  1.374   1   RF
7   Ken Boyer   1964-10-12  WS  5   STL     NYY     W 5-2   5   4   0   1   0   0   0   0   0   0   0   0   0   0   0   1   0   0   0   0.098   0.276   1.152   4   3B
8   Bud Metheny     1943-10-06  WS  2   NYY     STL     L 3-4   4   3   0   0   0   0   0   0   0   0   0   0   0   0   0   1   0   0   0   0.029   0.038   .995    2   RF
9   Roger Peckinpaugh   1925-10-15  WS  7   WSH     PIT     L 7-9   4   3   1   1   0   0   1   2   0   0   0   0   0   0   0   1   0   0   0   0.286   1.554   1.065   7   SS

 */
public class PostWar {
 // TODO to implement
}
