<template>
  <div class="archives-page">
    <div class="content-layout">
      <main class="main-content">
        <header class="ledger-head">
          <h1>归档</h1>
          <span class="head-meta">{{ totalPosts }} 篇 · {{ archives.length }} 个年份</span>
        </header>

        <div class="timeline">
          <section
            v-for="(item, index) in archives"
            :key="index"
            class="year-group"
            v-animate-on-scroll
          >
            <button
              type="button"
              class="year-header"
              :aria-expanded="!collapsedYears[item.year]"
              @click="toggleYear(item.year)"
            >
              <span class="year">{{ item.year }}</span>
              <span class="year-rule"></span>
              <span class="year-count">{{ item.posts?.length || 0 }}</span>
              <i class="fas fa-chevron-down toggle-icon" :class="{ 'is-open': !collapsedYears[item.year] }"></i>
            </button>

            <transition
              name="expand"
              @enter="startTransition"
              @leave="endTransition"
            >
              <div class="posts-list" v-show="!collapsedYears[item.year]">
                <a
                  v-for="post in item.posts"
                  :key="post.id"
                  href="javascript:void(0)"
                  class="post-item"
                  @click="goToPost(post.id)"
                >
                  <span class="post-date">
                    <span class="month">{{ formatMonth(post.createTime) }}</span>
                    <span class="day">{{ formatDay(post.createTime) }}</span>
                  </span>
                  <span class="post-title">{{ post.title }}</span>
                </a>
              </div>
            </transition>
          </section>
        </div>
      </main>
      <Sidebar />
    </div>
  </div>
</template>

<script>
import gsap from 'gsap'
import ScrollTrigger from 'gsap/ScrollTrigger'
import Sidebar from '@/components/Sidebar/index.vue'
import { getArchivesApi } from '@/api/article'
gsap.registerPlugin(ScrollTrigger)

export default {
  name: 'Archives',
  components: {
    Sidebar
  },
  data() {
    return {
      archives: [],
      collapsedYears: {}
    }
  },
  computed: {
    totalPosts() {
      return this.archives.reduce((sum, item) => sum + (item.posts?.length || 0), 0)
    }
  },
  methods: {
    formatMonth(date) {
      return new Date(date).toLocaleString('zh-CN', { month: 'short' })
    },
    formatDay(date) {
      return new Date(date).getDate().toString().padStart(2, '0')
    },
    goToPost(id) {
      this.$router.push(`/post/${id}`)
    },
    toggleYear(year) {
      // Vue 3 的 Proxy 能追踪新增/修改的属性，不再需要 Vue 2 的 set API
      this.collapsedYears[year] = !this.collapsedYears[year]
    },
    startTransition(element) {
      element.style.height = 'auto'
      const height = element.scrollHeight
      element.style.height = '0px'
      // 触发回流
      element.offsetHeight
      element.style.height = height + 'px'
    },
    endTransition(element) {
      element.style.height = element.scrollHeight + 'px'
      // 触发回流
      element.offsetHeight
      element.style.height = '0px'
    }
  },
  async created() {
    try {
      const res = await getArchivesApi()
      // data 可能为 null，模板和 computed 都按数组处理
      this.archives = res.data || []
      this.archives.forEach(item => {
        this.collapsedYears[item.year] = false
      })
    } catch (error) {
      this.$message.error(error.message)
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  归档 · 技术台账
//
//  年份是一行等宽的「年份 ——— 篇数」，下面挂该年的文章行。
//  日期列等宽右对齐，标题左对齐，行与行之间一条 1px 线。
//  没有卡片、没有阴影、hover 不位移。
// ════════════════════════════════════════════════════════════════

.archives-page {
  max-width: 1400px;
  margin: 0 auto;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: clamp(36px, 4vw, 64px);
  padding: $spacing-lg clamp(20px, 4vw, 48px) $spacing-xl;
  min-height: calc(100vh - 80px);
  align-items: start;

  @include responsive(lg) {
    grid-template-columns: 1fr;
    padding: $spacing-lg $spacing-md;
  }

  @include responsive(md) {
    padding: $spacing-md;
  }
}

.main-content {
  min-width: 0;
}

// ── 页头 ──
.ledger-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: $spacing-md;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--rule-strong);
  margin-bottom: clamp(28px, 4vh, 44px);

  h1 {
    font-family: $font-serif;
    font-size: clamp(1.6rem, 3vw, 2.1rem);
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
  }

  .head-meta {
    @include ledger-meta;
  }
}

// ── 年份分组 ──
.year-group {
  margin-bottom: clamp(34px, 5vh, 56px);

  &:last-child {
    margin-bottom: 0;
  }
}

.year-header {
  width: 100%;
  display: flex;
  align-items: baseline;
  gap: 14px;
  padding: 0 0 12px;
  margin-bottom: 4px;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;

  .year {
    font-family: $font-mono;
    font-size: 1.05rem;
    font-variant-numeric: tabular-nums;
    color: var(--text-primary);
    letter-spacing: 0.02em;
    flex: none;
  }

  // 年份和篇数之间拉一条实线，把「年份 ——— 篇数」读成一行台账
  .year-rule {
    flex: 1;
    height: 1px;
    background: var(--rule);
    transform: translateY(-3px);
  }

  .year-count {
    @include ledger-meta;
    flex: none;
  }

  .toggle-icon {
    flex: none;
    font-size: 0.72rem;
    color: var(--text-tertiary);
    transition: transform 0.25s ease;
    transform: translateY(-1px);

    &.is-open {
      transform: translateY(-1px) rotate(-180deg);
    }
  }

  &:hover .year {
    color: var(--accent);
  }
}

// ── 文章行 ──
.post-item {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  gap: clamp(16px, 2vw, 28px);
  align-items: baseline;
  padding: 12px 14px;
  margin: 0 -14px;
  border-bottom: 1px solid var(--rule);
  text-decoration: none;
  transition: background 0.2s ease;

  &:hover {
    background: var(--row-hover);

    .post-title {
      color: var(--accent);
    }
  }

  .post-date {
    font-family: $font-mono;
    font-variant-numeric: tabular-nums;
    font-size: 0.76rem;
    color: var(--text-tertiary);
    display: flex;
    gap: 6px;
    white-space: nowrap;

    .month {
      color: var(--text-tertiary);
    }
  }

  .post-title {
    color: var(--text-primary);
    font-size: 0.98rem;
    line-height: 1.5;
    transition: color 0.2s ease;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.posts-list {
  overflow: hidden;
  transition: height 0.3s ease-in-out;
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s ease-in-out;
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
}

@include responsive(md) {
  .post-item {
    grid-template-columns: 64px minmax(0, 1fr);
    gap: 14px;
    padding: 12px 10px;
    margin: 0 -10px;
  }
}

// 尊重系统的「减少动态效果」设置
@media (prefers-reduced-motion: reduce) {
  .toggle-icon,
  .posts-list,
  .expand-enter-active,
  .expand-leave-active {
    transition: none !important;
  }
}
</style>
