<script setup lang="ts">
defineProps<{
  currentView: 'home' | 'history';
}>();

defineEmits<{
  (e: 'navigate', view: 'home' | 'history'): void;
}>();
</script>

<template>
  <nav class="app-navigation">
    <button 
      class="nav-btn" 
      :class="{ active: currentView === 'home' }"
      @click="$emit('navigate', 'home')"
      title="Strona główna"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="nav-icon">
        <path d="M3 13h1v7c0 1.103.897 2 2 2h12c1.103 0 2-.897 2-2v-7h1a1 1 0 0 0 .707-1.707l-9-9a.999.999 0 0 0-1.414 0l-9 9A1 1 0 0 0 3 13zm7 7v-5h4v5h-4zm2-15.586 6 6V15l.001 5H16v-5c0-1.103-.897-2-2-2h-4c-1.103 0-2 .897-2 2v5H6v-9.586l6-6z"/>
      </svg>
    </button>
    
    <button 
      class="nav-btn" 
      :class="{ active: currentView === 'history' }"
      disabled
      @click="$emit('navigate', 'history')"
      title="Historia szans"
    >
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" class="nav-icon">
        <path d="M7 11h2v2H7zm0 4h2v2H7zm4-4h2v2h-2zm0 4h2v2h-2zm4-4h2v2h-2zm0 4h2v2h-2z"/>
        <path d="M5 22h14c1.103 0 2-.897 2-2V6c0-1.103-.897-2-2-2h-2V2h-2v2H9V2H7v2H5c-1.103 0-2 .897-2 2v14c0 1.103.897 2 2 2zM19 8l.001 12H5V8h14z"/>
      </svg>
    </button>
  </nav>
</template>

<style scoped src="../assets/styles/components/app-navigation.css"></style>
