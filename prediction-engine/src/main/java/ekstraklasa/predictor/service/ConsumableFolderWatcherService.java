package ekstraklasa.predictor.service;

import ekstraklasa.predictor.cache.SimulatedStandingCacheRepository;
import ekstraklasa.predictor.cache.TeamStrengthCacheRepository;
import ekstraklasa.predictor.reader.CSVFileReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumableFolderWatcherService {

    private final SimulatedStandingService simulatedStandingService;
    private final SimulatedStandingCacheRepository simulatedStandingCacheRepository;

    private final TeamStrengthService teamStrengthService;
    private final TeamStrengthCacheRepository teamStrengthCacheRepository;

    @Value("${consumable.folder.path:${user.dir}/resources/consumable}")
    private String consumableFolderPath;

    @Value("${consumable.montecarlo.simulations:20000}")
    private int monteCarloSimulations;

    /**
     * Trigger method.
     *
     * @return true  -> jeśli znaleziono i przetworzono plik
     *         false -> jeśli nie znaleziono żadnego pliku CSV
     */
    public boolean consume() {

        Path dir = Paths.get(consumableFolderPath).toAbsolutePath();

        try {
            if (!Files.exists(dir)) {
                log.info("Consumable folder does not exist, creating: {}", dir);
                Files.createDirectories(dir);
                return false;
            }

            Optional<Path> csvFile;

            try (Stream<Path> files = Files.list(dir)) {
                csvFile = files
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv"))
                        .findFirst();
            }

            if (csvFile.isEmpty()) {
                log.info("No CSV file found in {}", dir);
                return false;
            }

            Path filePath = csvFile.get();
            log.info("Found CSV file to consume: {}", filePath);

            waitForFileStable(filePath, 3, 300);
            CSVFileReader.ReadResult read = CSVFileReader.readFromPath(filePath);

            simulatedStandingService.generateAndSaveSimulatedStandings(
                    read,
                    monteCarloSimulations,
                    Instant.now()
            );
            simulatedStandingCacheRepository.refresh();

            teamStrengthService.calculateWithFile(read);
            teamStrengthCacheRepository.refresh();


            Files.deleteIfExists(filePath);
            log.info("Successfully consumed and deleted file: {}", filePath);

            return true;

        } catch (Exception e) {
            log.error("Error during consume()", e);
            return false;
        }
    }

    private void waitForFileStable(Path path, int attempts, long sleepMs) throws InterruptedException {
        long previousSize = -1;
        for (int i = 0; i < attempts; i++) {
            try {
                long size = Files.size(path);
                if (size == previousSize) return;
                previousSize = size;
            } catch (IOException ignored) {
            }
            Thread.sleep(sleepMs);
        }
    }
}
