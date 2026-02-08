package ekstraklasa.predictor.model;

import lombok.Data;

/**
 * @author Witold Drożdżowski (drozdzowski.witold@gmail.com)
 * @since 02.2026
 */
@Data
public class LeagueStandingsEntry {
    private FootballClub footballClub;
    private int position;
    private int playedGames;
    private int wonGames;
    private int drawnGames;
    private int lostGames;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference;
    private int points;
}
