package ekstraklasa.predictor.model;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TeamStrengthModel {
    private String id;
    private String footballClubCode;
    private OffsetDateTime timestamp;
    private Double strength;
}

