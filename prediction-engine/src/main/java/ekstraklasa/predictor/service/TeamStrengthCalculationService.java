package ekstraklasa.predictor.service;

import ekstraklasa.predictor.constant.FootballClubValueConstants;
import ekstraklasa.predictor.model.FootballClub;
import ekstraklasa.predictor.model.MatchResult;
import ekstraklasa.predictor.model.MatchStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serwis obliczający "siłę" drużyny na podstawie jej poprzednich meczów i dostępnych statystyk.
 *
 * Zasady:
 * - Bierzemy pod uwagę maksymalnie ostatnie RECENCY_WINDOW meczów (domyślnie 7).
 * - Każdy mecz ma skompresowany zbiór metryk (atakowe i defensywne).
 * - Normalizacja metryk odbywa się względem maksymalnej wartości tej metryki w rozważanym zbiorze meczów.
 * - Ostateczny wynik to uśredniona ważona suma metryk z zastosowaniem wagi recencyjnej (nowsze mecze silniej wpływają).
 * - Dodatkowo uwzględniamy wycenę klubu (market value) oraz punkty sezonowe z wagami.
 *
 * Metody i nazwy są opisowe, żeby logika była czytelna i łatwa do testowania.
 */
public class TeamStrengthCalculationService {

    // --- Konfiguracja / wagi metryk (możesz zmieniać aby dopasować model) ---
    private static final int RECENCY_WINDOW = 8; // ile ostatnich meczów wziąć mocniej pod uwagę
    private static final double RECENCY_WEIGHT_FACTOR = 0.15; // maksymalny dodatkowy mnożnik dla najnowszego meczu (np. 0.6 -> 1.6x)

    // Wagi poszczególnych metryk w ostatecznym composite score (powinny sumować się do około 1.0 jeśli wszystkie pozytywne)
    private static final double WEIGHT_GOALS_FOR = 0.21;
    private static final double WEIGHT_GOALS_AGAINST = 0.18; // traktujemy jako negatywny wskaźnik (mniej = lepiej)
    private static final double WEIGHT_POSSESSION = 0.03;
    private static final double WEIGHT_EXPECTED_GOALS = 0.24;
    private static final double WEIGHT_SHOTS_ON_TARGET = 0.14;
    private static final double WEIGHT_TOTAL_SHOTS = 0.04;
    private static final double WEIGHT_PASSES = 0.02;
    private static final double WEIGHT_TACKLES = 0.01;
    private static final double WEIGHT_INTERCEPTIONS = 0.01;
    private static final double WEIGHT_KEEPER_SAVES = 0.02;
    private static final double WEIGHT_CARDS = 0.03; // kary (yellow/red) obniżają wynik
    private static final double WEIGHT_MATCH_RESULT_POINTS = 0.15; // punkty za wynik (3/1/0) jako osobna składowa

    // Nowe wagi: silna waga dla wyceny klubu oraz punkty sezonowe
    private static final double WEIGHT_MARKET_VALUE = 0.74; // dość mocna waga
    private static final double WEIGHT_SEASON_POINTS = 0.10; // dodatkowa waga za tabelę/uzyskane punkty

    private static final double DEFAULT_STRENGTH = 0.1; // zwracane gdy brak danych


