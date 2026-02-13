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
@Document(collection = "team_strengths")
public class TeamStrengthEntity {

    @Id
    private String id;

    @Indexed
    private String footballClubCode;
    private Instant timestamp;
    private Double strength;

}

