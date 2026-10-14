<template>
  <div class="notifications-page">
    <!-- 左侧消息分类 -->
    <div class="notifications-sidebar">
      <div class="sidebar-header">
        <h2>消息分类</h2>
      </div>
      <div class="category-list">
        <div 
          v-for="category in categories" 
          :key="category.type"
          class="category-item"
          :class="{ active: currentCategory === category.type }"
          @click="switchCategory(category.type)"
        >
          <i :class="category.icon"></i>
          <span>{{ category.name }}</span>
          <el-badge 
            v-if="category.unread" 
            :value="category.unread" 
            class="category-badge"
          />
        </div>
      </div>
    </div>

    <!-- 右侧消息列表 -->
    <div class="notifications-main">
      <div class="notifications-header">
        <h1>{{ currentCategoryName }}</h1>
        <div class="header-actions">
          <el-button type="link" @click="markAllAsRead" v-if="hasUnread">
            <i class="fas fa-check-double"></i> 全部已读
          </el-button>
        </div>
      </div>

      <div class="notifications-content" v-loading="loading">
        <template v-if="notifications.length">
          <div 
            v-for="notification in filteredNotifications" 
            :key="notification.id" 
            class="notification-item"
            :class="{ unread: !notification.isRead }"
            @click="handleNotificationClick(notification)"
          >
            <div class="notification-icon">
              <i :class="getNotificationIcon(notification.type)"></i>
            </div>
            <div class="notification-body">
              <div class="notification-title">{{ notification.title }}</div>
              <div class="notification-message">
                <span v-if="notification.type === 'comment'">
                  <span v-if="notification.fromUserId">
                    {{ notification.fromNickname }} 回复了你在 
                    <span class="article-title" @click="handleArticleClick(notification.articleId)">{{ notification.articleTitle }}</span> 中的评论
                  </span>
                  <span v-else>
                    {{ notification.fromNickname }} 评论了你的文章
                  </span>

                  <div v-html="$sanitize(notification.message)"></div>
                </span>

                <span v-if="notification.type === 'like'">
                  {{ notification.toNickname }} 点赞了你的 
                  <span class="article-title" @click="handleArticleClick(notification.articleId)">{{ notification.articleTitle }}</span>
                  文章
                </span>

                <span v-if="notification.type === 'follow'">
                  {{ notification.fromNickname }} 关注了你
                </span>

                <span v-if="notification.type === 'system'">
                  {{ notification.message }}
                </span>
              </div>
          
              <div class="notification-time">{{ formatTime(notification.createTime) }}</div>
            </div>
            <div class="notification-actions">
              <el-button 
                type="link" 
                size="small"
                @click.stop="deleteNotification(notification.id)"
              >
                <i class="fas fa-trash-alt"></i>
              </el-button>
            </div>
          </div>

          <div class="pagination-box">
            <el-pagination background v-if="notifications.length" @current-change="handlePageChange"
              :current-page="params.pageNum" :page-size="params.pageSize" layout="prev, pager, next" :total="total">
            </el-pagination>
          </div>
        </template>
        
        <div v-else class="empty-state">
          <i class="fas fa-bell-slash"></i>
          <p>暂无{{ currentCategoryName }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getNotificationsApi, markNotificationAsReadApi, markAllNotificationsAsReadApi,
   getUnreadNotificationsCountApi, deleteNotificationApi } from '@/api/message'
