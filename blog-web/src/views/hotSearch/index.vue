<template>
  <div class="hot-search-container">
    <div class="hot-search">
      <header class="ledger-head">
        <h1>热搜</h1>
        <span class="head-meta">{{ getCurrentEngine.label }} · {{ currentTabLabel }}</span>
      </header>

      <!-- 搜索 -->
      <section class="search-panel">
        <div class="search-input">
          <input
            v-model="searchText"
            :placeholder="`在 ${getCurrentEngine.label} 中搜索…`"
            @keyup.enter="handleSearch"
          >
          <button class="search-btn" @click="handleSearch" aria-label="搜索">
            <i class="fas fa-search"></i>
          </button>
        </div>
        <div class="search-engines">
          <button
            v-for="engine in searchEngines"
            :key="engine.name"
            :class="['engine-btn', { active: currentEngine === engine.name }]"
            @click="currentEngine = engine.name"
          >
            <svg-icon :icon-class="engine.icon"></svg-icon>
            {{ engine.label }}
          </button>
        </div>
      </section>

      <!-- 热搜列表 -->
      <section class="hot-lists">
        <div class="hot-tabs">
          <button
            v-for="tab in hotTabs"
            :key="tab.type"
            :class="['tab-btn', { active: currentTab === tab.type }]"
            @click="currentTab = tab.type"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="hot-content">
          <ol class="hot-grid" v-loading="loading">
            <li
              v-for="(item, index) in currentHotList"
              :key="index"
              class="hot-row"
              @click="handleHotItemClick(item)"
            >
              <span class="rank" :class="getRankClass(index)">{{ String(index + 1).padStart(2, '0') }}</span>
              <div class="row-main">
                <!-- 榜单数据由后端透传第三方接口，协议不可信：非 http(s) 时降级为纯文本，避免 javascript: 伪协议 -->
                <a
                  v-if="safeExternalUrl(item.url)"
                  :href="safeExternalUrl(item.url)"
                  target="_blank"
                  rel="noopener noreferrer"
                  class="title"
                  @click.stop
                >{{ item.keyword }}</a>
                <span v-else class="title">{{ item.keyword }}</span>
                <span class="row-meta">
                  <span class="hot-value" v-if="item.hotValue">{{ formatHotValue(item.hotValue) }}</span>
                  <span class="trend" v-if="item.trend">
                    <i :class="getTrendIcon(item.trend)"></i>
                  </span>
                  <span class="hot-tag" v-if="item.tag">{{ item.tag }}</span>
                </span>
              </div>
            </li>
          </ol>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { getHotListApi } from '@/api/hot-search'
