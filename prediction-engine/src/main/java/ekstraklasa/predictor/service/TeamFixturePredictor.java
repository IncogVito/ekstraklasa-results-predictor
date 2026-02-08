package ekstraklasa.predictor.service;

import ekstraklasa.predictor.model.FootballClub;
import ekstraklasa.predictor.model.MatchFixture;
import ekstraklasa.predictor.model.MatchProbability;

import java.util.HashMap;
import java.util.Map;

/**
 * Predictor for a single fixture based on precomputed team strengths.
 *
 * Algorithm overview:
 * - Uses normalized team strengths (expected in range ~[0,1]).
 * - Applies a home advantage multiplier to the home team's strength.
 * - Optionally applies a small boost based on the result of a previous meeting (flag provided
 *   externally to avoid iterating over all fixtures every call).
 * - Converts three raw scores (home, draw, away) to probabilities using a softmax-like transform
 *   (exponentiation + normalization) so they sum to 1.
 */
public class TeamFixturePredictor {

    // small constants / weights — tweakable
    private static final double HOME_ADVANTAGE = 0.10; // home team gets +10% strength
    private static final double PREVIOUS_MEETING_WEIGHT = 0.06; // small boost if previous meeting favors a side

    // softmax scaling factors
    private static final double STRENGTH_SCALE = 2.5; // scale for exponentiation of team strength
    private static final double DRAW_SCALE = 1.6; // draw component scale (usually lower than win scales)

    /**
     * Predict probability for a fixture given a map of team strengths (keyed by team code),
     * the fixture and a map of precomputed previous meeting flags.
     *
     * previousMeetingFlag key format: "homeCode#awayCode". Value meaning:
     *   1  -> previous meeting was a home win (gives small boost to home)
     *   0  -> previous meeting was a draw (no boost)
     *  -1  -> previous meeting was an away win (gives small boost to away)
     * If no entry exists, it's treated as 0 (no effect).
     *
     * @param teamStrengthByCode map teamCode->strength (expected normalized, e.g. 0.0..1.0)
     * @param fixture the fixture to predict
     * @param previousMeetingFlag precomputed flags for previous meetings (no iteration needed)
     * @return MatchProbability with homeWin, draw, awayWin (summing to 1)
     */
    public static MatchProbability predictFixture(Map<String, Double> teamStrengthByCode,
                                                  MatchFixture fixture,
                                                  Map<String, Integer> previousMeetingFlag) {
        if (fixture == null) {
            return new MatchProbability(0.33, 0.34, 0.33);
        }

        String homeCode = fixture.getHomeTeamCode();
        String awayCode = fixture.getAwayTeamCode();
        if (homeCode == null || awayCode == null) {
            return new MatchProbability(0.33, 0.34, 0.33);
        }

        double homeStrength = teamStrengthByCode.getOrDefault(homeCode, 0.5);
        double awayStrength = teamStrengthByCode.getOrDefault(awayCode, 0.5);

        // Apply home advantage
        double adjustedHomeStrength = homeStrength * (1.0 + HOME_ADVANTAGE);
        double adjustedAwayStrength = awayStrength;

        // Apply previous meeting flag if present
        String key = pairKey(homeCode, awayCode);
        int prevFlag = previousMeetingFlag == null ? 0 : previousMeetingFlag.getOrDefault(key, 0);
        if (prevFlag > 0) {
            adjustedHomeStrength += PREVIOUS_MEETING_WEIGHT;
        } else if (prevFlag < 0) {
            adjustedAwayStrength += PREVIOUS_MEETING_WEIGHT;
        }

        // Build softmax-style components (we exponentiate scaled strengths)
        double homeComponent = Math.exp(adjustedHomeStrength * STRENGTH_SCALE);
        double awayComponent = Math.exp(adjustedAwayStrength * STRENGTH_SCALE);

        // Draw component uses average strength and a smaller scale
        double averageStrength = (adjustedHomeStrength + adjustedAwayStrength) / 2.0;
        double drawComponent = Math.exp(averageStrength * DRAW_SCALE);

        double sum = homeComponent + drawComponent + awayComponent;
        if (sum <= 0 || Double.isNaN(sum) || Double.isInfinite(sum)) {
            return new MatchProbability(0.33, 0.34, 0.33);
        }

        double homeProb = homeComponent / sum;
        double drawProb = drawComponent / sum;
        double awayProb = awayComponent / sum;

        return new MatchProbability(homeProb, drawProb, awayProb);
    }

    /**
     * Convenience overload which accepts the strength map keyed by FootballClub objects.
     * It will convert it to a code->strength map and call the main predictor.
     */
    public static MatchProbability predictFixtureFromClubMap(Map<FootballClub, Double> teamStrengthByClub,
                                                            MatchFixture fixture,
                                                            Map<String, Integer> previousMeetingFlag) {
        Map<String, Double> byCode = new HashMap<>();
        if (teamStrengthByClub != null) {
            for (Map.Entry<FootballClub, Double> e : teamStrengthByClub.entrySet()) {
                FootballClub club = e.getKey();
                if (club != null && club.getCode() != null && e.getValue() != null) {
                    byCode.put(club.getCode(), e.getValue());
                }
            }
        }
        return predictFixture(byCode, fixture, previousMeetingFlag);
    }

    private static String pairKey(String home, String away) {
        return home + "#" + away;
    }
}
