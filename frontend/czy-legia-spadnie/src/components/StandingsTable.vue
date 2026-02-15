<script setup lang="ts">
import { useStandingsStore } from "../stores/standings";

const store = useStandingsStore();

const isLegia = (name: string) => name.toLowerCase().includes("legia");

const formatPercent = (value?: number) => {
  if (value == null) return "—";
  return `${value.toFixed(1)}%`;
};
</script>

<template>
  <div class="standings-card" :class="{ simulated: store.simulated }">
    <div class="header">
      <h2>
        Tabela Ekstraklasy
      </h2>
        <span v-if="store.simulated" class="sim-badge">SYMULACJA</span>
    </div>
    <div class="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>Poz</th>
            <th class="team-name">Drużyna</th>
            <th>M</th>
            <th>Pkt</th>
            <th class="chance-col">Top 4</th>
            <th class="chance-col">Spadek</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(team, index) in store.currentTable"
            :key="team.id"
            :class="{
              'legia-row': isLegia(team.name),
              'relegation-zone': index >= store.currentTable.length - 3,
            }"
          >
            <td>{{ index + 1 }}</td>
            <td class="team-name">{{ team.name }}</td>
            <td>{{ team.played }}</td>
            <td class="points">{{ team.points }}</td>
            <td class="chance-top4">{{ formatPercent(team.top4Chance) }}</td>
            <td class="chance-relegation">{{ formatPercent(team.relegationChance) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped src="../assets/styles/components/standings-table.css"></style>
