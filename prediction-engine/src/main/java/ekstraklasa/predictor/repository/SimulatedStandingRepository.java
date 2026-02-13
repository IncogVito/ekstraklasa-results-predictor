package ekstraklasa.predictor.repository;

import ekstraklasa.predictor.entity.SimulatedStandingEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SimulatedStandingRepository extends MongoRepository<SimulatedStandingEntity, String> {

    List<SimulatedStandingEntity> findByFootballClubCodeOrderByTimestampDesc(String footballClubCode);

    Optional<SimulatedStandingEntity> findTopByFootballClubCodeOrderByTimestampDesc(String footballClubCode);

}

