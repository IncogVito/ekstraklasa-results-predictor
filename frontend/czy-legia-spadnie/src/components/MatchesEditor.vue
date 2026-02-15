<script setup lang="ts">
import type { Match } from "../types";
import { ref, computed, watch } from "vue";
import SimulatedScore from "./SimulatedScore.vue";
import { useMatchesStore } from "../stores/matches";

const store = useMatchesStore();

const emit = defineEmits<{
  (e: "recalculate"): void;
  (e: "reset"): void;
}>();

const currentMatchday = computed(() => store.matchdays[currentMatchdayIndex.value] ?? 0);
const currentMatchdayIndex = ref(0);

watch(
  () => store.matchdays,
  (newMatchdays) => {
    if (newMatchdays.length > 0) {
      const startOfToday = new Date();
      startOfToday.setHours(0, 0, 0, 0);

      const sortedMatches = [...store.allMatches]
        .filter((m) => m.date)
        .sort((a, b) => new Date(a.date!).getTime() - new Date(b.date!).getTime());

      const upcomingMatch = sortedMatches.find(
        (m) => new Date(m.date!) >= startOfToday
      );

      if (upcomingMatch) {
        const index = newMatchdays.indexOf(upcomingMatch.matchday);
        if (index !== -1) {
          currentMatchdayIndex.value = index;
        }
      } else {
        currentMatchdayIndex.value = newMatchdays.length - 1;
      }
    }
  },
  { once: true }
);

const groupedMatches = computed(() => {
  const round = store.getRoundByMatchday(currentMatchday.value);
  if (!round) return [];

  const current = [...round.matches].sort((a, b) => {
    // Treat missing dates as very old or very future? 
    // Usually they should have dates now. If not, put them at the end or beginning.
    // Let's assume they have dates or empty string.
    return (a.date || "").localeCompare(b.date || "");
  });

  const orderedGroups: { date: string; matches: Match[] }[] = [];

  current.forEach((match) => {
    let key = "Nieznana data";
    if (match.date) {
      const dateObj = new Date(match.date);
      const dayName = new Intl.DateTimeFormat("pl-PL", {
        weekday: "long",
      }).format(dateObj);
      const dayDate = new Intl.DateTimeFormat("pl-PL", {
        day: "2-digit",
        month: "2-digit",
      }).format(dateObj);
      const formattedKey = `${dayName}, ${dayDate}`;
      key = formattedKey.charAt(0).toUpperCase() + formattedKey.slice(1);
    }

    let group = orderedGroups.find((g) => g.date === key);
    if (!group) {
      group = { date: key, matches: [] };
      orderedGroups.push(group);
    }
    group.matches.push(match);
  });

  return orderedGroups;
});

const formatTime = (dateStr?: string, isFinished?: boolean) => {
  const time = dateStr
    ? new Intl.DateTimeFormat("pl-PL", {
        hour: "2-digit",
        minute: "2-digit",
      }).format(new Date(dateStr))
    : "--:--";
  
  if (isFinished) {
    return `${time} FT`;
  }
  return time;
};

const prevMatchday = () => {
  if (currentMatchdayIndex.value > 0) currentMatchdayIndex.value--;
};

const nextMatchday = () => {
  if (currentMatchdayIndex.value < store.matchdays.length - 1)
    currentMatchdayIndex.value++;
};

const calculateTriggered = ref(false);

const triggerRecalculate = () => {
  emit("recalculate");
  calculateTriggered.value = true;
};

const resetCalculation = () => {
  emit("reset");
  calculateTriggered.value = false;
};
</script>

<template>
  <div class="matches-card">
    <div class="pagination">
      <button
        class="page-btn"
        @click="prevMatchday"
        :disabled="currentMatchdayIndex <= 0"
      >
        &lt; Poprzednia
      </button>
      <span class="current-matchday">Kolejka {{ currentMatchday }}</span>
      <button
        class="page-btn"
        @click="nextMatchday"
        :disabled="currentMatchdayIndex >= store.matchdays.length - 1"
      >
        Następna &gt;
      </button>
    </div>

    <div class="matches-list">
      <div
        v-for="group in groupedMatches"
        :key="group.date"
        class="match-group"
      >
        <div class="date-header">{{ group.date }}</div>

        <div v-for="match in group.matches" :key="match.id" class="match-row">
          <div class="match-time"
          :class="{ 'match-time--finished': match.isFinished }"
          >{{ formatTime(match.date, match.isFinished) }}</div>
          <div class="team home">{{ match.homeTeam }}</div>
          <div class="score-inputs">
            <SimulatedScore
              :id-prefix="`match-${match.id}`"
              :home-score="match.homeScore"
              @update:home-score="(val) => store.updateMatchScore(match.id, val, match.awayScore ?? null)"
              :away-score="match.awayScore"
              @update:away-score="(val) => store.updateMatchScore(match.id, match.homeScore ?? null, val)"
              :disabled="match.isFinished"
            />
          </div>
          <div class="team away">{{ match.awayTeam }}</div>
        </div>
      </div>
    </div>

    <div class="actions">
      <button
        @click="resetCalculation"
        class="reset-btn"
        :disabled="!calculateTriggered"
      >
        Resetuj
      </button>
      <button
        @click="triggerRecalculate"
        class="calc-btn"
        :disabled="!store.hasChanges"
      >
        Przelicz Szanse
      </button>
    </div>
  </div>
</template>

<style scoped src="../assets/styles/components/matches-editor.css"></style>
