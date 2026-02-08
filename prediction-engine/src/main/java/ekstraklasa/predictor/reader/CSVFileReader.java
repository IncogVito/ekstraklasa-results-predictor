package ekstraklasa.predictor.reader;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import ekstraklasa.predictor.constant.CharConstants;
import ekstraklasa.predictor.model.FootballClub;
import ekstraklasa.predictor.model.MatchFixture;
import ekstraklasa.predictor.model.MatchResult;
import ekstraklasa.predictor.model.MatchStats;
import ekstraklasa.predictor.model.Winner;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class CSVFileReader {
    private static final String RESOURCE_CSV = "/ekstraklasa-results.csv";
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9_]+");

    public static ReadResult readConstantFile() throws IOException {
        try (Reader r = new InputStreamReader(Objects.requireNonNull(CSVFileReader.class.getResourceAsStream(RESOURCE_CSV)), StandardCharsets.UTF_8);
             CSVReader csv = new CSVReader(r)) {

            String[] header = csv.readNext();
            if (header == null) throw new IOException("Empty CSV");

            List<MatchFixture> fixtures = new ArrayList<>();
            List<MatchResult> results = new ArrayList<>();
            Map<String, FootballClub> clubsByCode = new HashMap<>();

            String[] row;
            while ((row = csv.readNext()) != null) {
                if (row.length < header.length) continue;

                String matchId = get(row, indexOf(header, "matchId", header));
                String roundStr = get(row, indexOf(header, "round", header));
                Integer round = tryParseInt(roundStr);
                String roundName = get(row, indexOf(header, "roundName", header));
                String pageUrl = get(row, indexOf(header, "pageUrl", header));

                String homeName = get(row, indexOf(header, "homeName", header));
                String homeId = get(row, indexOf(header, "homeId", header));
                String awayName = get(row, indexOf(header, "awayName", header));
                String awayId = get(row, indexOf(header, "awayId", header));

                String homeCode = toCode(homeName);
                String awayCode = toCode(awayName);

                // collect clubs
                collectClub(clubsByCode, homeId, homeName, homeCode);
                collectClub(clubsByCode, awayId, awayName, awayCode);

                String utcTimeStr = get(row, indexOf(header, "utcTime", header));
                Instant utcTime = tryParseInstant(utcTimeStr);

                String finishedStr = get(row, indexOf(header, "finished", header));
                boolean finished = "true".equalsIgnoreCase(finishedStr) || "1".equals(finishedStr);

                String scoreStr = get(row, indexOf(header, "scoreStr", header));

                MatchStats homeStats = buildStats(row, header, "__home");
                MatchStats awayStats = buildStats(row, header, "__away");

                MatchFixture fixture = new MatchFixture();
                fixture.setMatchId(matchId);
                fixture.setRound(round);
                fixture.setRoundName(roundName);
                fixture.setPageUrl(pageUrl);
                fixture.setHomeName(homeName);
                fixture.setHomeId(homeId);
                fixture.setHomeTeamCode(homeCode);
                fixture.setAwayName(awayName);
                fixture.setAwayId(awayId);
                fixture.setAwayTeamCode(awayCode);
                fixture.setUtcTime(utcTime);
                fixture.setHomeMatchStats(homeStats);
                fixture.setAwayMatchStats(awayStats);

                MatchResult matchResult = new MatchResult();
                matchResult.setMatchId(matchId);
                matchResult.setFinished(finished);
                matchResult.setScoreStr(scoreStr);
                matchResult.setHomeTeamCode(homeCode);
                matchResult.setAwayTeamCode(awayCode);
                if (scoreStr != null && scoreStr.contains("-")) {
                    String[] parts = scoreStr.split("-");
                    Integer homeGoals = tryParseInt(parts[0].trim());
                    Integer awayGoals = tryParseInt(parts[1].trim());

                    matchResult.setHomeGoals(homeGoals);
                    matchResult.setAwayGoals(awayGoals);

                    if (ObjectUtils.allNotNull(homeGoals, awayGoals)) {
                        matchResult.setWinner(Winner.fromScore(homeGoals, awayGoals));
                    }
                }
                matchResult.setHomeMatchStats(homeStats);
                matchResult.setAwayMatchStats(awayStats);

                if (finished) results.add(matchResult);
                else fixtures.add(fixture);
            }

            return new ReadResult(fixtures, results, new ArrayList<>(clubsByCode.values()));
        } catch (CsvValidationException e) {
            throw new IOException(e);
        }
    }

    private static void collectClub(Map<String, FootballClub> map, String id, String name, String code) {
        if (code == null) return;
        FootballClub existing = map.get(code);
        if (existing == null) {
            FootballClub fc = new FootballClub();
            fc.setId(id);
            fc.setName(name);
            fc.setCode(code);
            map.put(code, fc);
        } else if (existing.getId() == null && id != null) {
            existing.setId(id);
        }
    }

    private static String toCode(String name) {
        if (name == null) return null;

        return StringUtils.stripAccents(name)
                .trim()
                .toLowerCase()
                .replace(CharConstants.BLANK_SPACE, CharConstants.UNDERSCORE);
    }

    private static MatchStats buildStats(String[] row, String[] header, String suffix) {
        MatchStats s = new MatchStats();
        s.setBallPossession(tryParseDouble(get(row, indexOf(header, "BallPossesion" + suffix, header))));
        s.setOffsides(tryParseInt(get(row, indexOf(header, "Offsides" + suffix, header))));
        s.setShotsOffTarget(tryParseInt(get(row, indexOf(header, "ShotsOffTarget" + suffix, header))));
        s.setShotsOnTarget(tryParseInt(get(row, indexOf(header, "ShotsOnTarget" + suffix, header))));
        s.setAccurateCrosses(tryParseInt(get(row, indexOf(header, "accurate_crosses" + suffix, header))));
        s.setAccuratePasses(tryParseInt(get(row, indexOf(header, "accurate_passes" + suffix, header))));
        s.setAerialsWon(tryParseInt(get(row, indexOf(header, "aerials_won" + suffix, header))));
        s.setBigChance(tryParseInt(get(row, indexOf(header, "big_chance" + suffix, header))));
        s.setBigChanceMissedTitle(tryParseInt(get(row, indexOf(header, "big_chance_missed_title" + suffix, header))));
        s.setBlockedShots(tryParseInt(get(row, indexOf(header, "blocked_shots" + suffix, header))));
        s.setClearances(tryParseInt(get(row, indexOf(header, "clearances" + suffix, header))));
        s.setCorners(tryParseInt(get(row, indexOf(header, "corners" + suffix, header))));
        s.setDefense(tryParseDouble(get(row, indexOf(header, "defense" + suffix, header))));
        s.setDiscipline(tryParseInt(get(row, indexOf(header, "discipline" + suffix, header))));
        s.setDribblesSucceeded(tryParseInt(get(row, indexOf(header, "dribbles_succeeded" + suffix, header))));
        s.setDuelWon(tryParseInt(get(row, indexOf(header, "duel_won" + suffix, header))));
        s.setDuels(tryParseInt(get(row, indexOf(header, "duels" + suffix, header))));
        s.setExpectedGoals(tryParseDouble(get(row, indexOf(header, "expected_goals" + suffix, header))));
        s.setExpectedGoalsNonPenalty(tryParseDouble(get(row, indexOf(header, "expected_goals_non_penalty" + suffix, header))));
        s.setExpectedGoalsOnTarget(tryParseDouble(get(row, indexOf(header, "expected_goals_on_target" + suffix, header))));
        s.setExpectedGoalsOpenPlay(tryParseDouble(get(row, indexOf(header, "expected_goals_open_play" + suffix, header))));
        s.setExpectedGoalsSetPlay(tryParseDouble(get(row, indexOf(header, "expected_goals_set_play" + suffix, header))));
        s.setFouls(tryParseInt(get(row, indexOf(header, "fouls" + suffix, header))));
        s.setGroundDuelsWon(tryParseInt(get(row, indexOf(header, "ground_duels_won" + suffix, header))));
        s.setInterceptions(tryParseInt(get(row, indexOf(header, "interceptions" + suffix, header))));
        s.setKeeperSaves(tryParseInt(get(row, indexOf(header, "keeper_saves" + suffix, header))));
        s.setLongBallsAccurate(tryParseInt(get(row, indexOf(header, "long_balls_accurate" + suffix, header))));
        s.setTackles(tryParseInt(get(row, indexOf(header, "matchstats.headers.tackles" + suffix, header))));
        s.setOppositionHalfPasses(tryParseInt(get(row, indexOf(header, "opposition_half_passes" + suffix, header))));
        s.setOwnHalfPasses(tryParseInt(get(row, indexOf(header, "own_half_passes" + suffix, header))));
        s.setPasses(tryParseInt(get(row, indexOf(header, "passes" + suffix, header))));
        s.setPlayerThrows(tryParseInt(get(row, indexOf(header, "player_throws" + suffix, header))));
        s.setRedCards(tryParseInt(get(row, indexOf(header, "red_cards" + suffix, header))));
        s.setShotBlocks(tryParseInt(get(row, indexOf(header, "shot_blocks" + suffix, header))));
        s.setShots(tryParseInt(get(row, indexOf(header, "shots" + suffix, header))));
        s.setShotsInsideBox(tryParseInt(get(row, indexOf(header, "shots_inside_box" + suffix, header))));
        s.setShotsOutsideBox(tryParseInt(get(row, indexOf(header, "shots_outside_box" + suffix, header))));
        s.setShotsWoodwork(tryParseInt(get(row, indexOf(header, "shots_woodwork" + suffix, header))));
        s.setTotalShots(tryParseInt(get(row, indexOf(header, "total_shots" + suffix, header))));
        s.setTouchesOppBox(tryParseInt(get(row, indexOf(header, "touches_opp_box" + suffix, header))));
        s.setYellowCards(tryParseInt(get(row, indexOf(header, "yellow_cards" + suffix, header))));
        return s;
    }

    private static int indexOf(String[] header, String name, String[] fullHeader) {
        for (int i = 0; i < header.length; i++) if (name.equals(header[i])) return i;
        return -1;
    }

    private static String get(String[] row, int idx) {
        if (idx < 0 || idx >= row.length) return null;
        String v = row[idx];
        return v == null || v.isEmpty() ? null : v.trim();
    }

    private static Integer tryParseInt(String v) {
        try {
            if (v == null) return null;
            return Integer.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static Double tryParseDouble(String v) {
        try {
            if (v == null) return null;
            return Double.valueOf(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static Instant tryParseInstant(String v) {
        try {
            if (v == null) return null;
            return Instant.parse(v);
        } catch (Exception e) {
            return null;
        }
    }

    public static class ReadResult {
        public final List<MatchFixture> fixtures;
        public final List<MatchResult> results;
        public final List<FootballClub> clubs;

        public ReadResult(List<MatchFixture> fixtures, List<MatchResult> results, List<FootballClub> clubs) {
            this.fixtures = fixtures;
            this.results = results;
            this.clubs = clubs;
        }
    }
}
