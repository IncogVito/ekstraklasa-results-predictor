package ekstraklasa.predictor.service;

import ekstraklasa.predictor.model.*;
import ekstraklasa.predictor.reader.CSVFileReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Prosty Monte Carlo: dla podanej liczby symulacji generuje wyniki dla wszystkich fixtures
 * na podstawie przewidywań i agreguje procentowe prawdopodobieństwo zajęcia danej pozycji przez drużyny.
 */
public class MonteCarloSimulation {

    private static final int DEFAULT_SIMULATIONS = 20000;
    private static final Random rand = new Random();


    public static SimulationResult run(CSVFileReader.ReadResult read, int simulations) throws Exception {
        List<MatchFixture> fixtures = read.fixtures;
        List<MatchResult> baseResults = read.results;
        List<FootballClub> clubs = read.clubs;

        if (fixtures == null || fixtures.isEmpty()) {
            throw new IllegalStateException("No fixtures to simulate");
        }

        // calculate strengths once
        var strength = TeamStrengthCalculationService.calculateAllTeamsStrength(clubs, baseResults);

        SimulationResult simulationResult = new SimulationResult(clubs.size());

        // ensure all teams are present
        for (FootballClub club : clubs) {
            simulationResult.ensureTeam(club.getCode());
        }

        for (int s = 0; s < simulations; s++) {
            System.out.print("\rSimulating... " + (s + 1) + "/" + simulations);

            // build simulation results list: copy base finished results
            List<MatchResult> allResults = new ArrayList<>(baseResults);

            // simulate all fixtures
            for (MatchFixture fixture : fixtures) {
                MatchProbability p = TeamFixturePredictor.predictFixtureFromClubMap(strength, fixture, null);
                Winner winner = OutcomeSampler.sample(p);

                MatchResult mr = new MatchResult();
                mr.setMatchId(fixture.getMatchId());
                mr.setFinished(true);
                mr.setHomeTeamCode(fixture.getHomeTeamCode());
                mr.setAwayTeamCode(fixture.getAwayTeamCode());

                // generate plausible score
                int homeGoals = 0;
                int awayGoals = 0;
                switch (winner) {
                    case HOME_WIN:
                        homeGoals = rand.nextInt(3) + 1; // 1..3
                        awayGoals = rand.nextInt(homeGoals); // 0..homeGoals-1
                        break;
                    case AWAY_WIN:
                        awayGoals = rand.nextInt(3) + 1; // 1..3
                        homeGoals = rand.nextInt(awayGoals); // 0..awayGoals-1
                        break;
                    default: // DRAW
                        homeGoals = rand.nextInt(3); // 0..2
                        awayGoals = homeGoals;
                }
                mr.setHomeGoals(homeGoals);
                mr.setAwayGoals(awayGoals);
                mr.setWinner(Winner.fromScore(homeGoals, awayGoals));

                allResults.add(mr);
            }

            // calculate table
            var standings = TableCalculationsService.calculateLeagueStandings(allResults);

            // increment positions
            for (var entry : standings) {
                String code = entry.getFootballClub() == null ? null : entry.getFootballClub().getCode();
                if (code == null) continue;
                simulationResult.increment(code, entry.getPosition());
            }
        }

        simulationResult.setSimulations(simulations);
        return simulationResult;
    }
}
