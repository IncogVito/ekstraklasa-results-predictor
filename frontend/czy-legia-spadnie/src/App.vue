<script setup lang="ts">
import { ref, onMounted } from "vue";
import PercentageDisplay from "./components/PercentageDisplay.vue";
import StandingsTable from "./components/StandingsTable.vue";
import MatchesEditor from "./components/MatchesEditor.vue";
import AppNavigation from "./components/AppNavigation.vue";
import HistoryChart from "./components/HistoryChart.vue";
import {
  calculateRelegationProbability,
  getHistoricalProbability,
} from "./services/mockData";
import { useStandingsStore } from "./stores/standings";
import { useMatchesStore } from "./stores/matches";

const standingsStore = useStandingsStore();
const matchesStore = useMatchesStore();

const currentView = ref<"home" | "history">("home");
const probability = ref(0);
const historyData = ref(getHistoricalProbability());

const loadInitialData = async () => {
  await standingsStore.loadFromApi();
  await matchesStore.loadMatches();

  probability.value = calculateRelegationProbability(matchesStore.allMatches);
};

onMounted(() => {
  loadInitialData();
});

const handleRecalculate = () => {
  probability.value = calculateRelegationProbability(matchesStore.allMatches);
  standingsStore.simulateStandings(matchesStore.allMatches);
};

const handleReset = () => {
  standingsStore.resetSimulation();
  matchesStore.resetMatches();
  probability.value = calculateRelegationProbability(matchesStore.allMatches);
};
</script>

<template>
  <main class="app-container">
    <AppNavigation
      :current-view="currentView"
      @navigate="(view) => (currentView = view)"
    />

    <div class="content" v-if="currentView === 'home'">
      <PercentageDisplay />

      <div class="grid-layout">
        <div class="left-col">
          <StandingsTable />
        </div>
        <div class="right-col">
          <MatchesEditor
            @recalculate="handleRecalculate"
            @reset="handleReset"
          />
        </div>
      </div>
    </div>

    <div class="content history-view" v-else>
      <HistoryChart :data="historyData" />
    </div>
  </main>
</template>

<style>
@import "./assets/styles/main.css";
</style>
