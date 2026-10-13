<template>
  <div class="tags-page">
    <div class="content-layout">
      <main class="main-content">
        <el-card class="content-card">
          <div class="page-header">
            <h3>标签云</h3>
            <p class="subtitle">
              共 {{ tags.length }} 个标签<template v-if="tags.length > visibleTags.length">，展示 {{ visibleTags.length }} 个</template>
            </p>
            <p class="cloud-hint">底部色条表示文章数量</p>
          </div>

          <div class="tags-cloud" ref="tagsCloud">
            <div
              v-for="(tag, index) in tagCloud"
              :key="`tag-${tag.name}`"
              class="tag-item"
              :class="[{ active: activeTag === tag.name }, `hue-${index % 6}`]"
              :style="{
                '--tag-ratio': tag.ratio,
                '--animation-delay': `${Math.min(index * 0.03, 0.6)}s`
              }"
              @click="selectTag(tag.name, tag.id)"
            >
              <i class="fas fa-tag tag-icon"></i>
              <span class="tag-name">{{ tag.name }}</span>
              <span class="tag-count">{{ tag.articleNum }}</span>
              <span class="tag-bar" aria-hidden="true"></span>
            </div>
          </div>

      
        </el-card>
        <div v-if="activeTag" class="tag-posts" ref="postsSection">
            <div class="tag-header">
              <div class="header-line"></div>
              <h2>
                <i class="fas fa-bookmark"></i>
                {{ activeTag }}
                <span class="post-count">
                  <span class="count-number">{{ total }}</span>
                  篇文章
                </span>
              </h2>
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
          </div>
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
     * 颜色交给模板按序号循环取 6 个色相，避免 46 个标签变成 46 种随机颜色。
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
      const res = await getArticlesApi(this.params)
      this.selectedTagPosts = res.data.records
      this.total = res.data.total
      this.loading = false
    },
  
  },
  async created() {
    const res = await getTagsApi()
    this.tags = res.data
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
.tags-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: $spacing-lg 0;
  min-height: calc(100vh - 80px);
  position: relative;
  
  &::before {
    content: '';
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    // 旧版是两团紫蓝径向光晕，新设计层次靠实线不靠光晕，去掉
    background: none;
    pointer-events: none;
    z-index: -1;
  }
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: $spacing-xl;
  padding: 0 $spacing-lg;
  min-height: 100vh;
  align-items: start;
  
  @include responsive(lg) {
    grid-template-columns: 1fr;
    padding: $spacing-md;
  }
}

.main-content {
  position: relative;
}

