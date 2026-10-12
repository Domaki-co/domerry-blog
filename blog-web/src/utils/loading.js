import { createApp } from 'vue'
import Loading from '@/components/common/Loading.vue'

let loadingApp = null
let loadingInstance = null

export const showLoading = () => {
  if (!loadingInstance) {
    // Vue 2 的「构造器 + 手动 $mount()」在 Vue 3 里就是 createApp + mount(容器)
    loadingApp = createApp(Loading)
    loadingInstance = loadingApp.mount(document.createElement('div'))
    document.body.appendChild(loadingInstance.$el)
  }
  loadingInstance.visible = true
}

export const hideLoading = () => {
  if (loadingInstance) {
    loadingInstance.visible = false
  }
}
