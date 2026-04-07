import { createApp } from 'vue'
import { createPinia } from 'pinia'
import '@unocss/reset/tailwind.css'
import 'virtual:uno.css'
import '@/styles/reset.scss'
import '@/styles/tokens.scss'
import '@/styles/motion.scss'
import '@/styles/utilities.scss'
import App from '@/App.vue'
import router from '@/router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
