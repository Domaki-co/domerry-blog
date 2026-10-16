<template>
  <section class="hero">
    <!--
      首屏大图：只用一张 —— 最新那篇文章的封面。
      不做轮播：轮播是「每 6 秒换一张装饰图」，既抢首屏带宽，也和「工程笔记」的
      台账语言不是一路。这张图有明确语义（它是这一篇的封面，和右侧「最新」文案同源），
      所以留着；它被装进一个 macOS 窗口壳里浮在右侧，左边让给文字。
      窗口壳要的是硬边矩形，和原先那条「往左淡出到纸白」的 mask 互斥，按定稿取窗口壳。
    -->
    <div v-if="cover && !coverFailed" class="hero-media">
      <div class="hero-window">
        <!-- 三色控制点纯装饰，不绑行为，整条标题栏对读屏隐藏 -->
        <div class="window-bar" aria-hidden="true">
          <span class="window-lights">
            <i class="light-close"></i>
            <i class="light-min"></i>
            <i class="light-zoom"></i>
          </span>
          <span class="window-title">cover</span>
        </div>
        <div class="window-body">
          <img
            :src="cover"
            :alt="latest ? latest.title : ''"
            loading="eager"
            fetchpriority="high"
            decoding="async"
            @error="coverFailed = true"
          >
        </div>
      </div>
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

        <!-- 小组件带：便签 + 运行状态胶囊。两个都是真数据，没有可显示的内容就不渲染 -->
        <div class="hero-widgets reveal">
          <div v-if="motto" class="widget-note">
            <span class="widget-label">便签</span>
            <p class="widget-note-text">{{ motto }}</p>
          </div>
          <div class="widget-status">
            <span class="status-text">
              已运行 <b>{{ runningDays }}</b> 天
            </span>
            <span class="status-divider" aria-hidden="true"></span>
            <span class="status-text">{{ updatedToday ? '今日已更新' : '今日未更新' }}</span>
            <span
              class="status-dot"
              :class="{ 'is-live': updatedToday }"
              aria-hidden="true"
            ></span>
          </div>
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
    },
    /**
     * 便签文案：后台「作者信息」这一栏，全站只有侧栏在用它，这里当座右铭复用
     */
    motto() {
      const info = this.$store.state.webSiteInfo
      return (info && info.authorInfo) || ''
    },
    /**
     * 已运行天数。起始日和页脚同源（layout/Footer/index.vue:38 的 startDate），
     * 那边改了这里要一起改
     */
    runningDays() {
      const start = new Date('2021-07-01T00:00:00')
      const days = Math.floor((Date.now() - start.getTime()) / 86400000)
      return days > 0 ? days : 0
    },
    /**
     * 列表里最新的一天。取最大值而不是第一条：置顶文章的 createTime 可能更早
     */
    latestUpdateDate() {
      const days = this.carousels
        .map(item => String((item && item.createTime) || '').slice(0, 10))
        .filter(Boolean)
        .sort()
      return days.length ? days[days.length - 1] : ''
    },
    /**
     * 今天是否更新过。createTime 是后端按本地时间排出来的字符串，这里只比对到「天」，
     * 跨时区部署时可能差一天，当状态灯够用了
     */
    updatedToday() {
      if (!this.latestUpdateDate) return false
      const now = new Date()
      const pad = value => String(value).padStart(2, '0')
      const today = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
      return this.latestUpdateDate === today
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
//  左边站名 + 最新一篇 + 两个小组件，右边一个浮动的封面视窗，底下一条实线压住统计行。
//  高度从 100vh 降到 ~64vh —— 纯内容不需要占满一屏，让下面的台账早点露出来。
//
//  动效只有三处：四段文字依次浮起、视窗悬停时朝观察者转出并抬起、状态点呼吸。
//  前两个纯装饰，prefers-reduced-motion 下连同呼吸灯一起停掉。
// ════════════════════════════════════════════════════════════════

$header-offset: 64px;
// 右侧封面视窗的宽度与留给它的呼吸位。这两个值同时决定「窗口多宽」和「正文留多宽」，
// 写成变量，免得两边各改一处、改着改着窗口就压到文字上
$window-w: clamp(240px, 30vw, 420px);
$window-gap: clamp(24px, 3vw, 56px);

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

// ── 右侧封面视窗 ──
// 这一层只当定位带：居中到 1400px 内容列、右对齐到正文右缘，窗口本体再靠右端摆放。
// 定位（translateX）和景深（perspective/rotateY）分挂两层，同一个元素上会互相顶掉
.hero-media {
  position: absolute;
  top: $header-offset;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: min(1400px, 100%);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  // 和 .hero-inner 用同一个 clamp，窗口右缘才和正文右缘对齐
  padding-right: clamp(20px, 4vw, 48px);
  // 这条带横跨整个右侧，自己不吃事件，免得挡掉正文选择和「向下」
  pointer-events: none;
}

.hero-window {
  pointer-events: auto;
  width: $window-w;
  border-radius: $border-radius-lg;
  background: var(--card-bg);
  // 1px 微细外边框：窗口和纸白底之间要有条线，否则浅色下糊成一片
  border: 1px solid var(--rule);
  box-shadow: 0 24px 48px -20px rgba(0, 0, 0, 0.22), 0 2px 8px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  overflow: hidden;
  // 函数列表两边写成同一组（rotateY 0 → -2deg），过渡才是插值而不是离散替换
  transform: perspective(1000px) rotateY(0deg) translateZ(0);
  transition: transform 0.5s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.5s cubic-bezier(0.16, 1, 0.3, 1);

  // 朝观察者轻微转出 + 抬起。translateZ 挂在 perspective() 之后才吃得到景深
  &:hover {
    transform: perspective(1000px) rotateY(-2deg) translateZ(14px);
    box-shadow: 0 34px 60px -22px rgba(0, 0, 0, 0.3), 0 2px 10px rgba(0, 0, 0, 0.08),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);
  }
}

