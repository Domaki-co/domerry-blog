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
            <i :class="item.icon"></i>
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
        <a href="javascript:void(0)" class="search-btn" @click="handleSearch">
          <i class="fas fa-search"></i>
          <span class="search-text">搜索</span>
        </a>

        <!-- 修改消息按钮的跳转路径 -->
        <router-link to="/notifications" class="message-btn">
          <i class="far fa-bell"></i>
          <span class="message-count" v-if="showBage()" />
        </router-link>

        <!-- 移动端搜索按钮 -->
        <button class="mobile-search-btn" @click="handleSearch">
          <i class="fas fa-search"></i>
        </button>

        <div class="user-info">
          <div v-if="$store.state.userInfo" class="user-section" @mouseleave="showDropdown = false">
            <div class="avatar" @mouseenter="showDropdown = true">
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

export default {
  name: 'TheHeader',
  data() {
    return {
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
          name: '相册',
          path: '/photos',
          icon: 'fas fa-images'
        },
        {
          name: '留言板',
          path: '/messages',
          icon: 'fas fa-envelope'
        },
        {
          name: '友链',
          path: '/friends',
          icon: 'fas fa-users'
        },
        {
          name: '更多',
          path: '/about',
          icon: 'fas fa-info-circle',
          children: [
            {
              name: '关于我',
              path: '/about',
              icon: 'fas fa-user'
            },
            // {
            //   name: '网站源码',
            //   path: 'https://gitee.com/Domaki/shiyi-blog',
            //   icon: 'fab fa-github',
            //   colorClass: 'github-link',
            //   external: true
            // },
            {
              name: '后台管理',
              path: import.meta.env.VITE_APP_ADMIN_URL || 'http://localhost:3000',
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
  },
  methods: {
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
    // 添加滚动事件监听
    window.addEventListener('scroll', this.handleScroll)

    // 保留原有的事件监听
    document.addEventListener('click', (e) => {
      const userSection = this.$el.querySelector('.user-section')
      if (userSection && !userSection.contains(e.target)) {
        this.showDropdown = false
      }
    })
  },
  beforeUnmount() {
    // 移除滚动事件监听
    window.removeEventListener('scroll', this.handleScroll)
    document.removeEventListener('click', this.closeDropdown)
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
  z-index: 1000;
  background: color-mix(in srgb, var(--surface) 86%, transparent);
  backdrop-filter: saturate(180%) blur(12px);
  -webkit-backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid var(--rule);
  transition: background 0.3s ease, border-color 0.3s ease, transform 0.3s ease;
  transform: translateY(0);

  &.header-hidden {
    transform: translateY(-100%);
  }
}

.navbar {
  padding: 0 $spacing-xl;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  height: $header-height;
  position: relative;
  gap: $spacing-lg;
}

// ── 左：站标 ──
.nav-left {
  flex-shrink: 0;

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

// ── 中：菜单（整体靠右，紧挨右侧操作区）──
.nav-center {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex: 1;
  gap: 2px;
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
    transition: color 0.2s ease;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 10px;
    border-radius: $border-radius-sm;
    font-size: 0.9rem;
    position: relative;
    white-space: nowrap;

    i {
      font-size: 0.85em;
      color: var(--text-tertiary);
      transition: color 0.2s ease;
    }

    // 当前页：琥珀色 + 一条下划线，不用背景块
    &.active {
      color: var(--text-primary);

      i {
        color: var(--accent);
      }

      &::after {
        content: '';
        position: absolute;
        left: 10px;
        right: 10px;
        bottom: 0;
        height: 2px;
        background: var(--accent);
        border-radius: 1px;
      }
    }

    &:hover {
      color: var(--text-primary);

      i {
        color: var(--accent);
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

// ── 右：搜索 / 消息 / 头像 ──
.nav-right {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  flex-shrink: 0;

  .search-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 12px;
    background: transparent;
    border-radius: $border-radius-sm;
    color: var(--text-secondary);
    text-decoration: none;
    transition: all 0.2s ease;
    border: 1px solid var(--rule);

    i {
      font-size: 0.85em;
    }

    .search-text {
      font-size: 0.85rem;
    }

    &:hover {
      color: var(--text-primary);
      border-color: var(--rule-strong);
      background: var(--row-hover);
    }
  }

  .message-btn {
    position: relative;
    display: flex;
    align-items: center;
    padding: 7px;
    color: var(--text-secondary);
    text-decoration: none;
    transition: color 0.2s ease;
    border-radius: $border-radius-sm;

    i {
      font-size: 1rem;
    }

    &:hover {
      color: var(--text-primary);
      background: var(--row-hover);
    }

    .message-count {
      position: absolute;
      top: 4px;
      right: 4px;
      background: var(--accent);
      width: 7px;
      height: 7px;
      border-radius: 50%;
      padding: 0;
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
        // 要盖住 .user-dropdown 的 `top: calc(100% + 10px)`，少 1px 就会留出
        // 鼠标划过去时触发 mouseleave、把菜单关掉的死区
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
      transition: border-color 0.2s ease;

      &:hover {
        border-color: var(--accent);
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
@media screen and (max-width: 1400px) {
  .nav-center .nav-link {
    font-size: 0.84rem;
    padding: 6px 8px;
    gap: 5px;

    &.active::after {
      left: 8px;
      right: 8px;
    }
  }
}

@media screen and (max-width: 1200px) {
  .navbar {
    padding: 0 $spacing-lg;
  }

  .nav-center .nav-link {
    font-size: 0.8rem;
    padding: 6px 6px;

    i {
      display: none;
    }

    &.has-dropdown {
      padding-right: 18px;

      .dropdown-icon {
        display: block;
      }
    }

    &.active::after {
      left: 6px;
      right: 6px;
    }
  }

  .nav-right .search-btn .search-text {
    display: none;
  }
}

@include responsive(lg) {
  .nav-center {
    display: none;
  }
}

@include responsive(md) {
  .navbar {
    padding: 0 $spacing-md;
  }

  .menu-btn,
  .mobile-search-btn {
    display: block;
  }

  .nav-right .search-btn,
  .nav-right .message-btn,
  .nav-right .user-info {
    display: none;
  }

  // 移动端站标居中
  .nav-left {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);

    .logo .logo-text {
      max-width: 150px;
      font-size: 1rem;
    }
  }
}

// 深色模式下顶栏底色略加深，避免和页面底色糊在一起
:root[data-theme='dark'] .site-header {
  background: color-mix(in srgb, var(--surface) 88%, transparent);
}
</style>
