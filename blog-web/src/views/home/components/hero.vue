<template>
  <section class="hero" :class="{ 'is-plain': !showCover }">
    <!--
      首屏 = 一张站点固定封面铺满视口，站名居中压在图上。
      这里是「首页图」，不放任何文章信息：最新一篇只留一枚 Latest 胶囊当入口，
      文章台账要往下滚动才出现。图是纯装饰（站名就在旁边），整层对读屏隐藏。
    -->
    <div class="hero-cover" aria-hidden="true">
      <img
        v-if="showCover"
        :src="HERO_COVER"
        alt=""
        loading="eager"
        fetchpriority="high"
        decoding="async"
        @error="coverFailed = true"
      >
    </div>
    <!-- 遮罩：图不可预知，白字需要一个与主题无关的底（理由见样式块） -->
    <div class="hero-scrim" aria-hidden="true"></div>

    <div class="hero-inner">
      <!--
        Latest 胶囊：有最新文章时是入口，没有时退化成不可点的标签。
        aria-label 补上标题 —— 可见文字只有「Latest」，读屏听不出它通向哪一篇
      -->
      <router-link
        v-if="latest"
        class="hero-pill reveal"
        :to="`/post/${latest.id}`"
        :aria-label="`最新一篇：${latest.title}`"
      >
        Latest
      </router-link>
      <span v-else class="hero-pill reveal">Latest</span>

      <h1 class="hero-name reveal">{{ siteName }}</h1>
      <p v-if="siteSummary" class="hero-tag reveal">{{ siteSummary }}</p>
    </div>

    <button type="button" class="hero-scroll" @click="$emit('scroll-to-posts')">
      <span aria-hidden="true">↓</span>
      向下
    </button>
  </section>
</template>

<script>
// 首屏封面：换图只改这一行，或直接替换 public/static/img/ 下的文件；置空则退回纯墨色首屏
// const HERO_COVER = '/static/img/beijing.jpg'
const HERO_COVER = '/static/img/book.png'