import { formatTime } from '@/utils/time'
export default {
  name: 'Notifications',
  data() {
    return {
      currentCategory: 'all',
      categories: [
        { 
          type: 'all',
          name: '全部消息',
          icon: 'fas fa-bell',
          unread: 0
        },
        { 
          type: 'system',
          name: '系统消息',
          icon: 'fas fa-cog',
          unread: 0
        },
        { 
          type: 'comment',
          name: '评论消息',
          icon: 'fas fa-comment',
          unread: 0
        },
        { 
          type: 'like',
          name: '点赞消息',
          icon: 'fas fa-heart',
          unread: 0
        },
        { 
          type: 'follow',
          name: '关注消息',
          icon: 'fas fa-user-plus',
          unread: 0
        }
      ],
      notifications: [],
      params:{
        pageNum: 1,
        pageSize: 10,
        type: null
      },
      total: 0,
      loading: false
    }
  },
  computed: {
    currentCategoryName() {
      const category = this.categories.find(c => c.type === this.currentCategory)
      return category ? category.name : '全部消息'
    },
    filteredNotifications() {
      if (this.currentCategory === 'all') {
        return this.notifications
      }
      return this.notifications.filter(n => n.type === this.currentCategory)
    },
    hasUnread() {
      return this.filteredNotifications.some(notification => !notification.isRead)
    }
  },
  methods: {
    /**
     * 切换消息分类
     */
    switchCategory(type) {
      this.currentCategory = type
      this.params.type = type === 'all' ? null : type
      this.params.pageNum = 1
      this.fetchNotifications()
    },
    /**
     * 获取消息通知图标
     */
    getNotificationIcon(type) {
      const category = this.categories.find(c => c.type === type)
      return category ? category.icon : 'fas fa-bell'
    },
    /**
     * 格式化时间
     */
    formatTime(time) {
      return formatTime(time)
    },
    /**
     * 处理文章点击
     */
    handleArticleClick(id) {
      this.$router.push(`/post/${id}`)
    },
    /**
     * 更新未读数
     */
    updateUnreadCount(all) {
      // 更新每个分类的未读数
      this.categories.forEach(category => {
        if (all) {
          category.unread = 0
          return
        }
        if (category.type === 'all') {
          category.unread = this.notifications.filter(n => !n.isRead).length
        } else {
          category.unread = this.notifications.filter(
            n => n.type === category.type && !n.isRead
          ).length
        }
      })
    },
    /**
     * 获取消息通知
     */
    async fetchNotifications() {
      this.loading = true
      try {
        const { data } = await getNotificationsApi(this.params)
        this.notifications = data.records
        this.total = data.total
      } catch (error) {
        this.$message.error('获取消息通知失败')
      } finally {
        this.loading = false
      }
    },

    /**
     * 处理分页
     */
    handlePageChange(page) {
      this.params.pageNum = page
      this.fetchNotifications()
    },  
    /**
     * 标记全部已读
     */
    async markAllAsRead() {
      try {
        await markAllNotificationsAsReadApi()
        this.fetchNotifications()
        this.updateUnreadCount(true)
        this.$message.success('已将所有消息标记为已读')
      } catch (error) {
        this.$message.error('操作失败')
      }
    },
    /**
     * 处理消息通知点击
     */
    async handleNotificationClick(notification) {
      if (!notification.isRead) {
        try {
          await markNotificationAsReadApi(notification.id)
          notification.isRead = true
          this.updateUnreadCount()
        } catch (error) {
          this.$message.error('标记已读失败')
        }
      }
    },
    /**
     * 删除消息通知
     */
    async deleteNotification(id) {
      this.$confirm('确定要删除这条消息吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteNotificationApi(id)
          this.updateUnreadCount()
          this.$message.success('删除成功')
        } catch (error) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },
    /**
     * 获取未读消息数量
     */
    async getUnreadNotificationsCount() {
      const { data } = await getUnreadNotificationsCountApi()
      let unreadCount = 0
      this.categories.forEach(category => {
        if (category.type !== 'all') {
          category.unread = data[category.type]?.num || 0
          unreadCount += category.unread
        }
      })
      this.categories.find(c => c.type === 'all').unread = unreadCount
    } 
  },
  created() {
    if(!this.$store.state.userInfo){
      this.$router.push('/login')
      return
    }
    this.fetchNotifications()
    this.getUnreadNotificationsCount()
    }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  消息中心 · 技术台账
//
//  左侧分类是文字列表，当前项用强调色 + 左侧 2px 竖线标记；
//  右侧消息是行式列表，未读用一条强调色竖线区分，不用彩色图标底。
// ════════════════════════════════════════════════════════════════

.notifications-page {
  display: flex;
  gap: clamp(28px, 4vw, 56px);
  max-width: 1400px;
  margin: 0 auto;
  padding: clamp(24px, 4vh, 44px) clamp(20px, 4vw, 48px);
  min-height: calc(100vh - 64px);
  align-items: flex-start;
}

// ── 左侧分类 ──
.notifications-sidebar {
  width: 200px;
  flex-shrink: 0;

  .sidebar-header {
    padding-bottom: 12px;
    margin-bottom: 6px;
    border-bottom: 1px solid var(--rule-strong);

    h2 {
      @include ledger-heading;
      margin: 0;
    }
  }

  .category-item {
    position: relative;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 9px 0 9px 12px;
    color: var(--text-secondary);
    cursor: pointer;
    border-left: 2px solid transparent;
    transition: color 0.2s ease, border-color 0.2s ease;

    i {
      width: 16px;
      text-align: center;
      font-size: 0.9rem;
      color: var(--text-tertiary);
      transition: color 0.2s ease;
    }

    span {
      flex: 1;
      font-size: 0.9rem;
    }

    &:hover {
      color: var(--text-primary);
    }

    &.active {
      color: var(--text-primary);
      border-left-color: var(--accent);

      i {
        color: var(--accent);
      }
    }

    .category-badge {
      margin-left: auto;

      :deep(.el-badge__content) {
        background-color: var(--accent);
        border: none;
        padding: 0 6px;
        height: 16px;
        line-height: 16px;
        border-radius: $border-radius-sm;
        font-family: $font-mono;
        font-size: 11px;
        font-weight: 400;
      }
    }
  }
}

// ── 右侧列表 ──
.notifications-main {
  flex: 1;
  min-width: 0;

  .notifications-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    gap: $spacing-md;
    padding-bottom: 14px;
    margin-bottom: 2px;
    border-bottom: 1px solid var(--rule-strong);

    h1 {
      font-family: $font-serif;
      font-size: 1.35rem;
      font-weight: 600;
      color: var(--text-primary);
      margin: 0;
    }

    .header-actions .el-button {
      color: var(--accent);

      i {
        margin-right: 4px;
      }
    }
  }
}

