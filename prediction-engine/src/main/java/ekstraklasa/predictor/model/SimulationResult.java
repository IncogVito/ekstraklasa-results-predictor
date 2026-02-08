package ekstraklasa.predictor.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Zbiera liczniki pozycji dla każdej drużyny po serii symulacji.
 */
public class SimulationResult {
    private final Map<String, int[]> positionCounts = new HashMap<>();
    private final int leagueSize;
    private int simulations = 0;

    public SimulationResult(int leagueSize) {
        this.leagueSize = leagueSize;
    }

    public void ensureTeam(String teamCode) {
        if (!positionCounts.containsKey(teamCode)) {
            positionCounts.put(teamCode, new int[leagueSize]);
        }
    }

    public void increment(String teamCode, int position) {
        ensureTeam(teamCode);
        if (position < 1 || position > leagueSize) return;
        positionCounts.get(teamCode)[position - 1]++;
    }

    public void setSimulations(int simulations) {
        this.simulations = simulations;
    }

    public int getSimulations() {
        return simulations;
    }

    public Map<String, double[]> getPercentages() {
        Map<String, double[]> res = new HashMap<>();
        if (simulations <= 0) return res;
        for (Map.Entry<String, int[]> e : positionCounts.entrySet()) {
            int[] counts = e.getValue();
            double[] pct = new double[counts.length];
            for (int i = 0; i < counts.length; i++) {
                pct[i] = (counts[i] * 100.0) / simulations;
            }
            res.put(e.getKey(), pct);
        }
        return res;
    }

    public Map<String, int[]> getPositionCounts() {
        return positionCounts;
    }

    public int getLeagueSize() {
        return leagueSize;
    }
}

