<template>
  <div class="moments-container">
    <!-- 说说列表 -->
    <div class="moments-list" v-loading="loading">
      <div v-for="moment in moments" :key="moment.id" class="moment-item">
        <div class="user-avatar">
          <img v-lazy="moment.avatar" :key="moment.avatar" class="avatar" />
          <!-- 移动端显示的用户信息 -->
          <div class="mobile-user-info">
            <span class="name">{{ moment.nickname }}</span>
            <span class="time">{{ formatTime(moment.createTime) }}</span>
          </div>
        </div>
        <div class="moment-main">
          <!-- PC端显示的用户信息 -->
          <div class="moment-header">
            <span class="name">{{ moment.nickname }}</span>
            <span class="time">{{ formatTime(moment.createTime) }}</span>
          </div>
          <div class="moment-content-wrapper">
            <div class="moment-content" v-html="$sanitize(moment.content)"></div>
            <div class="moment-images" v-if="moment.images?.length">
              <img v-for="(img, index) in moment.images" :key="img" v-lazy="img"
                @click="previewImage(moment.images, index)" />
            </div>
          </div>
        </div>
      </div>
      <div class="pagination-box">
        <el-pagination background v-if="moments.length" @current-change="handlePageChange"
          :current-page="params.pageNum" :page-size="params.pageSize" layout="prev, pager, next" :total="total">
        </el-pagination>
      </div>
    </div>

    <!-- 添加图片预览组件 -->
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
      total: 0,
      params: {
        pageNum: 1,
        pageSize: 10,
      },
    }
  },
  mounted() {
    this.fetchMoments()
  },

  methods: {
    parseImages(images) {
      if (!images) return []
      return images.split(',').filter(img => img)
    },
    async fetchMoments() {
      try {
        this.loading = true
        const res = await getMoments(this.params)
        this.moments = res.data.records
        this.total = res.data.total
        this.moments.forEach(moment => {
          moment.images = this.parseImages(moment.images)
        })
      } catch (error) {
        console.error('获取说说列表失败:', error)
      } finally {
        this.loading = false
      }
    },

    handlePageChange(page) {
      this.params.pageNum = page
      this.fetchMoments()
    },

    previewImage(images, index) {
      // 使用图片预览组件的 show 方法
      this.$refs.imagePreview.show(images, index)
    },

    formatTime(time) {
      return formatTime(time)
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  说说 · 技术台账
//
//  头像 | 正文，条目之间一条 1px 线。正文不再套一层不对称圆角卡片，
//  时间用等宽字，图标去掉。
// ════════════════════════════════════════════════════════════════

.moments-container {
  max-width: 720px;
  margin: 0 auto;
  padding: clamp(24px, 4vh, 44px) clamp(20px, 4vw, 32px);
  min-height: calc(100vh - 70px);
}

.moment-item {
  display: flex;
  gap: 16px;
  padding: 24px 0;
  border-bottom: 1px solid var(--rule);

  &:first-child {
    padding-top: 8px;
  }

  @media screen and (max-width: 768px) {
    flex-direction: column;
    gap: 12px;
    padding: 20px 0;
  }
}

.user-avatar {
  flex-shrink: 0;
  width: 42px;

  .avatar {
    width: 42px;
    height: 42px;
    border-radius: $border-radius-sm;
    border: 1px solid var(--rule);
    object-fit: cover;
  }

  @media screen and (max-width: 768px) {
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
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;

  .name {
    font-size: 0.95rem;
    font-weight: 500;
    color: var(--text-primary);
  }

  .time {
    @include ledger-meta;
  }
}

.moment-content-wrapper {
  .moment-content {
    color: var(--text-primary);
    line-height: 1.8;
    font-size: 0.95rem;
    white-space: pre-wrap;
    word-break: break-word;

    :deep(li) {
      margin-left: 30px;
    }
  }

  .moment-images {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 8px;
    margin-top: 14px;

    img {
      width: 100%;
      aspect-ratio: 4 / 3;
      object-fit: cover;
      border: 1px solid var(--rule);
      border-radius: $border-radius-sm;
      cursor: zoom-in;
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
</style>
