<template>
  <aside class="sidebar" :class="{ 'has-notice': announcements.length > 0 }">
    <!-- 作者块：头像 + 昵称 + 签名，下面一条三格站点统计 -->
    <section class="block author-block">
      <div class="author">
        <!--
          头像来自 MinIO 外链：地址为空不渲染、解码完才淡入，取不到仍走
          handleAvatarError（先退游客头像，再彻底隐藏），避免闪破图标
        -->
        <img
          v-if="$store.state.webSiteInfo.authorAvatar"
          class="avatar"
          :src="$store.state.webSiteInfo.authorAvatar"
          :alt="$store.state.webSiteInfo.author"
          :class="{ 'is-painted': avatarPainted }"
          @load="avatarPainted = true"
          @error="handleAvatarError"
        >
        <div class="who">
          <div class="name">{{ $store.state.webSiteInfo.author }}</div>
          <div class="motto">{{ $store.state.webSiteInfo.authorInfo }}</div>
        </div>
      </div>

      <!--
        站点统计回到作者卡：数字跟身份同处一卡，读者不必为「这站多少文章」再往上找一条横带。
        三格居中、无分隔线也无边框，层次只由数字与标签的字号差表达
      -->
      <div class="author-stats">
        <div class="stat">
          <b class="stat-value">{{ articleCountText }}</b>
          <span class="stat-label">文章篇数</span>
        </div>
        <div class="stat">
          <b class="stat-value">{{ readCountText }}</b>
          <span class="stat-label">累计阅读</span>
        </div>
        <div class="stat">
          <b class="stat-value">{{ tagCountText }}</b>
          <span class="stat-label">归类标签</span>
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
      <!--
        置顶与推荐共用一张卡：两组的结构完全一致（等宽日期 + 标题），
        拆成两张卡只是平白多一条边框、一道阴影和 16px 间距
      -->
      <section class="block" v-if="postGroups.length">
        <div class="ledger-group" v-for="group in postGroups" :key="group.title">
          <h4>{{ group.title }}</h4>
          <ul class="recent">
            <li v-for="post in group.posts" :key="post.id">
              <router-link :to="`/post/${post.id}`">
                <span class="d">{{ shortDate(post.createTime) }}</span>
                <span class="t">{{ post.title }}</span>
              </router-link>
            </li>
          </ul>
        </div>
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
import { getArticlesApi, getRecommendArticlesApi } from '@/api/article'
import { getTagsApi } from '@/api/tags'
import { formatCount } from '@/utils/format'
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
      // 后端 /api/article/getRecommends 返回的已发布推荐文（模板里标「推荐」）
      hot: [],
      tags: [],
      announcements: [],
      // 文章总数：接口回来前保持 null，用来区分「没回来」与「真的是 0」
      articleTotal: null,
      // 头像解码完才淡入（见模板注释）
      avatarPainted: false
    }
  },
  computed: {
    // 三态：没回来给「—」，真的 0 由 formatCount 也渲染成「—」（规则见 src/utils/format.js）
    articleCountText() {
      return this.articleTotal === null ? '—' : formatCount(this.articleTotal)
    },
    readCountText() {
      return formatCount(this.$store.state.siteAccess)
    },
    // 复用 mounted 里 getTagsApi 的结果，不再为计数多发一次请求
    tagCountText() {
      return formatCount(this.tags.length)
    },
    /**
     * 侧边栏标签云：丢掉没有文章的标签，按文章数倒序，再按数量上限截断
     * （规则见 src/utils/tags.js）
     */
    visibleTags() {
      return pickDisplayTags(this.tags)
    },
    /**
     * 置顶与推荐合成一张卡里的两组。两组都可能为空（没人置顶 / 没人标记推荐），
     * 空组直接剔掉，否则卡片里会留下一个光秃秃的标题
     */
    postGroups() {
      return [
        { title: '置顶', posts: this.sticks },
        { title: '推荐', posts: this.hot }
      ].filter(group => group.posts.length > 0)
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
      // 后端 getRecommends 走的 getArticlesByCondition(is_recommend) 不带 limit，
      // 标记了多少条就返回多少条；置顶那条有 STICK_LIMIT=5 兜底，这里没有，
      // 不截断的话侧栏会被撑成一长条。取 5 条和置顶保持一致
      this.hot = (res.data || []).slice(0, 5)
    }).catch(() => {
      // 推荐卡拿不到就保持空数组：postGroups 会把空组剔掉，卡片整块不渲染
      this.$message.error('获取推荐文章失败')
    })
    getTagsApi().then(res => {
      this.tags = res.data || []
    }).catch(() => {
      // 标签云与文章计数都读 this.tags，失败时保持空数组，计数显示为「—」
      this.$message.error('获取标签失败')
    })
    // 只要 total，别把整页数据拉回来：pageSize 给 1，载荷压到最小
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
    }
  }
}
</script>

