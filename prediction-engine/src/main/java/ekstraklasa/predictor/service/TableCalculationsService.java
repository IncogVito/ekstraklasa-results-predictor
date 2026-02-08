package ekstraklasa.predictor.service;

import ekstraklasa.predictor.model.FootballClub;
import ekstraklasa.predictor.model.LeagueStandingsEntry;
import ekstraklasa.predictor.model.MatchResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Witold Drożdżowski (drozdzowski.witold@gmail.com)
 * @since 02.2026
 */
public class TableCalculationsService {

    public static List<LeagueStandingsEntry> calculateLeagueStandings(List<MatchResult> results) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, LeagueStandingsEntry> standingsMap = new HashMap<>();
        Set<String> processedMatchIds = new HashSet<>();
        List<MatchResult> uniqueResults = new ArrayList<>();

        for (MatchResult matchResult : results) {
            if (!isValidResult(matchResult)) {
                continue;
            }

            String matchId = matchResult.getMatchId();
            if (matchId != null && processedMatchIds.contains(matchId)) {
                // pomijamy duplikaty tego samego meczu
                continue;
            }
            if (matchId != null) {
                processedMatchIds.add(matchId);
            }

            // dodajemy do listy unikalnych wyników (przydatne do head-to-head)
            uniqueResults.add(matchResult);

            String homeTeamCode = matchResult.getHomeTeamCode();
            String awayTeamCode = matchResult.getAwayTeamCode();
            Integer homeGoals = matchResult.getHomeGoals();
            Integer awayGoals = matchResult.getAwayGoals();

            LeagueStandingsEntry homeEntry = getOrCreateStandingsEntry(standingsMap, homeTeamCode);
            LeagueStandingsEntry awayEntry = getOrCreateStandingsEntry(standingsMap, awayTeamCode);

            updateEntriesForMatch(homeEntry, awayEntry, homeGoals, awayGoals);
        }

        List<LeagueStandingsEntry> sortedStandings = new ArrayList<>(standingsMap.values());
        // sortujemy z wykorzystaniem head-to-head
        sortStandingsUsingHeadToHead(sortedStandings, uniqueResults);
        assignPositions(sortedStandings, uniqueResults);

