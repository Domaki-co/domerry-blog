<template>
  <section class="hero">
    <!-- 背景轮播：多张封面时自动淡入淡出 -->
    <div v-if="slides.length" class="hero-media">
      <div
        v-for="(slide, index) in slides"
        :key="slide.key"
        class="hero-slide"
        :class="{ 'is-active': index === activeIndex }"
      >
        <img
          v-if="!failedIndexes.includes(index)"
          :src="slide.cover"
          :alt="slide.title"
          @error="handleImageError(index)"
        >
      </div>
    </div>

    <!-- 文字贴底左对齐，右下角竖排「向下」 -->
    <div class="hero-inner">
      <h1 class="hero-name reveal">{{ siteName }}</h1>
      <p v-if="siteSummary" class="hero-tag reveal">{{ siteSummary }}</p>

      <div v-if="latest" class="hero-latest reveal">
        <div class="line">
          <time>{{ latest.date }}</time>
          <span v-if="latest.category" class="cat">{{ latest.category }}</span>
        </div>
        <h2>
          <router-link :to="`/post/${latest.id}`">{{ latest.title }}</router-link>
        </h2>
      </div>
    </div>

    <button class="hero-scroll" type="button" @click="$emit('scroll-down')">
      向下
    </button>
  </section>
</template>

