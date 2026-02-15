<script setup lang="ts">
import { computed, ref } from 'vue';

const props = defineProps<{
  homeScore?: number | null;
  awayScore?: number | null;
  idPrefix: string;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  (e: 'update:homeScore', value: number | null): void;
  (e: 'update:awayScore', value: number | null): void;
}>();

const bothScoredApplied = computed(() => {
  return !props.disabled && !!props.homeScore && !!props.awayScore;
});

const handleInput = (event: Event, type: 'home' | 'away') => {
  if (props.disabled) return;
  const input = event.target as HTMLInputElement;
  let val = input.value;

  // Limit to 1 digit
  if (val.length > 1) {
    val = val.slice(-1);
    input.value = val;
  }

  const numVal = val === '' ? null : parseInt(val, 10);
  
  if (type === 'home') {
    emit('update:homeScore', numVal);
    if (val !== '') {
      const awayInput = document.getElementById(`${props.idPrefix}-away`);
      awayInput?.focus();
    }
  } else {
    emit('update:awayScore', numVal);
  }
};

const handleKeydown = (event: KeyboardEvent) => {
  if (props.disabled) return;
  if (['e', 'E', '+', '-', '.', ','].includes(event.key)) {
    event.preventDefault();
  }
};
</script>

<template>
  <div class="simulated-score" :class="{ disabled, 'simulated-score--both': bothScoredApplied }">
    <input 
      :id="`${idPrefix}-home`"
      type="number" 
      :value="homeScore"
      @input="(e) => handleInput(e, 'home')"
      @keydown="handleKeydown"
      :disabled="disabled"
      placeholder="-"
      min="0"
      max="9"
    />
    <span class="colon">:</span>
    <input 
      :id="`${idPrefix}-away`"
      type="number" 
      :value="awayScore"
      @input="(e) => handleInput(e, 'away')"
      @keydown="handleKeydown"
      :disabled="disabled"
      placeholder="-"
      min="0"
      max="9"
    />
  </div>
</template>

<style scoped src="../assets/styles/components/simulated-score.css"></style>
