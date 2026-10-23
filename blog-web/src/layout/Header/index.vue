<template>
  <header
    class="site-header"
    :class="{ 'header-hidden': !isHeaderVisible, 'is-over-hero': overHero }"
  >
    <nav class="navbar">
      <!-- 移动端菜单按钮 -->
      <button class="menu-btn" @click="handleOpenMobileMenu">
        <i class="fas fa-bars"></i>
      </button>

      <div class="nav-left">
        <router-link to="/" class="logo">
          <span class="logo-mark">~/</span>
          <!--
            站标是 MinIO 上的外链图：加载没完成时浏览器会先占住这块 30x30 的位子，
            地址为空或取不到时就画一枚「图找不到」的破图标，而它正好在页面最显眼处。
            所以三件事——地址为空不渲染、解码完才淡入、取不到就整张撤掉，只留「~/」+ 站名。
          -->
          <img
            v-if="logoSrc && !logoFailed"
            :src="logoSrc"
            :alt="$store.state.webSiteInfo.name"
            :class="{ 'is-painted': logoPainted }"
            @load="logoPainted = true"
            @error="logoFailed = true"
          >
          <span class="logo-text">{{$store.state.webSiteInfo.name}}</span>
        </router-link>
      </div>

      <div class="nav-center">
        <div
          v-for="item in filteredMenuItems"
          :key="item.path"
          class="nav-item"
          @mouseleave="handleMouseLeave"
        >
          <router-link
            :to="item.path"
            class="nav-link"
            :class="{
              'has-dropdown': item.children,
              'active': isActive(item)
            }"
            @mouseenter="handleMouseEnter(item)"
          >
            <i v-if="item.icon" :class="item.icon"></i>
            {{ item.name }}
            <i v-if="item.children" class="fas fa-chevron-down dropdown-icon"></i>
          </router-link>

          <div v-if="item.children"
               class="dropdown-menu"
               :class="{ active: activeDropdown === item.name }"
          >
            <a
              href="javascript:void(0)"
              v-for="child in item.children"
              :key="child.path"
              class="dropdown-item"
              :class="{ 'active': isChildActive(child) }"
              @click="handleDropdownItemClick(child)"
            >
              <i :class="child.icon"></i>
              {{ child.name }}
            </a>
          </div>
        </div>
      </div>

      <div class="nav-right">
        <a
          href="javascript:void(0)"
          class="search-btn"
          aria-keyshortcuts="Meta+K Control+K"
          @click="handleSearch"
        >
          <i class="fas fa-search"></i>
          <span class="search-text">搜索</span>
          <!-- 这枚键帽是真话：⌘K/Ctrl+K 已绑在 components/Search/index.vue 的 keydown 里 -->
          <kbd class="search-kbd" aria-hidden="true">{{ isMac ? '⌘K' : 'Ctrl K' }}</kbd>
        </a>

        <!-- 修改消息按钮的跳转路径 -->
        <router-link to="/notifications" class="message-btn" title="消息通知">
          <i class="far fa-bell"></i>
          <span class="message-count" v-if="showBage()" />
        </router-link>

        <!-- 阅读模式：首页不渲染首屏整屏大图，只留文章列表。
             它只影响首页，别的页面挂着这枚按钮只会让人以为点了没反应 -->
        <button
          v-if="$route.path === '/'"
          type="button"
          class="reading-toggle-btn"
          :class="{ 'is-on': readingMode }"
          :title="readingMode ? '退出阅读模式' : '进入阅读模式'"
          :aria-label="readingMode ? '退出阅读模式' : '进入阅读模式'"
          :aria-pressed="readingMode ? 'true' : 'false'"
          @click="handleToggleReading"
        >
          <i :class="readingMode ? 'fas fa-book-open' : 'fas fa-newspaper'"></i>
        </button>

        <!-- 一键深浅色主题切换按钮 -->
        <button
          ref="themeToggleEl"
          type="button"
          class="theme-toggle-btn"
          :class="{ 'is-dark': isDark, 'is-pulsing': themePulsing }"
          :title="isDark ? '切换到白天模式' : '切换到黑夜模式'"
          :aria-label="isDark ? '切换到白天模式' : '切换到黑夜模式'"
          @click="toggleTheme"
        >
          <!--
            日月图标是一枚自绘 SVG，不再是两个 Font Awesome 字符互切：
            太阳组「转出去 + 缩掉 + 淡出」，月亮组反向进来，只动 transform / opacity。
          -->
          <svg
            class="theme-icon"
            viewBox="0 0 24 24"
            width="16"
            height="16"
            aria-hidden="true"
            focusable="false"
          >
            <g class="icon-sun">
              <circle class="sun-core" cx="12" cy="12" r="4.3" />
              <g class="sun-rays">
                <line x1="12" y1="1.7" x2="12" y2="4.1" />
                <line x1="12" y1="19.9" x2="12" y2="22.3" />
                <line x1="1.7" y1="12" x2="4.1" y2="12" />
                <line x1="19.9" y1="12" x2="22.3" y2="12" />
                <line x1="4.7" y1="4.7" x2="6.4" y2="6.4" />
                <line x1="17.6" y1="17.6" x2="19.3" y2="19.3" />
                <line x1="4.7" y1="19.3" x2="6.4" y2="17.6" />
                <line x1="17.6" y1="6.4" x2="19.3" y2="4.7" />
              </g>
            </g>
            <path class="icon-moon" d="M20.5 14.7A8.7 8.7 0 0 1 9.3 3.5a8.7 8.7 0 1 0 11.2 11.2z" />
          </svg>
        </button>

        <!-- 移动端搜索按钮 -->
        <button class="mobile-search-btn" @click="handleSearch" title="搜索">
          <i class="fas fa-search"></i>
        </button>

        <div class="user-info">
          <div v-if="$store.state.userInfo" class="user-section" @mouseleave="showDropdown = false">
            <div class="avatar" @mouseenter="showDropdown = true" @click="showDropdown = !showDropdown">
              <el-avatar :src="$store.state.userInfo.avatar"  />
            </div>
            <!-- 用户下拉菜单 -->
            <div class="user-dropdown" v-show="showDropdown">
              <div class="dropdown-header">
                <img :src="$store.state.userInfo.avatar" :alt="$store.state.userInfo.nickname">
                <div class="user-details">
                  <span class="username">{{ $store.state.userInfo.nickname }}</span>
                  <span class="role">{{ $store.getters.isAdmin ? '管理员' : '普通用户' }}</span>
                </div>
              </div>
              <div class="dropdown-divider"></div>
              <router-link to="/user/profile" class="dropdown-item">
                <i class="fas fa-user"></i>
                个人中心
              </router-link>
              <!-- 音乐组件开关 -->
              <div class="dropdown-item dropdown-switch-item" @click.stop>
                <div class="switch-meta">
                  <i class="fas fa-music"></i>
                  <span>音乐组件</span>
                </div>
                <el-switch
                  v-model="musicSwitch"
                  size="small"
                  active-color="var(--accent)"
                  @change="handleMusicToggle"
                />
              </div>
              <div class="dropdown-item" @click="handleLogout">
                <i class="fas fa-sign-out-alt"></i>
                退出登录
              </div>
            </div>
          </div>
          <div v-else class="avatar" @click="handleLogin">
            <el-avatar class="avatar-icon"
              :src="$store.state.webSiteInfo.touristAvatar"
            />
          </div>
        </div>
      </div>
    </nav>
  </header>
