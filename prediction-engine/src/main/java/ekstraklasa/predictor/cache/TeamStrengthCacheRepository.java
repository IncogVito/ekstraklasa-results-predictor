package ekstraklasa.predictor.cache;

import ekstraklasa.predictor.entity.TeamStrengthEntity;
import ekstraklasa.predictor.model.TeamStrengthModel;
import ekstraklasa.predictor.repository.TeamStrengthRepository;
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
public class TeamStrengthCacheRepository {
    private final TeamStrengthRepository teamStrengthRepository;
    private final AtomicReference<List<TeamStrengthModel>> cache = new AtomicReference<>(new ArrayList<>());

    public TeamStrengthCacheRepository(TeamStrengthRepository teamStrengthRepository) {
        this.teamStrengthRepository = teamStrengthRepository;
    }

    @PostConstruct
    public void init() {
        try {
            refresh();
        } catch (Exception e) {
            log.error("Initial team strength cache refresh failed", e);
        }
    }

    public List<TeamStrengthModel> getAll() {
        return cache.get();
    }

    @Scheduled(fixedRateString = "${cache.refresh.ms:3600000}") // default 1h
    public void scheduledRefresh() {
        try {
            refresh();
        } catch (Exception e) {
            log.error("Scheduled team strength cache refresh failed", e);
        }
    }

    public synchronized void refresh() {
        log.info("Refreshing team strength cache...");
        List<TeamStrengthEntity> all = teamStrengthRepository.findAll();
        if (all.isEmpty()) {
            cache.set(new ArrayList<>());
            log.info("No team strengths found - cache set to empty list");
            return;
        }

        log.info("Fetched {} team strengths from repository", all.size());

        // choose newest timestamp
        Instant newest = all.stream()
                .map(TeamStrengthEntity::getTimestamp)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        if (newest == null) {
            cache.set(new ArrayList<>());
            log.warn("No valid timestamps found in team strengths - cache set to empty list");
            return;
        }

        List<TeamStrengthModel> mapped = all.stream()
                .filter(e -> newest.equals(e.getTimestamp()))
                .map(this::toModel)
                .collect(Collectors.toList());

        cache.set(mapped);
        log.info("Team strength cache refreshed with {} entries for timestamp={}", mapped.size(), newest);
    }

    private TeamStrengthModel toModel(TeamStrengthEntity e) {
        TeamStrengthModel m = new TeamStrengthModel();
        m.setId(e.getId());
        m.setFootballClubCode(e.getFootballClubCode());
        if (e.getTimestamp() != null) {
            m.setTimestamp(OffsetDateTime.ofInstant(e.getTimestamp(), ZoneOffset.UTC));
        }
        m.setStrength(e.getStrength());
        return m;
    }
}