    public static Map<FootballClub, Double> calculateAllTeamsStrength(List<FootballClub> clubs, List<MatchResult> allMatches) {
        // Obliczamy punkty sezonowe dla każdej drużyny
        Map<String, Integer> seasonPointsByCode = computeSeasonPoints(allMatches);
        int maxSeasonPoints = seasonPointsByCode.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // Obliczamy maksymalną wycenę w dostępnych stałych
        int maxMarketValue = FootballClubValueConstants.CLUB_MARKET_VALUE_EUR.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        Map<FootballClub, Double> strengthMap = new HashMap<>();
        for (FootballClub club : clubs) {
            double baseMatchScore = calculateTeamStrength(club.getCode(), allMatches);

            // Normalizujemy punkty sezonowe
            int clubPoints = seasonPointsByCode.getOrDefault(club.getCode(), 0);
            double normalizedSeasonPoints = maxSeasonPoints > 0 ? (clubPoints / (double) maxSeasonPoints) : 0.0;

            // Normalizujemy wycenę rynkową
            Integer clubMarketValue = FootballClubValueConstants.CLUB_MARKET_VALUE_EUR.get(club.getCode());
            double normalizedMarketValue = (clubMarketValue != null && maxMarketValue > 0) ? (clubMarketValue / (double) maxMarketValue) : 0.0;

            // Łączymy: reweighted base score + market value + season points
            double baseWeight = 1.0 - (WEIGHT_MARKET_VALUE + WEIGHT_SEASON_POINTS);
            if (baseWeight < 0) baseWeight = 0.0; // zabezpieczenie

            double finalStrength = baseMatchScore * baseWeight
                    + normalizedMarketValue * WEIGHT_MARKET_VALUE
                    + normalizedSeasonPoints * WEIGHT_SEASON_POINTS;

            strengthMap.put(club, clamp(finalStrength, 0.0, 1.0));
        }
        return strengthMap;
    }


