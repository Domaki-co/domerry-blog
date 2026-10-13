<template>
  <div class="article-list-component" v-loading="loading">
    <!-- 台账式列表：日期 | 标题+摘要+元信息 | 缩略图 -->
    <transition-group name="post-list" tag="ol" class="ledger">
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

          <!-- 缩略图：加载中显示 lazy 占位图，无封面或加载失败回退到 book -->
          <router-link :to="`/post/${post.id}`" class="entry-thumb">
            <img
              v-lazy="{ src: post.cover || defaultCover, loading: lazyPlaceholder, error: defaultCover }"
              :key="post.cover || defaultCover"
              :alt="post.title"
              @error="handleImageError">
          </router-link>
        </div>
      </li>
    </transition-group>

    <el-empty v-if="!loading && articles.length === 0" description="暂无文章" />

    <div class="pagination-box">
      <el-pagination
        background
        v-if="articles.length"
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

const DEFAULT_COVER = '/static/img/book.png'
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
      default: {
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  computed: {
    /**
     * 是否管理员（只有管理员才允许调用后台置顶接口）
     */
    isAdmin() {
      return this.$store.getters.isAdmin
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
//  文章台账
//
//  三列：日期（年小/月日大）| 标题+摘要+元信息 | 118px 缩略图。
//  hover 只做底色变化和标题下划线展开，不位移 —— 位移会让整列
//  文字在鼠标扫过时抖动，台账要的是稳。
// ════════════════════════════════════════════════════════════════

.ledger {
  list-style: none;
  margin: 0;
  padding: 0;
}

.entry {
  border-bottom: 1px solid var(--rule);
}

.entry-link {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr) 118px;
  gap: clamp(16px, 2vw, 28px);
  align-items: start;
  padding: 24px 18px;
  margin: 0 -18px;
  transition: background 0.22s ease;

  &:hover {
    background: var(--row-hover);

    .entry-title a {
      background-size: 100% 1px;
    }

    .entry-thumb img {
      filter: saturate(1);
    }
  }
}

.entry-date {
  font-family: $font-mono;
  font-variant-numeric: tabular-nums;
  padding-top: 3px;

  .y {
    display: block;
    font-size: 0.68rem;
    color: var(--text-tertiary);
    letter-spacing: 0.05em;
  }

  .md {
    display: block;
    font-size: 0.95rem;
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
  align-items: baseline;
  gap: 9px;
  flex-wrap: wrap;
  margin: 0;

  a {
    color: var(--text-primary);
    text-decoration: none;
    // 下划线从左侧展开
    background-image: linear-gradient(currentColor, currentColor);
    background-size: 0% 1px;
    background-repeat: no-repeat;
    background-position: 0 100%;
    transition: background-size 0.32s ease;
  }
}

// 置顶 / 私密：描边小标签，不用实心色块
.badge {
  font-family: $font-mono;
  font-size: 0.63rem;
  letter-spacing: 0.06em;
  line-height: 1;
  padding: 3px 6px;
  border: 1px solid var(--accent);
  color: var(--accent);
  border-radius: $border-radius-sm;
  flex: none;
  transform: translateY(-1px);

  &.muted {
    border-color: var(--rule-strong);
    color: var(--text-tertiary);
  }
}

.entry-sum {
  margin-top: 9px;
  color: var(--text-secondary);
  font-size: 0.92rem;
  line-height: 1.7;
  max-width: 62ch;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.entry-meta {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  font-family: $font-mono;
  font-size: 0.72rem;
  color: var(--text-tertiary);
  letter-spacing: 0.02em;

  .cat {
    color: var(--text-secondary);
  }
}

.entry-thumb {
  width: 118px;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  border: 1px solid var(--rule);
  background: var(--row-hover);
  display: block;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    filter: saturate(0.9);
    transition: filter 0.3s ease;
  }

}

// 管理员置顶按钮：融进元信息行，不额外占位
.stick-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 7px;
  font-family: $font-mono;
  font-size: 0.68rem;
  letter-spacing: 0.02em;
  border-radius: $border-radius-sm;
  border: 1px solid var(--rule);
  background: transparent;
  color: var(--text-tertiary);
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
}

// 列表进场：轻微上浮，不做大幅位移
.post-list-enter-active {
  transition: opacity 0.5s ease, transform 0.5s ease;
  transition-delay: calc(0.04s * var(--index, 0));
}

.post-list-leave-active {
  transition: opacity 0.3s ease;
}

.post-list-enter,
.post-list-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@include responsive(lg) {
  .entry-link {
    grid-template-columns: 64px minmax(0, 1fr) 108px;
    gap: 16px;
    padding: 20px 14px;
    margin: 0 -14px;
  }

  .entry-thumb {
    width: 108px;
  }
}

@include responsive(md) {
  .entry-link {
    grid-template-columns: 64px minmax(0, 1fr);
    gap: 14px;
    padding: 20px 12px;
    margin: 0 -12px;
  }

  // 窄屏优先保住标题和摘要，缩略图让位
  .entry-thumb {
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
</style>
