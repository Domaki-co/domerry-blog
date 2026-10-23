import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import gsap from 'gsap'
import 'animate.css'
import VueLazyload from 'vue-lazyload'

import ScrollTrigger from 'gsap/ScrollTrigger'
gsap.registerPlugin(ScrollTrigger)

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'animate.css'
import '@/styles/global.scss'
import '@/styles/elmentui.scss'

// Element Plus 不再提供 element-ui 的 el-icon-* 字体图标（theme-chalk 里没有这套字体），
// 因此把本项目实际用到的图标按 SVG 组件注册到全局：
// 模板里可以写 <el-icon><Edit /></el-icon>，也可以给 el-button/el-input 的 icon 属性传字符串
// （Element Plus 的 icon prop 类型包含 String，会按全局组件名解析）。
import {
  ArrowRight, Back, Calendar, Camera, ChatLineRound, ChatLineSquare, Check, Clock,
  Close, CoffeeCup, Collection, DArrowLeft, DArrowRight, Delete, Document, Edit, Key, Link,
  Lock, Menu, Message, Minus, Notebook, Plus, Reading, Refresh, RefreshLeft,
  RefreshRight, Search, Star, Top, Upload, User, UserFilled, VideoPause, VideoPlay, View
} from '@element-plus/icons-vue'

const globalIcons = {
  ArrowRight, Back, Calendar, Camera, ChatLineRound, ChatLineSquare, Check, Clock,
  Close, CoffeeCup, Collection, DArrowLeft, DArrowRight, Delete, Document, Edit, Key, Link,
  Lock, Menu, Message, Minus, Notebook, Plus, Reading, Refresh, RefreshLeft,
  RefreshRight, Search, Star, Top, Upload, User, UserFilled, VideoPause, VideoPlay, View
}

// 全局 HTML 净化：v-html 渲染用户可控内容前必须经过 $sanitize
import { sanitizeHtml } from '@/utils/sanitize'

// 阅读模式的开关是模块级共享状态，必须在挂载前恢复：晚一步，首页会先按
// 「有大图」渲染一帧再抽掉，等于闪一下
import { initReadingMode } from '@/utils/reading-mode'
initReadingMode()

//表情组件
import EmojiPicker from '@/components/common/EmojiPicker.vue'

//点击外部
import ClickOutside from '@/directives/clickOutside'

//加载组件
import loading from './directives/loading'

//高亮
import 'highlight.js/styles/atom-one-dark.css'
import { animateOnScroll } from './directives/animate'

//图片预览组件
import ImagePreview from '@/components/common/ImagePreview.vue'

import 'virtual:svg-icons-register'

// 注册全局组件
import SvgIcon from '@/components/SvgIcon/index.vue'

const app = createApp(App)

// 配置 vue-lazyload
app.use(VueLazyload, {
  preLoad: 1.3,
  error: '/static/img/lazy.gif',
  loading: '/static/img/lazy.gif',
  attempt: 1,
  observer: true,
  observerOptions: {
    rootMargin: '0px',
    threshold: 0.1
  }
})

// 完整安装 Element Plus：$message / $confirm 等全局方法由 makeInstaller 注入，
// 改为按需自动导入会丢掉这些方法，切勿改成 unplugin-auto-import。
app.use(ElementPlus)

app.config.globalProperties.$sanitize = sanitizeHtml

app.component('mj-emoji', EmojiPicker)
app.component('mj-image-preview', ImagePreview)
app.component('svg-icon', SvgIcon)

for (const [name, icon] of Object.entries(globalIcons)) {
  app.component(name, icon)
}

app.directive('click-outside', ClickOutside)
app.directive('loading', loading)
app.directive('animate-on-scroll', animateOnScroll)

app.use(router)
app.use(store)

app.mount('#app')
