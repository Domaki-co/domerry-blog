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
    <Player />
    <div class="cursor-container"></div>
    <ContextMenu ref="contextMenuRef" />
  </div>
</template>

<script>
import TheHeader from '@/layout/Header/index.vue'
import TheFooter from '@/layout/Footer/index.vue'
import FloatingButtons from '@/components/common/FloatingButtons.vue'
import { getWebConfigApi, reportApi,getNoticeApi } from '@/api/site'
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
    await reportApi()
    const res = await getWebConfigApi()
    this.setSiteInfo(res.data)
    this.updateFavicon(res.data.favicon)
    this.$store.commit('setVisitorAccess', res.extra.visitorCount)
    this.$store.commit('setSiteAccess', res.extra.blogViewsCount)

    const noticeRes = await getNoticeApi()
    this.$store.commit('SET_NOTICE', noticeRes.data)
    initTheme()
    await this.handleThirdPartyLogin()
    //这里等待第三方登录处理完成在获取用户信息
    await this.getUserInfo();

    //跳转到缓存地址
    let url = getCookie('redirectUrl')
    if (url) {
      removeCookie('redirectUrl')
      window.location.href = url
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
     * 处理第三方登录用回调逻辑
     */
     async handleThirdPartyLogin() {
      let flag = window.location.href.indexOf("token") != -1;
      if (flag) {
        let token = window.location.href.split("token=")[1];
        this.$store.commit('SET_TOKEN', token);
      }
    },

    /**
     * 初始化鼠标点击效果
     */
    initCursorEffect() {
      const container = document.querySelector('.cursor-container')

      document.addEventListener('click', (e) => {
        const cursor = document.createElement('div')
        cursor.className = 'cursor-fx'
        cursor.style.left = `${e.clientX}px`
        cursor.style.top = `${e.clientY}px`
        container.appendChild(cursor)

        cursor.addEventListener('animationend', () => {
          cursor.remove()
        })
      })
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
  }
}
</script>

<style lang="scss">

@import 'animate.css';
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/all.min.css');
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
  background: var(--background);
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
