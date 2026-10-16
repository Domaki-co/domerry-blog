<template>
  <aside class="sidebar">
    <!-- 作者块：头像 + 昵称 + 签名，然后是仪表小格与社交方块 -->
    <section class="block author-block">
      <div class="author">
        <img
          class="avatar"
          :src="$store.state.webSiteInfo.authorAvatar"
          :alt="$store.state.webSiteInfo.author"
          @error="handleAvatarError"
        >
        <div class="who">
          <div class="name">{{ $store.state.webSiteInfo.author }}</div>
          <div class="motto">{{ $store.state.webSiteInfo.authorInfo }}</div>
        </div>
      </div>

      <!-- macOS 仪表小格：2×2，数字衬线等宽、标签在数字下方居中 -->
      <div class="metrics">
        <div class="metric"><b>{{ visitorCount }}</b><span>访客</span></div>
        <div class="metric"><b>{{ viewCount }}</b><span>浏览</span></div>
        <div class="metric"><b>{{ postCount }}</b><span>文章</span></div>
        <div class="metric"><b>{{ tagCount }}</b><span>标签</span></div>
      </div>
      <div class="social-links">
        <div v-for="item in socialLinksWithData" :key="item.type">
          <!-- v-if 必须挂在 el-tooltip 上：Element Plus 的 ElOnlyChild 要求默认插槽里存在真实元素，
               写在 <a> 上时插槽只剩注释节点，每次渲染都会报 ElementPlusError: [ElOnlyChild] no valid child node found -->
          <el-tooltip v-if="($store.state.webSiteInfo.showList || []).indexOf(item.type) !== -1"
            placement="top" :content="item.content">
            <a href="javascript:void(0)"
                :title="item.title" class="social-btn" @click="copyToClipboard(item)">
                <i :class="item.icon"></i>
            </a>
          </el-tooltip>
        </div>
      </div>
    </section>

    <!--
      公告：macOS 便签质感。/api/getNotice 只返回 content、没有时间字段，
      所以头部是图钉图标 + 「公告」，别照搬系统通知横幅右上角的时间戳位
    -->
    <section class="block note-block" v-if="announcements.length">
      <div class="note-head">
        <i class="fas fa-thumbtack" aria-hidden="true"></i>
        <span>公告</span>
      </div>
      <div class="announcement-content">
        <div class="announcement-item" v-for="(item, index) in announcements" :key="index">
          <span v-html="$sanitize(item.content)"></span>
        </div>
      </div>
    </section>

    <!--
      作者块和公告固定在视口内不参与滚动，推荐/置顶/标签云放进下面这个滚动区，
      否则侧边栏整体高于视口时，末尾的标签云要等页面滚到最底才露出来
    -->
    <div class="sidebar-scroll">
      <section class="block" v-if="sticks.length > 0">
        <h4>置顶</h4>
        <ul class="recent">
          <li v-for="post in sticks" :key="post.id">
            <router-link :to="`/post/${post.id}`">
              <span class="d">{{ shortDate(post.createTime) }}</span>
              <span class="t">{{ post.title }}</span>
            </router-link>
          </li>
        </ul>
      </section>

      <section class="block" v-if="hot.length > 0">
        <h4>近期</h4>
        <ul class="recent">
          <li v-for="post in hot" :key="post.id">
            <router-link :to="`/post/${post.id}`">
              <span class="d">{{ shortDate(post.createTime) }}</span>
              <span class="t">{{ post.title }}</span>
            </router-link>
          </li>
        </ul>
      </section>

      <section class="block">
        <h4>标签</h4>
        <!-- 等高气泡胶囊：字号不再按文章数分档，权重改由胶囊内的计数表达 -->
        <div class="cloud">
          <a
            v-for="tag in visibleTags"
            :key="tag.id"
            href="javascript:void(0)"
            @click="goToTag(tag)"
          >
            {{ tag.name }}<b class="n">{{ tag.articleNum }}</b>
          </a>
        </div>
      </section>
    </div>
  </aside>
</template>

<script>
import { getRecommendArticlesApi, getArticlesApi } from '@/api/article'
import { getTagsApi } from '@/api/tags'
import { pickDisplayTags } from '@/utils/tags'

