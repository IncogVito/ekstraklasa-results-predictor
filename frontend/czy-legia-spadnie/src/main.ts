import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import './assets/styles/main.css'
import App from './App.vue'
import HighchartsVue from 'highcharts-vue'

const app = createApp(App)
app.use(createPinia())
app.use(HighchartsVue)
app.mount('#app')
