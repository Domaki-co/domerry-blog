<template>
  <div
    class="article-list-component"
    :style="containerStyle"
    ref="listContainer"
  >
    <!-- 1. 骨架屏：请求进入加载态时平滑淡入，结构精准对应台账式布局 -->
    <div
      v-if="showSkeleton"
      class="skeleton-ledger"
      aria-busy="true"
      aria-label="正在加载文章列表"
    >
      <div v-for="i in skeletonCount" :key="`skeleton-${i}`" class="skeleton-entry">
        <div class="skeleton-date">
          <div class="skeleton-box sk-y"></div>
          <div class="skeleton-box sk-md"></div>
        </div>

        <div class="skeleton-main">
          <div class="skeleton-box sk-title"></div>
          <div class="skeleton-box sk-sum-1"></div>
          <div class="skeleton-box sk-sum-2"></div>
          <div class="skeleton-meta">
            <div class="skeleton-box sk-tag"></div>
            <div class="skeleton-box sk-tag"></div>
            <div class="skeleton-box sk-tag"></div>
          </div>
        </div>

        <div class="skeleton-thumb"></div>
      </div>
    </div>

    <!-- 2. 台账式列表：日期 | 标题+摘要+元信息 | 缩略图 -->
    <ol
      v-show="!showSkeleton"
      class="ledger"
      :class="transitionClasses"
    >
      <li v-for="post in articles" :key="post.id" class="entry">
        <div class="entry-link">
          <div class="entry-date">
            <span class="y">{{ dateParts(post.createTime).year }}</span>
            <span class="md">{{ dateParts(post.createTime).md }}</span>
          </div>

          <div class="entry-main">
            <h3 class="entry-title">
              <span v-if="post.isStick" class="badge">置顶</span>
              <span v-if="post.status === 4" class="badge muted">私密</span>
              <router-link :to="`/post/${post.id}`">{{ post.title }}</router-link>
            </h3>

            <p v-if="post.summary" class="entry-sum">{{ post.summary }}</p>

            <div class="entry-meta">
              <span v-if="post.categoryName" class="cat">{{ post.categoryName }}</span>
              <span>{{ readMinutes(post) }} 分钟</span>
              <span>{{ post.quantity || 0 }} 次浏览</span>
              <span v-if="post.commentNum">{{ post.commentNum }} 条评论</span>

              <!-- 管理员专属的置顶开关 -->
              <el-tooltip v-if="isAdmin && canStick(post)"
                :content="post.isStick === 1 ? '取消置顶' : '设为置顶'"
                placement="top">
                <button type="button" class="stick-btn" :class="{ 'is-stick': post.isStick === 1 }"
                  :disabled="stickLoadingId === post.id" @click.stop="handleToggleStick(post)">
                  <i class="fas fa-thumbtack"></i>
                  {{ post.isStick === 1 ? '取消置顶' : '置顶' }}
                </button>
              </el-tooltip>
            </div>
          </div>

          <!-- 缩略图：加载中显示 lazy 占位图，无封面或加载失败回退到 book-thumb -->
          <router-link :to="`/post/${post.id}`" class="entry-thumb">
            <img
              v-lazy="{ src: post.cover || defaultCover, loading: lazyPlaceholder, error: defaultCover }"
              :key="post.cover || defaultCover"
              :alt="post.title"
              @error="handleImageError">
          </router-link>
        </div>
      </li>
    </ol>

    <!-- 3. 空状态提示 -->
    <el-empty
      v-if="!loading && transitionPhase === 'idle' && articles.length === 0"
      description="暂无文章"
    />

    <!-- 4. 分页控制栏 -->
    <div class="pagination-box" v-if="articles.length || total > 0">
      <el-pagination
        background
        @current-change="$emit('page-change', $event)"
        :current-page="params.pageNum"
        :page-size="params.pageSize"
        layout="prev, pager, next"
        :total="total">
      </el-pagination>
    </div>
  </div>
</template>

<script>
import { updateArticleStatusApi } from '@/api/article'

// 缩略图兜底用 360×236 的小图：首页 6 个 180×118 的位图位若都指向 960×960 的
// book.png，一张就要解码 3.5MB（大图留给 hero，见 home/components/hero.vue）
const DEFAULT_COVER = '/static/img/book-thumb.png'
const LAZY_PLACEHOLDER = '/static/img/lazy.gif'

