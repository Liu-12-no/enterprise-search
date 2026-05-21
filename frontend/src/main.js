import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 引入 Element Plus 及其样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)

app.use(router)
app.use(ElementPlus) // 告诉 Vue 使用组件库

app.mount('#app')