export default {
  name: 'HomeHero',
  props: {
    // 由首页传入的置顶/最新文章列表，这里只取第一条当「最新」
    carousels: {
      type: Array,
      default: () => []
    }
  },
  emits: ['scroll-to-posts'],
  data() {
    return {
      // 交给模板用（Options API 的模板只能读实例属性），保持常量的书写形式便于对照
      HERO_COVER,
      // 封面加载失败就撤掉 img，露出 .hero 的墨底，不做占位图回退
      coverFailed: false
    }
  },
  computed: {
    /**
     * 首屏「最新」取列表第一条（后端按置顶 + 时间返回），无数据时胶囊退化成标签
     */
    latest() {
      const first = this.carousels.find(item => item && item.id)
      if (!first) return null
      return {
        id: first.id,
        title: first.title,
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
    },
    /**
     * 有图且没加载失败才渲染 img；否则整屏退回墨色，白字照样立得住
     */
    showCover() {
      return !!HERO_COVER && !this.coverFailed
    }
  },
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  首屏 · 全屏封面
//
//  取向上放弃「左文右窗」的扉页排版，改成门户式的整屏图 + 居中站名：
//  一张站点固定封面（与具体文章无关）铺满视口，站名居中压在图上，
//  底部中央是「向下」提示。高度严格一屏，文字垂直居中。
//
//  动效只有两处：三段文字依次浮起、「向下」上下浮动；都是纯装饰，
//  prefers-reduced-motion 下一起停掉。
// ════════════════════════════════════════════════════════════════

$header-offset: 64px;

.hero {
  position: relative;
  width: 100%;
  // 顶栏是 fixed 的、#app 又留了 64px padding：负边距把图顶到视口最上沿，
  // 等量 padding-top 再把内容让回顶栏下面。两条成对出现，缺一个图就铺不满。
  margin-top: -$header-offset;
  padding-top: $header-offset;
  // padding-top 是给固定顶栏让位的，它会把内容盒的中心推到视口中心下方 32px；
  // 补一条等量的下内边距把这块偏移抵消回来，justify-content:center 才是真的居中
  // （.hero-scroll 是绝对定位，不吃内边距，位置不受这条影响）
  padding-bottom: $header-offset;
  // 严格一屏；svh 写在 vh 后面覆盖它 —— 移动端地址栏收放时 svh 不跳
  min-height: 100vh;
  min-height: 100svh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  // 常驻墨底。写死理由：它要保证「图没解码完」「图加载失败」「HERO_COVER 置空」
  // 三种状态下白字都有底，而它不是主题色 —— 跟着 data-theme 翻转的话深色档下会
  // 变得更亮，白字反而消失，所以不去动 variables.scss。
  background: #12161c;
}

// 封面层：图铺满并裁切，自己不吃事件，文字层压在它上面
.hero-cover {
  position: absolute;
  inset: 0;

  img {
    display: block;
    width: 100%;
    height: 100%;
    // object-position 略偏上：横图默认居中会把天空裁掉一半
    object-fit: cover;
    object-position: center 45%;
  }
}

// 遮罩：分三段「顶压暗 / 中间全透 / 底压暗」。单层整体压暗是整个画面发浊、暖色被
// 洗成灰紫的原因，所以只在需要的地方给墨：顶带托住顶栏（那里有白字要达标）、
// 底带托住「向下」，中间整段是 0，照片本色透出来。
// 文字块原先单独铺过一圈径向压暗，已经去掉：它在图上是一块看得见的深色椭圆，
// 读起来像污渍而不是遮罩。现在标题的底只剩 .hero-name / .hero-tag 自己那层
// text-shadow；若换到很亮的图、白字开始发飘，先给中间这段一个很小的值（整张均匀
// 压暗），不要再把局部椭圆加回来。
// 写死墨色：遮罩要中和的是「不可预知的图」，跟主题无关；若跟着 data-theme 翻转，
// 深色档下遮罩会变亮，白字就没底了。
.hero-scrim {
  position: absolute;
  inset: 0;
  // 顶带用固定长度而不是百分比：顶栏是固定 64px 高，视口越矮它占的比例越大，
  // 写死的百分比在矮屏上会让顶栏下缘落进衰减段，白字直接掉到达标线以下
  //
  // 顶带从 0.84 松到 0.62：图上态的顶栏已经不是裸白字，而是深色玻璃壳
  // （Header/index.vue 的 .site-header.is-over-hero .navbar），壳自带 10% 白 + 模糊。
  // 遮罩再压着 0.84，图就没有明暗变化可模糊，玻璃会退化成一块深色板。
  // 两边是同一条契约：若换到很亮的图、白字开始发飘，先把 0.62 和壳的 0.1 一起往上加。
  background: linear-gradient(
    180deg,
    rgba(9, 12, 16, 0.62) 0,
    rgba(9, 12, 16, 0.62) calc(#{$header-offset} + #{$spacing-sm}),
    rgba(9, 12, 16, 0.2) 30%,
    rgba(9, 12, 16, 0) 44%,
    rgba(9, 12, 16, 0) 60%,
    rgba(9, 12, 16, 0.5) 100%
  );
}

// 图缺失 / 加载失败：遮罩撤掉 —— 它只为「不可预知的图」服务，平墨底上再叠一层
// 只会把底压花（.hero 的墨色已经足够）。
.hero.is-plain .hero-scrim {
  display: none;
}

.hero-inner {
  position: relative;
  z-index: 1;
  // 保留版心：居中标题在超宽屏上也和下方台账共用同一条左右边界
  width: 100%;
  max-width: $container-max;
  margin: 0 auto;
  padding: 0 $container-gutter;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: clamp(14px, 2.4vh, 24px);
}

// Latest 胶囊：白描边 + 半透明白底，不用填充色块（这套设计没有实心按钮）
.hero-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 16px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-family: $font-mono;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  color: #fff;
  text-decoration: none;
  transition: background-color 0.2s ease;
}

// 兜底的 span 版本不该有 hover 反馈，只给真链接
a.hero-pill:hover {
  background: rgba(255, 255, 255, 0.24);
}

.hero-name {
  // 全屏图上改成无衬线粗体：衬线字在照片背景里笔画会糊掉
  font-family: $font-sans;
  font-size: clamp(2.75rem, 8vw, 4.5rem);
  line-height: 1.08;
  letter-spacing: -0.01em;
  font-weight: 700;
  color: #fff;
  // 字边一层薄墨：平核已经够达标，这层只压住笔画和照片高光相接的那圈过渡
  text-shadow: 0 2px 16px rgba(9, 12, 16, 0.4);
}

.hero-tag {
  max-width: 44ch;
  font-size: clamp(0.9rem, 1.2vw, 1.05rem);
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.82);
  // 同上，小字号要的更近、更实一点
  text-shadow: 0 1px 8px rgba(9, 12, 16, 0.55);
}

// 「向下」提示：钉在底部中央。用 button 而不是 div —— 它是可点的
.hero-scroll {
  position: absolute;
  z-index: 1;
  left: 0;
  right: 0;
  bottom: clamp(20px, 3.5vh, 36px);
  // left/right 拉满 + width:max-content + 自动边距：居中交给布局，
  // transform 就留给浮动动画，两者不会互相顶掉
  width: max-content;
  margin: 0 auto;
  padding: 6px 10px;
  border: 0;
  background: none;
  font-family: $font-mono;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  color: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition: color 0.2s ease;
  animation: hero-bob 2.6s ease-in-out infinite;

  &:hover {
    color: #fff;
  }
}

@keyframes hero-bob {
  50% {
    transform: translateY(6px);
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
  // 窄屏两处下限要跟着降：2.75rem 的站名在 320–375px 上会顶到 gutter；
  // 底部提示往下多让一点，避开手机的 Home Indicator。
  // （旧版这里藏着「右侧封面视窗」的两条规则，本轮图要铺满手机，已整块删除）
  .hero-name {
    font-size: clamp(2.25rem, 11vw, 3.25rem);
  }

  .hero-scroll {
    bottom: clamp(28px, 6vh, 44px);
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

  // 「向下」的往复浮动是纯装饰，一并停掉；这里写在动画声明之后，权重相同才盖得住
  .hero-scroll {
    animation: none;
    transition: none;
  }
}
</style>
