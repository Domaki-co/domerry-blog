<template>
  <section class="hero">
    <!--
      首屏大图：只用一张 —— 最新那篇文章的封面。
      不做轮播：轮播是「每 6 秒换一张装饰图」，既抢首屏带宽，也和「工程笔记」的
      台账语言不是一路。这张图有明确语义（它是这一篇的封面，和右侧「最新」文案同源），
      所以留着；但它被压成右侧一条窄栏，左边让给文字。
    -->
    <div v-if="cover && !coverFailed" class="hero-media">
      <img
        :src="cover"
        :alt="latest ? latest.title : ''"
        loading="eager"
        fetchpriority="high"
        decoding="async"
        @error="coverFailed = true"
      >
    </div>

    <div class="hero-inner">
      <div class="hero-copy">
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
    // 由首页传入的置顶/最新文章列表，这里只取第一条当「最新」
    carousels: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      // 封面加载失败就撤掉整块图，露出 .hero 的纸白底，不做占位图回退
      coverFailed: false
    }
  },
  computed: {
    /**
     * 首屏「最新」取列表第一条（后端按置顶 + 时间返回），无数据时整块不渲染
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
    /**
     * 首屏大图 = 最新那篇的封面。没有就整块不渲染，走纯排版报头
     */
    cover() {
      const first = this.carousels.find(item => item && item.id && item.cover)
      return first ? first.cover : ''
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
    // 换了最新文章就重新给封面一次机会
    cover() {
      this.coverFailed = false
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  首屏 · 报头
//
//  取向上放弃「整屏大图 + 居中标题」那套门户长相，改成一本工程笔记的扉页：
//  左边站名 + 最新一篇，右边一条窄的封面竖栏，底下一条实线压住统计行。
//  高度从 100vh 降到 ~64vh —— 纯内容不需要占满一屏，让下面的台账早点露出来。
//
//  全页只有一处动效（三段文字依次浮起），prefers-reduced-motion 下停掉。
// ════════════════════════════════════════════════════════════════

$header-offset: 64px;

.hero {
  position: relative;
  width: 100%;
  // 不再占满整屏：内容说了算，但给一个下限免得窄内容时太扁
  min-height: min(64vh, 620px);
  // 顶栏是 fixed 的，#app 又留了 64px padding，这里负边距顶回页面最顶部
  margin-top: -$header-offset;
  padding-top: $header-offset;
  overflow: hidden;
  // 纸白底 + 一条底部实线，和全站的台账语言对齐
  background: var(--background);
  border-bottom: 1px solid var(--rule);
  display: flex;
  align-items: flex-end;
}

// ── 右侧封面竖栏 ──
.hero-media {
  position: absolute;
  top: $header-offset;
  right: 0;
  bottom: 0;
  width: clamp(180px, 26vw, 380px);
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    // 往左淡出到纸白，避免一条硬边把版面切成两半
    mask-image: linear-gradient(to left, #000 62%, transparent 100%);
    -webkit-mask-image: linear-gradient(to left, #000 62%, transparent 100%);
  }
}

.hero-inner {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: clamp(40px, 6vh, 72px) clamp(20px, 4vw, 48px) clamp(28px, 4vh, 44px);
  display: flex;
  flex-direction: column;
  gap: clamp(24px, 4vh, 40px);
}

.hero-copy {
  // 文字压在纸白上，右侧留出封面竖栏的宽度。
  // 注意全局是 border-box：max-width 含 padding，所以这里要把预留的右内边距加回去，
  // 否则真正留给文字的一列只剩 62ch - 300px ≈ 325px，
  // 没有 Songti SC 的机器（回退到 SimSun/Georgia，字形更宽）站名就会折成两行。
  max-width: min(100%, calc(62ch + clamp(0px, 22vw, 300px)));
  padding-right: clamp(0px, 22vw, 300px);
}

.hero-name {
  font-family: $font-serif;
  font-size: clamp(2.1rem, 5vw, 3.5rem);
  line-height: 1.08;
  letter-spacing: 0.005em;
  font-weight: 600;
  color: var(--text-primary);
}

.hero-tag {
  margin-top: 12px;
  font-size: clamp(0.86rem, 1.1vw, 0.98rem);
  color: var(--text-secondary);
  max-width: 44ch;
}

.hero-latest {
  margin-top: clamp(24px, 4vh, 40px);
  padding-top: 18px;
  // 1px 实线，不用卡片阴影
  border-top: 1px solid var(--rule);
  max-width: 58ch;

  .line {
    display: flex;
    align-items: center;
    gap: 10px;
    font-family: $font-mono;
    font-size: 0.76rem;
    letter-spacing: 0.04em;
    color: var(--accent);

    .cat {
      color: var(--text-tertiary);
    }
  }

  h2 {
    margin-top: 9px;
    font-family: $font-serif;
    font-size: clamp(1.2rem, 2.2vw, 1.65rem);
    line-height: 1.35;
    font-weight: 600;

    a {
      color: var(--text-primary);
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
  bottom: clamp(28px, 4vh, 44px);
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
  color: var(--text-tertiary);
  writing-mode: vertical-rl;
  transition: color 0.25s ease;

  &::after {
    content: '';
    width: 1px;
    height: 52px;
    background: linear-gradient(
      to bottom,
      var(--rule-strong),
      transparent
    );
  }

  &:hover {
    color: var(--text-primary);
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
  .hero-copy {
    padding-right: 0;
    max-width: 100%;
  }

  // 窄屏下封面竖栏会挤掉正文，直接撤掉，退回纯排版报头
  .hero-media {
    display: none;
  }
}

@include responsive(md) {
  .hero {
    min-height: min(56vh, 480px);
  }

  // 窄屏下竖排的「向下」会和正文抢空间
  .hero-scroll {
    display: none;
  }
}

// 尊重系统的「减少动态效果」设置
@media (prefers-reduced-motion: reduce) {
  .reveal {
    animation: none !important;
    transition: none !important;
    opacity: 1;
    transform: none;
  }
}
</style>