import { safeExternalUrl } from '@/utils/sanitize'
export default {
  name: 'HotSearch',
  data() {
    return {
      searchText: '',
      currentEngine: 'baidu',
      currentTab: 'weibo',
      loading: false,
      searchEngines: [
        { name: 'baidu', label: '百度', icon: 'baidu', url: 'https://www.baidu.com/s?wd=' },
        { name: 'google', label: '谷歌', icon: 'chrome', url: 'https://www.google.com/search?q=' },
        { name: 'gitee', label: '码云', icon: 'gitee', url: 'https://search.gitee.com/?q=' },
        { name: 'github', label: 'GitHub', icon: 'github', url: 'https://github.com/search?q=' }
      ],
      hotTabs: [
        { type: 'weibo', label: '微博热搜', icon: 'fab fa-weibo' },
        { type: 'zhihu', label: '知乎热榜', icon: 'fab fa-zhihu' },
        { type: 'toutiao', label: '头条热榜', icon: 'fas fa-fire' },
        { type: 'baidu', label: '百度热搜', icon: 'fas fa-chart-line' },
        { type: 'csdn', label: 'CSDN热榜', icon: 'fas fa-code' }
      ],
      hotLists: {
        weibo: [],
        zhihu: [],
        toutiao: [],
        baidu: [],
        csdn: []
      }
    }
  },
  computed: {
    getCurrentEngine() {
      return this.searchEngines.find(engine => engine.name === this.currentEngine)
    },
    currentHotList() {
      return this.hotLists[this.currentTab] || []
    },
    currentTabLabel() {
      const tab = this.hotTabs.find(item => item.type === this.currentTab)
      return tab ? tab.label : ''
    }
  },
  methods: {
    handleSearch() {
      if (!this.searchText.trim()){
        this.$message.warning('请输入搜索内容')
        return
      }
      const engine = this.getCurrentEngine
      const target = safeExternalUrl(engine.url + encodeURIComponent(this.searchText))
      if (!target) return
      // noopener 防止新页面通过 window.opener 反向操作本页
      window.open(target, '_blank', 'noopener,noreferrer')
    },
    getRankClass(index) {
      if (index < 3) return `rank-${index + 1}`
      return ''
    },
    formatHotValue(value) {
      if (value >= 10000) {
        return (value / 10000).toFixed(1) + 'w'
      }
      return value
    },
    async fetchHotList(type) {
      this.loading = true
      try {
        // 这里替换为实际的API调用
        const res = await getHotListApi(type)
        this.hotLists[type] = res.data.data
      } catch (error) {
        this.$message.error('获取热搜数据失败')
      } finally {
        this.loading = false
      }
    },
    handleHotItemClick(item) {
      const target = safeExternalUrl(item.url)
      if (!target) return
      // noopener 防止新页面通过 window.opener 反向操作本页
      window.open(target, '_blank', 'noopener,noreferrer')
    },
    safeExternalUrl,
    getTrendIcon(trend) {
      if (trend === '上升') return 'fas fa-arrow-up text-success'
      if (trend === '下降') return 'fas fa-arrow-down text-danger'
      return 'fas fa-minus text-warning'
    }
  },
  watch: {
    currentTab: {
      immediate: true,
      handler(newTab) {
        this.fetchHotList(newTab)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  热搜 · 技术台账
//
//  搜索框是一条细线输入，搜索引擎是文字切换；热搜是编号台账行，
//  名次用等宽数字，热度值和趋势并排成一行元信息。
// ════════════════════════════════════════════════════════════════

.hot-search-container {
  padding: clamp(24px, 4vh, 44px) clamp(20px, 4vw, 48px);
}

.hot-search {
  max-width: 1000px;
  margin: 0 auto;
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

// ── 搜索 ──
.search-panel {
  margin-bottom: clamp(32px, 5vh, 52px);
}

.search-input {
  position: relative;
  display: flex;
  align-items: center;
  border: 1px solid var(--rule-strong);
  border-radius: $border-radius-sm;
  background: var(--surface);
  padding: 0 6px 0 16px;
  transition: border-color 0.2s ease;

  &:focus-within {
    border-color: var(--accent);
  }

  input {
    flex: 1;
    min-width: 0;
    padding: 14px 0;
    border: none;
    background: none;
    font-size: 1rem;
    color: var(--text-primary);

    &::placeholder {
      color: var(--text-tertiary);
    }

    &:focus {
      outline: none;
    }
  }

  .search-btn {
    flex: none;
    width: 38px;
    height: 38px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    border-radius: $border-radius-md;
    background: var(--accent);
    color: #fff;
    cursor: pointer;
    box-shadow: 0 4px 14px rgba(168, 106, 16, 0.2);
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      filter: brightness(1.08);
      box-shadow: 0 6px 18px rgba(168, 106, 16, 0.28);
      transform: translateY(-1px);
    }

    &:active {
      filter: brightness(0.95);
      transform: translateY(0);
    }
  }
}

.search-engines {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 14px;

  .engine-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 4px 0;
    border: none;
    border-bottom: 1px solid transparent;
    background: none;
    color: var(--text-secondary);
    font-size: 0.88rem;
    cursor: pointer;
    transition: color 0.2s ease, border-color 0.2s ease;

    svg-icon {
      font-size: 1.05em;
    }

    &:hover {
      color: var(--text-primary);
    }

    &.active {
      color: var(--text-primary);
      border-bottom-color: var(--accent);
    }
  }
}

// ── 热搜 ──
.hot-tabs {
  display: flex;
  gap: clamp(14px, 1.6vw, 24px);
  padding-bottom: 12px;
  border-bottom: 1px solid var(--rule-strong);
  margin-bottom: 2px;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }

  .tab-btn {
    padding: 0 0 2px;
    border: none;
    border-bottom: 1px solid transparent;
    background: none;
    color: var(--text-secondary);
    font-size: 0.9rem;
    white-space: nowrap;
    cursor: pointer;
    transition: color 0.2s ease, border-color 0.2s ease;

    &:hover {
      color: var(--text-primary);
    }

    &.active {
      color: var(--text-primary);
      border-bottom-color: var(--accent);
    }
  }
}

.hot-content {
  min-height: 200px;
}

.hot-grid {
  list-style: none;
  margin: 0;
  padding: 0;
}

.hot-row {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 16px;
  align-items: baseline;
  padding: 13px 12px;
  margin: 0 -12px;
  border-bottom: 1px solid var(--rule);
  cursor: pointer;
  transition: background 0.2s ease;

  &:hover {
    background: var(--row-hover);

    .title {
      color: var(--accent);
    }
  }

  .rank {
    @include ledger-meta;
    font-size: 0.82rem;
    text-align: right;

    &.rank-1 {
      color: var(--accent);
    }

    &.rank-2 {
      color: var(--text-secondary);
    }
  }

  .row-main {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 16px;
    min-width: 0;
  }

  .title {
    color: var(--text-primary);
    text-decoration: none;
    font-size: 0.98rem;
    line-height: 1.5;
    transition: color 0.2s ease;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .row-meta {
    display: inline-flex;
    align-items: baseline;
    gap: 12px;
    flex: none;

    .hot-value {
      @include ledger-meta;
    }

    .trend i {
      font-size: 0.7rem;
      color: var(--text-tertiary);

      // 趋势是数据：上升/下降用强调色与中性色区分，不用红绿黄三色
      &.text-success { color: var(--accent); }
      &.text-danger { color: var(--text-secondary); }
      &.text-warning { color: var(--text-tertiary); }
    }

    .hot-tag {
      @include ledger-meta;
      padding: 1px 6px;
      border: 1px solid var(--rule);
      border-radius: $border-radius-sm;
      color: var(--text-secondary);
    }
  }
}

@include responsive(md) {
  .hot-row {
    grid-template-columns: 28px minmax(0, 1fr);
    gap: 12px;
    padding: 12px 8px;
    margin: 0 -8px;
  }

  .hot-row .row-meta .hot-tag {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hot-row,
  .hot-row .title,
  .tab-btn,
  .engine-btn,
  .search-input,
  .search-btn {
    transition: none !important;
  }
}
</style>
