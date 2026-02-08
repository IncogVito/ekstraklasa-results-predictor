package ekstraklasa.predictor.model;

/**
 * @author Witold Drożdżowski (drozdzowski.witold@gmail.com)
 * @since 02.2026
 */
public record MatchProbability(
        double homeWin,
        double draw,
        double awayWin
) {
}