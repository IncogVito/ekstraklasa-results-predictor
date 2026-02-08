package ekstraklasa.predictor.model;

public enum Winner {
    HOME_WIN,
    AWAY_WIN,
    DRAW;

    public static Winner fromScore(int homeGoals, int awayGoals) {
        return switch (Integer.compare(homeGoals, awayGoals)) {
            case 1 -> HOME_WIN;
            case -1 -> AWAY_WIN;
            default -> DRAW;
        };
    }
}
