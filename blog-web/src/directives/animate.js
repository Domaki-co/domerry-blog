// 进入视口时给元素加一个标记类（.animate-in 目前没有对应的 CSS，属于预留钩子）。
// Vue 2 的自定义指令没有 mounted 钩子（只有 bind/inserted），所以这个指令在迁移前从未真正生效；
// Vue 3 里 mounted 会被调用，于是必须同时补 unmounted 断开 IntersectionObserver，否则会泄漏观察器。
const observers = new WeakMap()

export const animateOnScroll = {
  mounted(el) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          el.classList.add('animate-in')
          observer.unobserve(el)
        }
      })
    }, {
      threshold: 0.1
    })

    observer.observe(el)
    observers.set(el, observer)
  },
  unmounted(el) {
    const observer = observers.get(el)
    if (observer) {
      observer.disconnect()
      observers.delete(el)
    }
  }
}
