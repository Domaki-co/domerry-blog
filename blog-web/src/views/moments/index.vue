<template>
  <div class="moments-container">
    <!-- 顶部台账题头 -->
    <header class="moments-header">
      <div class="header-badge">MOMENTS · 手记台账</div>
      <h1 class="header-title">碎语与手记</h1>
      <p class="header-desc">记录生活中的断片、顿悟与代码之外的呼吸</p>
    </header>

    <!-- 首次加载骨架屏 -->
    <div v-if="loading && moments.length === 0" class="moments-skeleton">
      <div v-for="i in 3" :key="i" class="skeleton-item">
        <div class="skeleton-avatar"></div>
        <div class="skeleton-body">
          <div class="skeleton-line short"></div>
          <div class="skeleton-line"></div>
          <div class="skeleton-line medium"></div>
        </div>
      </div>
    </div>

    <!-- 说说列表（时间轴流） -->
    <div v-else class="moments-timeline">
      <!-- 贯穿时间轴主线 -->
      <div class="timeline-spine"></div>

      <article
        v-for="(moment, index) in moments"
        :key="moment.id"
        class="moment-item"
        :style="{ animationDelay: `${Math.min(index * 40, 400)}ms` }"
      >
        <!-- 时间轴节点 -->
        <div class="timeline-node" :class="{ 'is-latest': index === 0 }">
          <span class="node-dot"></span>
        </div>

        <div class="user-avatar">
          <img v-lazy="moment.avatar" :key="moment.avatar" class="avatar" alt="avatar" />
          <!-- 移动端用户信息 -->
          <div class="mobile-user-info">
            <span class="name">{{ moment.nickname }}</span>
            <time class="time" :datetime="moment.createTime">{{ formatTime(moment.createTime) }}</time>
          </div>
        </div>

        <div class="moment-main">
          <!-- PC 端用户信息与工具栏 -->
          <div class="moment-header">
            <div class="user-meta">
              <span class="name">{{ moment.nickname }}</span>
              <time class="time" :datetime="moment.createTime" :title="moment.createTime">
                {{ formatTime(moment.createTime) }}
              </time>
            </div>
            <button
              class="share-btn"
              type="button"
              title="复制本条说说链接"
              @click="copyMomentLink(moment)"
            >
              <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
              </svg>
            </button>
          </div>

          <!-- 说说正文与多媒体 -->
          <div class="moment-content-wrapper">
            <div class="moment-content" v-html="$sanitize(moment.content)"></div>

            <div
              v-if="moment.images?.length"
              class="moment-images"
              :class="{
                'single-image': moment.images.length === 1,
                'four-images': moment.images.length === 4
              }"
            >
              <div
                v-for="(img, imgIdx) in moment.images"
                :key="img"
                class="image-box"
                @click="previewImage(moment.images, imgIdx)"
              >
                <img v-lazy="img" :key="img" alt="moment image" />
              </div>
            </div>
          </div>
        </div>
      </article>

      <!-- 滚动触底哨兵节点 -->
      <div ref="sentinel" class="scroll-sentinel"></div>

      <!-- 底部加载更多状态 -->
      <div v-if="loadingMore" class="loading-more-bar">
        <span class="loading-spinner"></span>
        <span class="loading-text">翻阅历史手记中...</span>
      </div>

      <!-- 已经到底状态 -->
      <div v-else-if="!hasMore && moments.length > 0" class="moments-end">
        <span class="end-line"></span>
        <span class="end-text">RECORDED IN TIME · 共 {{ total }} 条手记</span>
        <span class="end-line"></span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading && moments.length === 0" class="moments-empty">
        <p class="empty-title">暂无手记</p>
        <p class="empty-desc">墨香待染，静候第一笔记录</p>
      </div>
    </div>

    <!-- 图片预览组件 -->
    <mj-image-preview ref="imagePreview" />
  </div>
</template>

<script>
import { formatTime } from '@/utils/time'
import { getMoments } from '@/api/moments'

