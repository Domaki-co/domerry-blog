<template>
  <section class="hero" :class="{ 'is-plain': !hasVisual }">
    <!--
      首屏 = 一张站点固定封面铺满视口，站名居中压在图上。
      这里是「首页图」，不放任何文章信息：最新一篇只留一枚 Latest 胶囊当入口，
      文章台账要往下滚动才出现。图是纯装饰（站名就在旁边），整层对读屏隐藏。
    -->
    <div class="hero-cover" aria-hidden="true">
      <!-- 低清铺底：上一次成功加载的封面缩略图（约 1–2KB base64），几乎与 HTML 同帧就位 -->
      <div
        v-if="showPlaceholder"
        class="hero-cover-blur"
        :style="{ backgroundImage: `url(${lqip})` }"
      ></div>
      <img
        v-if="showCover"
        :src="coverSrc"
        alt=""
        loading="eager"
        fetchpriority="high"
        decoding="async"
        :class="{ 'is-painted': coverPainted }"
        @load="onCoverLoad"
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
// 首屏封面的「内置回退图」：图的正常来源是后台配置（store 的 webSiteInfo.homeCover）。
// 后台确实没配时才回退这张 —— 但只要站点配置还没回来，就一张图都不渲染，
// 否则会先下默认图、配置到了再换一次（双倍流量 + 肉眼可见的闪换）。
const HERO_COVER = '/static/img/234.jpg'

// 上一次成功加载过的封面地址，以及由它现算的低清缩略图（约 1–2KB 的 base64）。
// 作用：刷新页面时不用等 /api/webConfig 就能先发图；缩略图先把首屏铺满，
// 大图解码完再淡入，中间不会出现一块墨黑。
// 这两个 key 在 index.html 的预热脚本里也各读一次，改名要同步。
const COVER_URL_KEY = 'hero-cover-url'
const COVER_LQIP_KEY = 'hero-cover-lqip'
const LQIP_WIDTH = 24
const LQIP_QUALITY = 0.4

/**
 * 首屏退场的进度基准：滚过 hero 高度的 85% 就走完。
 * 0.85 而不是 1，是为了让退场在楼下台账完全进入视口之前收尾 ——
 * 卡在 1 的话，最后 15% 的滚动里文字还剩一点残影压在文章列表上。
 */
const PROGRESS_SPAN = 0.85

function readLocal(key) {
  try {
    return localStorage.getItem(key) || ''
  } catch (e) {
    // 隐私模式下 localStorage 直接抛错，当作没有缓存
    return ''
  }
}

function writeLocal(key, value) {
  try {
    if (value) localStorage.setItem(key, value)
    else localStorage.removeItem(key)
  } catch (e) {
    // 写不进去只是下次少一次预热，不影响页面
  }
}

/**
 * localStorage 里的值任何脚本都能改写，读回来先过滤：
 * 封面只认 http(s) 与站内绝对路径（排除 //host 形式，免得被写成任意第三方请求）
 */
function readCoverUrl() {
  const url = readLocal(COVER_URL_KEY)
  if (!url) return ''
  const sameOrigin = url.charAt(0) === '/' && url.charAt(1) !== '/'
  return sameOrigin || /^https?:\/\//i.test(url) ? url : ''
}

/**
 * 铺底图只认自己造出来的 data:image base64，别的一律丢掉
 */
