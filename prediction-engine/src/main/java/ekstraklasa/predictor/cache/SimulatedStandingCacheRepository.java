package ekstraklasa.predictor.cache;

import ekstraklasa.predictor.entity.SimulatedStandingEntity;
import ekstraklasa.predictor.model.TablePredictionModel;
import ekstraklasa.predictor.repository.SimulatedStandingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SimulatedStandingCacheRepository {
    private final SimulatedStandingRepository simulatedStandingRepository;
    private final AtomicReference<List<TablePredictionModel>> cache = new AtomicReference<>(new ArrayList<>());

    public SimulatedStandingCacheRepository(SimulatedStandingRepository simulatedStandingRepository) {
        this.simulatedStandingRepository = simulatedStandingRepository;
    }

    @PostConstruct
    public void init() {
        try {
            refresh();
        } catch (Exception e) {
            log.error("Initial cache refresh failed", e);
        }
    }

    public List<TablePredictionModel> getAll() {
        return cache.get();
    }

    @Scheduled(fixedRateString = "${cache.refresh.ms:3600000}") // default 1h
    public void scheduledRefresh() {
        try {
            refresh();
        } catch (Exception e) {
            log.error("Scheduled cache refresh failed", e);
        }
    }

    /**
     * Właściwe odświeżenie cache: pobiera wszystkie encje, wybiera najstarszy timestamp i zachowuje
     * tylko encje z tym timestampem (earliest). Mapuje do TablePredictionModel.
     */
    public synchronized void refresh() {
        log.info("Refreshing simulated standings cache...");
        List<SimulatedStandingEntity> all = simulatedStandingRepository.findAll();
        if (all.isEmpty()) {
            cache.set(new ArrayList<>());
            log.info("No simulated standings found - cache set to empty list");
            return;
        }

        log.info("Fetched {} simulated standings from repository", all.size());

        // find earliest non-null timestamp
        Instant newest = all.stream()
                .map(SimulatedStandingEntity::getTimestamp)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (newest == null) {
            cache.set(new ArrayList<>());
            log.warn("No valid timestamps found in simulated standings - cache set to empty list");
            return;
        }

        List<TablePredictionModel> mapped = all.stream()
                .filter(e -> newest.equals(e.getTimestamp()))
                .map(this::toTablePredictionModel)
                .collect(Collectors.toList());

        cache.set(mapped);
        log.info("Cache refreshed with {} entries for timestamp={}", mapped.size(), newest);
    }

    private TablePredictionModel toTablePredictionModel(SimulatedStandingEntity e) {
        TablePredictionModel m = new TablePredictionModel();
        m.setId(e.getId());
        m.setFootballClubCode(e.getFootballClubCode());
        // convert Instant -> OffsetDateTime (UTC)
        if (e.getTimestamp() != null) {
            m.setTimestamp(OffsetDateTime.ofInstant(e.getTimestamp(), ZoneOffset.UTC));
        }
        m.setMatchPlayed(e.getMatchPlayed());
        m.setRanking(e.getRanking());
        m.setPoints(e.getPoints());
        m.setTop4Prediction(e.getTop4Prediction());
        m.setRelegationPrediction(e.getRelegationPrediction());
        return m;
    }
}