export default {
  name: 'Moments',

  data() {
    return {
      moments: [],
      loading: false,
      loadingMore: false,
      total: 0,
      hasMore: true,
      params: {
        pageNum: 1,
        pageSize: 10,
      },
      observer: null,
    }
  },

  mounted() {
    this.fetchMoments()
  },

  beforeUnmount() {
    if (this.observer) {
      this.observer.disconnect()
      this.observer = null
    }
  },

  methods: {
    parseImages(images) {
      if (!images) return []
      return images.split(',').filter(img => img.trim())
    },

    async fetchMoments(isLoadMore = false) {
      if (isLoadMore) {
        if (this.loadingMore || !this.hasMore) return
        this.loadingMore = true
      } else {
        this.loading = true
        this.params.pageNum = 1
      }

      try {
        const res = await getMoments(this.params)
        const records = res.data?.records || []
        records.forEach(moment => {
          moment.images = this.parseImages(moment.images)
        })
        this.total = res.data?.total || 0

        if (isLoadMore) {
          this.moments.push(...records)
        } else {
          this.moments = records
        }

        this.hasMore = this.moments.length < this.total

        this.$nextTick(() => {
          this.initObserver()
        })
      } catch (error) {
        console.error('获取说说列表失败:', error)
      } finally {
        this.loading = false
        this.loadingMore = false
      }
    },

    loadMore() {
      if (!this.hasMore || this.loadingMore || this.loading) return
      this.params.pageNum += 1
      this.fetchMoments(true)
    },

    initObserver() {
      if (this.observer) {
        this.observer.disconnect()
      }
      if (!this.$refs.sentinel) return

      this.observer = new IntersectionObserver(
        (entries) => {
          if (entries[0].isIntersecting && this.hasMore && !this.loading && !this.loadingMore) {
            this.loadMore()
          }
        },
        {
          root: null,
          rootMargin: '240px',
          threshold: 0.05,
        }
      )

      this.observer.observe(this.$refs.sentinel)
    },

    previewImage(images, index) {
      this.$refs.imagePreview?.show(images, index)
    },

    copyMomentLink(moment) {
      const url = `${window.location.origin}${window.location.pathname}#moment-${moment.id}`
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(url).then(() => {
          this.$message?.success('已复制说说链接')
        }).catch(() => {
          this.fallbackCopy(url)
        })
      } else {
        this.fallbackCopy(url)
      }
    },

    fallbackCopy(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$message?.success('已复制说说链接')
      } catch (err) {
        this.$message?.info('请手动复制链接: ' + text)
      }
      document.body.removeChild(textarea)
    },

    formatTime(time) {
      return formatTime(time)
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  说说 · 技术台账与流式时间轴
//
//  冷灰纸白基底，无分页无限流加载。左侧 1px 贯穿轴线，带阶梯淡入微动效。
// ════════════════════════════════════════════════════════════════

.moments-container {
  max-width: 760px;
  margin: 0 auto;
  padding: clamp(28px, 5vh, 52px) clamp(20px, 4vw, 36px);
  min-height: calc(100vh - 70px);
}

// ── 顶部台账题头 ──
.moments-header {
  margin-bottom: clamp(32px, 6vh, 48px);
  padding-bottom: 20px;
  border-bottom: 1px solid var(--rule);

  .header-badge {
    @include ledger-heading;
    display: inline-block;
    margin-bottom: 8px;
    color: var(--accent);
  }

  .header-title {
    font-family: $font-serif;
    font-size: clamp(1.6rem, 3.2vw, 2.2rem);
    font-weight: 600;
    color: var(--text-primary);
    margin: 0 0 10px;
    letter-spacing: -0.01em;
  }

  .header-desc {
    color: var(--text-secondary);
    font-size: 0.92rem;
    line-height: 1.6;
    margin: 0;
  }
}

// ── 首次加载骨架屏 ──
.moments-skeleton {
  display: flex;
  flex-direction: column;
  gap: 28px;

  .skeleton-item {
    display: flex;
    gap: 18px;

    .skeleton-avatar {
      flex: none;
      width: 42px;
      height: 42px;
      border-radius: $border-radius-sm;
      background: var(--hover-bg);
      animation: skeletonPulse 1.4s ease-in-out infinite;
    }

    .skeleton-body {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 10px;

      .skeleton-line {
        height: 14px;
        background: var(--hover-bg);
        border-radius: 2px;
        animation: skeletonPulse 1.4s ease-in-out infinite;

        &.short { width: 35%; }
        &.medium { width: 70%; }
      }
    }
  }
}

@keyframes skeletonPulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 0.9; }
}

// ── 时间轴主体 ──
.moments-timeline {
  position: relative;
  display: flex;
  flex-direction: column;
}

// 贯穿中轴线（PC 端）
.timeline-spine {
  position: absolute;
  top: 14px;
  bottom: 40px;
  left: 20px;
  width: 1px;
  background: var(--rule);
  pointer-events: none;

  @media screen and (max-width: 768px) {
    display: none;
  }
}

// ── 说说条目卡片 ──
.moment-item {
  position: relative;
  display: flex;
  gap: 20px;
  padding: 24px 0 28px;
  border-bottom: 1px solid var(--rule);
  opacity: 0;
  animation: momentSlideIn 0.38s cubic-bezier(0.16, 1, 0.3, 1) forwards;

  &:first-of-type {
    padding-top: 4px;
  }

  @media screen and (max-width: 768px) {
    flex-direction: column;
    gap: 12px;
    padding: 20px 0;
  }
}

@keyframes momentSlideIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 轴线上的立体节点
.timeline-node {
  position: absolute;
  left: 17px;
  top: 36px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--surface);
  border: 1.5px solid var(--text-tertiary);
  z-index: 2;
  box-sizing: border-box;

  &.is-latest {
    border-color: var(--accent);
    background: var(--accent);
    box-shadow: 0 0 0 4px rgba(168, 106, 16, 0.16);

    &::after {
      content: '';
      position: absolute;
      inset: -3px;
      border-radius: 50%;
      border: 1px solid var(--accent);
      animation: nodePulse 2.4s cubic-bezier(0.24, 0, 0.38, 1) infinite;
    }
  }

  @media screen and (max-width: 768px) {
    display: none;
  }
}

