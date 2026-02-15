<script setup lang="ts">
import { computed } from 'vue';
import type { Options } from 'highcharts';

const props = defineProps<{
  data: { week: number; probability: number }[];
}>();

const chartOptions = computed<Options>(() => ({
  chart: {
    type: 'spline',
    backgroundColor: 'transparent',
    style: {
      fontFamily: 'Inter, system-ui, sans-serif'
    }
  },
  title: {
    text: undefined
  },
  xAxis: {
    categories: props.data.map(d => `Kol. ${d.week}`),
    lineColor: '#333',
    tickColor: '#333',
    labels: {
      style: { color: '#888' }
    }
  },
  yAxis: {
    title: { text: undefined },
    gridLineColor: '#2a2a2a',
    labels: {
      style: { color: '#888' },
      format: '{value}%'
    }
  },
  legend: {
    enabled: false
  },
  tooltip: {
    backgroundColor: 'rgba(30, 30, 30, 0.9)',
    style: { color: '#fff' },
    borderColor: '#444',
    borderRadius: 8,
    valueSuffix: '%'
  },
  plotOptions: {
    spline: {
      lineWidth: 3,
      marker: {
        fillColor: '#1e1e1e',
        lineWidth: 2,
        lineColor: null // inherit from series
      }
    }
  },
  series: [{
    type: 'spline',
    name: 'Szanse spadku',
    data: props.data.map(d => d.probability),
    color: '#ef4444',
    shadow: {
      color: 'rgba(239, 68, 68, 0.3)',
      width: 10,
      opacity: 0.1
    }
  }]
}));
</script>

<template>
  <div class="history-card">
    <h2>Historia Prawdopodobieństwa Spadku</h2>
    <div class="chart-container">
      <highcharts :options="chartOptions"></highcharts>
    </div>
  </div>
</template>

<style scoped>
.history-card {
  background: #1e1e1e;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
  text-align: center;
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
}

h2 {
  margin-top: 0;
  margin-bottom: 2rem;
  color: #ddd;
}

.chart-container {
  width: 100%;
  min-height: 400px;
}
</style>
