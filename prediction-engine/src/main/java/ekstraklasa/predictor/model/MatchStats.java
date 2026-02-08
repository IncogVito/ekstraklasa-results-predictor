package ekstraklasa.predictor.model;

import lombok.Data;

@Data
public class MatchStats {
    // Procenty / wartości zmiennoprzecinkowe
    private Double ballPossession;
    private Double defense;
    private Double expectedGoals;
    private Double expectedGoalsNonPenalty;
    private Double expectedGoalsOnTarget;
    private Double expectedGoalsOpenPlay;
    private Double expectedGoalsSetPlay;

    // Liczniki całkowite
    private Integer offsides;
    private Integer shotsOffTarget;
    private Integer shotsOnTarget;
    private Integer accurateCrosses;
    private Integer accuratePasses;
    private Integer aerialsWon;
    private Integer bigChance;
    private Integer bigChanceMissedTitle;
    private Integer blockedShots;
    private Integer clearances;
    private Integer corners;
    private Integer discipline; // liczba punktów dyscyplinarnych - karta/ punkty
    private Integer dribblesSucceeded;
    private Integer duelWon;
    private Integer duels;
    private Integer fouls;
    private Integer groundDuelsWon;
    private Integer interceptions;
    private Integer keeperSaves;
    private Integer longBallsAccurate;
    private Integer tackles;
    private Integer oppositionHalfPasses;
    private Integer ownHalfPasses;
    private Integer passes;
    private Integer playerThrows;
    private Integer redCards;
    private Integer shotBlocks;
    private Integer shots;
    private Integer shotsInsideBox;
    private Integer shotsOutsideBox;
    private Integer shotsWoodwork;
    private Integer totalShots;
    private Integer touchesOppBox;
    private Integer yellowCards;
}

