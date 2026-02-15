import type { TablePredictionModel, TeamStrengthMap } from '../types/api';

const BASE_URL = '/api/v1';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`API error: ${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

export async function getTablePredictions(): Promise<TablePredictionModel[]> {
  const response = await fetch(`${BASE_URL}/table-prediction`);
  return handleResponse<TablePredictionModel[]>(response);
}


export async function getTeamStrength(): Promise<TeamStrengthMap> {
  const response = await fetch(`${BASE_URL}/team-strength`);
  return handleResponse<TeamStrengthMap>(response);
}

import { SeasonOverviewResponseSchema, type SeasonOverviewResponse } from '../schemas/seasonOverview';

export async function getSeasonOverview(): Promise<SeasonOverviewResponse> {
  const response = await fetch(`${BASE_URL}/season-overview`);
  const data = await handleResponse<unknown>(response);
  return SeasonOverviewResponseSchema.parse(data);
}
