<script setup lang="ts">
import { useStandingsStore } from "../stores/standings";
import { computed } from "vue";
import { storeToRefs } from "pinia";

const {
    simulated,
    currentTable
  } = storeToRefs(useStandingsStore());

const probability = computed(() => {
    const legiaStanding = currentTable.value.find((team) => team.id === "legia_warszawa");
    return legiaStanding?.relegationChance ?? 10;
})

const colorClass = computed(() => {
  if (probability.value < 5) return "safe";
  if (probability.value < 20) return "warning";
  return "danger";
});
</script>

<template>
  <div class="percentage-card" :class="colorClass">
    <div class="label">Szansa na spadek Legii</div>
    <div class="value-container">
      <span class="value">{{ probability}}%</span>
    </div>
  </div>
</template>

<style scoped src="../assets/styles/components/percentage-display.css"></style>