        /**
         * Publiczna metoda obliczająca siłę drużyny.
         *
         * @param teamCode kod drużyny (porównywany z homeTeamCode/awayTeamCode w MatchResult)
         * @param matches lista meczów (kolejność zakładana: chronologiczna od najstarszych do najnowszych)
         * @return siła drużyny w skali przybliżonej do [0,1] (1 = najsilniejsza)
         */
    public static double calculateTeamStrength(String teamCode, List<MatchResult> matches) {
        if (teamCode == null || teamCode.isEmpty() || matches == null || matches.isEmpty()) {
            return DEFAULT_STRENGTH;
        }

        // Filtrujemy tylko zakończone mecze, w których brała udział dana drużyna
        List<MatchResult> teamMatches = matches.stream()
                .filter(m -> m.isFinished() && (teamCode.equals(m.getHomeTeamCode()) || teamCode.equals(m.getAwayTeamCode())))
                .collect(Collectors.toList());

        if (teamMatches.isEmpty()) {
            return DEFAULT_STRENGTH;
        }

        // Rozważamy maksymalnie ostatnie RECENCY_WINDOW meczów
        int considerCount = Math.min(RECENCY_WINDOW, teamMatches.size());
        List<MatchResult> consideredMatches = teamMatches.subList(teamMatches.size() - considerCount, teamMatches.size());

        // Wyciągamy surowe metryki dla każdego meczu
        List<Map<String, Double>> perMatchMetrics = new ArrayList<>();
        for (MatchResult match : consideredMatches) {
            boolean isHome = teamCode.equals(match.getHomeTeamCode());
            MatchStats stats = isHome ? match.getHomeMatchStats() : match.getAwayMatchStats();

            Map<String, Double> m = new HashMap<>();
            // Wyniki bramkowe
            m.put("goalsFor", safeDouble(isHome ? match.getHomeGoals() : match.getAwayGoals()));
            m.put("goalsAgainst", safeDouble(isHome ? match.getAwayGoals() : match.getHomeGoals()));

            // Statystyki (mogą być null)
            m.put("possession", safeDouble(stats == null ? null : stats.getBallPossession()));
            m.put("expectedGoals", safeDouble(stats == null ? null : stats.getExpectedGoals()));
            m.put("shotsOnTarget", safeDouble(stats == null ? null : stats.getShotsOnTarget()));
            m.put("totalShots", safeDouble(stats == null ? null : stats.getTotalShots()));
            m.put("passes", safeDouble(stats == null ? null : stats.getPasses()));
            m.put("tackles", safeDouble(stats == null ? null : stats.getTackles()));
            m.put("interceptions", safeDouble(stats == null ? null : stats.getInterceptions()));
            m.put("keeperSaves", safeDouble(stats == null ? null : stats.getKeeperSaves()));
            m.put("yellowCards", safeDouble(stats == null ? null : stats.getYellowCards()));
            m.put("redCards", safeDouble(stats == null ? null : stats.getRedCards()));

            // Punkty za wynik meczu (3/1/0)
            double matchPoints = 0.0;
            if (match.getHomeGoals() != null && match.getAwayGoals() != null) {
                int hg = match.getHomeGoals();
                int ag = match.getAwayGoals();
                if ((isHome && hg > ag) || (!isHome && ag > hg)) {
                    matchPoints = 3.0;
                } else if (hg == ag) {
                    matchPoints = 1.0;
                } else {
                    matchPoints = 0.0;
                }
            }
            m.put("matchPoints", matchPoints);

            perMatchMetrics.add(m);
        }

        // Obliczamy maksima dla normalizacji
        Map<String, Double> maxValues = computeMaxValues(perMatchMetrics);

        // Teraz dla każdego meczu obliczamy znormalizowany composite score
        List<Double> perMatchScores = new ArrayList<>();
        for (Map<String, Double> m : perMatchMetrics) {
            double score = 0.0;

            // Atak
            score += WEIGHT_GOALS_FOR * normalize(m.get("goalsFor"), maxValues.get("goalsFor"));
            score += WEIGHT_POSSESSION * normalize(m.get("possession"), maxValues.get("possession"));
            score += WEIGHT_EXPECTED_GOALS * normalize(m.get("expectedGoals"), maxValues.get("expectedGoals"));
            score += WEIGHT_SHOTS_ON_TARGET * normalize(m.get("shotsOnTarget"), maxValues.get("shotsOnTarget"));
            score += WEIGHT_TOTAL_SHOTS * normalize(m.get("totalShots"), maxValues.get("totalShots"));
            score += WEIGHT_PASSES * normalize(m.get("passes"), maxValues.get("passes"));

            // Obrona (goalsAgainst traktujemy jako negatywny: mniej lepsze)
            double normGoalsAgainst = normalize(m.get("goalsAgainst"), maxValues.get("goalsAgainst"));
            score += WEIGHT_GOALS_AGAINST * (1.0 - normGoalsAgainst);

            // Inne elementy defensywne
            score += WEIGHT_TACKLES * normalize(m.get("tackles"), maxValues.get("tackles"));
            score += WEIGHT_INTERCEPTIONS * normalize(m.get("interceptions"), maxValues.get("interceptions"));
            score += WEIGHT_KEEPER_SAVES * normalize(m.get("keeperSaves"), maxValues.get("keeperSaves"));

            // Kary (większa liczba kar obniża wynik)
            double cards = normalize(sum(m.get("yellowCards"), m.get("redCards")), sum(maxValues.get("yellowCards"), maxValues.get("redCards")));
            score -= WEIGHT_CARDS * cards; // subtracting penalty (WEIGHT_CARDS pozytywna — my odejmujemy)

            // Punkty za wynik meczu (3/1/0) -> znormalizowane do [0,1]
            double normPoints = normalize(m.get("matchPoints"), 3.0);
            score += WEIGHT_MATCH_RESULT_POINTS * normPoints;

            perMatchScores.add(clamp(score, 0.0, 1.0));
        }

        // Zastosowanie wag recencyjnych i uśrednienie
        double weightedSum = 0.0;
        double weightTotal = 0.0;
        int count = perMatchScores.size();
        for (int i = 0; i < count; i++) {
            // i == 0 -> najstarszy, i == count-1 -> najnowszy
            double recencyMultiplier = recencyMultiplier(i, count);
            weightedSum += perMatchScores.get(i) * recencyMultiplier;
            weightTotal += recencyMultiplier;
        }

        if (weightTotal <= 0) {
            return DEFAULT_STRENGTH;
        }

        double finalScore = weightedSum / weightTotal;
        // Ostateczne skalowanie/klamrowanie
        return clamp(finalScore, 0.0, 1.0);
    }

