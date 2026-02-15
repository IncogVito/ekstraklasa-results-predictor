import { z } from 'zod';

export const TeamSchema = z.object({
    id: z.string(),
    name: z.string(),
    played: z.number().int().min(0),
    points: z.number().int().min(0),
});

export const MatchSchema = z.object({
    id: z.string(),
    homeTeam: z.string(),
    awayTeam: z.string(),
    homeScore: z.number().int().min(0).nullable().optional(),
    awayScore: z.number().int().min(0).nullable().optional(),
    date: z.string().optional(),
});

export const StandingsSchema = z.object({
    updatedAt: z.string(),
    table: z.array(TeamSchema),
});

export const MatchesListSchema = z.array(MatchSchema);
