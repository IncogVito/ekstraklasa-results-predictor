package ekstraklasa.predictor.repository;

import ekstraklasa.predictor.entity.TeamStrengthEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface TeamStrengthRepository extends MongoRepository<TeamStrengthEntity, String> {

    List<TeamStrengthEntity> findByFootballClubCodeOrderByTimestampDesc(String footballClubCode);

    Optional<TeamStrengthEntity> findTopByFootballClubCodeOrderByTimestampDesc(String footballClubCode);

}