export default {
  name: 'ArticleList',
  props: {
    articles: {
      type: Array,
      required: true
    },
    loading: {
      type: Boolean,
      default: false
    },
    total: {
      type: Number,
      default: 0
    },
    params: {
      type: Object,
      default: () => ({
        pageNum: 1,
        pageSize: 10
      })
    },
    // 过渡状态阶段：'idle' | 'leaving' | 'loading' | 'entering'
    transitionPhase: {
      type: String,
      default: 'idle'
    },
    // 锁定的容器最小高度，避免布局塌陷抖动
    minHeight: {
      type: String,
      default: ''
    }
  },
  computed: {
    /**
     * 是否管理员（只有管理员才允许调用后台置顶接口）
     */
    isAdmin() {
      return this.$store.getters.isAdmin
    },
    /**
     * 是否展示骨架屏：
     * 1. 当处于显式 loading 过渡阶段
     * 2. 或首次加载（loading 且 articles 为空）
     */
    showSkeleton() {
      return (
        this.transitionPhase === 'loading' ||
        (this.loading && this.transitionPhase === 'idle' && this.articles.length === 0)
      )
    },
    /**
     * 列表过渡类名绑定
     */
    transitionClasses() {
      return {
        'is-leaving': this.transitionPhase === 'leaving',
        'is-entering': this.transitionPhase === 'entering',
        'is-idle': this.transitionPhase === 'idle'
      }
    },
    /**
     * 容器样式（锁定最小高度）
     */
    containerStyle() {
      return this.minHeight ? { minHeight: this.minHeight } : {}
    },
    /**
     * 骨架屏卡片展示数量（默认匹配 pageSize，最高截断到 6 张避免视觉冗余）
     */
    skeletonCount() {
      return Math.min(this.params?.pageSize || 6, 6)
    }
  },
  data() {
    return {
      // 正在提交置顶请求的文章id，用于禁用按钮防止重复点击
      stickLoadingId: null,
      // 暴露给模板：占位图与回退图
      defaultCover: DEFAULT_COVER,
      lazyPlaceholder: LAZY_PLACEHOLDER
    }
  },
  methods: {
    /**
     * 把时间拆成「年」和「月-日」两行，让日期列对齐成一条竖线
     */
    dateParts(time) {
      const date = new Date(time)
      if (!time || Number.isNaN(date.getTime())) {
        return { year: '', md: '' }
      }
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return { year: String(date.getFullYear()), md: `${month}-${day}` }
    },
    /**
     * 估算阅读时长。contentMd 在列表接口里可能为空，这里兜底避免报错
     */
    readMinutes(post) {
      const words = post.contentMd ? post.contentMd.split(' ').length : 0
      return Math.max(1, Math.ceil(words / 300))
    },
    handleImageError(e) {
      e.target.src = DEFAULT_COVER
    },
    /**
     * 能否置顶：只有上架(1)/私密(4) 才会出现在门户置顶栏；
     * 已置顶的仍放行，避免文章被改成下架/草稿后无法取消置顶
     */
    canStick(post) {
      return post.isStick === 1 || [1, 4].includes(Number(post.status))
    },
    /**
     * 置顶 / 取消置顶（仅管理员可见）
     */
    handleToggleStick(post) {
      if (this.stickLoadingId) return
      const next = post.isStick === 1 ? 0 : 1
      this.stickLoadingId = post.id
      updateArticleStatusApi({ id: post.id, isStick: next })
        .then(() => {
          post.isStick = next
          this.$message.success(next === 1 ? '已置顶' : '已取消置顶')
          //通知父组件刷新置顶快捷栏
          this.$emit('stick-changed')
        })
        .catch(() => {})
        .finally(() => {
          this.stickLoadingId = null
        })
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  文章列表 · macOS 风格行卡片体系
//
//  三列：日期（年等宽/月日）| 标题+摘要+元信息胶囊 | 120px 视网膜缩略图。
//  交互用 Finder 的行选中语汇：悬浮浮出「温润冷灰 / 微透青灰」高光底、
//  整行右移 4px，缩略图在裁切框内缓慢放大。行与行之间平时靠 1px 底线分隔，
//  hover 时底线隐去、由高光底接管层次。
// ════════════════════════════════════════════════════════════════

.article-list-component {
  position: relative;
  transition: min-height 0.28s cubic-bezier(0.16, 1, 0.3, 1);
}

.ledger {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  will-change: opacity, transform;

  &.is-idle {
    opacity: 1;
    transform: translateY(0);
    pointer-events: auto;
  }

  // 1. 请求发起时：旧内容淡出 + 轻微下移 8px
  &.is-leaving {
    opacity: 0;
    transform: translateY(8px);
    transition: opacity 0.18s ease-out, transform 0.18s ease-out;
    pointer-events: none;
  }

  // 2. 新数据渲染时：淡入 + 轻微上移复位
  &.is-entering {
    animation: ledgerPageEnter 0.32s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
}

@keyframes ledgerPageEnter {
  0% {
    opacity: 0;
    transform: translateY(8px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.entry {
  position: relative;
  border-radius: 10px;
  border: 1px solid transparent;
  transition: background 0.28s cubic-bezier(0.16, 1, 0.3, 1),
    transform 0.28s cubic-bezier(0.16, 1, 0.3, 1),
    box-shadow 0.28s cubic-bezier(0.16, 1, 0.3, 1),
    border-color 0.28s cubic-bezier(0.16, 1, 0.3, 1);

  // 未 hover 时的轻柔底线，macOS 优雅分隔
  &::after {
    content: '';
    position: absolute;
    bottom: -3px;
    left: 18px;
    right: 18px;
    height: 1px;
    background: var(--rule);
    transition: opacity 0.24s ease;
  }

  &:hover {
    // 走 Finder 的行选中逻辑：靠「温润冷灰」高光底，而不是白卡 + 外投影
    background: var(--row-hover-strong);
    border-color: var(--rule);
    // 整行右移 4px 做反馈；右侧是容器 padding，顶不出横向滚动条
    transform: translateX(4px);

    &::after {
      opacity: 0;
    }

    .entry-thumb img {
      transform: scale(1.03);
    }

    .entry-title a {
      color: var(--accent);
    }
  }
}

// 遵循系统「减弱动态效果」：位移与缩放属于纯装饰，高光底反馈保留
@media (prefers-reduced-motion: reduce) {
  .entry:hover {
    transform: none;

    .entry-thumb img {
      transform: none;
    }
  }
}

// 深色模式下的行卡片悬浮：微透青灰高光 + 顶边内发光
:root[data-theme='dark'] .entry {
  &:hover {
    background: var(--row-hover-strong);
    border-color: rgba(255, 255, 255, 0.1);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);

    .entry-title a {
      color: var(--text-primary);
    }
  }
}

.entry-link {
  display: grid;
  // 第三列必须与 .entry-thumb 的 width 同步：网格轨道不会跟随元素宽度变化
  grid-template-columns: 74px minmax(0, 1fr) 180px;
  gap: clamp(16px, 2vw, 28px);
  align-items: start;
  padding: 22px 18px;
}

.entry-date {
  font-family: $font-mono;
  font-variant-numeric: tabular-nums;
  padding-top: 4px;

  .y {
    display: block;
    font-size: 0.68rem;
    // 与 .entry-meta 同一类元信息：tertiary 在纸白上仅 2.84:1，11px 小字不达 AA
    color: var(--text-secondary);
    letter-spacing: 0.05em;
  }

  .md {
    display: block;
    font-size: 0.95rem;
    font-weight: 500;
    color: var(--text-secondary);
    letter-spacing: -0.01em;
  }
}

.entry-main {
  min-width: 0;
}

.entry-title {
  font-family: $font-serif;
  font-size: 1.2rem;
  line-height: 1.45;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
  margin: 0;

  a {
    color: var(--text-primary);
    text-decoration: none;
    transition: color 0.22s ease;
  }
}

// 置顶 / 私密：macOS 状态胶囊
.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: $font-mono;
  font-size: 0.65rem;
  font-weight: 500;
  letter-spacing: 0.04em;
  line-height: 1;
  padding: 3px 8px;
  border-radius: 999px;
  border: 1px solid rgba(168, 106, 16, 0.3);
  background: rgba(168, 106, 16, 0.08);
  color: var(--accent);
  flex: none;
  transform: translateY(-1px);
  box-shadow: 0 1px 3px rgba(168, 106, 16, 0.08);

  &::before {
    content: '';
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: currentColor;
  }

  &.muted {
    border-color: var(--rule-strong);
    background: rgba(0, 0, 0, 0.04);
    color: var(--text-tertiary);
    box-shadow: none;

    &::before {
      display: none;
    }
  }
}

:root[data-theme='dark'] .badge {
  border-color: rgba(217, 154, 61, 0.4);
  background: rgba(217, 154, 61, 0.12);

  &.muted {
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(255, 255, 255, 0.12);
  }
}

.entry-sum {
  margin-top: 9px;
  color: var(--text-secondary);
  font-size: 0.92rem;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.entry-meta {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  font-family: $font-mono;
  font-size: 0.78rem;
  // tertiary(#8b949f 在纸白上仅 2.84:1) 提亮到 secondary，仍暗于 .entry-title 的 primary
  color: var(--text-secondary);
  letter-spacing: 0.02em;

  // macOS Finder 风格分类标签胶囊
  .cat {
    display: inline-flex;
    align-items: center;
    padding: 2px 8px;
    border-radius: 6px;
    background: rgba(0, 0, 0, 0.04);
    color: var(--text-secondary);
    font-size: 0.7rem;
    transition: background 0.2s ease, color 0.2s ease;
  }
}

:root[data-theme='dark'] .entry-meta .cat {
  background: rgba(255, 255, 255, 0.06);
}

.entry-thumb {
  width: 180px;
  aspect-ratio: 3 / 2;
  overflow: hidden;
  border-radius: $border-radius-sm;
  // 比 --rule 明显一档的实线边界，把缩略图从纸白里托出来；--rule-strong 两套主题各有取值，
  // 不再需要深色覆盖，也不写死颜色
  border: 1px solid var(--rule-strong);
  background: var(--row-hover);
  display: block;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.28s ease, border-color 0.28s ease;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  }
}

// 管理员置顶按钮：macOS 微胶囊按钮
.stick-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  font-family: $font-mono;
  font-size: 0.68rem;
  letter-spacing: 0.02em;
  border-radius: 999px;
  border: 1px solid var(--rule);
  background: transparent;
  // 这一行里最后一处 2.84:1 的文字；hover 的 accent 当态不变
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;

  i {
    font-size: 0.8em;
  }

  &:hover:not(:disabled) {
    color: var(--accent);
    border-color: var(--accent);
  }

  &.is-stick {
    color: var(--accent);
    border-color: var(--accent);
    background: var(--accent-soft);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.pagination-box {
  margin-top: $spacing-xl;
  display: flex;
  justify-content: center;
}

// ── 骨架屏卡片体系 (macOS 台账风格) ──
.skeleton-ledger {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  animation: skeletonFadeIn 0.2s ease-out;
}

@keyframes skeletonFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.skeleton-entry {
  display: grid;
  // 与 .entry-link 同列宽，否则加载完成的一瞬布局会跳一下
  grid-template-columns: 74px minmax(0, 1fr) 180px;
  gap: clamp(16px, 2vw, 28px);
  align-items: start;
  padding: 22px 18px;
  border-radius: 10px;
  border: 1px solid transparent;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    bottom: -3px;
    left: 18px;
    right: 18px;
    height: 1px;
    background: var(--rule);
  }
}

.skeleton-box {
  background: var(--rule);
  border-radius: 6px;
  position: relative;
  overflow: hidden;

  // 微光扫光动效 (Shimmer Wave)
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    transform: translateX(-100%);
    background: linear-gradient(
      90deg,
      transparent,
      rgba(255, 255, 255, 0.45),
      transparent
    );
    animation: skeletonShimmer 1.5s infinite;
  }
}

:root[data-theme='dark'] .skeleton-box::after {
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.08),
    transparent
  );
}

@keyframes skeletonShimmer {
  100% {
    transform: translateX(100%);
  }
}

.skeleton-date {
  padding-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  .sk-y {
    width: 36px;
    height: 12px;
  }
  .sk-md {
    width: 48px;
    height: 18px;
  }
}

.skeleton-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;

  .sk-title {
    width: 65%;
    height: 22px;
  }
  .sk-sum-1 {
    width: 90%;
    height: 14px;
  }
  .sk-sum-2 {
    width: 75%;
    height: 14px;
  }
  .skeleton-meta {
    display: flex;
    gap: 10px;
    margin-top: 4px;

    .sk-tag {
      width: 50px;
      height: 16px;
      border-radius: 999px;
    }
  }
}

.skeleton-thumb {
  width: 180px;
  aspect-ratio: 3 / 2;
  border-radius: $border-radius-sm;
  background: var(--row-hover);
  border: 1px solid var(--rule-strong);
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    transform: translateX(-100%);
    background: linear-gradient(
      90deg,
      transparent,
      rgba(255, 255, 255, 0.35),
      transparent
    );
    animation: skeletonShimmer 1.5s infinite;
  }
}

:root[data-theme='dark'] .skeleton-thumb::after {
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.06),
    transparent
  );
}

@include responsive(lg) {
  .entry-link,
  .skeleton-entry {
    grid-template-columns: 64px minmax(0, 1fr) 108px;
    gap: 16px;
    padding: 18px 14px;
  }

  .entry-thumb,
  .skeleton-thumb {
    width: 108px;
  }
}

@include responsive(md) {
  .entry-link,
  .skeleton-entry {
    grid-template-columns: 64px minmax(0, 1fr);
    gap: 14px;
    padding: 18px 12px;
  }

  .entry-thumb,
  .skeleton-thumb {
    display: none;
  }

  .entry-title {
    font-size: 1.08rem;
  }

  .entry-sum {
    font-size: 0.88rem;
    -webkit-line-clamp: 3;
  }
}

@media (prefers-reduced-motion: reduce) {
  .ledger.is-leaving {
    opacity: 0;
    transform: none;
    transition: opacity 0.1s ease;
  }
  .ledger.is-entering {
    animation: none;
    opacity: 1;
    transform: none;
  }
  .skeleton-box::after,
  .skeleton-thumb::after {
    animation: none;
  }
}
</style>