export default {
  name: 'Sidebar',
  props: {
    //置顶文章由父级（首页）传入，自带请求与刷新时机，卡片只在有置顶时渲染
    sticks: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      hot: [],
      tags: [],
      // 文章总数：只取 total，不拉列表内容
      articleTotal: null,
      announcements: [],
      socialLinks: [
        {
          icon: 'fab fa-github',
          type: 'github',
          content: '点击跳转GitHub主页',
          icCopy: false
        },
        {
          icon: 'fab fa-git-alt',
          type: 'gitee',
          content: '点击跳转GitEE主页',
          icCopy: false
        },
        {
          icon: 'fab fa-qq',
          title: 'QQ',
          type: 'qq',
          content: '点击复制QQ号',
          icCopy: true
        },
        {
          icon: 'fas fa-users',
          title: 'QQ群',
          type: 'qqGroup',
          content: '点击复制QQ群号',
          icCopy: true
        },
        {
          icon: 'fas fa-at',
          title: '邮箱',
          type: 'email',
          content: '点击复制邮箱',
          icCopy: true
        },
        {
          icon: 'fab fa-weixin',
          title: '微信',
          type: 'wechat',
          content: '点击复制微信号',
          icCopy: true
        }
      ]
    }
  },
  computed: {
    socialLinksWithData() {
      return this.socialLinks.map(item => {
        const linkMap = {
          github: this.$store.state.webSiteInfo.github,
          gitee: this.$store.state.webSiteInfo.gitee,
          qq: this.$store.state.webSiteInfo.qqNumber,
          qqGroup: this.$store.state.webSiteInfo.qqGroup,
          email: this.$store.state.webSiteInfo.email,
          wechat: this.$store.state.webSiteInfo.wechat
        }
        return {
          ...item,
          link: linkMap[item.type]
        }
      })
    },
    /**
     * 仪表小格：文章总数由 /api/article/list 的 total 提供，
     * 标签数用已拉到的标签列表长度，访客/浏览量来自 store
     */
    postCount() {
      return this.articleTotal === null ? '—' : formatCount(this.articleTotal)
    },
    tagCount() {
      return this.tags.length ? formatCount(this.tags.length) : '—'
    },
    /**
     * 侧边栏标签云：丢掉没有文章的标签，按文章数倒序，再按数量上限截断
     * （规则见 src/utils/tags.js）。仪表小格里的 tagCount 仍是全量总数。
     */
    visibleTags() {
      return pickDisplayTags(this.tags)
    },
    visitorCount() {
      return formatCount(this.$store.state.visitorAccess)
    },
    viewCount() {
      return formatCount(this.$store.state.siteAccess)
    }
  },
  watch: {
    '$store.state.notice'(val) {
      if (val && val.right) {
        this.announcements = val.right
      }
    }
  },
  mounted() {
    getRecommendArticlesApi().then(res => {
      this.hot = res.data || []
    })
    getTagsApi().then(res => {
      this.tags = res.data || []
    })
    // 版记里的文章总数：只要 total，pageSize 取 1 避免白拉一页数据
    getArticlesApi({ pageNum: 1, pageSize: 1 })
      .then(res => {
        this.articleTotal = res.data?.total ?? 0
      })
      .catch(() => {
        this.articleTotal = 0
      })
    // 初始化公告数据
    if (this.$store.state.notice && this.$store.state.notice.right) {
      this.announcements = this.$store.state.notice.right
    }
  },
  methods: {
    /**
     * 列表里只需要「月-日」，年份在台账里不是关键信息
     */
    shortDate(time) {
      const date = new Date(time)
      if (!time || Number.isNaN(date.getTime())) return ''
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${month}-${day}`
    },
    goToTag(tag) {
      this.$router.push({ path: '/tags', query: { tagId: tag.id, tagName: tag.name } })
    },
    handleAvatarError(e) {
      if (this.$store.state.webSiteInfo.touristAvatar && e.target.src !== this.$store.state.webSiteInfo.touristAvatar) {
        e.target.src = this.$store.state.webSiteInfo.touristAvatar
      } else {
        e.target.style.visibility = 'hidden'
      }
    },
    /**
     * 复制到剪贴板
     */
    copyToClipboard(item) {
      if (item.icCopy) {
        navigator.clipboard.writeText(item.link).then(() => {
          this.$message.success(`${item.title}账号已复制到剪贴板`);
        }).catch(() => {
          this.$message.error('复制失败，请手动复制');
        });
      } else {
        window.open(item.link, '_blank')
      }
    }
  }
}

/**
 * 大数字加千分位，避免仪表小格里出现一长串数字把格子撑开
 */
function formatCount(value) {
  const n = Number(value)
  if (!n) return '—'
  return n.toLocaleString('en-US')
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  侧栏 · macOS 小组件群
//
//  作者名片（仪表小格 + 毛玻璃方块）、便签式公告、置顶/近期列表、
//  等高标签胶囊。每块都是一张卡片 —— --card-bg 底、1px 发丝边、12px 圆角，
//  块间距取小组件之间的 16px 排水槽。
//
//  滚动区 .sidebar-scroll 的 overflow 会裁掉卡片投影的边缘几像素，
//  所以卡片的立体感靠边框而不是投影，别把它加到一眼看得出被切的厚度。
// ════════════════════════════════════════════════════════════════

.sidebar {
  position: sticky;
  top: 88px;
  width: 100%;
  //侧边栏整体高于视口时，sticky 只会把它钉在 top:88px，超出屏幕的部分得等页面滚到最底
  //（sticky 约束解除）才露出来。做法：侧边栏自身做成定高 flex 列 —— 作者块和公告占自然
  //高度、始终可见，下面的 .sidebar-scroll 吃掉剩余高度并在内部滚动
  max-height: calc(100vh - 104px);
  display: flex;
  flex-direction: column;

  //作者块和公告是「固定」的那部分：必须显式 flex:0 0 auto
  //否则 flex 子项默认 flex-shrink:1，侧边栏内容超过 max-height 时它们会跟着 .sidebar-scroll
  //一起被按比例压缩（作者块能被压掉一半以上），表现就是"作者块显示不全"
  > .author-block,
  > .block:not(.author-block) {
    flex: 0 0 auto;
  }

  //滚动区：flex:1 吃掉剩余高度；min-height:0 是必须的，否则 flex 子项不会收缩到
  //内容高度以下，内部就滚不起来
  .sidebar-scroll {
    flex: 1 1 auto;
    min-height: 0;
    overflow-y: auto;
    overflow-x: hidden;

    > :last-child {
      margin-bottom: 0;
    }
  }
}

// ── 卡片：整个侧栏就是一组 macOS 小组件 ──
.block {
  margin-bottom: 16px;
  padding: 16px;
  background: var(--card-bg);
  border: 1px solid var(--rule);
  border-radius: $border-radius-lg;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);

  h4 {
    margin: 0 0 12px;
    font-family: $font-mono;
    font-size: 0.7rem;
    font-weight: 400;
    letter-spacing: 0.16em;
    color: var(--text-tertiary);
  }
}

// ── 作者块 ──
.author {
  display: flex;
  gap: 13px;
  align-items: center;

  // 头像光环：2px 底色留缝 + 一圈琥珀软光，极慢呼吸（对标 macOS 登录头像外圈）
  .avatar {
    width: 56px;
    height: 56px;
    padding: 2px;
    border-radius: 50%;
    object-fit: cover;
    background: var(--card-bg);
    border: 1px solid var(--rule);
    flex: none;
    box-shadow: 0 0 0 3px var(--accent-soft);
    animation: avatar-breathe 4.6s cubic-bezier(0.16, 1, 0.3, 1) infinite;
  }

  .who {
    min-width: 0;

    .name {
      font-family: $font-serif;
      font-size: 1.05rem;
      line-height: 1.3;
      color: var(--text-primary);
    }

    .motto {
      font-size: 0.82rem;
      color: var(--text-secondary);
      margin-top: 3px;
    }
  }
}

// ── 仪表小格：2×2，格子之间靠容器底色透出 1px 发丝线 ──
.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  margin-top: 16px;
  background: var(--rule);
  border: 1px solid var(--rule);
  border-radius: $border-radius-md;
  overflow: hidden;

  .metric {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1px;
    padding: 9px 4px 8px;
    background: var(--card-bg);
    transition: background 0.2s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      background: var(--row-hover);
    }

    // 衬线大字 + 等宽数字位：位数变化时格子不跟着抖
    b {
      font-family: $font-serif;
      font-size: 1.12rem;
      font-weight: 400;
      line-height: 1.25;
      color: var(--text-primary);
      font-variant-numeric: tabular-nums;
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    span {
      font-family: $font-mono;
      font-size: 0.66rem;
      color: var(--text-tertiary);
    }
  }
}

// ── 社交：macOS 控制中心那种方形毛玻璃块 ──
.social-links {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;

  .social-btn {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $border-radius-sm;
    font-size: 0.9rem;
    // 图标统一单色，hover 才点亮成琥珀
    color: var(--text-secondary);
    background: var(--row-hover);
    border: 1px solid var(--rule);
    backdrop-filter: blur(12px) saturate(160%);
    -webkit-backdrop-filter: blur(12px) saturate(160%);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
    transition: color 0.35s cubic-bezier(0.16, 1, 0.3, 1),
                background 0.35s cubic-bezier(0.16, 1, 0.3, 1),
                border-color 0.35s cubic-bezier(0.16, 1, 0.3, 1),
                box-shadow 0.35s cubic-bezier(0.16, 1, 0.3, 1),
                transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
    text-decoration: none;

    &:hover {
      color: var(--accent);
      background: var(--accent-soft);
      border-color: rgba($accent, 0.3);
      box-shadow: 0 2px 6px var(--accent-soft);
      transform: translateY(-1px);
    }

    &:active {
      transform: scale(0.95);
    }
  }
}

// ── 公告：便签。接口没有时间字段，头部是图钉 + 「公告」，不是通知横幅的时间戳位 ──
.note-block {
  background: var(--accent-soft);
  border-color: rgba($accent, 0.22);
  box-shadow: 0 1px 3px rgba(168, 106, 16, 0.06), inset 0 1px 0 rgba(255, 255, 255, 0.6);
}

.note-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  font-family: $font-mono;
  font-size: 0.66rem;
  letter-spacing: 0.16em;
  color: var(--accent);

  i {
    font-size: 0.68rem;
  }
}

.announcement-content {
  font-size: 0.85rem;
  line-height: 1.75;
  color: var(--text-secondary);

  .announcement-item + .announcement-item {
    margin-top: 8px;
  }
}

// ── 近期 / 置顶：等宽日期 + 标题 ──
.recent {
  list-style: none;
  margin: 0;
  padding: 0;

  li {
    border-bottom: 1px solid var(--rule);

    &:last-child {
      border-bottom: 0;
    }
  }

  a {
    display: flex;
    gap: 11px;
    padding: 10px 4px;
    align-items: baseline;
    text-decoration: none;
    border-radius: $border-radius-sm;
    transition: color 0.2s cubic-bezier(0.16, 1, 0.3, 1),
                background 0.2s cubic-bezier(0.16, 1, 0.3, 1),
                transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      background: var(--row-hover);
      transform: translateX(3px);

      .t {
        color: var(--accent);
      }
    }
  }

  .d {
    font-family: $font-mono;
    font-size: 0.7rem;
    color: var(--text-tertiary);
    flex: none;
    padding-top: 2px;
    font-variant-numeric: tabular-nums;
  }

  .t {
    font-size: 0.88rem;
    line-height: 1.5;
    color: var(--text-secondary);
    transition: color 0.2s ease;
  }
}

// ── 标签：等高气泡胶囊（不再按文章数分字号，权重放胶囊里的计数）──
.cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;

  a {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    height: 27px;
    padding: 0 11px;
    border-radius: 999px;
    border: 1px solid var(--rule);
    font-size: 0.8rem;
    line-height: 1;
    color: var(--text-secondary);
    text-decoration: none;
    transition: color 0.35s cubic-bezier(0.16, 1, 0.3, 1),
                background 0.35s cubic-bezier(0.16, 1, 0.3, 1),
                border-color 0.35s cubic-bezier(0.16, 1, 0.3, 1),
                box-shadow 0.35s cubic-bezier(0.16, 1, 0.3, 1),
                transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      color: var(--accent);
      background: var(--accent-soft);
      border-color: rgba($accent, 0.3);
      box-shadow: 0 2px 6px var(--accent-soft);
      transform: translateY(-1px);
    }

    &:active {
      transform: scale(0.96);
    }

    .n {
      font-family: $font-mono;
      font-size: 0.68rem;
      font-weight: 400;
      color: var(--text-tertiary);
      font-variant-numeric: tabular-nums;
    }
  }
}

// 呼吸光环：只动半径、不动透明度，避免整块头像忽明忽暗
@keyframes avatar-breathe {
  0%, 100% { box-shadow: 0 0 0 3px var(--accent-soft); }
  50%      { box-shadow: 0 0 0 5px var(--accent-soft); }
}

// 深色模式适配
:root[data-theme='dark'] {
  .sidebar {
    .block {
      background: var(--card-bg);
      border-color: rgba(255, 255, 255, 0.08);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.08);
    }

    .note-block {
      background: rgba(217, 154, 61, 0.08);
      border-color: rgba(217, 154, 61, 0.25);
      box-shadow: inset 0 1px 0 rgba(217, 154, 61, 0.12);
    }

    .metrics {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.08);

      .metric {
        background: var(--card-bg);

        &:hover {
          background: rgba(255, 255, 255, 0.04);
        }
      }
    }

    .social-links .social-btn {
      background: rgba(255, 255, 255, 0.06);
      border-color: rgba(255, 255, 255, 0.1);

      &:hover {
        background: rgba(217, 154, 61, 0.2);
        border-color: rgba(217, 154, 61, 0.4);
        box-shadow: 0 2px 6px rgba(217, 154, 61, 0.2);
      }
    }

    .cloud a {
      background: rgba(255, 255, 255, 0.04);
      border-color: rgba(255, 255, 255, 0.08);

      &:hover {
        background: rgba(217, 154, 61, 0.18);
        border-color: rgba(217, 154, 61, 0.4);
        box-shadow: 0 2px 6px rgba(217, 154, 61, 0.2);
      }
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .author .avatar {
    animation: none;
  }

  .social-links .social-btn:hover,
  .cloud a:hover,
  .recent a:hover {
    transform: none;
  }
}

// 侧边栏在 <=1024px 时整体隐藏（首页另有 MobileStickyBar 兜底）
@include responsive(lg) {
  .sidebar {
    display: none;
  }
}
</style>