.window-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 30px;
  padding: 0 12px;
  // 磨砂标题栏：深浅两套靠 --surface-rgb 自己的值走，不用各写一份
  background: rgba(var(--surface-rgb), 0.72);
  backdrop-filter: blur(12px) saturate(160%);
  border-bottom: 1px solid var(--rule);
}

.window-lights {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: none;

  i {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    // 0.5px 内描边（而不是 border）：纯色圆点在高分屏上会发毛，压一圈边才像系统画的
    box-shadow: inset 0 0 0 0.5px rgba(0, 0, 0, 0.14);
  }

  .light-close {
    background: #ff5f56;
  }

  .light-min {
    background: #ffbd2e;
  }

  .light-zoom {
    background: #27c93f;
  }
}

.window-title {
  font-family: $font-mono;
  font-size: 0.64rem;
  letter-spacing: 0.08em;
  color: var(--text-tertiary);
}

.window-body {
  // 定死比例：封面尺寸不一，不锁的话窗口会随图变得又高又矮
  aspect-ratio: 16 / 10;
  background: var(--surface);

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
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
  // 文字压在纸白上，右侧留出封面视窗的宽度 + 一段呼吸位。
  // 注意全局是 border-box：max-width 含 padding，所以这里要把预留的右内边距加回去，
  // 否则真正留给文字的一列只剩 62ch - 420px ≈ 200px，
  // 没有 Songti SC 的机器（回退到 SimSun/Georgia，字形更宽）站名就会折成两行。
  max-width: min(100%, calc(62ch + #{$window-w} + #{$window-gap}));
  padding-right: calc(#{$window-w} + #{$window-gap});
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

// ── 首屏小组件带 ──
.hero-widgets {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: clamp(20px, 3vh, 32px);
}

// 灵感便签：Stickies 的暖琥珀半透明。边框用 accent 的透明色，不是实色
.widget-note {
  display: flex;
  align-items: baseline;
  gap: 10px;
  max-width: 46ch;
  padding: 9px 13px;
  border-radius: $border-radius-md;
  background: var(--accent-soft);
  border: 1px solid rgba($accent, 0.22);
  backdrop-filter: blur(10px) saturate(150%);

  .widget-label {
    flex: none;
    font-family: $font-mono;
    font-size: 0.62rem;
    letter-spacing: 0.14em;
    color: var(--accent);
  }

  .widget-note-text {
    font-size: 0.82rem;
    line-height: 1.5;
    color: var(--text-secondary);
    // 便签最多两行，再多就盖过正文了
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}

// 运行状态胶囊：毛玻璃底 + 呼吸绿点
.widget-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 13px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid var(--rule);
  backdrop-filter: blur(12px) saturate(150%);

  .status-text {
    font-family: $font-mono;
    font-size: 0.7rem;
    letter-spacing: 0.02em;
    // 天数从 3 位涨到 4 位时胶囊宽度不跳，靠等宽数字
    font-variant-numeric: tabular-nums;
    color: var(--text-tertiary);

    b {
      font-weight: 600;
      color: var(--text-secondary);
    }
  }

  .status-divider {
    width: 1px;
    height: 11px;
    background: var(--rule-strong);
  }

  .status-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--text-tertiary);
    opacity: 0.45;

    // 只有「今日已更新」才亮。这个绿和视窗三色里的绿灯同源
    &.is-live {
      background: #27c93f;
      opacity: 1;
      animation: live-pulse 2.4s cubic-bezier(0.16, 1, 0.3, 1) infinite;
    }
  }
}

// 呼吸＝一圈扩散开的光晕，比直接改 opacity 更像系统里的活动指示
@keyframes live-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(39, 201, 63, 0.45);
  }

  50% {
    box-shadow: 0 0 0 5px rgba(39, 201, 63, 0);
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

// 四段文字依次浮起
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

  &:nth-child(4) {
    animation-delay: 0.54s;
  }
}

@keyframes reveal {
  to {
    opacity: 1;
    transform: none;
  }
}

// 深色下窗口靠外发光高光线立住，投影也要更重一档才有浮起感
:root[data-theme='dark'] .hero-window {
  border-color: rgba(255, 255, 255, 0.1);
  box-shadow: 0 24px 48px -20px rgba(0, 0, 0, 0.6), 0 2px 8px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);

  // 这个 hover 必须重写：本选择器权重高于上面的 .hero-window:hover，
  // 只覆盖基础态的话，深色下悬停就没有投影反馈了
  &:hover {
    box-shadow: 0 34px 60px -22px rgba(0, 0, 0, 0.7), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  }

  // 深色底上控制点自带的那圈压边要更暗，否则三个圆点会发光
  .window-lights i {
    box-shadow: inset 0 0 0 0.5px rgba(0, 0, 0, 0.35);
  }
}

:root[data-theme='dark'] .hero-widgets {
  .widget-note {
    border-color: rgba($accent, 0.3);
  }

  .widget-status {
    background: rgba(255, 255, 255, 0.04);
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

  // 景深浮起和呼吸灯都是纯装饰，一并停掉。这里写在 :hover 之后，权重相同才盖得住
  .hero-window,
  .hero-window:hover {
    transform: none;
    transition: none;
  }

  .widget-status .status-dot.is-live {
    animation: none;
    box-shadow: 0 0 0 3px rgba(39, 201, 63, 0.18);
  }
}
</style>
