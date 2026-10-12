import { createApp } from 'vue'
import Loading from '../components/common/Loading.vue'

export default {
  beforeMount(el, binding) {
    // 设置父元素定位，以便loading可以相对定位
    const position = getComputedStyle(el).position
    if (position === 'static' || position === '') {
      el.style.position = 'relative'
    }

    // 确保父元素有明确的高度
    if (el.style.minHeight === '') {
      el.style.minHeight = '100px'
    }

    // Vue 2 的「构造器 + propsData 手动实例化」在 Vue 3 里对应
    // createApp(组件, props)：app.mount(容器) 返回根组件实例，容器本身不进入文档，
    // 真正挂到页面上的还是 vm.$el。
    const app = createApp(Loading, {
      local: true // 默认使用局部加载模式
    })
    const vm = app.mount(document.createElement('div'))
    el.instance = vm
    el.loadingApp = app
    el.appendChild(vm.$el)

    if (binding.value) {
      el.instance.visible = true
    }
  },

  updated(el, binding) {
    if (binding.value !== binding.oldValue && el.instance) {
      el.instance.visible = binding.value
    }
  },

  unmounted(el) {
    if (el.instance) {
      if (el.instance.$el.parentNode === el) {
        el.removeChild(el.instance.$el)
      }
      el.loadingApp.unmount()
      el.instance = null
      el.loadingApp = null
    }
  }
}