.notification-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px 4px 16px 16px;
  border-bottom: 1px solid var(--rule);
  transition: background 0.2s ease;

  // 未读：左侧一条强调色竖线，不用整块淡色底
  &.unread::before {
    content: '';
    position: absolute;
    left: 0;
    top: 16px;
    bottom: 16px;
    width: 2px;
    background: var(--accent);
  }

  &:hover {
    background: var(--row-hover);
  }

  .notification-icon {
    width: 20px;
    flex-shrink: 0;
    padding-top: 2px;
    text-align: center;

    i {
      font-size: 0.95rem;
      color: var(--text-tertiary);
    }
  }

  &.unread .notification-icon i {
    color: var(--accent);
  }

  .notification-body {
    flex: 1;
    min-width: 0;

    .notification-title {
      font-size: 0.95rem;
      color: var(--text-primary);
      margin-bottom: 5px;
      font-weight: 500;
    }

    .notification-message {
      font-size: 0.9rem;
      color: var(--text-secondary);
      margin-bottom: 6px;
      line-height: 1.6;

      .article-title {
        color: var(--accent);
        cursor: pointer;

        &:hover {
          text-decoration: underline;
        }
      }
    }

    .notification-time {
      @include ledger-meta;
      margin-top: 6px;
    }
  }

  .notification-actions {
    margin-left: 12px;
    visibility: hidden;

    .el-button {
      padding: 4px;

      i {
        font-size: 0.9rem;
        color: var(--text-tertiary);
      }

      &:hover i {
        color: var(--accent);
      }
    }
  }

  &:hover .notification-actions {
    visibility: visible;
  }
}

.empty-state {
  padding: 56px 20px;
  text-align: center;
  color: var(--text-tertiary);

  i {
    font-size: 1.6rem;
    margin-bottom: 12px;
    opacity: 0.6;
  }

  p {
    @include ledger-meta;
    margin: 0;
  }
}

@media screen and (max-width: 768px) {
  .notifications-page {
    flex-direction: column;
    gap: $spacing-lg;
    padding: 20px $spacing-md;
  }

  .notifications-sidebar {
    width: 100%;

    .category-list {
      display: flex;
      gap: 4px 18px;
      flex-wrap: wrap;
    }

    .category-item {
      padding: 8px 0;
      border-left: none;
      border-bottom: 2px solid transparent;

      &.active {
        border-bottom-color: var(--accent);
      }
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .category-item,
  .notification-item {
    transition: none !important;
  }
}
</style>