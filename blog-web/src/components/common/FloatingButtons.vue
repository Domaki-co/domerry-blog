<template>
  <div
    class="floating-buttons"
    :class="{ 'show-top': showBackToTop, 'over-hero': overHero }"
  >

    <el-tooltip content="切换主题" placement="left">
      <button class="float-btn theme-btn" @click="toggleTheme" title="切换主题">
        <i :class="['fas', isDarkMode ? 'fa-sun' : 'fa-moon']"></i>
      </button>
    </el-tooltip>

    <el-tooltip content="回到顶部" placement="left">
      <button 
        v-show="showBackToTop"
        class="float-btn top-btn" 
        @click="scrollToTop"
        title="回到顶部"
      >
        <i class="fas fa-arrow-up"></i>
      </button>
    </el-tooltip>
  </div>
</template>

<script>
import { getThemeMode, setThemeMode, initTheme } from '@/utils/theme'

export default {
  name: 'FloatingButtons',
  data() {
    return {
      isDarkMode: false,
      showBackToTop: false,
      // 首页首屏：整屏大图右下角已经有「向下」指示，这里要让位
      overHero: false
    }
  },
  computed: {
    themeIcon() {
      return ['fas', this.isDarkMode ? 'fa-sun' : 'fa-moon']
    }
  },
  methods: {
    toggleTheme() {
      this.isDarkMode = !this.isDarkMode
      const mode = this.isDarkMode ? 'dark' : 'light'
      setThemeMode(mode)
    },
    scrollToTop() {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      })
    },
    handleScroll() {
      this.showBackToTop = window.pageYOffset > 300
      this.updateOverHero()
    },
    /**
     * 首页首屏大图上，右下角是「向下」指示的位置。
     * 悬浮按钮是 fixed 的，会和大图上的指示叠在一起，所以首屏内先淡出。
     */
    updateOverHero() {
      if (this.$route.path !== '/') {
        this.overHero = false
        return
      }
      // 大图高度就是视口高度，滚出大图后按钮再出现
      this.overHero = window.pageYOffset < window.innerHeight - 120
    }
  },
  watch: {
    '$route.path'() {
      this.updateOverHero()
    }
  },
  mounted() {
    this.isDarkMode = initTheme()
    window.addEventListener('scroll', this.handleScroll)
    this.updateOverHero()
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.handleScroll)
  }
}
</script>

<style lang="scss" scoped>
.floating-buttons {
  position: fixed;
  right: 20px;
  bottom: 100px;
  display: flex;
  flex-direction: column;
  gap: $spacing-sm;
  z-index: 1000;
  transition: opacity 0.3s ease, transform 0.3s ease;

  // 首页首屏让位给大图上的「向下」指示
  &.over-hero {
    opacity: 0;
    pointer-events: none;
    transform: translateY(8px);
  }
}

.float-btn {
  width: 40px;
  height: 40px;
  border: 1px solid var(--rule);
  border-radius: 10px;
  background: var(--surface);
  color: var(--text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    color: var(--accent);
    border-color: var(--accent);
    transform: translateY(-2px);
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.1);
  }

  // 主题切换按钮：实底 + 白图标
  &.theme-btn {
    background: var(--accent);
    border-color: var(--accent);
    color: #fff;
    box-shadow: 0 4px 14px rgba(168, 106, 16, 0.25);

    &:hover {
      background: var(--accent);
      filter: brightness(1.08);
      box-shadow: 0 6px 18px rgba(168, 106, 16, 0.35);
      color: #fff;
    }
  }
  

  &.top-btn {
    opacity: 0;
    visibility: hidden;
    transition: opacity 0.25s ease, visibility 0.25s ease;

    .show-top & {
      opacity: 1;
      visibility: visible;
    }
  }

  i {
    font-size: 1.2em;
  }
}

@include responsive(sm) {
  .floating-buttons {
    right: 15px;
    bottom: 80px;
  }

  .float-btn {
    width: 36px;
    height: 36px;
  }
}


</style> 