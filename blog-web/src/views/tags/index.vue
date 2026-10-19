<template>
  <div class="tags-page">
    <div class="content-layout">
      <main class="main-content">
        <header class="ledger-head">
          <h1>标签</h1>
          <span class="head-meta">
            共 {{ tags.length }} 个<template v-if="tags.length > visibleTags.length"> · 展示 {{ visibleTags.length }} 个</template>
          </span>
        </header>

        <section class="cloud-block">
          <div class="cloud-head">
            <h2>标签云</h2>
            <span class="cloud-hint">底部色条长度表示文章数量</span>
          </div>

          <div class="tags-cloud" ref="tagsCloud">
            <a
              v-for="tag in tagCloud"
              :key="`tag-${tag.name}`"
              href="javascript:void(0)"
              class="tag-item"
              :class="{ active: activeTag === tag.name }"
              :style="{ '--tag-ratio': tag.ratio }"
              @click="selectTag(tag.name, tag.id)"
            >
              <span class="tag-name">{{ tag.name }}</span>
              <span class="tag-count">{{ tag.articleNum }}</span>
              <span class="tag-bar" aria-hidden="true"></span>
            </a>
          </div>
        </section>

        <section v-if="activeTag" class="tag-posts" ref="postsSection">
          <div class="tag-header">
            <h2>{{ activeTag }}</h2>
            <span class="post-count">
              <span class="count-number">{{ total }}</span>
              篇
            </span>
          </div>

          <div class="articles-wrapper" v-loading="loading">
            <ArticleList
              :articles="selectedTagPosts"
              :total="total"
              :params="params"
              @article-click="goToPost"
              @page-change="changePage"
            />
          </div>
        </section>
      </main>
      <Sidebar class="sidebar-container" />
    </div>
  </div>
</template>

<script>
import ArticleList from '@/components/ArticleList/index.vue'
import Sidebar from '@/components/Sidebar/index.vue'
import { getTagsApi } from '@/api/tags'
import { getArticlesApi } from '@/api/article'
import { pickDisplayTags } from '@/utils/tags'


export default {
  name: 'Tags',
  components: {
    ArticleList,
    Sidebar
  },
  data() {
    return {
      activeTag: null,
      tags:[],
      selectedTagPosts:[],
      total:0,
      totatlCount:0,
      loading:false,
      params:{
        pageNum:1,
        pageSize:10,
        tagId:null
      }
    }
  },
  computed: {
    /**
     * 标签云展示的标签：先丢掉没有文章的，再按文章数倒序，最后按数量上限截断。
     * 规则和数量都在 src/utils/tags.js + src/config/index.js 里统一调整。
     */
    visibleTags() {
      return pickDisplayTags(this.tags)
    },
    /**
     * 标签云条目：字号统一，只用底部长条表示文章数占最大值的比例。
     * 单一强调色，不再按序号循环 6 个色相。
     */
    tagCloud() {
      const tags = this.visibleTags
      if (!tags.length) return []

      const max = Math.max(...tags.map(tag => Number(tag.articleNum) || 0))

      return tags.map(tag => {
        const count = Number(tag.articleNum) || 0
        return {
          ...tag,
          ratio: max > 0 ? Number((count / max).toFixed(3)) : 0
        }
      })
    }
  },
  methods: {
    /**
     * 跳转文章
     */
    goToPost(id) {
      this.$router.push(`/post/${id}`)
    },
    /**
     * 选择标签
     */
    async selectTag(tagName,tagId) {
      if(this.activeTag === tagName){
        return
      }
      this.activeTag = this.activeTag === tagName ? null : tagName
      this.params.tagId = tagId
      this.params.pageNum = 1
      this.getArticleList()
    },
    /**
     * 切换页码
     */
    changePage(pageNum) {
      this.params.pageNum = pageNum
      this.getArticleList()
      window.scrollTo({
        top: this.$refs.postsSection?.offsetTop - 80,
        behavior: 'smooth'
      })
    },
    /**
     * 获取文章列表
     */
    async getArticleList() {
      this.loading = true
      try {
        const res = await getArticlesApi(this.params)
        this.selectedTagPosts = res.data?.records || []
        this.total = res.data?.total || 0
      } catch (error) {
        this.$message.error(error.message)
      } finally {
        // 请求失败也必须复位，否则 v-loading 会一直转
        this.loading = false
      }
    },
  
  },
  async created() {
    try {
      const res = await getTagsApi()
      this.tags = res.data || []
    } catch (error) {
      this.$message.error(error.message)
      return
    }
    if(this.$route.query.tagId){
      this.selectTag(this.$route.query.tagName,this.$route.query.tagId)
    }else{
      // 默认选中排在最前（文章最多）的那个标签
      const defaultTag = this.visibleTags[0]
      if (defaultTag) {
        this.selectTag(defaultTag.name, defaultTag.id)
      }
    }
  },
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  标签 · 技术台账
//
//  标签云不是彩色胶囊墙：统一字号，只有底部长条（长度 = 文章数占比）
//  和等宽数字是变量。选中态用强调色描边 + 实底，其余保持墨色。
// ════════════════════════════════════════════════════════════════

.tags-page {
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

// ── 标签云 ──
.cloud-block {
  margin-bottom: clamp(36px, 5vh, 56px);
}

.cloud-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 18px;

  h2 {
    font-family: $font-serif;
    font-size: 1.15rem;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
    flex: none;
  }

  // 标题右侧拉一条实线到提示文字
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: var(--rule);
    transform: translateY(-3px);
  }

  .cloud-hint {
    @include ledger-meta;
    flex: none;
  }
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
}

