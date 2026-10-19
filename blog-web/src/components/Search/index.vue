<template>
  <el-dialog
    v-model="dialogVisible"
    title="搜索"
    width="650px"
    @closed="handleClose"
  >
    <div 
      class="search-input-wrapper"
      :class="{ loading }"
    >
      <i class="fas fa-search search-icon"></i>
      <input
        ref="searchInput"
        v-model="params.keyword"
        type="text"
        class="search-input"
        placeholder="输入关键词搜索文章..."
        @keyup.enter="handleSearch"
      />
      <div v-if="params.keyword" class="clear-btn" @click="clearSearch">
        <i class="fas fa-times"></i>
      </div>
      <span class="enter-tip">
        <kbd class="keycap">⏎</kbd>
        搜索
        <span class="esc-hint">
          <kbd class="keycap">esc</kbd>
          关闭
        </span>
      </span>
    </div>

    <div class="hot-searches" v-if="searchResults.length === 0">
      <h4>
        <i class="fas fa-fire"></i>
        标签搜索
      </h4>
      <div class="hot-tags">
        <router-link 
          v-for="tag in tags" 
          :key="tag.id"
          :to="`/tags?tagId=${tag.id}&tagName=${tag.name}`"
          class="hot-tag"
        >
          {{ tag.name }}
        </router-link>
      </div>
    </div>
    
    <div v-if="searchResults.length > 0" class="search-results" v-loading="loading">
      <div
        v-for="item in searchResults"
        :key="item.id"
        class="search-result-item"
        @click="handleResultClick(item)"
      >
        <div class="result-header">
          <h3 v-html="highlightKeyword(item.title)"></h3>
          <span class="result-date">{{ formatDate(item.createTime) }}</span>
        </div>
        <p v-html="highlightKeyword(item.summary)"></p>
        <div class="result-footer">
          <span class="result-category">
            <i class="fas fa-folder"></i>
            {{ item.categoryName }}
          </span>
          <span class="result-views">
            <i class="fas fa-eye"></i>
            {{ item.quantity }} 阅读
          </span>
        </div>
      </div>

    
      <div class="pagination-box">
        <el-pagination background v-if="total" @current-change="handlePageChange" :current-page="params.pageNum"
          :page-size="params.pageSize" layout="prev, pager, next" :total="total">
        </el-pagination>
      </div>
    </div>

    <el-empty
      v-if="searchResults.length === 0"
      description="输入关键词搜索文章.."
    />
  </el-dialog>
</template>

