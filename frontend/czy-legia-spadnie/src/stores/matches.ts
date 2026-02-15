import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Match, MatchRound } from '../types';
import type { FootballClub, MatchResult, MatchFixture } from '../schemas/seasonOverview';
import { getSeasonOverview } from '../services/api';

export const useMatchesStore = defineStore('matches', () => {
  const clubs = ref<FootballClub[]>([]);
  const results = ref<MatchResult[]>([]);
  const fixtures = ref<MatchFixture[]>([]);
  const simulatedFixtures = ref<Record<string, { home: number | null; away: number | null; modified: boolean }>>({});

  const isLoading = ref(false);
  const error = ref<string | null>(null);

  // --- Helpers ---
  function getClubName(code: string): string {
    const club = clubs.value.find((c) => c.code === code);
    return club ? club.name : code;
  }

  // --- Getters ---

  const allMatches = computed<Match[]>(() => {
    const mappedResults: Match[] = results.value.map((r) => ({
      id: r.matchId,
      homeTeam: getClubName(r.homeTeamCode),
      awayTeam: getClubName(r.awayTeamCode),
      homeScore: r.homeGoals,
      awayScore: r.awayGoals,
      date: r.utcTime,
      matchday: r.round || 0,
      isFinished: true,
      isModified: false,
    }));

    const resultIds = new Set(mappedResults.map((r) => r.id));

    const mappedFixtures: Match[] = fixtures.value
      .filter((f) => !resultIds.has(f.matchId))
      .map((f) => {
        const sim = simulatedFixtures.value[f.matchId];
        return {
          id: f.matchId,
          homeTeam: getClubName(f.homeTeamCode || ''),
          awayTeam: getClubName(f.awayTeamCode || ''),
          homeScore: sim ? sim.home : null,
          awayScore: sim ? sim.away : null,
          date: f.utcTime,
          matchday: f.round,
          isFinished: false,
          isModified: sim ? sim.modified : false,
        };
      });

    return [...mappedResults, ...mappedFixtures];
  });

  const rounds = computed<MatchRound[]>(() => {
    const map = new Map<number, Match[]>();
    allMatches.value.forEach((m) => {
      if (!map.has(m.matchday)) {
        map.set(m.matchday, []);
      }
      map.get(m.matchday)!.push(m);
    });

    return Array.from(map.entries())
      .sort(([a], [b]) => a - b)
      .map(([matchday, matches]) => ({ matchday, matches }));
  });

  const matchdays = computed(() =>
    rounds.value.map((r) => r.matchday).sort((a, b) => a - b),
  );

  const hasChanges = computed(() => {
    return Object.values(simulatedFixtures.value).some((s) => s.modified);
  });

  // --- Actions ---

  async function loadMatches() {
    isLoading.value = true;
    error.value = null;
    try {
      const data = await getSeasonOverview();
      clubs.value = data.clubs;
      results.value = data.results;
      fixtures.value = data.fixtures;
      
      // Initialize simulated fixtures empty or default?
      // For now keep existing simulations if we want persistence, but for "load" usually we reset or just load data.
      // If we want to preserve edits across re-fetches (e.g. polling), we should be careful.
      // Assuming loadMatches is init:
      // simulatedFixtures.value = {}; // Reset simulations on reload? Or keep them?
      // Let's reset for now to ensure clean state from API
      // simulatedFixtures.value = {}; 
      // Actually, if we just navigate away and back, we might want to keep it? 
      // User request doesn't specify persistence. Let's start with empty.
      
    } catch (e: any) {
      error.value = e.message;
      console.error('Failed to load season overview', e);
    } finally {
      isLoading.value = false;
    }
  }

  function updateMatchScore(
    matchId: string,
    homeScore: number | null,
    awayScore: number | null,
  ) {
    // Only allow for fixtures
    const fixture = fixtures.value.find((f) => f.matchId === matchId);
    if (!fixture) {
      console.warn('Cannot update score for non-fixture match or unknown match');
      return;
    }

    // Update simulation state
    simulatedFixtures.value[matchId] = {
      home: homeScore,
      away: awayScore,
      modified: true, // Mark as modified even if null, if it was touched? Or only if values exist?
      // Requirement: "czy bylo wgl zmieniane". 
    };
    
    // Create new object reference to trigger reactivity if needed, though ref of object should handle property changes if reactive.
    // simFixtures is ref, internal object is plain. Reassigning key works.
  }

  function resetMatches() {
    simulatedFixtures.value = {};
  }

  function getRoundByMatchday(matchday: number): MatchRound | undefined {
    return rounds.value.find((r) => r.matchday === matchday);
  }

  return {
    rounds,
    allMatches,
    matchdays,
    hasChanges,
    isLoading,
    error,
    loadMatches,
    updateMatchScore,
    resetMatches,
    getRoundByMatchday,
    getClubName,
  };
});