<style lang="scss" scoped>
// ════════════════════════════════════════════════════════════════
//  侧栏 · macOS 小组件群
//
//  作者名片（毛玻璃方块）、便签式公告、置顶/推荐列表、
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
    //鼠标停在侧栏上滚动时别把滚动链传给页面：内层滚到底继续滚，
    //页面会突然接手跳一屏（滚动手感断裂）
    overscroll-behavior: contain;

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
    // 只有淡入不参与 keyframes（那套只动 box-shadow），加载中/失败都不留破图标
    opacity: 0;
    transition: opacity 0.2s ease;

    &.is-painted {
      opacity: 1;
    }
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

// ── 站点统计：三格一排，无分隔线也无边框 ──
.author-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 18px;
  text-align: center;

  .stat-value {
    display: block;
    font-size: 1.35rem;
    font-weight: 700;
    line-height: 1;
    color: var(--text-primary);
    // 数字等宽，三格的小数点与位权才对得齐
    font-variant-numeric: tabular-nums;
  }

  .stat-label {
    display: block;
    margin-top: 6px;
    font-size: 0.75rem;
    color: var(--text-secondary);
  }
}

// ── 公告：便签。接口没有时间字段，头部是图钉 + 「公告」，不是通知横幅的时间戳位 ──
// 去暖洗：暖琥珀整片铺在冷灰纸白上会跟页面底色打架，底与边界回到中性卡片，
// 公告身份只由 .note-head 的图钉 + 琥珀等宽标题承担
.note-block {
  background: var(--card-bg);
  border-color: var(--rule);
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

// ── 置顶 / 推荐：等宽日期 + 标题 ──
// 同一张卡里的第二组顶上补一条发丝线，把两组的行分开；
// .recent li:last-child 没有下边框，正好不跟这条线叠成双线
.ledger-group + .ledger-group {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--rule);
}

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

  .cloud a:hover,
  .recent a:hover {
    transform: none;
  }
}

// <=1024px 时侧边栏只保留公告卡，作者卡 / 置顶 / 推荐 / 标签云整体隐藏
// （置顶由首页的 MobileStickyBar 承接）。
// 公告必须留着：$store.state.notice.right 全站只有侧边栏这一个消费者，
// 整块 display:none 的话后台填的右侧公告在移动端谁都看不到。
// order:-1 把它提到主栏之前 —— 单列布局下侧栏排在 </main> 后面，
// 不提前就会掉到文章列表底下，等于还是没人看得到。
@include responsive(lg) {
  // 没有右侧公告时整块不占位：单列布局里侧栏仍在主栏之前，
  // 空元素会被 grid 排成一行并白吃一个 gap（这里 36-64px）
  .sidebar:not(.has-notice) {
    display: none;
  }

  .sidebar {
    position: static;
    top: auto;
    max-height: none;
    order: -1;
  }

  .author-block,
  .sidebar-scroll {
    display: none;
  }
}
</style>
