package ekstraklasa.predictor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * @author Witold Drożdżowski (drozdzowski.witold@gmail.com)
 * @since 02.2026
 */
@Data()
@AllArgsConstructor(staticName = "of")
public class LeagueTable {
    private List<LeagueStandingsEntry> standingsEntry;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String singleEntry = "%s. %s - MP: %d, W: %d, D: %d, L: %d, GF: %d, GA: %d, Pkt: %d\n";
        for (LeagueStandingsEntry entry : standingsEntry) {
            sb.append(String.format(singleEntry,
                    entry.getPosition(),
                    entry.getFootballClub().getName(),
                    entry.getPlayedGames(),
                    entry.getWonGames(),
                    entry.getDrawnGames(),
                    entry.getLostGames(),
                    entry.getGoalsFor(),
                    entry.getGoalsAgainst(),
                    entry.getPoints()
            )).append(System.lineSeparator());
        }

        return "###LeagueTable###" + System.lineSeparator() + sb;
    }
}
