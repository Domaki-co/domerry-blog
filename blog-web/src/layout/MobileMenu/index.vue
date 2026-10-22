<template>
  <el-drawer 
    v-model="visible" 
    direction="ltr" 
    :with-header="false"
    size="50%"
  >
    <div class="mobile-menu">
      <div class="menu-header">
        <!-- 配置回来前不渲染：空 src 会先画一枚破图标（同 Header 的处理） -->
        <img v-if="logoSrc && !logoFailed" :src="logoSrc" alt="logo" class="logo" @error="logoFailed = true">
        <h2 class="site-name">{{ $store.state.webSiteInfo.name }}</h2>
      </div>
      <div class="menu-content">
        <TransitionGroup name="menu-item">
          <router-link 
            v-for="route in routes" 
            :key="route.path"
            :to="route.path"
            class="menu-item"
            :class="{ active: $route.path === route.path }"
            @click="closeMenu"
          >
            <i v-if="route.meta?.icon" :class="route.meta.icon"></i>
            <span>{{ getMenuTitle(route) }}</span>
          </router-link>
        </TransitionGroup>
      </div>
      <div class="menu-footer">
        <p>© {{ new Date().getFullYear() }} {{ $store.state.webSiteInfo.title }}</p>
      </div>
    </div>
  </el-drawer>
</template>

<script>
export default {
  name: 'MobileMenu',
  data() {
    return {
      visible: false,
      // 取不到站标就整张撤掉，不留破图标（地址本身来自 store，见 computed.logoSrc）
      logoFailed: false
    }
  },
  watch: {
    '$store.state.mobileMenuVisible': {
      immediate: true,
      handler(val) {
        this.visible = val
      }
    },
    visible(val) {
      if (val !== this.$store.state.mobileMenuVisible) {
        this.$store.commit('setMobileMenuVisible', val)
      }
    },
    // 后台换站标后重新给这张图一次机会，否则失败标记会一直压着它
    logoSrc() {
      this.logoFailed = false
    }
  },
  computed: {
    // 站点配置（/api/webConfig）回来前是 undefined；空串会给 <img> 一个坏地址
    logoSrc() {
      return this.$store.state.webSiteInfo.logo || ''
    },
    routes() {
      const rootRoute = this.$router.options.routes.find(route => route.path === '/')
      if (!rootRoute || !rootRoute.children) return []
      
      return rootRoute.children.filter(route => {
        if (!route.name || route.meta?.hidden) return false
        return true
      })
    }
  },
  methods: {
    closeMenu() {
      this.$store.commit('setMobileMenuVisible', false)
    },
    getMenuTitle(route) {
      return route.meta?.title?.split(' - ')[0] || route.name
    }
  }
}
</script>

<style lang="scss" scoped>

.mobile-menu {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--surface);
}

.menu-header {
  padding: 20px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  height: 36px;
  width: auto;
  border-radius: $border-radius-sm;
  transition: transform 0.3s ease;
}

.logo:hover {
  transform: scale(1.05);
}

.site-name {
  font-family: $font-serif;
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.menu-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  color: var(--text-secondary);
  text-decoration: none;
  border-radius: $border-radius-sm;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.menu-item i {
  margin-right: 12px;
  font-size: 18px;
  transition: transform 0.3s ease;
}

.menu-item span {
  flex: 1;
  font-weight: 500;
}

.menu-item:hover {
  background-color: var(--hover-bg);
  color: var(--text-primary);
  transform: translateX(4px);
}

.menu-item:hover i {
  transform: scale(1.1);
}

.menu-item.active {
  background-color: var(--accent);
  color: #fff;
}

.menu-footer {
  padding: 16px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 0.9rem;
  border-top: 1px solid var(--border-color);
}

/* 菜单项动画 */
.menu-item-enter-active,
.menu-item-leave-active {
  transition: all 0.3s ease;
}

.menu-item-enter,
.menu-item-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
