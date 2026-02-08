package ekstraklasa.predictor;

import ekstraklasa.predictor.model.LeagueTable;
import ekstraklasa.predictor.model.SimulationResult;
import ekstraklasa.predictor.reader.CSVFileReader;
import ekstraklasa.predictor.service.MonteCarloSimulation;
import ekstraklasa.predictor.service.OutcomeSampler;
import ekstraklasa.predictor.service.TableCalculationsService;
import ekstraklasa.predictor.service.TeamFixturePredictor;
import ekstraklasa.predictor.service.TeamStrengthCalculationService;
import lombok.SneakyThrows;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    @SneakyThrows
    static void main() {
        var result = CSVFileReader.readConstantFile();
        System.out.print(result);
        var standings = TableCalculationsService.calculateLeagueStandings(result.results);
        System.out.print(LeagueTable.of(standings));

        var strength = TeamStrengthCalculationService.calculateAllTeamsStrength(result.clubs, result.results);
        var sortedStrength = strength.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .toList();


        var prediction = TeamFixturePredictor.predictFixtureFromClubMap(strength, result.fixtures.getFirst(), null);
        var predictedResult = OutcomeSampler.sample(prediction);
        System.out.print(prediction);

        // Run Monte Carlo simulation (default 2000 iterations)
        SimulationResult sim = MonteCarloSimulation.run();
        System.out.println("\n--- Monte Carlo simulation results (percentages) ---");
        var pct = sim.getPercentages();
        for (var e : pct.entrySet()) {
            String code = e.getKey();
            double[] arr = e.getValue();
            StringBuilder sb = new StringBuilder();
            sb.append(code).append(": ");
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(i + 1).append("st: ").append(String.format("%.2f%%", arr[i]));
            }
            System.out.println(sb.toString());
        }
    }
}