<script>
import { getArticlesApi } from '@/api/article'
import { getTagsApi } from '@/api/tags'
export default {
  name: 'SearchDialog',
  
  data() {
    return {
      searchResults: [],
      loading: false,
      searchTimeout: null,
      // 请求序号：用于丢弃过期响应
      searchRequestId: 0,
      tags: [],
      params: {
        keyword: '',
        pageNum: 1,
        pageSize: 10
      },
      hasSearched: false,
      total: 0,
    }
  },
  computed: {
    // el-dialog 在 Element Plus 里是 v-model（modelValue）语义；
    // 且 Vuex 的 state 不能直接赋值（v-model 会生成赋值语句），必须提交 mutation
    dialogVisible: {
      get() {
        return this.$store.state.searchVisible
      },
      set(val) {
        this.$store.commit('SET_SEARCH_VISIBLE', val)
      }
    }
  },
  watch: {
    dialogVisible: {
      handler(newVal, oldVal) {
        if (newVal) {
          getTagsApi()
            .then(res => {
              this.tags = res.data || []
            })
            .catch(error => {
              this.$message.error(error.message)
            })
        }
      }
    }
  },  
  methods: {
    /**
     * 处理分页
     */
    handlePageChange(page) {
      this.params.pageNum = page
      this.handleSearch()
    },
    /**
     * 搜索
     */
    async handleSearch() {
        // 每次搜索领一个序号：关键词/翻页切得快时，先发出的慢响应不能覆盖后发出的结果。
        // 序号在「清空关键词」的早退之前就领走，否则清空后仍在途的响应会把结果又写回来。
        const requestId = ++this.searchRequestId

        if (!this.params.keyword.trim()) {
          this.searchResults = []
          this.hasSearched = false
          // 序号已换代，在途请求不会再走到 finally，这里自己收掉 loading
          this.loading = false
          return
        }

        this.loading = true
        try {
          const res = await getArticlesApi(this.params)
          if (requestId !== this.searchRequestId) return
          this.total = res.data?.total || 0
          this.searchResults = (res.data?.records || []).map(item => ({
            ...item,
            title: this.truncateText(item.title, 50),
            summary: this.truncateText(item.summary || item.content, 150)
          }))
          this.hasSearched = true
        } catch (error) {
          // 过期请求的失败不再提示，用户已经换了关键词/页码
          if (requestId !== this.searchRequestId) return
          this.$message.error(error.message)
        } finally {
          if (requestId === this.searchRequestId) {
            this.loading = false
          }
        }
    },
    /**
     * 截断文本
     */
    truncateText(text, maxLength) {
      if (!text) return ''
      return text.length > maxLength 
        ? text.substring(0, maxLength) + '...'
        : text
    },
    /**
     * 高亮关键词：先 HTML 转义原文，再包 <mark>，保证输出可安全用于 v-html
     */
    highlightKeyword(text) {
      if (!this.params.keyword || !text) return this.escapeHtml(text || '')
      const keywords = this.params.keyword.split(/\s+/).filter(k => k)
      let highlightedText = this.escapeHtml(text)

      keywords.forEach(keyword => {
        const regex = new RegExp(`(${this.escapeRegExp(this.escapeHtml(keyword))})`, 'gi')
        highlightedText = highlightedText.replace(regex, '<mark>$1</mark>')
      })

      return highlightedText
    },

    escapeHtml(string) {
      return String(string)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
    },
    /**
     * 转义正则表达式
     */
    escapeRegExp(string) {
      return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    },
    /**
     * 处理搜索结果点击
     */
    handleResultClick(item) {
      this.$router.push(`/post/${item.id}`)
      this.close()
    },

    /**
     * 关闭搜索对话框
     */
    close() {
      this.$store.commit('SET_SEARCH_VISIBLE', false)
      this.params.keyword = ''
      this.searchResults = []
      this.hasSearched = false
    },
    /**
     * 处理搜索对话框关闭
     */
    handleClose() {
      this.close()
    },
    /**
     * 处理键盘事件
     */
    handleKeydown(e) {
      if (e.key === 'Escape') {
        this.close()
        return
      }
      // ⌘K / Ctrl+K 开搜索（Esc 关），Header 上那枚键帽就是这个快捷键的提示
      if ((e.metaKey || e.ctrlKey) && String(e.key).toLowerCase() === 'k') {
        e.preventDefault()
        this.$store.commit('SET_SEARCH_VISIBLE', true)
      }
    },
    /**
     * 清除搜索
     */
    clearSearch() {
      this.params.keyword = ''
      this.searchResults = []
      this.hasSearched = false
    },
    /**
     * 格式化日期
     */
    formatDate(date) {
      return new Date(date).toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      })
    }
  },

  mounted() {
    document.addEventListener('keydown', this.handleKeydown)
  },

  beforeUnmount() {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout)
    }
    document.removeEventListener('keydown', this.handleKeydown)
  }
}
</script>

