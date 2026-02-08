package ekstraklasa.predictor.service;

import ekstraklasa.predictor.model.MatchProbability;
import ekstraklasa.predictor.model.Winner;

import java.util.Random;

/**
 * @author Witold Drożdżowski (drozdzowski.witold@gmail.com)
 * @since 02.2026
 */
public class OutcomeSampler {

    private final static Random random = new Random();

    public static Winner sample(MatchProbability p) {
        double r = random.nextDouble();

        if (r < p.homeWin())
            return Winner.HOME_WIN;
        else if (r < p.homeWin() + p.draw())
            return Winner.DRAW;
        else
            return Winner.AWAY_WIN;
    }
}