    /**
     * Oblicza siłę drużyny łącząc wynik oparty na meczach z wyceną rynkową i punktami sezonowymi.
     * @param teamCode kod drużyny
     * @param matches lista meczów tej drużyny (chronologiczna od najstarszych)
     * @param clubMarketValue wartość rynkowa klubu (EUR) lub null jeśli brak
     * @param clubSeasonPoints zdobyte punkty w sezonie
     * @param maxMarketValue maksymalna wartość rynkowa spośród wszystkich klubów (używana do normalizacji)
     * @param maxSeasonPoints maksymalna liczba punktów w sezonie (używana do normalizacji)
     * @return siła drużyny w skali [0,1]
     */
    public static double calculateTeamStrength(String teamCode,
                                               List<MatchResult> matches,
                                               Integer clubMarketValue,
                                               int clubSeasonPoints,
                                               int maxMarketValue,
                                               int maxSeasonPoints) {
        double baseMatchScore = calculateTeamStrength(teamCode, matches);

        double normalizedMarketValue = (clubMarketValue != null && maxMarketValue > 0) ? (clubMarketValue / (double) maxMarketValue) : 0.0;
        double normalizedSeasonPoints = maxSeasonPoints > 0 ? (clubSeasonPoints / (double) maxSeasonPoints) : 0.0;

        double baseWeight = 1.0 - (WEIGHT_MARKET_VALUE + WEIGHT_SEASON_POINTS);
        if (baseWeight < 0) baseWeight = 0.0;

        double finalStrength = baseMatchScore * baseWeight
                + normalizedMarketValue * WEIGHT_MARKET_VALUE
                + normalizedSeasonPoints * WEIGHT_SEASON_POINTS;

        return clamp(finalStrength, 0.0, 1.0);
    }

    // --- Metody pomocnicze ---

    private static double safeDouble(Number n) {
        return n == null ? 0.0 : n.doubleValue();
    }

    private static double sum(Double a, Double b) {
        double aa = a == null ? 0.0 : a;
        double bb = b == null ? 0.0 : b;
        return aa + bb;
    }

    private static Map<String, Double> computeMaxValues(List<Map<String, Double>> metricsList) {
        Map<String, Double> max = new HashMap<>();
        for (Map<String, Double> m : metricsList) {
            for (Map.Entry<String, Double> e : m.entrySet()) {
                String k = e.getKey();
                double v = e.getValue() == null ? 0.0 : e.getValue();
                max.put(k, Math.max(max.getOrDefault(k, 0.0), v));
            }
        }
        return max;
    }

    private static double normalize(Double value, Double max) {
        double v = value == null ? 0.0 : value.doubleValue();
        double m = max == null ? 0.0 : max.doubleValue();
        if (m <= 0.0) return 0.0;
        return v / m;
    }

    private static double clamp(double v, double min, double max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static Map<String, Integer> computeSeasonPoints(List<MatchResult> allMatches) {
        Map<String, Integer> points = new HashMap<>();
        if (allMatches == null) return points;

        for (MatchResult match : allMatches) {
            if (!match.isFinished()) continue;
            if (match.getHomeGoals() == null || match.getAwayGoals() == null) continue;

            String home = match.getHomeTeamCode();
            String away = match.getAwayTeamCode();
            int hg = match.getHomeGoals();
            int ag = match.getAwayGoals();

            if (hg > ag) {
                points.put(home, points.getOrDefault(home, 0) + 3);
                points.put(away, points.getOrDefault(away, 0) + 0);
            } else if (hg < ag) {
                points.put(home, points.getOrDefault(home, 0) + 0);
                points.put(away, points.getOrDefault(away, 0) + 3);
            } else {
                points.put(home, points.getOrDefault(home, 0) + 1);
                points.put(away, points.getOrDefault(away, 0) + 1);
            }
        }
        return points;
    }

    /**
     * Prosty schemat wag recencyjnych: starzejace mecze mają mniejszy wpływ, najnowszy ma największy.
     * Zwraca mnożnik >= 1.0. Dla najstarszego ~1.0, dla najnowszego ~1.0 + RECENCY_WEIGHT_FACTOR
     */
    private static double recencyMultiplier(int index, int total) {
        if (total <= 1) return 1.0 + RECENCY_WEIGHT_FACTOR;
        // normalizujemy index do [0,1]
        double normalizedPos = (index + 1) / (double) total; // najstarszy -> ~1/total (bliskie 0), najnowszy -> 1
        return 1.0 + (normalizedPos * RECENCY_WEIGHT_FACTOR);
    }
}