<style lang="scss" scoped>
.search-input-wrapper {
  display: flex;
  align-items: center;
  width: 100%;
  border: 1px solid var(--rule-strong);
  border-radius: $border-radius-sm;
  padding: 12px 15px;
  margin-bottom: 20px;
  position: relative;
  transition: border-color 0.2s ease;
  
  &:focus-within {
    border-color: var(--accent);
  }
  
  .search-icon {
    color: var(--text-tertiary);
    font-size: 18px;
    margin-right: 10px;
  }
  
  .clear-btn {
    cursor: pointer;
    padding: 4px;
    color: var(--text-tertiary);
    transition: all 0.3s ease;
    
    &:hover {
      color: #f56c6c;
    }
  }
  
  &.loading::after {
    content: '';
    position: absolute;
    right: 15px;
    width: 20px;
    height: 20px;
    border: 2px solid var(--rule);
    border-top-color: $primary;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
  
  .enter-tip {
    position: absolute;
    right: 15px;
    color: var(--text-tertiary);
    font-size: 12px;
    display: flex;
    align-items: center;
    gap: 6px;
    white-space: nowrap;
    opacity: 0.6;
    pointer-events: none;

    // 键帽：和 Header 搜索按钮上那枚同一套画法
    .keycap {
      padding: 1px 5px 2px;
      border-radius: $border-radius-sm;
      background: var(--row-hover);
      border: 1px solid var(--rule);
      box-shadow: 0 1px 0 var(--rule-strong);
      font-family: $font-mono;
      font-size: 0.68rem;
      line-height: 1.35;
      color: var(--text-secondary);
    }

    // 两个快捷键之间用一条发丝线隔开，不靠间距硬撑
    .esc-hint {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      margin-left: 4px;
      padding-left: 10px;
      border-left: 1px solid var(--rule);
    }
  }
  
  .clear-btn + .enter-tip {
    right: 40px;
  }
  
  &.loading .enter-tip {
    display: none;
  }
  
  &:focus-within .enter-tip {
    opacity: 0;
    transform: translateX(10px);
    transition: all 0.3s ease;
  }
}

// 窄屏的搜索框放不下两组提示，只留「回车」
@include responsive(md) {
  .search-input-wrapper .enter-tip .esc-hint {
    display: none;
  }
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  padding: 5px;
  width: 100%;
  background: transparent;
  color: $text-secondary;
}


.hot-searches {
  margin-top: 30px;
  
  h4 {
    margin: 0 0 15px;
    font-family: $font-mono;
    font-size: 13px;
    letter-spacing: 0.08em;
    color: var(--text-tertiary);
    display: flex;
    align-items: center;
    
    i {
      color: var(--accent);
      margin-right: 8px;
    }
  }
}

.search-results {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 10px;
  margin-top: $spacing-md;
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-thumb {
    background-color: var(--rule-strong);
    border-radius: 3px;
    
    &:hover {
      background-color: var(--text-tertiary);
    }
  }
}

.search-result-item {
  padding: 15px;
  cursor: pointer;
  border-radius: $border-radius-sm;
  margin-bottom: 10px;
  transition: background 0.2s ease, border-color 0.2s ease;
  border: 1px solid var(--rule);
  
  &:hover {
    background: var(--row-hover);
    border-color: var(--accent);
  }
  
  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    
    h3 {
      margin: 0;
      font-size: 16px;
      color: $primary;
      flex: 1;
    }
    
    .result-date {
      font-size: 12px;
      color: var(--text-tertiary);
      margin-left: 10px;
    }
  }
  
  p {
    margin: 0 0 10px;
    font-size: 14px;
    color: var(--text-secondary);
    line-height: 1.6;
  }
  
  .result-footer {
    display: flex;
    gap: 15px;
    font-size: 12px;
    color: var(--text-tertiary);
    
    i {
      margin-right: 4px;
    }
    
    .result-category,
    .result-views {
      display: flex;
      align-items: center;
    }
  }
}


.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hot-tag {
  padding: 6px 12px;
  // 台账取向：标签是描边条目，不是灰底胶囊
  background-color: transparent;
  border: 1px solid var(--rule);
  border-radius: $border-radius-sm;
  font-family: $font-mono;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  &:hover {
    color: var(--accent);
    border-color: var(--accent);
    background-color: transparent;
  }
}

:deep(mark) {
  // 搜索命中高亮：旧值 rgba(202,90,210) 是旧紫主题残留
  background-color: var(--accent-soft);
  padding: 0 2px;
  border-radius: 2px;
  color: var(--accent);
  font-weight: 500;
  transition: all 0.3s ease;
  
  &:hover {
    background-color: rgba($accent, 0.3);
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style> 