        return sortedStandings;
    }

    // --- helper methods -----------------------------------------------------------------

    private static boolean isValidResult(MatchResult matchResult) {
        if (matchResult == null) {
            return false;
        }
        if (!matchResult.isFinished()) {
            return false;
        }
        if (matchResult.getHomeGoals() == null || matchResult.getAwayGoals() == null) {
            return false;
        }
        if (matchResult.getHomeTeamCode() == null || matchResult.getAwayTeamCode() == null) {
            return false;
        }
        return true;
    }

    private static LeagueStandingsEntry getOrCreateStandingsEntry(Map<String, LeagueStandingsEntry> standingsMap, String teamCode) {
        LeagueStandingsEntry existingEntry = standingsMap.get(teamCode);
        if (existingEntry != null) {
            return existingEntry;
        }

        LeagueStandingsEntry newEntry = new LeagueStandingsEntry();
        FootballClub minimalClub = new FootballClub();
        minimalClub.setCode(teamCode);
        minimalClub.setName(teamCode);
        newEntry.setFootballClub(minimalClub);

        newEntry.setPlayedGames(0);
        newEntry.setWonGames(0);
        newEntry.setDrawnGames(0);
        newEntry.setLostGames(0);
        newEntry.setGoalsFor(0);
        newEntry.setGoalsAgainst(0);
        newEntry.setGoalDifference(0);
        newEntry.setPoints(0);

        standingsMap.put(teamCode, newEntry);
        return newEntry;
    }

    private static void updateEntriesForMatch(LeagueStandingsEntry homeEntry, LeagueStandingsEntry awayEntry, Integer homeGoals, Integer awayGoals) {
        // aktualizujemy liczby meczów
        homeEntry.setPlayedGames(homeEntry.getPlayedGames() + 1);
        awayEntry.setPlayedGames(awayEntry.getPlayedGames() + 1);

        // aktualizujemy bramki
        homeEntry.setGoalsFor(homeEntry.getGoalsFor() + homeGoals);
        homeEntry.setGoalsAgainst(homeEntry.getGoalsAgainst() + awayGoals);
        awayEntry.setGoalsFor(awayEntry.getGoalsFor() + awayGoals);
        awayEntry.setGoalsAgainst(awayEntry.getGoalsAgainst() + homeGoals);

        // wynik meczu i punkty
        int comparison = Integer.compare(homeGoals, awayGoals);
        if (comparison > 0) { // home win
            homeEntry.setWonGames(homeEntry.getWonGames() + 1);
            awayEntry.setLostGames(awayEntry.getLostGames() + 1);
            homeEntry.setPoints(homeEntry.getPoints() + 3);
        } else if (comparison < 0) { // away win
            awayEntry.setWonGames(awayEntry.getWonGames() + 1);
            homeEntry.setLostGames(homeEntry.getLostGames() + 1);
            awayEntry.setPoints(awayEntry.getPoints() + 3);
        } else { // draw
            homeEntry.setDrawnGames(homeEntry.getDrawnGames() + 1);
            awayEntry.setDrawnGames(awayEntry.getDrawnGames() + 1);
            homeEntry.setPoints(homeEntry.getPoints() + 1);
            awayEntry.setPoints(awayEntry.getPoints() + 1);
        }
    }

    private static void sortStandingsUsingHeadToHead(List<LeagueStandingsEntry> standings, List<MatchResult> uniqueResults) {
        // najpierw ustawiamy ogólną różnicę bramek
        for (LeagueStandingsEntry entry : standings) {
            int goalsFor = entry.getGoalsFor();
            int goalsAgainst = entry.getGoalsAgainst();
            entry.setGoalDifference(goalsFor - goalsAgainst);
        }

        // grupujemy drużyny po liczbie punktów
        Map<Integer, List<LeagueStandingsEntry>> groupsByPoints = new HashMap<>();
        for (LeagueStandingsEntry entry : standings) {
            Integer pts = entry.getPoints();
            groupsByPoints.computeIfAbsent(pts, k -> new ArrayList<>()).add(entry);
        }

        // posortujemy klucze punktowe malejąco
        List<Integer> sortedPoints = new ArrayList<>(groupsByPoints.keySet());
        sortedPoints.sort(Comparator.reverseOrder());

        List<LeagueStandingsEntry> result = new ArrayList<>();
        for (Integer pts : sortedPoints) {
            List<LeagueStandingsEntry> group = groupsByPoints.get(pts);
            if (group.size() <= 1) {
                // pojedynczy element - dodajemy bez zmian
                if (group.size() == 1) result.add(group.get(0));
                continue;
            }

            // budujemy mini-ligę head-to-head dla tej grupy
            Set<String> groupCodes = new HashSet<>();
            for (LeagueStandingsEntry e : group) {
                String code = e.getFootballClub() == null ? null : e.getFootballClub().getCode();
                if (code != null) groupCodes.add(code);
            }

            Map<String, HeadToHeadStats> h2h = computeHeadToHeadMap(groupCodes, uniqueResults);

            group.sort((a, b) -> {
                String aCode = a.getFootballClub() == null ? "" : a.getFootballClub().getCode();
                String bCode = b.getFootballClub() == null ? "" : b.getFootballClub().getCode();

                HeadToHeadStats ha = h2h.getOrDefault(aCode, new HeadToHeadStats());
                HeadToHeadStats hb = h2h.getOrDefault(bCode, new HeadToHeadStats());

                // 1) punkty w bezpośrednich meczach
                int cmp = Integer.compare(hb.points, ha.points);
                if (cmp != 0) return cmp;
                // 2) różnica bramek w bezpośrednich meczach
                cmp = Integer.compare(hb.goalDifference(), ha.goalDifference());
                if (cmp != 0) return cmp;
                // 3) ogólna różnica bramek
                cmp = Integer.compare(b.getGoalDifference(), a.getGoalDifference());
                if (cmp != 0) return cmp;
                // dalsze kryteria deterministyczne: gole zdobyte, potem nazwa
                cmp = Integer.compare(b.getGoalsFor(), a.getGoalsFor());
                if (cmp != 0) return cmp;
                return getClubSortName(a).compareTo(getClubSortName(b));
            });

            result.addAll(group);
        }

        // nadpisujemy listę wejściową posortowaną listą wynikową
        standings.clear();
        standings.addAll(result);
    }

    private static Map<String, HeadToHeadStats> computeHeadToHeadMap(Set<String> groupCodes, List<MatchResult> uniqueResults) {
        Map<String, HeadToHeadStats> map = new HashMap<>();
        for (String code : groupCodes) {
            map.put(code, new HeadToHeadStats());
        }

        for (MatchResult m : uniqueResults) {
            String home = m.getHomeTeamCode();
            String away = m.getAwayTeamCode();
            if (home == null || away == null) continue;
            if (!groupCodes.contains(home) || !groupCodes.contains(away)) continue;

            HeadToHeadStats hHome = map.get(home);
            HeadToHeadStats hAway = map.get(away);
            if (hHome == null || hAway == null) continue;

            int hg = m.getHomeGoals();
            int ag = m.getAwayGoals();

            hHome.goalsFor += hg;
            hHome.goalsAgainst += ag;
            hAway.goalsFor += ag;
            hAway.goalsAgainst += hg;

            if (hg > ag) {
                hHome.points += 3;
            } else if (hg < ag) {
                hAway.points += 3;
            } else {
                hHome.points += 1;
                hAway.points += 1;
            }
        }

        return map;
    }

    private static boolean areTied(LeagueStandingsEntry a, LeagueStandingsEntry b, List<MatchResult> uniqueResults) {
        if (!Objects.equals(a.getPoints(), b.getPoints())) return false;
        String aCode = a.getFootballClub() == null ? null : a.getFootballClub().getCode();
        String bCode = b.getFootballClub() == null ? null : b.getFootballClub().getCode();
        if (aCode == null || bCode == null) return false;

        Set<String> pair = new HashSet<>();
        pair.add(aCode);
        pair.add(bCode);
        Map<String, HeadToHeadStats> h2h = computeHeadToHeadMap(pair, uniqueResults);
        HeadToHeadStats ha = h2h.getOrDefault(aCode, new HeadToHeadStats());
        HeadToHeadStats hb = h2h.getOrDefault(bCode, new HeadToHeadStats());

        boolean tiedOnH2HPoints = ha.points == hb.points;
        boolean tiedOnH2HGoalDiff = ha.goalDifference() == hb.goalDifference();
        boolean tiedOnOverallGoalDiff = Objects.equals(a.getGoalDifference(), b.getGoalDifference());

        return tiedOnH2HPoints && tiedOnH2HGoalDiff && tiedOnOverallGoalDiff;
    }

    private static void assignPositions(List<LeagueStandingsEntry> sortedStandings, List<MatchResult> uniqueResults) {
        if (sortedStandings == null || sortedStandings.isEmpty()) {
            return;
        }

        for (int index = 0; index < sortedStandings.size(); index++) {
            LeagueStandingsEntry currentEntry = sortedStandings.get(index);
            if (index == 0) {
                currentEntry.setPosition(1);
                continue;
            }

            LeagueStandingsEntry previousEntry = sortedStandings.get(index - 1);

            if (areTied(currentEntry, previousEntry, uniqueResults)) {
                // ex aequo - ta sama pozycja
                currentEntry.setPosition(previousEntry.getPosition());
            } else {
                // pozycja jest indeks + 1 (numeracja pomija miejsca zajęte przez ex aequo)
                currentEntry.setPosition(index + 1);
            }
        }
    }

    private static String getClubSortName(LeagueStandingsEntry entry) {
        if (entry == null || entry.getFootballClub() == null) {
            return "";
        }
        String name = entry.getFootballClub().getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        String code = entry.getFootballClub().getCode();
        return code == null ? "" : code;
    }

    private static class HeadToHeadStats {
        int points = 0;
        int goalsFor = 0;
        int goalsAgainst = 0;

        int goalDifference() {
            return goalsFor - goalsAgainst;
        }
    }
}
