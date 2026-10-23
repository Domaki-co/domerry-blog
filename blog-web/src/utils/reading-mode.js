import { ref } from 'vue'

/**
 * 阅读模式：首页不渲染首屏整屏大图，只留筛选器 + 文章列表。
 *
 * 为什么是一个模块级 ref 而不是 Vuex：开关只有一个布尔，但要同时被三处消费 ——
 * Header 的按钮、首页的渲染、顶栏 / 悬浮按钮的「图上态」判据。往 store 里加一个
 * module 得到的是同样的东西，外加一层 action / mutation 样板；共享 ref 正好够用。
 *
 * 落盘用 localStorage 而不是跟账号走：它表达的是「这台机器上我现在不想看大图」，
 * 不是一份要同步到所有设备的用户偏好。
 */
const STORAGE_KEY = 'domerry-reading-mode'

/** 全局开关，模板里直接读它就有响应式 */
export const readingMode = ref(false)

function persist(on) {
  try {
    window.localStorage.setItem(STORAGE_KEY, on ? '1' : '0')
  } catch (e) {
    // 隐私模式 / 配额满：开关本身照常生效，只是记不住，不值得打扰用户
  }
}

/**
 * 设置开关，并同步 <html data-reading-mode="on|off">
 *
 * 那个 data 属性是给「不在组件树里」的样式留的钩子（比如以后要给滚动条、
 * 页面底色做区分），组件内部的样式一律用 readingMode 绑类名，不依赖它。
 *
 * @param {boolean} on 是否进入阅读模式
 */
export function setReadingMode(on) {
  readingMode.value = !!on
  if (typeof document !== 'undefined') {
    document.documentElement.dataset.readingMode = on ? 'on' : 'off'
  }
  persist(!!on)
}

export function toggleReadingMode() {
  setReadingMode(!readingMode.value)
}

/** 启动时读一次：读不到、或存储被禁，就当作关 */
export function initReadingMode() {
  let on = false
  try {
    on = window.localStorage.getItem(STORAGE_KEY) === '1'
  } catch (e) {
    on = false
  }
  setReadingMode(on)
}