function readLqip() {
  const dataUrl = readLocal(COVER_LQIP_KEY)
  return /^data:image\//i.test(dataUrl) ? dataUrl : ''
}

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
      // 封面加载失败就撤掉 img，露出 .hero 的墨底，不做占位图回退
      coverFailed: false,
      // 图已经解码完、可以淡入了；淡入之前是低清铺底
      coverPainted: false,
      cachedCover: readCoverUrl(),
      lqip: readLqip(),
      // 已经为哪个地址算过缩略图：同一个地址不必每次加载都过一遍 canvas
      lqipUrl: ''
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
     * 站点配置是否已经落定（成功落库或请求失败都会置位，见 App.vue / store）
     */
    siteInfoLoaded() {
      return !!this.$store.state.siteInfoLoaded
    },
    /**
     * 后台配置的封面（没配就是空串）
     */
    configuredCover() {
      const info = this.$store.state.webSiteInfo
      return (info && info.homeCover) || ''
    },
    /**
     * 封面图：配置过就用配置的，确实没配才回退内置图
     */
    heroCover() {
      return this.configuredCover || HERO_COVER
    },
    /**
     * 实际交给 <img> 的地址：
     *   配置已落定 → 用配置图（没配就是内置图）
     *   还没落定   → 用上次成功加载过的地址（浏览器缓存里通常已经有，秒出）
     * 配置落定前不碰内置图，避免「先下默认图再换掉」这次多余的请求
     */
    coverSrc() {
      return this.siteInfoLoaded ? this.heroCover : this.cachedCover
    },
    /**
     * 有地址且没加载失败才渲染 img
     */
    showCover() {
      return !!this.coverSrc && !this.coverFailed
    },
    /**
     * 低清铺底只在「大图还没画上去」且「没失败」时出现
     */
    showPlaceholder() {
      return !!this.lqip && !this.coverPainted && !this.coverFailed
    },
    /**
     * 首屏有没有任何画面（真图或缩略图）。都没有才退回纯墨底，
     * 此时遮罩也要撤掉 —— 它只为「不可预知的图」服务，平墨底上再叠一层只会把底压花
     */
    hasVisual() {
      return this.showCover || this.showPlaceholder
    }
  },
  watch: {
    /**
     * 换图时清掉上一次的失败/已画标记：coverFailed 一旦置 true 就再没有复位的机会，
     * 不清的话后台改好的新图会被 v-if 挡在门外，首屏永久停在墨底
     */
    coverSrc() {
      this.coverFailed = false
      this.coverPainted = false
    }
  },
  mounted() {
    this.setupScrollProgress()
  },
  beforeUnmount() {
    this.teardownScrollProgress()
  },
  methods: {
    /**
     * 把「首屏滚掉了多少」写进 --hero-p（0 = 顶部，1 = 滚过 hero 高度的 85%），
     * 样式块里所有退场效果都只消费这一个变量。
     *
     * 为什么不用 CSS 原生 animation-timeline: scroll()：Chrome/Edge 115+ 有，
     * Safari 至今没有。首屏是首页第一眼，不能「在 Safari 上什么都不动」，
     * 所以进度统一自己算，一条代码路径覆盖所有浏览器。
     * 成本可控：一个 passive 监听 + rAF 节流，每帧只写一个自定义属性，
     * 换算全在样式的 calc 里（transform / opacity / filter，都是合成层属性）。
     */
    setupScrollProgress() {
      // 系统开了「减少动态效果」就整段跳过，和样式块最后那段处理保持一致
      if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
      this.progressHandler = () => {
        // 一帧只写一次：滚动事件比帧还密集，不节流等于每帧算好几遍
        if (this.progressRaf) return
        this.progressRaf = window.requestAnimationFrame(() => {
          this.progressRaf = null
          const span = (this.$el.offsetHeight || 1) * PROGRESS_SPAN
          const progress = Math.min(1, Math.max(0, window.pageYOffset / span))
          this.$el.style.setProperty('--hero-p', progress.toFixed(4))
        })
      }
      window.addEventListener('scroll', this.progressHandler, { passive: true })
      // 视口一变，同样的 scrollY 对应的进度就变了（改窗口大小 / 手机转屏）
      window.addEventListener('resize', this.progressHandler)
      // 立刻跑一次：带着滚动位置刷新、或从 history 返回时不必等第一次滚动才对齐
      this.progressHandler()
    },
    teardownScrollProgress() {
      if (this.progressHandler) {
        window.removeEventListener('scroll', this.progressHandler)
        window.removeEventListener('resize', this.progressHandler)
        this.progressHandler = null
      }
      if (this.progressRaf) {
        window.cancelAnimationFrame(this.progressRaf)
        this.progressRaf = null
      }
    },
    /**
     * 图解码完成：淡入，并把地址和缩略图记下来给下次刷新用
     */
    onCoverLoad(event) {
      const img = event.target
      this.coverPainted = true
      // 只缓存后台配的那张：内置回退图是本站静态资源、本来就有 HTTP 缓存，
      // 且一旦把它记成「上次的封面」，下次进站就会先闪一下默认图 —— 正是要避免的
      if (this.coverSrc && this.coverSrc === this.configuredCover) {
        this.rememberCover(this.coverSrc, img)
      }
    },
    rememberCover(url, img) {
      if (!url) return
      if (url !== this.cachedCover) {
        this.cachedCover = url
        writeLocal(COVER_URL_KEY, url)
      }
      if (url === this.lqipUrl) return
      const dataUrl = this.makeLqip(img)
      if (!dataUrl) return
      this.lqipUrl = url
      this.lqip = dataUrl
      writeLocal(COVER_LQIP_KEY, dataUrl)
    },
    /**
     * 把封面压到 24px 宽再导成 base64，当低清铺底用。
     * 跨域图没放开 CORS 时 canvas 会被污染、toDataURL 抛错 —— 拿不到就算了，
     * 首屏退回墨底，主流程不受影响
     */
    makeLqip(img) {
      if (!img.naturalWidth || !img.naturalHeight) return ''
      try {
        const width = LQIP_WIDTH
        const height = Math.max(1, Math.round((width * img.naturalHeight) / img.naturalWidth))
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) return ''
        ctx.drawImage(img, 0, 0, width, height)
        return canvas.toDataURL('image/jpeg', LQIP_QUALITY)
      } catch (e) {
        return ''
      }
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  首屏 · 全屏封面
//
//  取向上放弃「左文右窗」的扉页排版，改成门户式的整屏图 + 居中站名：
//  一张站点固定封面（与具体文章无关）铺满视口，站名居中压在图上，
//  底部中央是「向下」提示。高度是一屏减掉底部那条「窗帘」边，文字垂直居中。
//
//  结构上大图是 sticky 钉住的（top: 0, z-index: 1），首页的文章面板从底部往上
//  盖在它上面（views/home/index.vue 的 .home-sheet，z-index: 2）—— 往下滚时
//  观感是「图没动、内容滑上来」，比图自己滚走更有纵深。
//
//  动效：三段文字依次浮起、「向下」上下浮动、封面极缓慢推近；都是纯装饰，
//  prefers-reduced-motion 下一起停掉。往下滚之后的退场（文字淡出上浮、封面
//  下沉压暗）是滚动绑定的，进度来自 hero.vue 的 setupScrollProgress，见文件末尾。
// ════════════════════════════════════════════════════════════════

$header-offset: 64px;

.hero {
  // 钉在视口顶上：往下滚时大图不跟着走，让下面的文章面板（.home-sheet）从底部
  // 盖上它 —— 这就是「窗帘」。sticky 的包含块是 .home-page，它整页都够高，
  // 所以大图会一直钉到页面底部（被面板盖着，看不见，也不占额外高度）。
  // 注意：这里绝不能加 transform（会变成包含块，把 sticky 和 -64px 负边距一起废掉）
  position: sticky;
  top: 0;
  // 面板是 z-index: 2（见 views/home/index.vue），大图退到 1 才会被盖住
  z-index: 1;
  width: 100%;
  // 本项目没有全局 box-sizing 重置（global.scss 里没有 *{box-sizing:border-box}），
  // 不写这行的话 min-height 量的是内容盒，下面上下各 64px 的内边距会加在
  // 一屏高度之外：整块比一屏高 128px，底部那条「窗帘」边会被顶到折线以下
  // 完全露不出来，顶栏 / 悬浮按钮的判据也会跟着错 128px
  box-sizing: border-box;
  // 顶栏是 fixed 的、#app 又留了 64px padding：负边距把图顶到视口最上沿，
  // 等量 padding-top 再把内容让回顶栏下面。两条成对出现，缺一个图就铺不满。
  margin-top: -$header-offset;
  padding-top: $header-offset;
  // padding-top 是给固定顶栏让位的，它会把内容盒的中心推到视口中心下方 32px；
  // 补一条等量的下内边距把这块偏移抵消回来，justify-content:center 才是真的居中
  // （.hero-scroll 是绝对定位，不吃内边距，位置不受这条影响）
  padding-bottom: $header-offset;
  // 高度 = 一屏 - 底部露出的那条「窗帘」边。svh 写在 vh 后面覆盖它 ——
  // 移动端地址栏收放时 svh 不跳。边长定义在 global.scss 的 --sheet-peek，
  // 顶栏和悬浮按钮的判据也读同一个值（utils/scroll.js 的 sheetPeek）。
  // 当前这个值是 0：进页面时大图正好占满一屏，面板完全在折线以下，
  // 想让它先露一条边就把它改大（这里和那两处判据会一起跟上）
  min-height: calc(100vh - var(--sheet-peek, 84px));
  min-height: calc(100svh - var(--sheet-peek, 84px));
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  // 常驻墨底。写死理由：它要保证「图没解码完」「图加载失败」「后台没配封面图」
  // 三种状态下白字都有底，而它不是主题色 —— 跟着 data-theme 翻转的话深色档下会
  // 变得更亮，白字反而消失，所以不去动 variables.scss。
  background: #12161c;
}

// 封面层：图铺满并裁切，自己不吃事件，文字层压在它上面
.hero-cover {
  position: absolute;
  inset: 0;
  // 模糊铺底放大后会溢出到视口外，裁掉省得撑出横向滚动条
  overflow: hidden;

  img {
    display: block;
    // 上下各多铺 80px 余量给滚动视差用：封面要往下沉最多 60px，余量必须大于这个
    // 位移，否则滚到后半程顶边会露出底下的墨底。左右各 6%，横向没有位移，
    // 只是给模糊底图的溢出留点裁切余量
    position: absolute;
    top: -80px;
    left: -6%;
    // 宽高必须显式写出来，不能交给「inset + width/height: auto」——
    // 绝对定位的替换元素在 width/height 是 auto 时，盒子按图片自身像素尺寸定
    // （CSS 2.1 §10.3.8），inset 只决定左上角落点：窗口一旦宽过图片原始宽度
    // （这张图 2160px），右边就露出 .hero 的墨底，窗口最大化时会看到一块空白。
    // 写成「containing block 的尺寸 + 那 80px / 6% 余量」，盒子就永远等于铺满区域
    width: calc(100% + 12%);
    height: calc(100% + 160px);
    // 图在盒子里等比缩放后裁切铺满（不会被拉变形）：这是「按比例拉伸」的正解，
    // 盒子多大都自适应，换任意尺寸的图、任意窗口比例都不会留白
    // object-position 略偏上：横图默认居中会把天空裁掉一半
    object-fit: cover;
    object-position: center 45%;
    // 入场时极缓慢地推近一点（20s 一次，不做循环）：首屏摆在那里也是「活」的，
    // 和天空层 160s 的漂移一个路子。起点放大 6%、终点正好 1 —— 只往里推，
    // 不会缩到 1 以下，配合上下 80px 的余量，边缘永远不露底
    animation: heroPush 20s cubic-bezier(0.16, 1, 0.3, 1) forwards;
    // 先透明，解码完成后（.is-painted）再淡入 —— 直接置顶会出现一下「突然变脸」
    opacity: 0;
    transition: opacity 0.45s ease;

    &.is-painted {
      opacity: 1;
    }
  }
}

@keyframes heroPush {
  from {
    transform: scale(1.06);
  }
  to {
    transform: scale(1);
  }
}

// 低清铺底：上一次封面的 24px 缩略图拉满 + 模糊，盖住图还没到的那段时间。
// 放大一点是为了吃掉 blur 在边缘溢出的透明，否则四周会有一圈暗边
.hero-cover-blur {
  position: absolute;
  inset: 0;
  background-position: center 45%;
  background-size: cover;
  filter: blur(22px);
  transform: scale(1.08);
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
  // 主题压暗层：大图是照片，换主题时它原本一动不动，看不出刚才那一下点生效了。
  // 深色档把 --hero-dim 抬到 22% 黑（见 styles/global.scss），整张图压暗一点；
  // 白天是 0 透明。写在 background 简写之后 —— 简写会把 background-color 重置掉。
  // 走背景色而不是 filter: brightness()：filter 会作用到整棵子树并且每帧重算，
  // 这条只在换主题时变一次，交给 transition 慢慢过渡就够
  background-color: var(--hero-dim, transparent);
  transition: background-color var(--theme-duration) var(--theme-ease);
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

// ════════════════════════════════════════════════════════════════
//  首屏退场：往下划时封面沉下去并压暗，文字先淡出缩小
//
//  进度只有一个来源：hero.vue 的 setupScrollProgress 用 rAF 把「滚过 hero
//  高度的 85%」写进 --hero-p（0 → 1），下面全部拿它算。
//  不用 CSS 原生 animation-timeline: scroll() —— Chrome/Edge 115+ 有，
//  Safari 至今没有。首屏是首页第一眼，不能「换个浏览器就完全不动」，
//  所以进度自己算，一条代码路径覆盖所有浏览器。
//
//  只动 transform / opacity，且只动 .hero-cover、.hero-cover img、
//  .hero-cover::after 和 .hero-inner —— 绝不能给 .hero 本身加 transform：
//  Header 的 is-over-hero 判据是「scrollY >= innerHeight - 64 - --sheet-peek」
//  （见 layout/Header/index.vue 的 updateOverHero），它假设大图钉住不动、只比
//  视口矮一条边；.hero 一旦成为 transform 包含块，sticky 定位和 -64px 负边距
//  都会跟着变，顶栏会在图上态 / 实心态之间错帧。
// ════════════════════════════════════════════════════════════════

// 文字层：淡出 + 上浮 + 轻微缩小。
//   1.5 倍速淡出：p≈0.67 就退干净，比位移更早收尾，避免「半透明站名压着
//   文章列表」；收 8% 的 scale 让文字像往远处走 —— 只做位移的话看起来
//   更像整块被推上去，少了纵深。
.hero-inner {
  opacity: max(0, calc(1 - var(--hero-p, 0) * 1.5));
  transform: translateY(calc(var(--hero-p, 0) * -70px)) scale(calc(1 - var(--hero-p, 0) * 0.08));
  will-change: transform, opacity;
}

// 封面：反向缓慢下沉 + 放大，做出「图比字沉、退得更慢」的视差。
//   · 下沉 60px、放大 12%：位移得和文字的上浮同一个量级才看得出层次。
//     之前只给 3% 位移 + 4% 放大，肉眼几乎看不出图在动。
//   · 变换写在 .hero-cover 这一层，img 那一层留给入场时的缓慢推近（heroPush）——
//     两层各跑各的 transform，不会互相覆盖，这正是 demo 里
//     .cover-parallax / .cover-zoom 的分工。
//   · img 的盒子上下各比 .hero 多出 80px（见 .hero-cover img 的 inset），
//     这个余量大于 60px 的下沉位移，再放大 12% 也不会在边缘露出墨底
.hero-cover {
  transform: translateY(calc(var(--hero-p, 0) * 60px)) scale(calc(1 + var(--hero-p, 0) * 0.12));
  will-change: transform;
}

// 压暗：叠一层纯黑、只算 opacity，而不是给 img 加 filter: brightness()。
// 视觉上等价，但 filter 会让整张全屏图每帧重新跑一遍滤镜，而纯色层的
// opacity 是最便宜的一条合成路径。
.hero-cover::after {
  content: '';
  position: absolute;
  inset: 0;
  background: #000;
  opacity: calc(var(--hero-p, 0) * 0.45);
  pointer-events: none;
}

// 「向下」提示：6 倍速先走（p≈0.17 就没了）—— 它贴在视口底部一路跟着滚，
// 是现在最显眼的瑕疵。hero-bob 是无限循环的时间轴动画、动的是 transform，
// 这里只接 opacity，两条互不干扰。
.hero-scroll {
  opacity: max(0, calc(1 - var(--hero-p, 0) * 6));
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
// 注意：这一块必须留在文件最后。滚动退场那几组权重和这里相同，靠「后出现」
// 才盖得住——挪到前面，上面那些 transform / opacity 会照样生效，
// 等于绕过了用户的无障碍设置。
// 两头都要管：JS 那边在 setupScrollProgress 里已经跳过监听，这里再把样式钉平，
// 是为了覆盖「页面开着时用户中途改了系统设置」那种情况。
@media (prefers-reduced-motion: reduce) {
  .reveal {
    animation: none !important;
    transition: none !important;
    opacity: 1;
    transform: none;
  }

  // 文字层随滚动淡出上浮：纯装饰，停掉后首屏就是静态的
  .hero-inner {
    opacity: 1;
    transform: none;
  }

  // 「向下」的往复浮动和滚动淡出一起停掉
  .hero-scroll {
    animation: none;
    transition: none;
    opacity: 1;
  }

  // 封面淡入同理：关掉过渡直接换图，不做视觉特效；入场推近和视差也停掉
  .hero-cover img {
    transition: none;
    animation: none;
    transform: none;
  }

  .hero-cover {
    transform: none;
  }

  // 滚动压暗是叠在图上的一层黑，必须显式撤掉，否则滚动还在把图压暗
  .hero-cover::after {
    display: none;
  }

  // 主题压暗层本身保留（它表达的是「现在在深色档」，不是动效），只把 560ms
  // 的渐变收掉 —— 换主题时直接切过去，不晃
  .hero-scrim {
    transition: none;
  }
}
</style>
