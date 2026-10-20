<template>
  <div class="app-container">
    <TheHeader />
    <MobileMenu />
    <SearchDialog />
    <router-view class="main-container" />
    <TheFooter />
    <FloatingButtons />
    <Lantern />
<!--    <RandomVideo />-->
    <Player v-if="$store.state.musicEnabled" />
    <div class="cursor-container"></div>
    <ContextMenu ref="contextMenuRef" />
  </div>
</template>

<script>
import TheHeader from '@/layout/Header/index.vue'
import TheFooter from '@/layout/Footer/index.vue'
import FloatingButtons from '@/components/common/FloatingButtons.vue'
import { getWebConfigApi, reportApi,getNoticeApi } from '@/api/site'
import { exchangeCodeApi } from '@/api/auth'
import { mapActions } from 'vuex'
import { initTheme } from '@/utils/theme'
import SearchDialog from '@/components/Search/index.vue'
import MobileMenu from '@/layout/MobileMenu/index.vue'
import Lantern from '@/components/Lanterns/index.vue'
// import RandomVideo from '@/components/RandomVideo/index.vue'
import Player from '@/layout/player.vue'
//import Fireworks from '@/components/Fireworks/index.vue'
import { getCookie,removeCookie } from '@/utils/cookie'
import ContextMenu from '@/components/ContextMenu/index.vue'

