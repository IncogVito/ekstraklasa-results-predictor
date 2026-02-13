package ekstraklasa.predictor.service;

import ekstraklasa.predictor.entity.TeamStrengthEntity;
import ekstraklasa.predictor.model.FootballClub;
import ekstraklasa.predictor.reader.CSVFileReader;
import ekstraklasa.predictor.repository.TeamStrengthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamStrengthService {
    private final TeamStrengthRepository teamStrengthRepository;
    public void calculateWithFile(CSVFileReader.ReadResult read) {
        Map<FootballClub, Double> strengths = TeamStrengthCalculationService.calculateAllTeamsStrength(read.clubs, read.results);
        if (strengths.isEmpty()) {
            log.info("No team strengths calculated - nothing to save");
            return;
        }

        Instant ts = Instant.now();
        List<TeamStrengthEntity> entities = new ArrayList<>();
        for (Map.Entry<FootballClub, Double> e : strengths.entrySet()) {
            FootballClub club = e.getKey();
            Double strength = e.getValue();
            if (club == null || club.getCode() == null) continue;
            TeamStrengthEntity ent = TeamStrengthEntity.builder()
                    .footballClubCode(club.getCode())
                    .timestamp(ts)
                    .strength(strength)
                    .build();
            entities.add(ent);
        }

        if (entities.isEmpty()) {
            log.info("No team strength entities to save");
            return;
        }

        teamStrengthRepository.saveAll(entities);
        log.info("Saved {} team strength records to repository", entities.size());

    }
}

