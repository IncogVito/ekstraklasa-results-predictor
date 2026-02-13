package ekstraklasa.predictor.service;

import ekstraklasa.predictor.entity.SimulatedStandingEntity;
import ekstraklasa.predictor.model.SimulationResult;
import ekstraklasa.predictor.model.LeagueStandingsEntry;
import ekstraklasa.predictor.reader.CSVFileReader;
import ekstraklasa.predictor.repository.SimulatedStandingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class SimulatedStandingService {

    private final SimulatedStandingRepository simulatedStandingRepository;

    // nowa metoda przyjmująca już wczytany plik (np. z folderu consumable)
    public List<SimulatedStandingEntity> generateAndSaveSimulatedStandings(CSVFileReader.ReadResult read, Integer monteCarloSimulations, Instant timestamp) throws Exception {
        if (read == null) return Collections.emptyList();

        List<LeagueStandingsEntry> standings = TableCalculationsService.calculateLeagueStandings(read.results);

        Map<String, double[]> percentages = Collections.emptyMap();
        if (monteCarloSimulations != null && monteCarloSimulations > 0) {
            SimulationResult sim = MonteCarloSimulation.run(read, monteCarloSimulations);
            percentages = sim.getPercentages();
        }

        Instant ts = timestamp == null ? Instant.now() : timestamp;

        List<SimulatedStandingEntity> entities = new ArrayList<>();
        final int relegationSlots = 2; // domyślna liczba miejsc spadkowych

        for (LeagueStandingsEntry entry : standings) {
            if (entry == null || entry.getFootballClub() == null) continue;
            String code = entry.getFootballClub().getCode();
            if (code == null) continue;

            Double top4Prediction = null;
            Double relegationPrediction = null;

            if (percentages != null && percentages.containsKey(code)) {
                double[] arr = percentages.get(code);
                // sumujemy prawdopodobieństwa dla pozycji 1..4
                int topN = Math.min(4, arr.length);
                double sumTop = 0.0;
                for (int i = 0; i < topN; i++) sumTop += arr[i];
                top4Prediction = sumTop;

                // sumujemy prawdopodobieństwa spadku (ostatnie `relegationSlots` pozycji)
                int start = Math.max(0, arr.length - relegationSlots);
                double sumRel = 0.0;
                for (int i = start; i < arr.length; i++) sumRel += arr[i];
                relegationPrediction = sumRel;
            }

            SimulatedStandingEntity entity = SimulatedStandingEntity.builder()
                    .footballClubCode(code)
                    .timestamp(ts)
                    .matchPlayed(entry.getPlayedGames())
                    .ranking(entry.getPosition())
                    .points(entry.getPoints())
                    .top4Prediction(top4Prediction)
                    .relegationPrediction(relegationPrediction)
                    .build();

            entities.add(entity);
        }

        if (entities.isEmpty()) return Collections.emptyList();

        log.info("Saving {} simulated standings to the database...", entities.size());
        return simulatedStandingRepository.saveAll(entities);
    }
}
