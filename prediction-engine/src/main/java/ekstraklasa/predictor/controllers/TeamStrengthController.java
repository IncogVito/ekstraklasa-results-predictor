package ekstraklasa.predictor.controllers;

import ekstraklasa.predictor.api.TeamStrengthApi;
import ekstraklasa.predictor.cache.TeamStrengthCacheRepository;
import ekstraklasa.predictor.model.TeamStrengthModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Witold Drożdżowski (drozdzowski.witold@gmail.com)
 * @since 02.2026
 */
@RestController
@RequestMapping("/api")
public class TeamStrengthController implements TeamStrengthApi {

    private final TeamStrengthCacheRepository teamStrengthCacheRepository;

    public TeamStrengthController(TeamStrengthCacheRepository teamStrengthCacheRepository) {
        this.teamStrengthCacheRepository = teamStrengthCacheRepository;
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/team-strength",
            produces = { "application/json" }
    )

    public ResponseEntity<Map<String, Double>> teamStrengthGet(
    ) {
        var list = teamStrengthCacheRepository.getAll();
        var map = list.stream()
                .filter(m -> m.getFootballClubCode() != null && m.getStrength() != null)
                .collect(Collectors.toMap(TeamStrengthModel::getFootballClubCode, TeamStrengthModel::getStrength));
        return ResponseEntity.ok(map);
    }
}