.tag-item {
  position: relative;
  display: inline-flex;
  align-items: baseline;
  gap: 7px;
  padding: 7px 11px 9px;
  border: 1px solid var(--rule);
  border-radius: $border-radius-sm;
  text-decoration: none;
  color: var(--text-secondary);
  overflow: hidden;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;

  .tag-name {
    position: relative;
    z-index: 1;
    font-size: 0.88rem;
  }

  .tag-count {
    position: relative;
    z-index: 1;
    @include ledger-meta;
    font-size: 0.68rem;
  }

  // 底部长条：长度 = 该标签文章数占最大值的比例。是数据，不是装饰。
  .tag-bar {
    position: absolute;
    left: 0;
    bottom: 0;
    height: 2px;
    width: calc(var(--tag-ratio, 0) * 100%);
    background: var(--accent);
    opacity: 0.35;
    transition: opacity 0.2s ease;
    pointer-events: none;
  }

  &:hover {
    border-color: var(--accent);
    color: var(--accent);

    .tag-bar {
      opacity: 0.8;
    }
  }

  &.active {
    background: var(--accent);
    border-color: var(--accent);
    color: #fff;

    .tag-count {
      color: rgba(255, 255, 255, 0.85);
    }

    .tag-bar {
      opacity: 0;
    }
  }
}

// ── 选中标签下的文章 ──
.tag-posts {
  animation: ledgerIn 0.35s ease-out;
}

.tag-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--rule-strong);
  margin-bottom: 6px;

  h2 {
    font-family: $font-serif;
    font-size: 1.25rem;
    font-weight: 600;
    color: var(--text-primary);
    margin: 0;
    flex: none;
  }

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
    display: inline-flex;
    align-items: baseline;
    gap: 4px;

    .count-number {
      color: var(--accent);
      font-size: 0.95rem;
    }
  }
}

.sidebar-container {
  position: sticky;
  top: 80px;

  @include responsive(lg) {
    position: relative;
    top: 0;
    height: auto;
    overflow-y: visible;
  }
}

@keyframes ledgerIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@include responsive(sm) {
  .tags-cloud {
    gap: 7px 8px;
  }

  .tag-item {
    padding: 6px 9px 8px;

    .tag-name {
      font-size: 0.84rem;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .tag-item,
  .tag-bar,
  .tag-posts {
    animation: none !important;
    transition: none !important;
  }
}
</style>