.page-header {
  margin-bottom: $spacing-sm;
  text-align: center;
  position: relative;
  padding: $spacing-xl 0;
  overflow: hidden;
  
  h3 {
    font-family: $font-serif;
    font-size: 1.6em;
    font-weight: 600;
    // 台账取向：墨色衬线标题，不用渐变裁切文字
    color: var(--text-primary);
    margin-bottom: $spacing-sm;
    letter-spacing: 0.02em;
    position: relative;
    display: inline-block;
  }
  
  .subtitle {
    color: var(--text-secondary);
    font-size: 1.2em;
    opacity: 0.8;
    display: inline-block;
    padding: $spacing-xs $spacing-lg;
  }

  .cloud-hint {
    margin-top: $spacing-xs;
    font-size: 0.85em;
    color: var(--text-tertiary);
  }
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: $spacing-md;
  margin-bottom: $spacing-sm;
  padding: 0 $spacing-md $spacing-md;
  justify-content: center;

  .tag-item {
    position: relative;
    display: flex;
    align-items: center;
    gap: 0.5em;
    padding: 0.4em 0.7em;
    font-size: 1rem;
    line-height: 1.2;
    color: var(--text-secondary);
    background: transparent;
    border: 1px solid var(--rule);
    border-radius: $border-radius-sm;
    cursor: pointer;
    overflow: hidden;
    transition: border-color 0.25s ease, color 0.25s ease, background 0.25s ease;
    animation: tagAppear 0.4s ease-out backwards;
    animation-delay: var(--animation-delay, 0s);

    .tag-icon {
      font-size: 0.8em;
      transition: transform 0.3s ease, color 0.25s ease;
    }

    // 6 个色相按标签顺序循环，给图标上色；色相取自同一组低饱和色，不刺眼
    &.hue-0 .tag-icon { color: #b7791f; }
    &.hue-1 .tag-icon { color: #2f8f83; }
    &.hue-2 .tag-icon { color: #3b6ea5; }
    &.hue-3 .tag-icon { color: #7a5aa8; }
    &.hue-4 .tag-icon { color: #b5527a; }
    &.hue-5 .tag-icon { color: #4f8a4f; }

    .tag-name {
      position: relative;
      z-index: 1;
    }

    .tag-count {
      position: relative;
      z-index: 1;
      font-family: $font-mono;
      font-size: 0.72em;
      color: var(--text-tertiary);
      transition: color 0.25s ease;
    }

    // 底部长条：长度 = 该标签文章数占最大值的比例。是数据，不是装饰
    .tag-bar {
      position: absolute;
      left: 0;
      bottom: 0;
      height: 2px;
      width: calc(var(--tag-ratio, 0) * 100%);
      background: var(--accent);
      opacity: 0.3;
      transition: opacity 0.25s ease;
      pointer-events: none;
    }

    &:hover {
      border-color: var(--accent);
      color: var(--accent);

      .tag-icon {
        transform: rotate(-10deg);
      }

      .tag-bar {
        opacity: 0.75;
      }
    }

    &.active {
      background: var(--accent);
      border-color: var(--accent);
      color: #fff;

      .tag-icon {
        color: #fff;
      }

      .tag-count {
        color: rgba(255, 255, 255, 0.85);
      }

      .tag-bar {
        opacity: 0;
      }
    }
  }
}

.tag-header {
  margin-bottom: $spacing-xl;
  padding: $spacing-md $spacing-xl;
  background: transparent;
  border-bottom: 1px solid var(--rule-strong);
  border-radius: 0;
  position: relative;
  overflow: hidden;
  
  .header-line {
    position: absolute;
    top: 0;
    left: 0;
    width: 2px;
    height: 100%;
    background: var(--accent);
    
    &::before,
    &::after {
      content: '';
      position: absolute;
      left: 0;
      width: 100%;
      height: 10px;
      background: var(--surface);
      filter: blur(4px);
    }
    
    &::before {
      top: 0;
    }
    
    &::after {
      bottom: 0;
    }
  }

  h2 {
    font-family: $font-serif;
    font-size: 1.6em;
    color: var(--text-primary);
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin: 0;

    i {
      color: var(--accent);
      font-size: 0.9em;
      transition: transform 0.3s ease;
    }

    .post-count {
      font-family: $font-mono;
      font-size: 0.6em;
      color: var(--text-secondary);
      margin-left: auto;
      padding: $spacing-xs $spacing-md;
      background: transparent;
      border: 1px solid var(--rule);
      transition: all 0.3s ease;
      border-radius: $border-radius-lg;
      letter-spacing: 0.5px;
      display: flex;
      align-items: center;
      gap: $spacing-xs;
      
      .count-number {
        color: $primary;
        font-weight: 600;
        font-size: 1.2em;
      }

      &:hover {
        border-color: rgba($primary, 0.4);
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba($primary, 0.15);
      }
    }
  }
}

.articles-wrapper {
  padding: 0 $spacing-lg;
  animation: slideUp 0.6s ease-out;
}

.sidebar-container {
  position: sticky;
  top: 80px;
  /* 确保在移动端时取消sticky效果 */
  @include responsive(lg) {
    position: relative;
    top: 0;
    height: auto;
    overflow-y: visible;
  }
}

// 深色模式：色相整体提亮，保证在深底上仍有对比
:root[data-theme='dark'] .tag-item {
  &.hue-0 .tag-icon { color: #e0a94a; }
  &.hue-1 .tag-icon { color: #4bbfae; }
  &.hue-2 .tag-icon { color: #6aa3dd; }
  &.hue-3 .tag-icon { color: #a98fd6; }
  &.hue-4 .tag-icon { color: #e08aab; }
  &.hue-5 .tag-icon { color: #7bc47b; }
}

@keyframes tagAppear {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@include responsive(sm) {
  .tags-page {
    padding: $spacing-md 0;
  }

  .content-card {
    border-radius: $border-radius-lg;
    margin: 0;
  }

  .page-header {
    padding: $spacing-lg 0;
    
    h1 {
      font-size: 2.4em;
    }

  }

  .tags-cloud {
    padding: 0 $spacing-md $spacing-lg;
    gap: $spacing-sm;
    
    .tag-item {
      font-size: 0.9rem;
    }
  }

  .tag-header {
    padding: $spacing-sm $spacing-md;
    
    h2 {
      font-size: 1.4em;
    }
  }

  .articles-wrapper {
    padding: 0 $spacing-md;
  }
}

.header-waves,
.wave {
  display: none;
}
</style> 
