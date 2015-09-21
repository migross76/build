package util;

/*
 * The idea is to have stat accumulators (Player, Team/Franchise, Season) that collect individual stats and bin them based on certain traits (year, team, player).
 * The individual bins will be dynamic, such that I can register a new stat (getting a new offset) and use the id to set/get that value.
 * Bins will also be sorted, such as by year or frequency.
 * I can then calculate new values based on the old values.
 * I'll need some map of accumulators that will know how to register stats and also how to look up a specific accumulator.
 * Database population stops being grabbing raw object tables, but rather callbacks to populate these accumulators. 
 * I can create sets of stats, which each know how to register and populate themselves, so I can make one call to get all of their data.
 * 
 * Questions:
 * - What do I do with the Master table? Does it stay as an object table? Or must I populate certain accumulators with that data?
 * - How do I handle positional information, especially to do weighted appearances? I also want to leverage those values in other calculations (e.g., catcher boost).
 *   Maybe optionally attach a positional accumulator (bin by position) on each main accumulator.
 * - How many of the apps can be replaced with this new functionality? Hopefully, all plus some more.
 * 
 * StatLogics:
 * - WAR, WAA, and positive versions of each
 * - possibly keeping pitching and batting wins separate, until ready to combine
 * - WeightedWar
 * - WeightedWins (WAA * factor)
 * - ELO
 * - HOF (in/out, year, voting, how elected)
 * - Games into Seasons (35G/SP, 50G/RP, 100G/C, 120G/BT)
 * - Postseason - generate an approximate WAR value for these, ideally broken down by level
 * - Slots - fill in a player in this location with positional availability
 * - Shared Constants - e.g., final year in DB
 * -*** Hall Draft - ratio of per 100 players, or per 20 10-year players
 *                 - start with first 10 blocks, select 10;
 *                 - drop oldest block, add new block, select next
 * 
 * Apps:
 * - BestMinusFirst : Player, pWAR, by team, >WAR, primary(WAR) position
 * - BestSeasons : Season & Player, wWAR - how much above each player's season is to other top 10 players
 * - BestTeams : Team & Franchise, by player-season, WAR, WAA
 * - Eliminator, HallAnnual : Season[Player], then Player accumulating by season 
 * - HallAnalysis : Player (totals only), wWAR, primary(WAR), ELO, HOF, and others when they're ingested
 * - HallEra : Player[year], determine peak 5-year span (or 5-season span)
 * - HallPercentage & HP2 : Player[year], primary(WAR), HOF, 10-year player filter; bin by year and position
 * - HallReboot : Player[year] by birthYear, wWAR, primary(WAR)
 * - HOFVotes : Player[year], HOFvotes
 * - Incrementals : Player[year], wWAR, need to grab best N-year span
 * - OneFranchise : Player[franchise], games, years, primary(G)
 * - OneTwoPunch : Player[franchise block], Player, WAR
 * - Organize : Franchise[player], wWAR, primary(WAR)
 * - Overrated : Player, WAR, ELO, active
 * - Playoff : Team[wins]
 * - PlayoffSim : Team, Win%, runs scored, runs allowed
 * - Rushmore : Player[team|franchise], Team|Franchise[player], WAR
 * - SpanLeaders : Season[player], Player[position], WAR, primary(WAR)
 * - Spread : Season[player], standard deviation, WAR
 * - Top100 : Position[player], wWAR, primary(WAR)
 * - Top1000 : Player, wWAR, primary(WAR), HOF
 * - Top112Rating : Player, many different ranks
 * - TopRanks : Player, many different ranks
 * - TwoGloves : Position games per year
 * 
 * Big Apps:
 * - AllTime : draft 30 equal teams, picking the best seasons of the best players of all time
 * - Dynasty : given a year, draft teams that will succeed over multiple years; ideally re-draft N slots the each year (i.e. Trout >> Cabrera due to age)
 * - MyHall : develop comprehensive rating system to pick my HOF - regular season, playoffs, missed time, excellence, longevity, acclaim, off-field, character
 * 
 */
public class StatBlock {

}