@keyframes nodePulse {
  0% {
    transform: scale(0.9);
    opacity: 0.9;
  }
  70% {
    transform: scale(2.2);
    opacity: 0;
  }
  100% {
    transform: scale(2.2);
    opacity: 0;
  }
}

// ── 用户头像与移动端栏 ──
.user-avatar {
  position: relative;
  flex-shrink: 0;
  width: 42px;
  margin-left: 36px; // 避开时间轴线与节点

  .avatar {
    width: 42px;
    height: 42px;
    border-radius: $border-radius-sm;
    border: 1px solid var(--rule);
    object-fit: cover;
    background: var(--surface);
    display: block;
    transition: transform 0.2s ease;

    &:hover {
      transform: scale(1.05);
    }
  }

  @media screen and (max-width: 768px) {
    margin-left: 0;
    width: auto;
    display: flex;
    align-items: center;
    gap: 12px;

    .avatar {
      width: 38px;
      height: 38px;
    }
  }
}

.mobile-user-info {
  display: none;
  flex-direction: column;

  @media screen and (max-width: 768px) {
    display: flex;
  }

  .name {
    font-size: 0.95rem;
    font-weight: 500;
    color: var(--text-primary);
  }

  .time {
    @include ledger-meta;
    margin-top: 2px;
  }
}

// ── 主体区域 ──
.moment-main {
  flex: 1;
  min-width: 0;

  @media screen and (max-width: 768px) {
    .moment-header {
      display: none;
    }
  }
}

.moment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;

  .user-meta {
    display: flex;
    align-items: baseline;
    gap: 10px;

    .name {
      font-size: 0.95rem;
      font-weight: 500;
      color: var(--text-primary);
    }

    .time {
      @include ledger-meta;
    }
  }

  .share-btn {
    border: none;
    background: transparent;
    color: var(--text-tertiary);
    cursor: pointer;
    padding: 4px;
    border-radius: $border-radius-sm;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    opacity: 0.6;
    transition: opacity 0.2s ease, color 0.2s ease, background 0.2s ease;

    &:hover {
      opacity: 1;
      color: var(--accent);
      background: var(--hover-bg);
    }
  }
}

// ── 正文与图片展示 ──
.moment-content-wrapper {
  .moment-content {
    color: var(--text-primary);
    line-height: 1.8;
    font-size: 0.95rem;
    white-space: pre-wrap;
    word-break: break-word;

    :deep(p) {
      margin: 0 0 8px;
      &:last-child { margin-bottom: 0; }
    }

    :deep(li) {
      margin-left: 24px;
    }

    :deep(a) {
      color: var(--accent);
      text-decoration: underline;
      text-underline-offset: 3px;
    }
  }

  .moment-images {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 8px;
    margin-top: 14px;

    .image-box {
      overflow: hidden;
      border: 1px solid var(--rule);
      border-radius: $border-radius-sm;
      background: var(--surface);
      cursor: zoom-in;

      img {
        width: 100%;
        height: 100%;
        aspect-ratio: 4 / 3;
        object-fit: cover;
        display: block;
        transition: transform 0.25s cubic-bezier(0.2, 0, 0.25, 1);
      }

      &:hover img {
        transform: scale(1.04);
      }
    }

    // 单图大画幅
    &.single-image {
      grid-template-columns: minmax(0, 380px);

      .image-box img {
        aspect-ratio: 16 / 10;
        max-height: 280px;
      }
    }

    // 四图 2x2 网格
    &.four-images {
      grid-template-columns: repeat(2, minmax(0, 190px));
    }
  }
}

// ── 触底哨兵与加载/结束状态 ──
.scroll-sentinel {
  height: 10px;
  width: 100%;
  pointer-events: none;
}

.loading-more-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 32px 0 16px;

  .loading-spinner {
    width: 16px;
    height: 16px;
    border: 2px solid var(--rule);
    border-top-color: var(--accent);
    border-radius: 50%;
    animation: spinnerSpin 0.7s linear infinite;
  }

  .loading-text {
    @include ledger-meta;
    font-size: 0.8rem;
  }
}

@keyframes spinnerSpin {
  to { transform: rotate(360deg); }
}

.moments-end {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 40px 0 20px;

  .end-line {
    flex: 1;
    max-width: 80px;
    height: 1px;
    background: var(--rule);
  }

  .end-text {
    @include ledger-meta;
    font-size: 0.72rem;
    color: var(--text-tertiary);
    letter-spacing: 0.08em;
  }
}

.moments-empty {
  text-align: center;
  padding: 80px 0;

  .empty-title {
    font-family: $font-serif;
    font-size: 1.2rem;
    color: var(--text-secondary);
    margin: 0 0 6px;
  }

  .empty-desc {
    @include ledger-meta;
    color: var(--text-tertiary);
  }
}

@media (prefers-reduced-motion: reduce) {
  .moment-item,
  .timeline-node.is-latest::after,
  .image-box img {
    animation: none !important;
    transition: none !important;
  }
}
</style>
