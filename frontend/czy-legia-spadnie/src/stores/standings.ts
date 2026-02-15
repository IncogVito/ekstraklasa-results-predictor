import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Team, Match, SimulatedTeamStanding } from '../types';
import type { TablePredictionModel } from '../types/api';
import { getTablePredictions } from '../services/api';
import { updateStandings } from '../services/mockData';
import { useMatchesStore } from './matches';

export const useStandingsStore = defineStore('standings', () => {
  const matchesStore = useMatchesStore();
  const simulated = ref(false);
  const predictions = ref<TablePredictionModel[]>([]);
  
  const realScoreTable = computed<Team[]>(() => {
    return predictions.value
      .sort((a, b) => a.ranking - b.ranking)
      .map((p) => ({
        id: p.footballClubCode,
        name: matchesStore.getClubName(p.footballClubCode),
        played: p.matchPlayed ?? 0,
        points: p.points,
        top4Chance: p.top4Prediction != null ? p.top4Prediction : undefined,
        relegationChance:
          p.relegationPrediction != null ? p.relegationPrediction : undefined,
      }));
  });

  const simulatedScoreTable = ref<SimulatedTeamStanding[]>([]);

  const currentTable = computed(() =>
    simulated.value ? simulatedScoreTable.value : realScoreTable.value,
  );

  async function loadFromApi() {
    const data = await getTablePredictions();
    predictions.value = data;

    simulatedScoreTable.value = realScoreTable.value.map((t) => ({
      ...t,
      simulatedScore: false,
    }));

    simulated.value = false;
  }

  function simulateStandings(matches: Match[]) {
    const updated = updateStandings(matches);

    const realPointsMap = new Map(
      realScoreTable.value.map((t) => [t.id, t.points]),
    );

    simulatedScoreTable.value = updated.map((t) => ({
      ...t,
      simulatedScore: t.points !== (realPointsMap.get(t.id) ?? t.points),
    }));

    simulated.value = true;
  }

  function resetSimulation() {
    simulatedScoreTable.value = realScoreTable.value.map((t) => ({
      ...t,
      simulatedScore: false,
    }));
    simulated.value = false;
  }

  return {
    simulated,
    realScoreTable,
    simulatedScoreTable,
    currentTable,
    loadFromApi,
    simulateStandings,
    resetSimulation,
  };
});
