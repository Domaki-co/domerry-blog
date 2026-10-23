<template>
  <div
    class="floating-buttons"
    :class="{ 'show-top': showBackToTop, 'over-hero': overHero }"
  >

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
import { sheetPeek } from '@/utils/scroll'
import { readingMode } from '@/utils/reading-mode'

// 首页首屏那条「窗帘」边（大图比视口矮的那一条），JS 侧从 global.scss 的
// --sheet-peek 读，挂载 / resize 时刷新，滚动回调里不读
let peek = 0

export default {
  name: 'FloatingButtons',
  data() {
    return {
      showBackToTop: false,
      // 首页首屏：整屏大图右下角已经有「向下」指示，这里要让位
      overHero: false
    }
  },
  computed: {
    // 共享 ref 读成 computed，watch 才有东西可盯
    readingMode: () => readingMode.value
  },
  methods: {
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
      // 大图是 sticky 钉住的、比视口矮 --sheet-peek 那一条，所以按「大图下缘
      // 滑到这条线以下」算。多减的 120 是给首屏「向下」提示留的余量，
      // 它贴在同一个角上；判据和 Header 同源（utils/scroll.js 的 sheetPeek）
      // 阅读模式下首页没有大图，右下角也没有「向下」指示，按钮正常显示
      this.overHero =
        !this.readingMode && window.pageYOffset < window.innerHeight - peek - 120
    },
    handleResize() {
      peek = sheetPeek()
      this.updateOverHero()
    }
  },
  watch: {
    '$route.path'() {
      this.updateOverHero()
    },
    readingMode() {
      this.updateOverHero()
    }
  },
  mounted() {
    peek = sheetPeek()
    window.addEventListener('scroll', this.handleScroll)
    window.addEventListener('resize', this.handleResize)
    this.updateOverHero()
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.handleScroll)
    window.removeEventListener('resize', this.handleResize)
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
