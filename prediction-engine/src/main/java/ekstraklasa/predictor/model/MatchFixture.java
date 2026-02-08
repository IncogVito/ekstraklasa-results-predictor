package ekstraklasa.predictor.model;

import jakarta.annotation.Nullable;
import lombok.Data;

import java.time.Instant;

@Data
public class MatchFixture {
    private String matchId;
    private Integer round;
    private String roundName;
    private String pageUrl;

    private String homeName;
    private String homeId;
    private String homeTeamCode;
    private String awayName;
    private String awayId;
    private String awayTeamCode;

    private Instant utcTime;

    @Nullable
    private MatchStats homeMatchStats;

    @Nullable
    private MatchStats awayMatchStats;
}
