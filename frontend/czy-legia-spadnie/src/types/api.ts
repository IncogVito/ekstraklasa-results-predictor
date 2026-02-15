export interface TablePredictionModel {
  id?: string;
  footballClubCode: string;
  timestamp: string;
  matchPlayed?: number;
  ranking: number;
  points: number;
  top4Prediction?: number;
  relegationPrediction?: number;
}

export type TeamStrengthMap = Record<string, number>;

export interface FootballClubModel {
  id: string;
  name: string;
  code: string;
}

export type WinnerModel = 'HOME' | 'AWAY' | 'DRAW';

export interface MatchResultModel {
  matchId: string;
  finished: boolean;
  round?: number;
  scoreStr?: string;
  winner?: WinnerModel;
  homeGoals?: number;
  awayGoals?: number;
  homeTeamCode: string;
  awayTeamCode: string;
}

export interface MatchFixtureModel {
  matchId: string;
  round: number;
  homeName?: string;
  homeId: string;
  homeTeamCode?: string;
  awayName?: string;
  awayId: string;
  awayTeamCode?: string;
  utcTime: string;
}

export interface SeasonOverviewResponse {
  clubs: FootballClubModel[];
  results: MatchResultModel[];
  fixtures: MatchFixtureModel[];
}
