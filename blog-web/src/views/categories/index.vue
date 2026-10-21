<template>
  <div class="categories-page">
    <div class="content-layout">
      <main class="main-content">
        <header class="ledger-head">
          <h1>分类</h1>
          <span class="head-meta">{{ categories.length }} 个分类</span>
        </header>

        <!-- 分类索引：文字 + 当前项下划线，吸顶在顶栏下方 -->
        <nav class="categories-nav">
          <a
            v-for="category in categories"
            :key="category.name"
            href="javascript:void(0)"
            class="category-tab"
            :class="{ active: activeCategory === category.name }"
            @click="scrollToCategory(category.name)"
          >
            {{ category.name }}
            <span class="tab-count">{{ category.posts.length }}</span>
          </a>
        </nav>

        <div class="categories-list">
          <section
            v-for="category in categories"
            :key="category.name"
            class="category-group"
            v-animate-on-scroll
            :ref="category.name"
          >
            <div class="category-header">
              <h2 class="category-name">{{ category.name }}</h2>
              <span class="post-count">{{ category.posts.length }} 篇</span>
            </div>

            <div class="posts-list">
              <a
                v-for="post in category.posts"
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
          </section>
        </div>

        <div v-if="loading" class="loading-state">
          <i class="fas fa-spinner fa-spin"></i>
          加载中…
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
import { getCategoriesApi } from '@/api/article'

gsap.registerPlugin(ScrollTrigger)

export default {
  name: 'Categories',
  components: {
    Sidebar
  },

  data() {
    return {
      activeCategory: null,
      categories: [],
      loading: false
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
    scrollToCategory(categoryName) {
      this.activeCategory = categoryName
      // 分类数据先于 DOM 到达，ref 可能尚未挂载，直接解引用会抛错中断滚动
      const element = this.$refs[categoryName]?.[0]
      if (!element) return
      element.scrollIntoView({ behavior: 'smooth', block: 'start' })
    },
    handleScroll() {
      const categories = this.categories
      for (let i = categories.length - 1; i >= 0; i--) {
        // scroll 事件在挂载前后都会触发，缺守卫时一次解引用失败就会打断整个循环
        const element = this.$refs[categories[i].name]?.[0]
        if (!element) continue
        const rect = element.getBoundingClientRect()
        if (rect.top <= 100) {
          this.activeCategory = categories[i].name
          break
        }
      }
    }
  },
  async created() {
    try {
      const res = await getCategoriesApi()
      this.categories = res.data
    } catch (error) {
      // 未捕获的 reject 会让页面停在空列表且没有任何提示
      this.$message.error('获取分类列表失败')
    }
  },
  mounted() {
    // 监听滚动以更新激活的分类
    window.addEventListener('scroll', this.handleScroll)
  },
  beforeUnmount() {
    window.removeEventListener('scroll', this.handleScroll)
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  分类 · 技术台账
//
//  顶部一条可横向滚动的文字索引（当前项下划线），下面是各分类的
//  文章行：日期 | 标题。分组标题用等宽小字 + 一条实线，不用图标。
// ════════════════════════════════════════════════════════════════

.categories-page {
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

// ── 分类索引 ──
.categories-nav {
  position: sticky;
  top: 64px;
  z-index: 10;
  display: flex;
  gap: clamp(14px, 1.6vw, 24px);
  flex-wrap: wrap;
  padding: 10px 0 14px;
  margin-bottom: clamp(24px, 3vh, 36px);
  background: var(--background);
  border-bottom: 1px solid var(--rule-strong);

  .category-tab {
    display: inline-flex;
    align-items: baseline;
    gap: 6px;
    color: var(--text-secondary);
    text-decoration: none;
    font-size: 0.9rem;
    padding-bottom: 2px;
    border-bottom: 1px solid transparent;
    white-space: nowrap;
    transition: color 0.2s ease, border-color 0.2s ease;

    .tab-count {
      @include ledger-meta;
      font-size: 0.68rem;
    }

    &:hover {
      color: var(--text-primary);
    }

    &.active {
      color: var(--text-primary);
      border-bottom-color: var(--accent);

      .tab-count {
        color: var(--accent);
      }
    }
  }
}

// ── 分组 ──
.category-group {
  margin-bottom: clamp(30px, 4vh, 48px);
  scroll-margin-top: 120px;

  &:last-child {
    margin-bottom: 0;
  }
}

.category-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 2px;

  .category-name {
    font-family: $font-serif;
    font-size: 1.15rem;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
    flex: none;
  }

  // 分类名右侧拉一条实线到篇数，读作「分类 ——— 篇数」
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: var(--rule);
    transform: translateY(-3px);
  }

  .post-count {
    @include ledger-meta;
    flex: none;
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

.loading-state {
  padding: $spacing-xl;
  text-align: center;
  @include ledger-meta;

  i {
    margin-right: $spacing-sm;
  }
}

@include responsive(md) {
  .post-item {
    grid-template-columns: 64px minmax(0, 1fr);
    gap: 14px;
    padding: 12px 10px;
    margin: 0 -10px;
  }

  .categories-nav {
    flex-wrap: nowrap;
    overflow-x: auto;
    scrollbar-width: none;

    &::-webkit-scrollbar {
      display: none;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .category-tab,
  .post-item,
  .post-title {
    transition: none !important;
  }
}
</style>