</template>

<script>
import { getThemeMode, setThemeModeWithTransition, prefersReducedMotion } from '@/utils/theme'
import { sheetPeek } from '@/utils/scroll'
import { readingMode, toggleReadingMode } from '@/utils/reading-mode'

// 顶栏高度。这个数在仓库里有多份副本：本组件样式里的 $header-height、
// src/styles/global.scss:17 的 #app padding-top、home/index.vue 的 $header-offset，
// 以及这里的常量。改一处必须四处同步。
const HEADER_HEIGHT = 64
// 首页窗帘面板底部露出那条边，JS 侧从 global.scss 的 --sheet-peek 读（见 utils/scroll.js），
// 这里先给个初值，挂载和 resize 时再刷新 —— 滚动回调里不能再读样式
let peek = 0

export default {
  name: 'TheHeader',
  data() {
    return {
      isDark: false,
      // 主题按钮点击光晕的开关，播完由 setTimeout 关掉（见 playThemePulse）
      themePulsing: false,
      // 键帽按平台换字：Mac 写 ⌘K，其它平台写 Ctrl K
      isMac: typeof navigator !== 'undefined' &&
        /mac|iphone|ipad/i.test(navigator.platform || navigator.userAgent || ''),
      searchQuery: '',
      showSearchPanel: false,
      lastScrollTop: 0,
      isHeaderVisible: true,
      // 顶栏是否正压在首屏大图上：图上摘掉玻璃壳换白字，滚出大图再恢复（见 .is-over-hero）
      overHero: false,
      // 站标加载态：解码完才淡入，取不到就先撤掉（见模板注释），避免闪一下破图标
      logoPainted: false,
      logoFailed: false,
      menuItems: [
        {
          name: '首页',
          path: '/',
          icon: 'fas fa-home'
        },
        {
          name: '文章归档',
          path: '/archives',
          icon: 'fas fa-archive',
          children: [
            {
              name: '归档',
              path: '/archive',
              icon: 'fas fa-clock'
            },
            {
              name: '分类',
              path: '/categories',
              icon: 'fas fa-folder'
            },
            {
              name: '标签',
              path: '/tags',
              icon: 'fas fa-tags'
            }
          ]
        },
        {
          name: '说说',
          path: '/moments',
          icon: 'fas fa-comment-dots'
        },
        {
          name: '音乐',
          path: '/resources',
          icon: 'fas fa-music'
        },
        {
          name: '留言板',
          path: '/messages',
          icon: 'fas fa-envelope'
        },
        {
          name: '更多',
          path: '/about',
          children: [
            {
              name: '相册',
              path: '/photos',
              icon: 'fas fa-images'
            },
            {
              name: '友链',
              path: '/friends',
              icon: 'fas fa-users'
            },
            {
              name: '关于我',
              path: '/about',
              icon: 'fas fa-user'
            },
            {
              name: '后台管理',
              path: import.meta.env.VITE_APP_ADMIN_URL || 'http://localhost:30001',
              icon: 'fas fa-tv',
              external: true
            }
          ]
        }
      ],
      activeDropdown: null,
      showDropdown: false,
      showSearch: false,
      unreadCount: 0
    }
  },
  computed: {
    // 站点配置（/api/webConfig）回来前是 undefined；空串会给 <img> 一个坏地址
    logoSrc() {
      return this.$store.state.webSiteInfo.logo || ''
    },
    filteredMenuItems() {
      return this.menuItems.map(item => ({
        ...item,
        path: item.children ? item.children[0].path : item.path
      }))
    },
    musicSwitch: {
      get() {
        return this.$store.state.musicEnabled
      },
      set(val) {
        this.$store.dispatch('toggleMusicEnabled', val)
      }
    },
    // 共享 ref 读成 computed，模板里就能直接绑
    readingMode: () => readingMode.value
  },
  watch: {
    // 路由切换后 DOM 还没更新，等一拍再量大图；否则从文章页滚着回首页时量到的是
    // 上一个页面（没有 .hero），顶栏会停在玻璃壳那一帧
    '$route.path'() {
      this.$nextTick(this.updateOverHero)
    },
    // 面板开合会改变「是否强制按非图上态渲染」
    '$store.state.searchVisible': 'updateOverHero',
    '$store.state.mobileMenuVisible': 'updateOverHero',
    // 阅读模式里没有大图，顶栏一律按实心态渲染
    readingMode() {
      this.updateOverHero()
    },
    // 后台换了站标要重新开始：失败标记不清掉，新图就永远不显示
    logoSrc() {
      this.logoPainted = false
      this.logoFailed = false
    }
  },
  methods: {
    handleMusicToggle(val) {
      if (val) {
        this.$message?.success('已开启音乐组件')
      } else {
        this.$message?.info('已关闭音乐组件')
      }
    },
    handleOpenMobileMenu() {
      this.$store.commit('SET_MOBILE_MENU_VISIBLE', true)
    },
    handleSearch() {
      this.$store.commit('SET_SEARCH_VISIBLE', true)
    },
    handleLogin() {
      // 处理登录逻辑
      this.$router.push('/login')
    },
    closeAllPanels() {
      // 原本只写 showMobileSearch，该标志已随死代码清理删除
    },
    closeMobileSearch() {
      this.searchQuery = ''
    },
    handleMouseEnter(item) {
      if (item.children) {
        this.activeDropdown = item.name
      }
    },
    handleMouseLeave() {
      this.activeDropdown = null
    },
    isActive(item) {
      if (item.children) {
        // 如果子菜单，检查是否有子菜单项被激活
        return item.children.some(child => this.isChildActive(child))
      }
      // 精确匹配路由路径
      return this.$route.path === item.path
    },
    isChildActive(child) {
      return this.$route.path === child.path
    },
    handleDropdownItemClick(item) {
      this.activeDropdown = null
      if (item.external) {
        window.open(item.path, '_blank')
        return
      }
      this.$router.push(item.path)
    },
    async handleLogout() {
      this.showDropdown = false
      //等登出流程（含接口失败时的本地清理）走完再提示，避免接口挂了也提示"已退出登录"
      await this.$store.dispatch('logout')
      this.$message.success('已退出登录')
      //退出后整体刷新页面：当前页面里已按登录态渲染出来的内容（头像、管理员专属按钮、
      //个人中心/编辑器等）需要一并重置成游客视角
      //停留在需要登录的页面时直接回首页，否则刷新出来的还是个加载失败的页面
      //留 300ms 让上面的提示先渲染出来，不然会立刻被刷新冲掉
      setTimeout(() => {
        if (this.$route.meta && this.$route.meta.requiresAuth) {
          this.$router.replace('/')
        } else {
          window.location.reload()
        }
      }, 300)
    },
    toggleTheme(event) {
      // 扩散起点要在这一帧就抓下来：事件处理函数返回后 event.currentTarget 会被置空。
      // 键盘 Enter / Space 触发时事件里没有坐标，resolveOrigin 会退到按钮中心。
      const origin = this.$refs.themeToggleEl || (event && event.currentTarget) || null

      this.isDark = !this.isDark
      this.playThemePulse()

      // 等 Vue 把图标和按钮状态刷进 DOM，再做整页快照。
      // View Transition 是静态快照，此刻 DOM 还没更新的话，「新」快照里会留着旧图标，
      // 动画收尾的一瞬间会看到图标跳回去。
      this.$nextTick(() => {
        setThemeModeWithTransition(this.isDark ? 'dark' : 'light', origin)
      })
    },
    /**
     * 点击反馈：一圈光晕从按钮上扩散出去。
     * 光晕本身由 CSS 动画负责，这里只开关 class，所以在开着「减少动态效果」时直接跳过。
     */
    playThemePulse() {
      if (prefersReducedMotion()) return
      clearTimeout(this.themePulseTimer)
      this.themePulsing = true
      this.themePulseTimer = setTimeout(() => {
        this.themePulsing = false
      }, 640)
    },
    showBage() {
      console.log(this.$store.state.isUnread)
     return this.$store.state.isUnread
    },
    handleScroll() {
      const currentScrollTop = window.pageYOffset || document.documentElement.scrollTop

      // 先更新图上态：下面那条 < 100px 的分支会提前 return，放在后面的话
      // 「大图还在、但滚动量很小」的状态永远不会被刷新
      this.updateOverHero()

      // 如果滚动位置小于 100px，始终显示 header
      if (currentScrollTop < 100) {
        this.isHeaderVisible = true
        this.lastScrollTop = currentScrollTop
        return
      }

      // 判断滚动方向
      if (currentScrollTop > this.lastScrollTop) {
        // 向下滚动
        this.isHeaderVisible = false
      } else {
        // 向上滚动
        this.isHeaderVisible = true
      }

      this.lastScrollTop = currentScrollTop
    },
    // 顶栏此刻是否压在首屏大图上。
    // 判据只有两条，都不查 DOM：
    //   1) 满屏大图只在首页（hero.vue 的 .hero 是那一页的局部组件）；
    //   2) 大图是 sticky 钉住的、高度为「一屏减一条边」（min-height: calc(100svh - --sheet-peek)），
    //      所以「滚过 一屏 - 那条边 - 顶栏高」它的下缘就滑到顶栏下面了。
    // 这里曾经用 document.querySelector('.hero').getBoundingClientRect() 直接量，
    // 会在路由切换时骗人：布局的页面过渡是 <transition mode="out-in">（layout/index.vue:8，
    // 0.3s），路由一换就量，离场的那一页还挂在 DOM 上、新页面还没挂载 —— 从首页点进
    // 文章，顶栏会一直停在图上态，白字压白底，整条看不见了；反过来从文章页回首页
    // 又会量不到大图、停在实心态。innerHeight 跟着视口走，任何时刻都是对的，不用等过渡。
    // 面板打开时一律按非图上态渲染：白字会压在自己的浅色面板上（搜索是居中的
    // el-dialog、移动端菜单是占左半屏的 el-drawer，两者都盖住顶栏所在的区域）。
    updateOverHero() {
      const panelOpen = this.$store.state.searchVisible || this.$store.state.mobileMenuVisible
      // 大图是 sticky 钉住的、比视口矮 --sheet-peek 那一条，所以「它离开顶栏」
      // 发生在「面板顶边滑到顶栏下缘」那一刻：scrollY >= 大图高度 - HEADER_HEIGHT
      // 阅读模式下首页根本没有大图，不存在「图上态」
      const heroGone =
        this.readingMode || window.pageYOffset >= window.innerHeight - peek - HEADER_HEIGHT
      this.overHero = !panelOpen && this.$route.path === '/' && !heroGone
    },
    handleToggleReading() {
      toggleReadingMode()
    },
    // 视口尺寸变了：重读窗帘边长（它可能跟着断点变），再重算图上态。
    // 顺序不能反 —— updateOverHero 用的是重读后的值
    handleResize() {
      peek = sheetPeek()
      this.updateOverHero()
    }
  },
  mounted() {
    this.isDark = getThemeMode() === 'dark'
    // 首屏那条「窗帘」边可能有响应式取值，挂载时读一次真值
    peek = sheetPeek()
    // 监听全局 data-theme 变更（与 FloatingButtons/右键菜单状态双向同步）
    this.themeObserver = new MutationObserver(() => {
      this.isDark = document.documentElement.getAttribute('data-theme') === 'dark'
    })
    this.themeObserver.observe(document.documentElement, {
      attributes: true,
      attributeFilter: ['data-theme']
    })

    // 添加滚动事件监听
    window.addEventListener('scroll', this.handleScroll)
    // 视口高度变了，大图下边缘的位置跟着变，图上态要重算
    window.addEventListener('resize', this.handleResize)
    // 进站 / 从文章页返回时先算一次：只靠滚动事件会留下一帧玻璃壳压在图上的错帧
    this.updateOverHero()

    // 点击空白处收起用户下拉菜单
    this.handleDocumentClick = (e) => {
      const userSection = this.$el.querySelector('.user-section')
      if (userSection && !userSection.contains(e.target)) {
        this.showDropdown = false
      }
    }
    document.addEventListener('click', this.handleDocumentClick)
  },
  beforeUnmount() {
    if (this.themeObserver) {
      this.themeObserver.disconnect()
    }
    clearTimeout(this.themePulseTimer)
    // 移除滚动和点击事件监听
    window.removeEventListener('scroll', this.handleScroll)
    window.removeEventListener('resize', this.handleResize)
    if (this.handleDocumentClick) {
      document.removeEventListener('click', this.handleDocumentClick)
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  顶栏 · 技术台账
//
//  首屏是一枚浮在整屏大图上的深色玻璃胶囊（白字），滚出大图后切成实心态。
//  菜单靠右，当前页用琥珀色下划线标记（图上态换成白色药丸）；图标去掉彩色，只留单色。
// ════════════════════════════════════════════════════════════════

// 页面布局契约：正文从 y=64px 开始。这个数在仓库里有四份副本（global.scss 的
// #app padding-top、hero.vue 的 $header-offset、本文件脚本里的 HEADER_HEIGHT、
// 这里），改一处必须四处同步 —— 所以胶囊整体必须落在 64px 以内。
$header-height: 64px;
// 胶囊高度按参考稿（glass-nav-demo.html：高 56px、上方 12px）取 56px，
// 但上留白收成 8px：8 + 56 = 64，胶囊下缘正好压在正文起始线上，不多占一像素。
$header-pill-height: 56px;
$header-pill-gap: 8px;

.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  // 定位带比契约高一条 $header-pill-gap：多出来的是胶囊下缘到正文之间的呼吸，
  // 它不参与页面布局（fixed），所以不影响上面那条 64px 契约
  height: $header-height + $header-pill-gap;
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  // 左右留白走版心令牌，导航胶囊的左缘才对得上下方内容列
  padding: $header-pill-gap $container-gutter 0;
  pointer-events: none; // 让两侧透明留白区域不遮挡页面下方点击
  transition: transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  transform: translateY(0);

  &.header-hidden {
    transform: translateY(-120%);
  }
}

.navbar {
  pointer-events: auto;
  width: 100%;
  max-width: calc(#{$container-max} - 2 * #{$container-gutter});
  height: $header-pill-height;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px 0 18px;
  position: relative;
  gap: 12px;

  // 磨砂玻璃胶囊质感
  background: color-mix(in srgb, var(--surface) 80%, transparent);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid var(--rule);
  box-shadow: 0 10px 30px -10px rgba(0, 0, 0, 0.05), 0 0 1px 1px rgba(255, 255, 255, 0.7) inset;
  transition: background 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
}

// ── 左：站标 ──
.nav-left {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-width: 0;

  .logo {
    display: flex;
    align-items: center;
    text-decoration: none;
    gap: $spacing-sm;

    img {
      height: 30px;
      width: 30px;
      border-radius: $border-radius-sm;
      object-fit: cover;
      // 解码完才淡入（见模板注释）：加载中与失败都不留下一枚破图标
      opacity: 0;
      transition: opacity 0.2s ease;

      &.is-painted {
        opacity: 1;
      }
    }

    // 终端提示符式的记号，呼应「工程笔记」的取向
    .logo-mark {
      font-family: $font-mono;
      font-size: 0.95rem;
      color: var(--accent);
      letter-spacing: -0.02em;
      // 与 .navbar 的 0.3s 同步：滚过大图下缘时胶囊是淡出的，站标不能硬切白↔黑
      transition: color 0.3s ease;
    }

    .logo-text {
      font-family: $font-serif;
      font-size: 1.08rem;
      font-weight: 600;
      color: var(--text-primary);
      letter-spacing: 0.01em;
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      // 同上：无壳态的站名要跟着胶囊一起淡入淡出
      transition: color 0.3s ease;
    }
  }
}

// ── 中：菜单（完美居中显示）──
.nav-center {
  flex: 0 0 auto;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  font-size: 1em;

  .nav-item {
    position: relative;
    white-space: nowrap;

    &:hover {
      .dropdown-menu {
        opacity: 1;
        visibility: visible;
        transform: translateY(0);
        pointer-events: auto;
      }

      .nav-link .dropdown-icon {
        transform: rotate(180deg);
      }
    }
  }

  .nav-link {
    color: var(--text-secondary);
    text-decoration: none;
    font-weight: 500;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    border-radius: 9999px;
    font-size: 0.9rem;
    position: relative;
    white-space: nowrap;

    i {
      font-size: 0.85em;
      color: var(--text-tertiary);
      transition: color 0.2s ease;
    }

    &:hover {
      color: var(--text-primary);
      background: var(--row-hover);

      i {
        color: var(--accent);
      }
    }

    // 当前页：高亮文字 + 底部细指示条
    &.active {
      color: var(--text-primary);
      background: var(--row-hover);
      font-weight: 600;

      i {
        color: var(--accent);
      }

      &::after {
        content: '';
        position: absolute;
        left: 50%;
        transform: translateX(-50%);
        bottom: 3px;
        width: 16px;
        height: 2px;
        background: var(--accent);
        border-radius: 2px;
      }
    }

    &.has-dropdown {
      padding-right: 22px;

      .dropdown-icon {
        position: absolute;
        right: 8px;
        font-size: 0.6em;
        transition: transform 0.25s ease;
      }
    }
  }
}

// ── 右：搜索 / 消息 / 主题切换 / 头像 ──
.nav-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
  gap: 6px;

  .search-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 5px 12px;
    background: var(--row-hover);
    border-radius: 9999px;
    color: var(--text-secondary);
    text-decoration: none;
    transition: all 0.2s ease;
    border: 1px solid var(--rule);

    i {
      font-size: 0.85em;
    }

    .search-text {
      font-size: 0.84rem;
    }

    // 键帽：底色比周围略深一档就够（浅色下白底白帽会糊掉），加一条底部实边当厚度
    .search-kbd {
      padding: 1px 5px 2px;
      border-radius: 4px;
      background: var(--surface);
      border: 1px solid var(--rule);
      box-shadow: 0 1px 0 var(--rule-strong);
      font-family: $font-mono;
      font-size: 0.68rem;
      line-height: 1.35;
      color: var(--text-tertiary);
    }

    &:hover {
      color: var(--text-primary);
      border-color: var(--rule-strong);
      background: var(--hover-bg);
    }
  }

  .message-btn {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    color: var(--text-secondary);
    text-decoration: none;
    transition: all 0.2s ease;
    border-radius: 50%;

    i {
      font-size: 0.95rem;
    }

    &:hover {
      color: var(--text-primary);
      background: var(--row-hover);
    }

    .message-count {
      position: absolute;
      top: 3px;
      right: 3px;
      background: var(--accent);
      width: 7px;
      height: 7px;
      border-radius: 50%;
      padding: 0;
    }
  }

  // ── 阅读模式开关：报纸 ⇄ 打开的书 ──
  // 和主题按钮同一套几何（32px 圆钮 + 一圈内描边），但只做颜色反馈 ——
  // 它不改变「当前页面正在显示什么颜色」，转起来反而会跟旁边的主题按钮抢视线
  .reading-toggle-btn {
    width: 32px;
    height: 32px;
    padding: 0;
    border-radius: 50%;
    border: 1px solid var(--rule);
    background: transparent;
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: background-color 0.25s ease, border-color 0.25s ease, color 0.25s ease;

    i {
      font-size: 0.95rem;
    }

    &:hover {
      background: var(--row-hover);
      border-color: var(--rule-strong);
      color: var(--accent);
    }

    &:focus-visible {
      outline: 2px solid var(--accent);
      outline-offset: 2px;
    }

    // 开启态：和主题按钮的 is-dark 同一个提示语言（琥珀底 + 强调色图标）
    &.is-on {
      background: var(--accent-soft);
      border-color: var(--rule-strong);
      color: var(--accent);

      &:hover {
        border-color: var(--accent);
      }
    }
  }

  // ── 主题切换：太阳 ⇄ 月亮 ──
  .theme-toggle-btn {
    // 认领自己的延迟档位：颜色比页面主体晚 40ms 到位，形成层次而不是「啪」地一起翻过去
    --theme-delay: 40ms;

    position: relative;
    width: 32px;
    height: 32px;
    padding: 0;
    border-radius: 50%;
    // 这圈内描边就是「轨道」，深色下会被琥珀色底色衬出来
    border: 1px solid var(--rule);
    background: transparent;
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    // 只过渡可合成 / 便宜的颜色属性；transform 单独给一条更慢的曲线做回弹
    transition: background-color 0.3s ease, border-color 0.3s ease, color 0.3s ease,
      box-shadow 0.3s ease, transform 0.35s cubic-bezier(0.22, 1, 0.36, 1);

    // 光晕：绝对定位在按钮外圈、不参与布局，点击时由动画放大淡出
    &::after {
      content: '';
      position: absolute;
      inset: -1px;
      border-radius: 50%;
      border: 1px solid var(--accent);
      opacity: 0;
      pointer-events: none;
    }

    &.is-pulsing::after {
      animation: themeHalo 0.64s cubic-bezier(0.22, 1, 0.36, 1);
    }

    &:hover {
      background: var(--row-hover);
      border-color: var(--rule-strong);
      color: var(--accent);
      transform: rotate(18deg) scale(1.06);
      box-shadow: 0 0 0 4px var(--accent-soft);
    }

    // 轻微回弹：按下缩一点，松开回到 hover 的尺寸
    &:active {
      transform: rotate(18deg) scale(0.94);
    }

    // 键盘焦点必须看得见：一圈实心描边 + 外发光，不依赖 hover
    &:focus-visible {
      outline: 2px solid var(--accent);
      outline-offset: 2px;
      box-shadow: 0 0 0 4px var(--accent-soft);
    }

    // 深色：内圈被琥珀色填一层，「滑块」落位
    &.is-dark {
      background: var(--accent-soft);
      border-color: var(--rule-strong);
      color: var(--accent);

      &:hover {
        border-color: var(--accent);
      }
    }
  }

  .theme-icon {
    width: 16px;
    height: 16px;
    // 只动 transform / opacity；描边色跟着 currentColor 走，
    // 颜色本身由 global.scss 那条 .theme-transition 规则负责过渡。
    fill: none;
    stroke: currentColor;
    stroke-width: 1.7;
    stroke-linecap: round;

    .icon-sun,
    .icon-moon,
    .sun-rays {
      // viewBox 是 24×24，圆心正好落在 (12,12)
      transform-box: view-box;
      transform-origin: 12px 12px;
      transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.36s ease;
    }

    // 浅色：太阳在场；月亮已经转到 -90° 缩没
    .icon-sun {
      transform: rotate(0deg) scale(1);
      opacity: 1;
    }

    .icon-moon {
      transform: rotate(-90deg) scale(0.4);
      opacity: 0;
    }
  }

  .theme-toggle-btn.is-dark .theme-icon {
    .icon-sun {
      transform: rotate(90deg) scale(0.4);
      opacity: 0;
    }

    .icon-moon {
      transform: rotate(0deg) scale(1);
      opacity: 1;
    }
  }

  // 悬停时太阳射线慢慢转，同样只动 transform
  .theme-toggle-btn:hover .theme-icon .sun-rays {
    animation: sunRaySpin 14s linear infinite;
  }

  .user-info {
    .user-section {
      position: relative;

      &::after {
        content: '';
        position: absolute;
        top: 100%;
        left: 0;
        width: 100%;
        height: 10px;
        background: transparent;
      }
    }

    .avatar {
      width: 32px;
      height: 32px;
      cursor: pointer;
      border-radius: 50%;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 1px solid var(--rule);
      transition: all 0.2s ease;

      &:hover {
        border-color: var(--accent);
        transform: scale(1.04);
      }

      .avatar-icon {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }
    .user-dropdown {
      position: absolute;
      top: calc(100% + 10px);
      right: 0;
      width: 232px;
      background: var(--surface);
      border: 1px solid var(--rule);
      border-radius: $border-radius-md;
      box-shadow: $shadow-lg;
      overflow: hidden;
      z-index: 1000;
      animation: slideDown 0.18s ease;

      .dropdown-header {
        padding: $spacing-md;
        display: flex;
        align-items: center;
        gap: $spacing-sm;
        background: var(--row-hover);
        border-bottom: 1px solid var(--rule);
        color: var(--text-primary);

        img {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          border: 1px solid var(--rule);
        }

        .user-details {
          .username {
            display: block;
            font-weight: 600;
            font-size: 0.92rem;
          }

          .role {
            display: block;
            font-size: 0.75rem;
            color: var(--text-tertiary);
            margin-top: 2px;
          }
        }
      }

      .dropdown-divider {
        height: 1px;
        background: var(--rule);
        margin: 0;
      }

      .dropdown-item {
        padding: 10px $spacing-md;
        display: flex;
        align-items: center;
        gap: $spacing-sm;
        color: var(--text-secondary);
        text-decoration: none;
        transition: all 0.2s;
        cursor: pointer;
        font-size: 0.88rem;

        i {
          font-size: 0.9rem;
          width: 16px;
          text-align: center;
          color: var(--text-tertiary);
        }

        &:hover {
          background: var(--row-hover);
          color: var(--text-primary);

          i {
            color: var(--accent);
          }
        }
      }

      .dropdown-switch-item {
        justify-content: space-between;
        cursor: default;

        .switch-meta {
          display: flex;
          align-items: center;
          gap: $spacing-sm;
        }

        &:hover {
          background: var(--row-hover);
        }
      }
    }
  }
}

.menu-btn,
.mobile-search-btn {
  display: none;
  background: none;
  border: none;
  padding: $spacing-sm;
  font-size: 1.1rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color 0.2s ease;

  &:hover {
    color: var(--text-primary);
  }
}

// ── 下拉菜单 ──
.dropdown-menu {
  // 浮层比页面主体晚 90ms 变色，和顶栏的 40ms 拉开一档，切换时有层次感
  --theme-delay: 90ms;

  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  background: var(--surface);
  border: 1px solid var(--rule);
  border-radius: $border-radius-md;
  box-shadow: $shadow-lg;
  width: max-content;
  min-width: 132px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(6px);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  pointer-events: none;
  padding: 4px 0;

  // 触发项和菜单之间有 6px 视觉空隙，鼠标往下移动时会先离开 .nav-item，
  // 触发 mouseleave 把菜单关掉。这块透明桥接条把空隙并入菜单自身的命中区域
  // （`overflow: hidden` 会把它裁掉，所以菜单上不能有 overflow: hidden）。
  // top/height 要跟上面的 `top: calc(100% + 6px)` 和 1px 边框对上。
  &::before {
    content: '';
    position: absolute;
    top: -7px;
    left: 0;
    width: 100%;
    height: 8px;
    background: transparent;
  }

  .dropdown-item {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    padding: 8px $spacing-md;
    color: var(--text-secondary);
    text-decoration: none;
    transition: all 0.2s ease;
    white-space: nowrap;
    font-size: 0.86rem;

    i {
      width: 16px;
      text-align: center;
      font-size: 0.85rem;
      color: var(--text-tertiary);
      margin-right: 0;
    }

    &:hover,
    &.active {
      color: var(--text-primary);
      background: var(--row-hover);

      i {
        color: var(--accent);
      }
    }
  }

  &.active {
    opacity: 1;
    visibility: visible;
    transform: translateY(0);
    pointer-events: auto;
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 主题按钮点击时的光晕：一圈描边向外扩散并淡出，不夸张
@keyframes themeHalo {
  from {
    opacity: 0.55;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(2.2);
  }
}

// 悬停时太阳射线自转
@keyframes sunRaySpin {
  to {
    transform: rotate(360deg);
  }
}

// ── 响应式 ──
@media screen and (max-width: 1200px) {
  .navbar {
    padding: 0 12px 0 16px;
  }

  .nav-center .nav-link {
    font-size: 0.86rem;
    padding: 6px 10px;
  }

  .nav-right .search-btn .search-text {
    display: none;
  }

  // 文字收掉时键帽一起收，只留放大镜图标
  .nav-right .search-btn .search-kbd {
    display: none;
  }
}

@include responsive(lg) {
  .nav-center {
    display: none;
  }
}

@include responsive(md) {
  .site-header {
    // 上留白跟桌面端同源，只收窄左右：手机上胶囊 46px（见下），下缘仍落在 64px 以内
    padding: $header-pill-gap 10px 0;
  }

  .navbar {
    padding: 0 12px;
    height: 46px;
  }

  .menu-btn,
  .mobile-search-btn {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .nav-right .search-btn,
  .nav-right .message-btn,
  .nav-right .user-info {
    display: none;
  }

  // 移动端右侧保留主题切换按钮
  .nav-right .theme-toggle-btn {
    display: flex;
    width: 30px;
    height: 30px;

    .theme-icon {
      width: 15px;
      height: 15px;
    }
  }

  // 阅读模式开关在窄屏同样保留（它比「搜索 / 消息」更常用，且只有一个图标宽），
  // 尺寸跟主题按钮对齐
  .nav-right .reading-toggle-btn {
    display: flex;
    width: 30px;
    height: 30px;
  }

  // 移动端站标居中
  .nav-left {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);

    .logo .logo-text {
      max-width: 140px;
      font-size: 0.96rem;
    }
  }
}

// 深色模式下胶囊底色与通透微投影
:root[data-theme='dark'] .navbar {
  background: color-mix(in srgb, var(--surface) 76%, transparent);
  box-shadow: 0 16px 36px -10px rgba(0, 0, 0, 0.5), 0 0 1px 1px rgba(255, 255, 255, 0.05) inset;
}

// ════════════════════════════════════════════════════════════════
//  图上态：首屏大图上的顶栏换「深色玻璃」壳
//
//  这一版推翻过一次：上一版在图上是把壳摘干净、只留裸白字，让 hero 遮罩的顶带托底。
//  改回来是因为质感 —— 壳一去，白字浮在照片上没有边界，顶栏就「不存在」了；
//  参考稿（glass-nav-demo.html 的 dark glass）用的是「低透明度白 + 模糊 + 白描边」，
//  而模糊要看得出是玻璃，就得让图还有明暗变化可模糊，所以 hero 那边的顶带也跟着
//  从 0.84 降到 0.62（hero.vue 的 .hero-scrim）—— 两边是同一条契约，动一边要看另一边。
//  这里全部写死白系、不走 data-theme：它压的是照片，和主题无关（理由同遮罩写死）。
//  必须排在上面那条深色规则之后：两者编译后特异性相同，靠源码顺序取胜。
//
//  这张表不能写成通配符（`.site-header.is-over-hero *`）—— 条上挂着两个自己的
//  实底面板，必须保持原样：`.dropdown-menu`（菜单下拉）、`.user-dropdown`（用户
//  下拉）。所以这里逐条点名，只覆盖胶囊条上的前景元素。
// ════════════════════════════════════════════════════════════════
.site-header.is-over-hero {
  // 玻璃配方三段：低透明度白（壳本身）、blur 配 saturate（饱和是玻璃和「灰雾」的分界，
  // 只模糊不饱和，照片会被洗成一块灰板）、细描边配内顶高光（壳的边，代替厚边框）。
  // backdrop-filter 与 -webkit- 前缀并存：Safari 15/16 仍只认前缀版。
  .navbar {
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(14px) saturate(160%);
    -webkit-backdrop-filter: blur(14px) saturate(160%);
    border-color: rgba(255, 255, 255, 0.22);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.25), 0 8px 30px rgba(0, 0, 0, 0.18);
  }

  // ── 左：站标 ──
  .nav-left .logo {
    .logo-mark {
      color: rgba(255, 255, 255, 0.75);
    }

    .logo-text {
      color: #fff;
    }
  }

  // ── 中：菜单 ──
  .nav-center .nav-link {
    color: rgba(255, 255, 255, 0.78);

    i {
      color: rgba(255, 255, 255, 0.6);
    }

    // hover 的底色不能再用 --row-hover：那是浅色，白字压上去会消失
    &:hover {
      color: #fff;
      background: rgba(255, 255, 255, 0.16);

      i {
        color: #fff;
      }
    }

    // 当前页：浅色态那枚 --accent 下划线只有 2px 高，压在这层玻璃上会被照片吃掉，
    // 这里换成实一点的白色药丸（参考稿 `li.active` 的做法）。两态各用各的语言：
    // 滚出大图后的实心态照旧沿用琥珀下划线。
    &.active {
      color: #fff;
      background: rgba(255, 255, 255, 0.2);

      i {
        color: #fff;
      }

      &::after {
        display: none;
      }
    }
  }

  // ── 右：搜索 / 消息 / 主题 / 头像 ──
  .nav-right {
    .search-btn {
      color: rgba(255, 255, 255, 0.82);
      background: rgba(255, 255, 255, 0.14);
      border-color: rgba(255, 255, 255, 0.24);

      // 键帽在图上是一枚「更亮一点的玻璃片」，实底会像贴纸
      .search-kbd {
        background: rgba(255, 255, 255, 0.16);
        border-color: rgba(255, 255, 255, 0.26);
        box-shadow: none;
        color: rgba(255, 255, 255, 0.82);
      }

      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.22);
        border-color: rgba(255, 255, 255, 0.38);
      }
    }

    .message-btn {
      color: rgba(255, 255, 255, 0.82);

      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.14);
      }
    }

    // .theme-toggle-btn.is-dark 的琥珀底色留着：它是「深色模式生效」的状态提示，
    // 图上也读得出来，不必跟着转白
    .theme-toggle-btn,
    .reading-toggle-btn {
      color: rgba(255, 255, 255, 0.85);
      border-color: rgba(255, 255, 255, 0.3);

      &:hover {
        color: #fff;
        background: rgba(255, 255, 255, 0.14);
        border-color: rgba(255, 255, 255, 0.5);
        box-shadow: none;
      }
    }

    .user-info .avatar {
      border-color: rgba(255, 255, 255, 0.6);

      &:hover {
        border-color: #fff;
      }
    }
  }

  // ── 移动端两个图标按钮 ──
  .menu-btn,
  .mobile-search-btn {
    color: rgba(255, 255, 255, 0.85);

    &:hover {
      color: #fff;
    }
  }
}
</style>