export default {
  name: 'App',
  components: {
    TheHeader,
    TheFooter,
    FloatingButtons,
    SearchDialog,
    MobileMenu,
    Lantern,
    // RandomVideo,
    Player,
 //   Fireworks
    ContextMenu,
  },

  async created() {
    // 主题在 index.html 的内联脚本里已经落到 <html> 上，这里只是同步一次内存态，
    // 不能再排在网络请求后面，否则深色模式要等好几个往返才生效
    initTheme()

    // 埋点上报不参与首屏，失败也不影响页面，直接放走不 await
    reportApi().catch(() => {})

    // 站点配置和公告互不依赖，并发拉取，别串成两段等待；
    // 用 allSettled 而不是 all：任一接口 500 都不该把另一个的结果一起丢掉
    const [configRes, noticeRes] = await Promise.allSettled([
      getWebConfigApi(),
      getNoticeApi(),
    ])

    const config = configRes.status === 'fulfilled' ? configRes.value : null
    const notice = noticeRes.status === 'fulfilled' ? noticeRes.value : null
    if (configRes.status === 'rejected') console.warn('站点配置加载失败，跳过', configRes.reason)
    if (noticeRes.status === 'rejected') console.warn('公告加载失败，跳过', noticeRes.reason)

    if (config && config.data) {
      this.setSiteInfo(config.data)
      this.updateFavicon(config.data.favicon)
      const extra = config.extra || {}
      this.$store.commit('setVisitorAccess', extra.visitorCount)
      this.$store.commit('setSiteAccess', extra.blogViewsCount)
    } else {
      // 没拿到配置也要置位：首屏封面据此从「等配置」切到「后台没配」，回退内置图
      this.$store.commit('markSiteInfoLoaded')
    }
    if (notice && notice.data) {
      this.$store.commit('SET_NOTICE', notice.data)
    }

    // 第三方登录必须先于 getUserInfo：token 要先落到 cookie，后面那次请求才带得上
    await this.handleThirdPartyLogin()
    //这里等待第三方登录处理完成在获取用户信息
    try {
      await this.getUserInfo()
    } catch (e) {
      // 拉取用户信息失败不能挡住后面的 redirectUrl 跳转
      console.warn('获取用户信息失败，跳过', e)
    }

    //跳转到缓存地址
    const redirectUrl = getCookie('redirectUrl')
    if (redirectUrl) {
      // 这个 cookie 是脚本可写的，跳转前必须校验：只接受同源地址，
      // 否则 window.location.href 就成了任意 URL 的开放重定向出口
      removeCookie('redirectUrl')
      const target = this.resolveRedirectUrl(redirectUrl)
      if (target) {
        window.location.href = target
      }
    }
  },
  methods: {
    ...mapActions(['setSiteInfo','getUserInfo']),

    /**
     * 用后台配置的图标替换构建时写死的 favicon,未配置则保持 index.html 里的默认值
     */
    updateFavicon(url) {
      if (!url) return
      let icon = document.querySelector("link[rel~='icon']")
      if (!icon) {
        icon = document.createElement('link')
        icon.rel = 'icon'
        document.head.appendChild(icon)
      }
      icon.href = url
    },

    /**
     * 校验 redirectUrl cookie：解析失败或不同源一律返回空串，由调用方丢弃
     */
    resolveRedirectUrl(url) {
      try {
        const target = new URL(url, window.location.origin)
        return target.origin === window.location.origin ? target.href : ''
      } catch (e) {
        return ''
      }
    },

    /**
     * 处理第三方登录回调：URL 里只带一次性 code，token 由后端换取接口下发。
     * 之前是直接把 URL 里的 token 写进 cookie，等于任何人构造一个带 token 参数的
     * 链接发给受害者，就能把对方的会话固定到攻击者账号上。
     */
    async handleThirdPartyLogin() {
      const params = new URLSearchParams(window.location.search)
      const code = params.get('code')
      const hasAuthParam = params.has('code') || params.has('token')
      if (!hasAuthParam) return
      try {
        if (code) {
          const res = await exchangeCodeApi(code)
          if (res && res.data) {
            this.$store.commit('SET_TOKEN', res.data)
          }
        }
      } catch (e) {
        // 换取失败（code 过期/已被用过）只影响本次登录，不能拖垮整个 created
        console.warn('第三方登录换取 token 失败', e)
      } finally {
        // 成功失败都要清：长期凭证和一次性 code 都不该留在地址栏、历史记录和 Referer 里
        this.clearAuthParams(params)
      }
    },

    /**
     * 抹掉地址栏里的 code / token 参数，保留其余查询串与 hash
     */
    clearAuthParams(params) {
      params.delete('code')
      params.delete('token')
      const query = params.toString()
      window.history.replaceState(
        {},
        '',
        window.location.pathname + (query ? `?${query}` : '') + window.location.hash
      )
    },

    /**
     * 初始化鼠标点击效果
     */
    initCursorEffect() {
      const container = document.querySelector('.cursor-container')
      // 容器不存在时直接返回，否则每次点击都会往 null 上 appendChild 抛错
      if (!container) return

      // 挂到实例上，便于 beforeUnmount 里解绑（同 initContextMenu 的处理）
      this.handleCursorClick = (e) => {
        const cursor = document.createElement('div')
        cursor.className = 'cursor-fx'
        cursor.style.left = `${e.clientX}px`
        cursor.style.top = `${e.clientY}px`
        container.appendChild(cursor)

        // 动画正常结束和下面的兜底谁先到谁回收，另一个再调也不能报错
        let recycled = false
        let fallbackTimer = null
        const recycle = () => {
          if (recycled) return
          recycled = true
          clearTimeout(fallbackTimer)
          cursor.remove()
        }
        // 动画被打断时（切到后台标签页、系统开了"减弱动态效果"、扩展禁用动画）
        // animationend 不会派发，只挂事件回收会让节点永久堆在 DOM 里
        fallbackTimer = setTimeout(recycle, 1000)
        cursor.addEventListener('animationend', recycle)
      }

      document.addEventListener('click', this.handleCursorClick)
    },

    initContextMenu() {
      // 挂到实例上，便于 beforeUnmount 里解绑（Vue 3 已移除「用实例方法监听生命周期钩子」的写法）
      this.handleContextMenu = (e) => {
        this.$refs.contextMenuRef.show(e)
      }

      this.handleClick = () => {
        this.$refs.contextMenuRef.hide()
      }

      document.addEventListener('contextmenu', this.handleContextMenu)
      document.addEventListener('click', this.handleClick)
    }
  },
  mounted() {
    this.initCursorEffect()
    this.initContextMenu()
  },
  beforeUnmount() {
    document.removeEventListener('contextmenu', this.handleContextMenu)
    document.removeEventListener('click', this.handleClick)
    document.removeEventListener('click', this.handleCursorClick)
  }
}
</script>

<style lang="scss">

@import 'animate.css';
// Font Awesome 5.15.4 改由 index.html 的 <link integrity> 引入：CSS 的 @import 没有
// integrity 字段，从第三方 CDN 拉样式时前端没有任何校验手段
* {
  margin: 0;
  padding: 0;
  // 原来这里挂的是商业字体 "font"（汉仪唐美人 55W），已移除，改用系统无衬线栈
  font-family: $font-sans;
  box-sizing: border-box;
}

// 正文默认无衬线；标题类交给各组件按需设成衬线（$font-serif）
body {
  font-family: $font-sans;
  color: var(--text-primary);
  // 页面底色统一交给 <html>（global.scss 里的 html + html::before 氛围层）。
  // body 只要还留着不透明底色，就会把 z-index:-1 的氛围层整个盖掉，所以这里必须是 transparent。
  background: transparent;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

// 日期 / 数字 / 元信息统一等宽，让台账的竖线对得齐
time,
code,
kbd,
samp,
pre {
  font-family: $font-mono;
}
</style>
