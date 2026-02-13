package ekstraklasa.predictor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "simulated_standings")
public class SimulatedStandingEntity {

    @Id
    private String id;

    @Indexed
    private String footballClubCode;
    private Instant timestamp;
    private Integer matchPlayed;
    private Integer ranking;
    private Integer points;
    private Double top4Prediction;
    private Double relegationPrediction;

}

