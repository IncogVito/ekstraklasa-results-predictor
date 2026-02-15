import { z } from 'zod';

export const FootballClubSchema = z.object({
  id: z.string(),
  name: z.string(),
  code: z.string(),
});

export const MatchResultSchema = z.object({
  matchId: z.string(),
  finished: z.boolean(),
  round: z.number().optional(),
  scoreStr: z.string().optional(),
  winner: z.enum(['HOME_WIN', 'AWAY_WIN', 'DRAW']).optional(),
  homeGoals: z.number().optional(),
  awayGoals: z.number().optional(),
  homeTeamCode: z.string(),
  awayTeamCode: z.string(),
  utcTime: z.string().optional(),
});

export const MatchFixtureSchema = z.object({
  matchId: z.string(),
  round: z.number(),
  homeName: z.string().optional(),
  homeId: z.string(),
  homeTeamCode: z.string().optional(),
  awayName: z.string().optional(),
  awayId: z.string(),
  awayTeamCode: z.string().optional(),
  utcTime: z.string(),
});

export const SeasonOverviewResponseSchema = z.object({
  clubs: z.array(FootballClubSchema),
  results: z.array(MatchResultSchema),
  fixtures: z.array(MatchFixtureSchema),
});

export type FootballClub = z.infer<typeof FootballClubSchema>;
export type MatchResult = z.infer<typeof MatchResultSchema>;
export type MatchFixture = z.infer<typeof MatchFixtureSchema>;
export type SeasonOverviewResponse = z.infer<typeof SeasonOverviewResponseSchema>;
