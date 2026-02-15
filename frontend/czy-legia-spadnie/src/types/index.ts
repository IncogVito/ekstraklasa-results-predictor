import type { MatchFixtureModel } from './api';

export interface Team {
    id: string;
    name: string;
    played: number;
    points: number;
    top4Chance?: number;
    relegationChance?: number;
    // logo?: string; 
}


export interface Match {
    id: string;
    homeTeam: string; // Team ID or Name
    awayTeam: string; // Team ID or Name
    homeScore?: number | null;
    awayScore?: number | null;
    date?: string;
    matchday: number;
    isFinished?: boolean;
    isModified?: boolean;
}

export interface MatchRound {
    matchday: number;
    matches: Match[];
}

export interface SimulatedTeamStanding extends Team {
    simulatedScore: boolean;
}

export interface Standings {
    updatedAt: string;
    table: Team[];
}

export interface ProbabilityResult {
    value: number; // 0-100
    trend: 'up' | 'down' | 'stable';
    color: 'success' | 'warning' | 'danger';
}

// User requested explicit SimulatedFixture type
export interface SimulatedFixture extends Match {
    userHomeScore?: number | null;
    userAwayScore?: number | null;
}
