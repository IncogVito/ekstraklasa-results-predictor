package ekstraklasa.predictor;

import ekstraklasa.predictor.model.LeagueTable;
import ekstraklasa.predictor.reader.CSVFileReader;
import ekstraklasa.predictor.service.TableCalculationsService;
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
    }
}
