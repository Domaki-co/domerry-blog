<template>
  <header
    class="site-header"
    :class="{ 'header-hidden': !isHeaderVisible }"
  >
    <nav class="navbar">
      <!-- 移动端菜单按钮 -->
      <button class="menu-btn" @click="handleOpenMobileMenu">
        <i class="fas fa-bars"></i>
      </button>

      <div class="nav-left">
        <router-link to="/" class="logo">
          <span class="logo-mark">~/</span>
          <img :src="$store.state.webSiteInfo.logo" :alt="$store.state.webSiteInfo.name">
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

        <!-- 一键深浅色主题切换按钮 -->
        <button
          class="theme-toggle-btn"
          :title="isDark ? '切换浅色模式' : '切换深色模式'"
          @click="toggleTheme"
        >
          <i :class="['fas', isDark ? 'fa-sun' : 'fa-moon']"></i>
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
import { getThemeMode, setThemeMode } from '@/utils/theme'

export default {
  name: 'TheHeader',
  data() {
    return {
      isDark: false,
      // 键帽按平台换字：Mac 写 ⌘K，其它平台写 Ctrl K
      isMac: typeof navigator !== 'undefined' &&
        /mac|iphone|ipad/i.test(navigator.platform || navigator.userAgent || ''),
      searchQuery: '',
      showSearchPanel: false,
      showMobileSearch: false,
      lastScrollTop: 0,
      isHeaderVisible: true,
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
      this.showMobileSearch = false
    },
    closeMobileSearch() {
      this.showMobileSearch = false
      this.searchQuery = ''
    },
    handleMobileSearch(tag) {
      this.searchQuery = tag
      this.$refs.mobileSearchInput.focus()
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
    toggleTheme() {
      this.isDark = !this.isDark
      setThemeMode(this.isDark ? 'dark' : 'light')
    },
    showBage() {
      console.log(this.$store.state.isUnread)
     return this.$store.state.isUnread
    },
    handleScroll() {
      const currentScrollTop = window.pageYOffset || document.documentElement.scrollTop

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
    }
  },
  mounted() {
    this.isDark = getThemeMode() === 'dark'
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
    // 移除滚动和点击事件监听
    window.removeEventListener('scroll', this.handleScroll)
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
//  首屏透明悬浮在整屏大图上（白字），滚出大图后切成毛玻璃实底。
//  菜单靠右，当前页用琥珀色下划线标记；图标去掉彩色，只留单色。
// ════════════════════════════════════════════════════════════════

$header-height: 64px;

.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: $header-height;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
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
  max-width: 1180px;
  height: 50px;
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
    }

    // 终端提示符式的记号，呼应「工程笔记」的取向
    .logo-mark {
      font-family: $font-mono;
      font-size: 0.95rem;
      color: var(--accent);
      letter-spacing: -0.02em;
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

  .theme-toggle-btn {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    border: 1px solid var(--rule);
    background: transparent;
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 0.92rem;
    transition: all 0.25s ease;

    &:hover {
      background: var(--row-hover);
      border-color: var(--rule-strong);
      color: var(--accent);
      transform: rotate(18deg);
    }
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
    padding: 0 10px;
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
    font-size: 0.85rem;
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
</style>
