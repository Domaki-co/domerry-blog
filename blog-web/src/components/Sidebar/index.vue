<template>
  <aside class="sidebar">
    <!-- 作者块：头像 + 昵称 + 签名，然后是等宽的数据行 -->
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

      <!-- 台账取向的「版记」：数字等宽，对齐成两列 -->
      <div class="colophon">
        <div><b>{{ postCount }}</b> 篇文章　<b>{{ tagCount }}</b> 个标签</div>
        <div><b>{{ visitorCount }}</b> 位访客　<b>{{ viewCount }}</b> 次浏览</div>
      </div>
      <div class="social-links">
        <div v-for="item in socialLinksWithData" :key="item.type">
          <!-- v-if 必须挂在 el-tooltip 上：Element Plus 的 ElOnlyChild 要求默认插槽里存在真实元素，
               写在 <a> 上时插槽只剩注释节点，每次渲染都会报 ElementPlusError: [ElOnlyChild] no valid child node found -->
          <el-tooltip v-if="$store.state.webSiteInfo.showList.indexOf(item.type) !== -1"
            placement="top" :content="item.content">
            <a href="javascript:void(0)"
                :title="item.title" class="social-btn" @click="copyToClipboard(item)">
                <i :class="item.icon"></i>
            </a>
          </el-tooltip>
        </div>
      </div>
    </section>

    <!-- 公告 -->
    <section class="block" v-if="announcements.length">
      <h4>公告</h4>
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
        <div class="cloud">
          <a
            v-for="tag in visibleTags"
            :key="tag.id"
            href="javascript:void(0)"
            :class="weightClass(tag.articleNum)"
            @click="goToTag(tag)"
          >
            {{ tag.name }}
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
     * 版记数字：文章总数由 /api/article/list 的 total 提供，
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
     * （规则见 src/utils/tags.js）。版记里的 tagCount 仍是全量总数。
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
    /**
     * 标签云按文章数分 5 级字号
     */
    weightClass(articleNum) {
      const n = Number(articleNum) || 0
      if (n >= 5) return 'w5'
      if (n >= 4) return 'w4'
      if (n >= 3) return 'w3'
      if (n >= 2) return 'w2'
      return 'w1'
    },
    goToTag(tag) {
      this.$router.push({ path: '/tags', query: { tagId: tag.id, tagName: tag.name } })
    },
    handleAvatarError(e) {
      e.target.style.visibility = 'hidden'
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
 * 大数字加千分位，避免版记里出现一长串数字把列撑开
 */
function formatCount(value) {
  const n = Number(value)
  if (!n) return '—'
  return n.toLocaleString('en-US')
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  侧栏 · 技术台账
//
//  没有卡片、没有阴影、没有彩色图标。作者块 + 版记（等宽数字）+
//  近期 + 标签云，块与块之间靠留白和一条 1px 线分隔。
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

.block {
  margin-bottom: 34px;

  h4 {
    margin: 0 0 14px;
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

  .avatar {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    object-fit: cover;
    border: 1px solid var(--rule);
    flex: none;
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

.colophon {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--rule);
  font-family: $font-mono;
  font-size: 0.73rem;
  color: var(--text-tertiary);
  line-height: 2;
  font-variant-numeric: tabular-nums;

  b {
    font-weight: 400;
    color: var(--text-secondary);
  }
}

.social-links {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 14px;

  .social-btn {
    width: 30px;
    height: 30px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $border-radius-sm;
    font-size: 0.95rem;
    // 图标统一单色，hover 才点亮成琥珀
    color: var(--text-tertiary);
    transition: color 0.2s ease, background 0.2s ease;
    text-decoration: none;

    &:hover {
      color: var(--accent);
      background: var(--row-hover);
    }
  }
}

// ── 公告 ──
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
    padding: 10px 0;
    align-items: baseline;
    text-decoration: none;
    transition: color 0.2s ease;

    &:hover .t {
      color: var(--accent);
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

// ── 标签云：按权重分 5 级字号 ──
.cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  align-items: baseline;

  a {
    color: var(--text-secondary);
    text-decoration: none;
    transition: color 0.2s ease;

    &:hover {
      color: var(--accent);
    }
  }

  .w5 { font-size: 1.02rem; }
  .w4 { font-size: 0.94rem; }
  .w3 { font-size: 0.87rem; }
  .w2 { font-size: 0.82rem; }
  .w1 {
    font-size: 0.78rem;
    color: var(--text-tertiary);
  }
}

// 侧边栏在 <=1024px 时整体隐藏（首页另有 MobileStickyBar 兜底）
@include responsive(lg) {
  .sidebar {
    display: none;
  }
}
</style>
