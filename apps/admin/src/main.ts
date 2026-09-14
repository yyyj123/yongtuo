import 'element-plus/theme-chalk/el-message-box.css'
import { createApp } from 'vue'
import { ElButton, ElInput } from 'element-plus'
import 'element-plus/es/components/base/style/css'
import 'element-plus/es/components/button/style/css'
import 'element-plus/es/components/input/style/css'
import App from './App.vue'
import router from './router'
import './styles/theme.css'

createApp(App)
  .use(router)
  .component('ElButton', ElButton)
  .component('ElInput', ElInput)
  .mount('#app')
