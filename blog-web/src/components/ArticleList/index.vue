<template>
  <div class="article-list-component" v-loading="loading">
    <transition-group name="post-list" tag="div" class="posts-list">
      <article v-for="post in articles" :key="post.id" class="post-item">
        <div class="post-content">
          <div class="post-main">
            <div class="post-text">
              <h3>
                <span v-if="post.isStick" class="stick-tag">
                  <i class="fas fa-thumbtack"></i>
                  置顶
                </span>
                <span v-if="post.status === 4" class="stick-tag private-tag">
                  <i class="fas fa-lock"></i>
                  私密
                </span>
                <span class="post-title underline" @click="$emit('article-click', post.id)">{{ post.title }}</span>
              </h3>
              <p class="post-excerpt">{{ post.summary }}</p>
            </div>
            <div class="post-image" @click="$emit('article-click', post.id)">
              <img v-lazy="resolveCover(post)" :key="resolveCover(post)" :alt="post.title">
              <div class="image-placeholder">
                <i class="fas fa-image"></i>
              </div>
            </div>
          </div>

          <div class="post-footer">
            <div class="footer-left">
              <div class="author-info">
                <el-avatar :size="24" :src="post.avatar"></el-avatar>
                <span class="author-name">{{ post.nickname }}</span>
              </div>
              <div class="post-date">
                <i class="far fa-calendar"></i>
                {{ formatTime(post.createTime) }}
              </div>
              <div class="post-view">
                <i class="far fa-eye"></i>
                {{ post.quantity }}
              </div>
            </div>
            <div class="footer-right">
              <el-tooltip v-if="isAdmin && canStick(post)"
                :content="post.isStick === 1 ? '取消置顶' : '设为置顶'"
                placement="top">
                <button type="button" class="stick-btn" :class="{ 'is-stick': post.isStick === 1 }"
                  :disabled="stickLoadingId === post.id" @click.stop="handleToggleStick(post)">
                  <i class="fas fa-thumbtack"></i>
                  {{ post.isStick === 1 ? '取消置顶' : '置顶' }}
                </button>
              </el-tooltip>
              <span class="category-tag">{{ post.categoryName }}</span>
              <span class="read-time">
                <i class="far fa-clock"></i>
                {{ Math.ceil(post.contentMd.split(" ").length / 300) }}分钟阅读
              </span>
            </div>
          </div>
        </div>
      </article>
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
import { formatTime } from '@/utils/time'
import { updateArticleStatusApi } from '@/api/article'

const LOADING_COVER = '/static/img/lazy.gif'
const DEFAULT_COVER = '/static/img/book.png'

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
      // 无封面文章:先展示 3 秒加载动画,之后回落到静态默认封面
      defaultCoverReady: false,
      // 正在提交置顶请求的文章id，用于禁用按钮防止重复点击
      stickLoadingId: null
    }
  },
  watch: {
    articles(val) {
      if (val.length && !this.defaultCoverTimer) {
        this.defaultCoverTimer = setTimeout(() => {
          this.defaultCoverReady = true
        }, 3000)
      }
    }
  },
  beforeUnmount() {
    clearTimeout(this.defaultCoverTimer)
  },
  methods: {
    formatTime(time) {
      return formatTime(time)
    },
    resolveCover(post) {
      return post.cover || (this.defaultCoverReady ? DEFAULT_COVER : LOADING_COVER)
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
.posts-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.post-item {
  @include card;
  background: var(--card-bg);
  padding: $spacing-lg;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }
}

.post-main {
  display: grid;
  grid-template-columns: 1fr 200px;
  gap: $spacing-lg;
  margin-bottom: $spacing-md;

  .post-text {
    h3 {
      font-size: 1.2em;
      margin-bottom: $spacing-md;
      line-height: 1.4;
      color: var(--text-primary);
      cursor: pointer;
      .stick-tag {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 0.6em;
        background: linear-gradient(135deg, $secondary, darken($secondary, 10%));
        color: white;
        padding: 3px 8px;
        border-radius: 4px;
        margin-right: $spacing-sm;

        i {
          transform: rotate(45deg);
        }
      }
      .private-tag {
        background: linear-gradient(135deg, #909399, darken(#909399, 10%));

        i {
          transform: none;
        }
      }
    }

    .post-title {
      &:hover {
        color: $primary;
      }
    }

    .post-excerpt {
      color: var(--text-secondary);
      line-height: 1.6;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  }

  .post-image {
    position: relative;
    height: 140px;
    border-radius: 8px;
    overflow: hidden;
    cursor: pointer;
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.3s ease;
    }

    &:hover img {
      transform: scale(1.05);
    }
  }
}

.post-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: $spacing-md;
  border-top: 1px solid var(--border-color);
  color: var(--text-secondary);
  .fa-calendar{
    color: $primary;
  }
  .fa-eye{
    color: #67c23a;
  }
  .fa-clock{
    color: #2fa9e1;
  }
  .footer-left {
    display: flex;
    align-items: center;
    gap: $spacing-lg;

    .author-info {
      display: flex;
      align-items: center;
      gap: $spacing-sm;

      .author-name {
        font-weight: 500;
        color: $primary;
      }
    }

    .post-date {
      color: var(--text-secondary);
      font-size: 0.9em;
      display: flex;
      align-items: center;
      gap: $spacing-sm;
    }
  }

  .footer-right {
    display: flex;
    align-items: center;
    gap: $spacing-lg;

    .category-tag {
      padding: 4px 12px;
      background: var(--hover-bg);
      border-radius: 20px;
      font-size: 0.9em;
      color: var(--text-secondary);
    }

    .read-time {
      color: var(--text-secondary);
      font-size: 0.9em;
      display: flex;
      align-items: center;
      gap: $spacing-sm;
    }

    // 管理员专属的置顶按钮
    .stick-btn {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      height: 24px;
      padding: 0 10px;
      font-size: 0.85em;
      border-radius: 20px;
      border: 1px solid var(--border-color);
      background: transparent;
      color: var(--text-secondary);
      cursor: pointer;
      transition: all 0.2s ease;

      i {
        transform: rotate(45deg);
      }

      &:hover:not(:disabled) {
        color: $primary;
        border-color: $primary;
      }

      &.is-stick {
        color: $secondary;
        border-color: $secondary;
        background: rgba($secondary, 0.12);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
  }
}

@include responsive(md) {
  .post-main {
    grid-template-columns: 1fr;
    
    .post-image {
      height: 200px;
      order: -1;
    }
  }
}

@include responsive(sm) {
  .post-item {
    padding: $spacing-sm;
  }

  .post-footer {
    flex-direction: column;
    gap: $spacing-md;
    
    .footer-left, .footer-right {
      width: 100%;
      justify-content: space-between;
    }
  }
}

// 保持动画效果不变
.post-list-enter-active {
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  transition-delay: calc(0.1s * var(--index));
}

.post-list-leave-active {
  transition: all 0.6s ease;
}

.post-list-enter,
.post-list-leave-to {
  opacity: 0;
  transform: translateY(30px);
}
</style>