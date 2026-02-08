package ekstraklasa.predictor.model;

import lombok.Data;

@Data
public class MatchResult {
    private String matchId;
    private boolean finished;
    private String scoreStr;
    private Winner winner;

    private Integer homeGoals;
    private Integer awayGoals;

    private String homeTeamCode;
    private String awayTeamCode;

    private MatchStats homeMatchStats;
    private MatchStats awayMatchStats;
}