<script>
export default {
  name: 'HomeHero',
  props: {
    carousels: {
      type: Array,
      default: () => []
    },
    interval: {
      type: Number,
      default: 6000
    }
  },
  data() {
    return {
      activeIndex: 0,
      timer: null,
      // 加载失败的封面下标集合。失败就撤掉这张图，露出 .hero 的深色底，
      // 不再回退到 beijing.jpg —— 那张图是开源清理时生成的 AI 热带插画，
      // 和这个运维博客完全不搭（详见 docs/新仓库开源交付方案.md）
      failedIndexes: []
    }
  },
  computed: {
    slides() {
      return this.carousels
        .filter(item => item && item.cover)
        .map(item => ({
          key: `slide-${item.id}`,
          id: item.id,
          title: item.title,
          cover: item.cover
        }))
    },
    /**
     * 首屏「最近更新」取轮播第一篇文章（后端按置顶+时间返回），无数据时整块不渲染
     */
    latest() {
      const first = this.carousels.find(item => item && item.id)
      if (!first) return null
      return {
        id: first.id,
        title: first.title,
        // 台账取向：日期等宽 + YYYY-MM-DD 定长
        date: String(first.createTime || '').slice(0, 10),
        category: first.categoryName || ''
      }
    },
    siteName() {
      const info = this.$store.state.webSiteInfo
      if (info && info.name) return info.name
      const title = import.meta.env.VITE_APP_TITLE || ''
      return title.split(/[-—|]/)[0].trim() || '欢迎'
    },
    siteSummary() {
      return this.$store.state.webSiteInfo.summary || ''
    }
  },
  watch: {
    carousels() {
      this.activeIndex = 0
      this.restart()
    }
  },
  mounted() {
    this.restart()
  },
  beforeUnmount() {
    this.stop()
  },
  methods: {
    restart() {
      this.stop()
      if (this.slides.length <= 1) return
      this.timer = setInterval(() => {
        // 切到后台标签页时暂停，回来接着轮播
        if (document.hidden) return
        this.activeIndex = (this.activeIndex + 1) % this.slides.length
      }, this.interval)
    },
    stop() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },
    /**
     * 封面加载失败：记下下标并撤掉这张图，露出 .hero 的深色底。
     * 不用占位图 —— 占位图本身会变成一块比纯色底更扎眼的异物。
     */
    handleImageError(index) {
      if (!this.failedIndexes.includes(index)) {
        this.failedIndexes.push(index)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  首屏 · 整屏大图
//
//  文字不居中：站名左对齐贴底，下面一条细线接「最近更新」。
//  这样首屏同时是门面和内容入口，而不是一块纯装饰的图。
//  全页只有这一处动效（图片收拢 + 文字依次浮起），
//  prefers-reduced-motion 下全部停掉。
// ════════════════════════════════════════════════════════════════

$header-offset: 64px;

.hero {
  position: relative;
  width: 100%;
  height: 100vh;
  height: 100dvh;
  min-height: 600px;
  // 顶栏是 fixed 的，#app 又留了 64px padding，这里负边距顶回页面最顶部
  margin-top: -$header-offset;
  overflow: hidden;
  background: #0b0e12;
  display: flex;
  align-items: flex-end;
}

.hero-media {
  position: absolute;
  inset: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  // 底部压暗保证白字可读，左侧再压一层让贴底的文字更实
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background:
      linear-gradient(
        to top,
        rgba(5, 7, 10, 0.94) 0%,
        rgba(5, 7, 10, 0.62) 26%,
        rgba(5, 7, 10, 0.14) 58%,
        rgba(5, 7, 10, 0.34) 100%
      ),
      linear-gradient(
        to right,
        rgba(5, 7, 10, 0.66) 0%,
        rgba(5, 7, 10, 0.18) 46%,
        rgba(5, 7, 10, 0) 78%
      );
  }
}

.hero-slide {
  position: absolute;
  inset: 0;
  opacity: 0;
  transition: opacity 1.2s ease;

  img {
    transform: scale(1.04);
  }

  &.is-active {
    opacity: 1;

    img {
      animation: heroSettle 1.8s cubic-bezier(0.22, 1, 0.36, 1) forwards;
    }
  }
}

@keyframes heroSettle {
  to {
    transform: scale(1);
  }
}

.hero-inner {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 clamp(20px, 4vw, 48px) clamp(48px, 7vh, 86px);
  color: #fff;
}

.hero-name {
  font-family: $font-serif;
  font-size: clamp(2.5rem, 6vw, 4.1rem);
  line-height: 1.06;
  letter-spacing: 0.005em;
  font-weight: 600;
  text-shadow: 0 2px 30px rgba(0, 0, 0, 0.45);
}

.hero-tag {
  margin-top: 14px;
  font-size: clamp(0.86rem, 1.1vw, 0.98rem);
  color: rgba(255, 255, 255, 0.76);
  max-width: 44ch;
}

.hero-latest {
  margin-top: clamp(30px, 5vh, 54px);
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.22);
  max-width: 640px;

  .line {
    display: flex;
    align-items: center;
    gap: 10px;
    font-family: $font-mono;
    font-size: 0.76rem;
    letter-spacing: 0.04em;
    // 大图上用亮一档的琥珀，否则 #a86a10 在暗底上发闷
    color: #e0a94a;

    .cat {
      color: rgba(255, 255, 255, 0.6);
    }
  }

  h2 {
    margin-top: 9px;
    font-family: $font-serif;
    font-size: clamp(1.25rem, 2.3vw, 1.75rem);
    line-height: 1.35;
    font-weight: 600;

    a {
      color: #fff;
      text-decoration: none;
      // 下划线从左侧展开，不用 border-bottom（会占掉行盒高度）
      background-image: linear-gradient(currentColor, currentColor);
      background-size: 0% 1px;
      background-repeat: no-repeat;
      background-position: 0 100%;
      transition: background-size 0.35s ease;

      &:hover {
        background-size: 100% 1px;
      }
    }
  }
}

.hero-scroll {
  position: absolute;
  right: clamp(20px, 4vw, 48px);
  bottom: clamp(48px, 7vh, 86px);
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-family: $font-mono;
  font-size: 0.72rem;
  letter-spacing: 0.14em;
  color: rgba(255, 255, 255, 0.55);
  writing-mode: vertical-rl;
  transition: color 0.25s ease;

  &::after {
    content: '';
    width: 1px;
    height: 52px;
    background: linear-gradient(
      to bottom,
      rgba(255, 255, 255, 0.5),
      rgba(255, 255, 255, 0)
    );
  }

  &:hover {
    color: #fff;
  }
}

// 三段文字依次浮起
.reveal {
  opacity: 0;
  transform: translateY(14px);
  animation: reveal 0.9s cubic-bezier(0.22, 1, 0.36, 1) forwards;

  &:nth-child(1) {
    animation-delay: 0.15s;
  }

  &:nth-child(2) {
    animation-delay: 0.28s;
  }

  &:nth-child(3) {
    animation-delay: 0.42s;
  }
}

@keyframes reveal {
  to {
    opacity: 1;
    transform: none;
  }
}

@include responsive(lg) {
  .hero {
    min-height: 520px;
  }

  .hero-latest {
    max-width: 100%;
  }
}

@include responsive(md) {
  .hero {
    min-height: 480px;
  }

  // 窄屏下竖排的「向下」会和正文抢空间
  .hero-scroll {
    display: none;
  }
}

// 尊重系统的「减少动态效果」设置
@media (prefers-reduced-motion: reduce) {
  .hero-slide,
  .hero-slide img,
  .reveal {
    animation: none !important;
    transition: none !important;
  }

  .reveal {
    opacity: 1;
    transform: none;
  }

  .hero-slide img {
    transform: none;
  }
}
</style>
