<template>
  <div class="article-page" v-loading="loading">

    <!-- 添加固定操作栏 -->
<!--    <div class="floating-action-bar" :style="{ left: actionBarLeft }">-->
<!--      <el-tooltip class="item" effect="dark" content="点赞" placement="top-start">-->
<!--        <div class="action-item" @click="toggleLike">-->
<!--          <el-badge :value="article.likeNum || 0" class="item">-->
<!--            <div class="action-button">-->
<!--              <i class="fas fa-thumbs-up" :class="{ active: article.isLike }"></i>-->
<!--            </div>-->
<!--          </el-badge>-->
<!--        </div>-->
<!--      </el-tooltip>-->
<!--      <el-tooltip class="item" effect="dark" content="收藏" placement="top-start">-->
<!--        <div class="action-item" @click="toggleFavorite">-->
<!--          <el-badge :value="article.favoriteNum || 0" class="item">-->
<!--            <div class="action-button">-->
<!--              <i class="fas fa-star" :class="{ active: article.isFavorite }"></i>-->
<!--            </div>-->
<!--          </el-badge>-->
<!--        </div>-->
<!--      </el-tooltip>-->
<!--      <el-tooltip class="item" effect="dark" content="沉浸式浏览" placement="top-start">-->
<!--        <div class="action-item" @click="toggleSidebar">-->
<!--          <div class="action-button">-->
<!--            <i class="fas fa-expand"></i>-->
<!--          </div>-->
<!--        </div>-->
<!--      </el-tooltip>-->
<!--      <div class="action-item reward-item">-->
<!--        <div class="action-button">-->
<!--          <i class="fas fa-yen-sign"></i>-->
<!--        </div>-->
<!--        <div class="reward-popup">-->
<!--          <div class="reward-content">-->
<!--            <img v-lazy="$store.state.webSiteInfo.weixinPay" alt="微信打赏" class="reward-qr">-->
<!--            <img v-lazy="$store.state.webSiteInfo.aliPay" alt="支付宝打赏" class="reward-qr">-->
<!--          </div>-->
<!--          <div class="reward-text">扫一扫，请我喝杯咖啡</div>-->
<!--        </div>-->
<!--      </div>-->
<!--      <el-tooltip class="item" effect="dark" content="评论" placement="top-start">-->
<!--        <div class="action-item" @click="scrollToComments">-->
<!--          <el-badge :value="article.commentNum || 0" class="item">-->
<!--            <div class="action-button">-->
<!--              <i class="fas fa-comment"></i>-->
<!--            </div>-->
<!--          </el-badge>-->
<!--        </div>-->
<!--      </el-tooltip>-->
<!--    </div>-->

    <div v-if="noPermission" class="no-permission">
      <el-empty description="该内容仅作者可见"></el-empty>
    </div>

    <div v-else-if="loadError" class="load-error">
      <el-empty description="加载失败">
        <el-button type="primary" @click="getArticle">点击重试</el-button>
      </el-empty>
    </div>

    <div class="content-layout" id="articleBox" :class="{ center: !showSidebar }" v-else>
      <main class="article-main">
        <!-- 文章头部 -->
        <header class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>

          <!-- 作者信息和元数据 -->
          <div class="article-info">
            <div class="author-info">
              <img v-lazy="article.avatar" alt="作者头像" class="author-avatar">
              <div class="author-meta">
                <span class="author-name">{{ article.nickname }}</span>
                <div class="post-meta">
                  <time class="publish-time">
                    <i class="far fa-calendar-alt"></i>
                    {{ article.createTime }}
                  </time>
                  <span class="meta-divider">·</span>
                  <span class="category">
                    <i class="fas fa-folder"></i>
                    {{ article.category.name }}
                  </span>
                </div>
              </div>
            </div>

            <div class="article-stats">
              <div class="stat-item">
                <i class="far fa-eye"></i>
                <span>{{ article.quantity }} 阅读</span>
              </div>
              <div class="stat-item">
                <i class="far fa-clock"></i>
                <span>{{ readTime }} 分钟</span>
              </div>
              <div class="stat-item">
                <i class="far fa-comment"></i>
                <span>{{ article.commentNum || 0 }} 评论</span>
              </div>
              <!-- 作者本人可随时进入编辑 -->
              <div class="stat-item edit-item" v-if="canEdit" @click="editArticle" title="编辑这篇文章">
                <i class="fas fa-edit"></i>
                <span>编辑</span>
              </div>
            </div>
          </div>
        </header>

        <!-- 转载声明：仅转载文章显示 -->
        <div class="reprint-notice" v-if="article.isOriginal === 0 && sourceDomain">
          <i class="fas fa-share-square"></i>
          本文转载自
          <a :href="article.originalUrl" target="_blank" rel="noopener nofollow">{{ sourceDomain }}</a>
          ，版权归原文作者所有。如有侵权，请联系站长删除。
        </div>

        <!-- AI简短介绍 -->
        <div v-if="article.aiDescribe" class="ai-description">
          <div class="ai-header" @click="isAiDescriptionExpanded = !isAiDescriptionExpanded">
            <i class="fas fa-robot"></i>
            <span>AI 摘要</span>
            <i class="fas" :class="isAiDescriptionExpanded ? 'fa-chevron-up' : 'fa-chevron-down'" style="margin-left:auto;"></i>
          </div>
          <transition
                name="expand"
                @enter="startTransition"
                @leave="endTransition"
                mode="out-in"
          >
            <div class="ai-content" v-show="isAiDescriptionExpanded">
              <span class="typing-text" ref="typingText"></span>
              <div class="ai-description-text">
                摘要由平台通过智能技术生成
              </div>
            </div>
          </transition>
        </div>

        <!-- 文章内容 -->
        <article class="article-content">
          <!-- 免费内容 -->
          <div v-if="article.readType === 1 || article.readType === 0" v-html="$sanitize(article.content)"></div>

          <!-- 会员内容 -->
          <div v-else-if="article.readType === 2" class="locked-content member">
            <div class="preview-content" v-html="getPreviewContent(article.content)"></div>
            <div class="content-locker">
              <div class="locker-icon">
                <i class="fas fa-crown"></i>
              </div>
              <h3>会员专享内容</h3>
              <p>成为会员即可阅读全文</p>
              <el-button type="primary" @click="handleUpgrade">立即开通会员</el-button>
            </div>
          </div>

          <!-- 付费内容 -->
          <div v-else-if="article.readType === 3" class="locked-content paid">
            <div class="preview-content" v-html="getPreviewContent(article.content)"></div>
            <div class="content-locker">
              <div class="locker-icon">
                <i class="fas fa-lock"></i>
              </div>
              <h3>付费阅读</h3>
              <p>支付 1 元即可阅读全文</p>
              <el-button type="primary" @click="handlePurchase">立即购买</el-button>
            </div>
          </div>
        </article>

        <!-- 文章底部 -->
        <footer class="article-footer">
          <!-- 版权声明提示 -->
          <div class="copyright-notice">
            <div class="notice-content">
              <div class="notice-item">
                <span>
                  博客内容遵循：<a href="https://creativecommons.org/licenses/by-nc-sa/4.0/" target="_blank" rel="noopener noreferrer">署名-非商业性使用-相同方式共享 4.0 国际 (CC BY-NC-SA 4.0) 协议</a>
                </span>
              </div>
              <div class="notice-item">
                <span>本文永久链接是：<a :href="currentUrl" target="_blank" rel="noopener noreferrer">{{ currentUrl }}</a></span>
              </div>
            </div>
          </div>

          <!-- 标签部分保持不变 -->
          <div class="tags-section">
            <i class="fas fa-tags"></i>
            <div class="tags-list">
              <router-link v-for="tag in article.tags" :key="tag.id" :to="`/tags?tagId=${tag.id}&tagName=${tag.name}`"
                class="tag-item">
                {{ tag.name }}
              </router-link>
            </div>
          </div>

          <!-- 修改操作按钮部分 -->
          <div class="article-actions">
            <button class="action-btn like" :class="{ active: article.isLike }" @click="toggleLike">
              <i class="fas fa-heart"></i>
              <span>{{ article.likeNum }}</span>
            </button>
            <div class="share-dropdown" v-click-outside="closeShareMenu">
              <button class="action-btn share" @click="toggleShareMenu">
                <i class="fas fa-share-alt"></i>
                分享
              </button>
              <div class="share-menu" v-show="showShareMenu">
                <button class="share-item" @click="shareToQQ">
                  <i class="fab fa-qq"></i>
                  QQ好友
                </button>
                <button class="share-item" @click="shareToQzone">
                  <i class="fas fa-star"></i>
                  QQ空间
                </button>
                <button class="share-item" @click="shareToWeibo">
                  <i class="fab fa-weibo"></i>
                  微博
                </button>
                <button class="share-item" @click="shareToWechat">
                  <i class="fab fa-weixin"></i>
                  微信
                </button>
                <button class="share-item" @click="copyLink">
                  <i class="fas fa-link"></i>
                  复制链接
                </button>
              </div>
            </div>
          </div>

          <!-- 导航部分保持不变 -->
          <!-- <nav class="article-nav">
            <div
              v-if="prevArticle"
              class="nav-item prev"
              @click="goToArticle(prevArticle.id)"
            >
              <i class="fas fa-arrow-left"></i>
              <div class="nav-content">
                <span class="label">上一篇</span>
                <span class="title">{{ prevArticle.title }}</span>
              </div>
            </div>
            <div
              v-if="nextArticle"
              class="nav-item next"
              @click="goToArticle(nextArticle.id)"
            >
              <div class="nav-content">
                <span class="label">下一篇</span>
                <span class="title">{{ nextArticle.title }}</span>
              </div>
              <i class="fas fa-arrow-right"></i>
            </div>
          </nav> -->
        </footer>

        <!-- 添加评论组件 -->
        <Comment
          :article-id="$route.params.id"
          :comment-count="article.commentNum || 0"
          :article-author-id="article.userId || ''"
          @comment-added="handleCommentAdded"
          @comment-deleted="handleCommentDeleted"
        />
      </main>

      <!-- 侧边栏 -->
      <aside v-if="showSidebar" class="article-sidebar desktop-only">
        <div class="toc-container">
          <div class="toc-header">
            <div class="title-wrapper">
            <i class="fas fa-list"></i>
            <span>目录</span>
            </div>
            <div class="header-actions">
              <div class="progress-wrapper" :class="{ completed: readProgress === 100 }">
                <i class="fas fa-book-reader"></i>
                <span class="progress-text">{{ readProgress }}</span>
              </div>
              <button class="toc-toggle" type="button" title="收起目录" aria-label="收起目录" @click="toggleSidebar">
                <el-icon><DArrowRight /></el-icon>
              </button>
            </div>
          </div>
          <div class="toc-content">
            <div
              v-for="item in tocItems"
              :key="item.id"
              class="toc-item"
              :class="{
              'active': activeHeading === item.id,
              [`depth-${item.depth || 0}`]: true,
              [`level-${item.level}`]: true
            }" @click="scrollToHeading(item.id)">
              <span class="toc-item-text">{{ item.text }}</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- 目录收起后的展开入口 -->
      <button
        v-if="!showSidebar"
        class="toc-restore desktop-only"
        type="button"
        title="展开目录"
        @click="toggleSidebar"
      >
        <el-icon><DArrowLeft /></el-icon>
        <span>目录</span>
      </button>
    </div>
    <mj-image-preview ref="imagePreview" />
    <payment-dialog
      v-model="showPaymentDialog"
      :title="article.title"
      :price="1"
      :article-id="$route.params.id"
      @payment-success="handlePaymentSuccess"
    />
    <membership-dialog
      v-model="showMembershipDialog"
      @payment-success="handleMembershipSuccess"
    />
    <el-dialog
      title="打开外部链接"
      v-model="externalLinkDialogVisible"
      width="520px"
      class="external-link-dialog"
      :close-on-click-modal="false"
      :before-close="cancelExternalLink"
    >
      <div class="external-link-dialog-content">
        <p>该链接指向本站以外的网站，我们不对外部网站的内容负责。</p>
        <p>您将前往以下网站：</p>
        <div class="external-link-url" :title="externalLinkUrl">{{ externalLinkUrl }}</div>
      </div>
      <template #footer>
        <div class="dialog-footer">
        <el-button type="primary" @click="confirmExternalLink">
          <i class="fas fa-external-link-alt"></i>
          继续访问
        </el-button>
        <el-button @click="cancelExternalLink">取消</el-button>
      </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getArticleDetailApi, likeArticleApi } from '@/api/article'
// highlight.js 全量包解压后 1.6MB（384 种语言），换成 core + 按需注册博客里会出现的语言。
// 未注册的语言 hljs 会自动退回纯文本，样式不变；需要新语言在 LANGUAGES 里加一行即可。
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import java from 'highlight.js/lib/languages/java'
import kotlin from 'highlight.js/lib/languages/kotlin'
import groovy from 'highlight.js/lib/languages/groovy'
import scala from 'highlight.js/lib/languages/scala'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import scss from 'highlight.js/lib/languages/scss'
import less from 'highlight.js/lib/languages/less'
import json from 'highlight.js/lib/languages/json'
import yaml from 'highlight.js/lib/languages/yaml'
import ini from 'highlight.js/lib/languages/ini'
import bash from 'highlight.js/lib/languages/bash'
import shell from 'highlight.js/lib/languages/shell'
import powershell from 'highlight.js/lib/languages/powershell'
import dockerfile from 'highlight.js/lib/languages/dockerfile'
import nginx from 'highlight.js/lib/languages/nginx'
import sql from 'highlight.js/lib/languages/sql'
import python from 'highlight.js/lib/languages/python'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import c from 'highlight.js/lib/languages/c'
import cpp from 'highlight.js/lib/languages/cpp'
import csharp from 'highlight.js/lib/languages/csharp'
import php from 'highlight.js/lib/languages/php'
import ruby from 'highlight.js/lib/languages/ruby'
import swift from 'highlight.js/lib/languages/swift'
import dart from 'highlight.js/lib/languages/dart'
import lua from 'highlight.js/lib/languages/lua'
import diff from 'highlight.js/lib/languages/diff'
import markdown from 'highlight.js/lib/languages/markdown'
import plaintext from 'highlight.js/lib/languages/plaintext'
import 'highlight.js/styles/atom-one-dark.css'

const LANGUAGES = {
  javascript,
  typescript,
  java,
  kotlin,
  groovy,
  scala,
  xml,
  css,
  scss,
  less,
  json,
  yaml,
  ini,
  bash,
  shell,
  powershell,
  dockerfile,
  nginx,
  sql,
  python,
  go,
  rust,
  c,
  cpp,
  csharp,
  php,
  ruby,
  swift,
  dart,
  lua,
  diff,
  markdown,
  plaintext
}

Object.keys(LANGUAGES).forEach((name) => {
  hljs.registerLanguage(name, LANGUAGES[name])
})
import Comment from '@/components/Comment/index.vue'
import PaymentDialog from '@/components/PaymentDialog/index.vue'
import MembershipDialog from '@/components/MembershipDialog/index.vue'
import { marked } from 'marked'
import { escapeHtml } from '@/utils/sanitize'

export default {
  name: 'Article',
  components: {
    Comment,
    PaymentDialog,
    MembershipDialog
  },
  data() {
    return {
      noPermission: false,
      // 非权限类错误（网络超时 / 500）走可重试的错误态，不能冒充"仅作者可见"
      loadError: false,
      article: {
        title: '',
        category: {},
        isOriginal: true,
        readType: 1,
        price: 0,
        likeNum: 0,
      },
      prevArticle: {
        id: 1,
        title: '默认文章',
      },
      nextArticle: {
        id: 1,
        title: '默认文章',
      },
      tocItems: [],
      readProgress: 0,
      showShareMenu: false,
      activeHeading: '',
      readTime: 0,
      likeDebounce: false,
      // 防抖定时器的 id：不存下来就没法在组件卸载时清掉，
      // 回调闭包会一直持有 this（整篇文章的数据）直到 5 秒后触发
      likeDebounceTimer: null,
      loading: false,
      actionBarLeft: '0px',
      showSidebar: true,
      contentLayout: 'center',
      collapsedCodeBlocks: new Set(),
      images: [],
      imageObserver: null,
      initTimer: null,
      imageObserverTimer: null,
      showPaymentDialog: false,
      showMembershipDialog: false,
      isAiDescriptionExpanded: true,
      externalLinkDialogVisible: false,
      externalLinkUrl: '',
    }
  },
  computed: {
    currentUrl() {
      // 永久链接只取 origin + pathname：query 里可能带敏感参数，
      // 而它会被拼进第三方分享/二维码接口，不能外传
      return window.location.origin + window.location.pathname
    },
    /**
     * 当前登录用户是否为文章作者（作者本人可在详情页直接编辑）
     */
    canEdit() {
      const userInfo = this.$store.state.userInfo
      return !!userInfo && this.article.userId != null
        && Number(userInfo.id) === Number(this.article.userId)
    },
    /**
     * 转载来源的域名，用于署名区展示
     */
    sourceDomain() {
      try {
        const url = new URL(this.article.originalUrl)
        // 只认 http(s)：javascript://x.com/... 也能被 URL 解析出 hostname，
        // 直接展示会渲染出一个看似合法的来源域名
        return ['http:', 'https:'].includes(url.protocol) ? url.hostname : ''
      } catch (e) {
        return ''
      }
    }
  },
  methods: {
    /**
     * 获取文章详情
     */
    async getArticle() {
      this.loading = true
      this.noPermission = false
      this.loadError = false
      // 换文章（或重试）前先清掉上一篇的观察器与定时器
      this.resetDomState()
      // 这里不再 hljs.configure({ ignoreUnescapedHTML: true })：那条配置会吞掉
      // 「代码块内存在未转义 HTML」的告警，而该告警正是「内容被当作真实标签解析」
      // 的信号。正文渲染前已经过 $sanitize（模板里 v-html="$sanitize(article.content)"），
      // 所以默认告警即使偶发也可以接受——宁可看到噪声，也不要丢掉这个信号。
      try {
        const res = await getArticleDetailApi(this.$route.params.id)
        this.article = {
          ...res.data,
          content: res.data.content ? this.addLazyLoadToImages(res.data.content) : ''
        }

        // SEO：详情页使用文章自身的标题和摘要，避免所有文章共用同一个 title
        document.title = `${this.article.title} - doma's blog`
        const summary = this.article.summary || this.article.aiDescribe || ''
        document.querySelector('meta[name="description"]')
          ?.setAttribute('content', summary.length > 150 ? summary.slice(0, 150) : summary)

        // 等待下一个 tick，确保文章内容渲染完成
        await this.$nextTick()

        // 使用 setTimeout 确保 DOM 完全渲染。
        // DOM 助手只在这里调用一次：mounted 里再调一遍会让复制按钮/行号/折叠按钮重复处理
        this.initTimer = setTimeout(() => {
          this.initTimer = null
          this.generateToc()
          document.querySelectorAll('pre code').forEach((block) => {
            hljs.highlightElement(block)
          })
          this.addCodeLangLabels()
          this.addCopyButtons()
          this.addLineNumbers()
          this.initImagePreview()
          this.updateActionBarPosition()

          // 添加一个额外的延时来处理代码块的展开/折叠
          this.initializeCodeBlocks()

          // AI摘要
          if (this.article.aiDescribe) {
            const typingText = this.$refs.typingText
            if (!typingText) return
            // 使用marked解析Markdown文本
            const htmlContent = this.$sanitize(marked(this.article.aiDescribe || ''))
            typingText.innerHTML = htmlContent
          }
        }, 100)

        // 计算阅读时间
        const textContent = this.article.content.replace(/<[^>]+>/g, ' ')
        this.readTime = Math.ceil(textContent.split(/\s+/).length / 300)

      } catch (error) {
        const message = error?.message || ''
        // 只有明确的权限错误才展示"仅作者可见"：403 是后端 NotPermissionException，
        // 而私密文章用的是 ServiceException("该内容仅作者可见")，落在 code 500 上，
        // 所以文案命中也要认；网络超时、500、404 一律走可重试的错误态
        if (error?.code === 403 || /仅作者可见|无权限|未登录/.test(message)) {
          this.noPermission = true
        } else {
          this.loadError = true
        }
        this.$message.error(message || '获取文章详情失败')
      } finally {
        this.loading = false
      }
    },
    /**
     * 编辑私密日记（进入编辑器，保存后回到本页）
     */
    editArticle() {
      this.$router.push(`/editor?id=${this.article.id}`)
    },
    /**
     * 为文章内容中的图片添加懒加载
     */
    addLazyLoadToImages(content) {
      const loadingImage = this.getLoadingImage()
      // 逐个 <img> 重建属性：原实现无条件再拼一个 class="lazy-image"，
      // 遇到本来就有 class 的图片会产出重复属性，浏览器只认第一个，lazy-image 就失效了
      return content.replace(/<img\b([^>]*)>/gi, (tag, attrText) => {
        const attrs = attrText.match(/([^\s=/>]+)(?:\s*=\s*("[^"]*"|'[^']*'|[^\s"'>]+))?/g) || []
        const kept = []
        let src = ''
        let hasClass = false

        attrs.forEach((attr) => {
          const eq = attr.indexOf('=')
          const name = (eq === -1 ? attr : attr.slice(0, eq)).toLowerCase()
          const value = eq === -1 ? '' : attr.slice(eq + 1).trim().replace(/^["']|["']$/g, '')

          if (name === 'src') {
            src = value
          } else if (name === 'data-src') {
            // 正文里可能已经带了 data-src（编辑器粘贴的 HTML），
            // 保留会拼出两个同名属性，下面统一用真实地址重建
            return
          } else if (name === 'class') {
            kept.push('class="' + (value ? value + ' lazy-image' : 'lazy-image') + '"')
            hasClass = true
          } else {
            kept.push(attr)
          }
        })

        // 没有 src 的图片原样返回，别把占位图塞进一个本来就没图的标签
        if (!src) {
          return tag
        }

        if (!hasClass) {
          kept.push('class="lazy-image"')
        }
        // 真实地址挪到 data-src，src 换成占位图，由 IntersectionObserver 换回来
        kept.push('src="' + loadingImage + '"', 'data-src="' + src + '"')
        return '<img ' + kept.join(' ') + '>'
      })
    },
    /**
     * 获取加载中的图片
     */
    getLoadingImage() {
      return '/static/img/lazy.gif'
    },
    /**
     * 生成目录
     */
    generateToc() {
      const headings = document.querySelectorAll('.article-content h1,.article-content h2,.article-content h3,.article-content h4,.article-content h5,.article-content h6')
      const tocItems = Array.from(headings).map(heading => {
        const id = heading.textContent.trim().toLowerCase().replace(/\s+/g, '-')
        heading.id = id
        return {
          id,
          text: heading.textContent,
          level: parseInt(heading.tagName.charAt(1))
        }
      })
      this.tocItems = this.flattenTocTree(this.buildTocTree(tocItems))
    },
    /**
     * 构建目录层级结构
     */
    buildTocTree(items) {
      const result = []
      const stack = []
      items.forEach(item => {
        const level = item.level
        while (stack.length && stack[stack.length - 1].level >= level) stack.pop()

        const node = { ...item, children: [] }
        if (stack.length) stack[stack.length - 1].children.push(node)
        else result.push(node)
        stack.push(node)
      })

      return result
    },
    /**
     * 展开目录层级数据
     */
    flattenTocTree(nodes, depth = 0) {
      return nodes.reduce((items, node) => {
        const children = this.flattenTocTree(node.children, depth + 1)
        items.push({ ...node, depth })

        return [...items, ...children]
      }, [])
    },
    /**
     * 点赞
     */
    toggleLike() {
      //防止频繁点击 设置一个5秒的防抖
      if (this.likeDebounce) {
        this.$message.warning('请于 5 秒后再试')
        return
      }
      // 防抖必须在请求发出前置位：放在成功回调里的话，请求失败时防抖形同虚设，可以无限连点
      this.likeDebounce = true
      // 实现点赞功能
      likeArticleApi(this.$route.params.id).then(res => {
        if (this.article.isLike) {
          this.article.likeNum--
        } else {
          this.article.likeNum++
        }
        this.$message.success(this.article.isLike ? '取消点赞成功' : '点赞成功')
        this.article.isLike = !this.article.isLike
      }).catch(error => {
        // 失败也要给提示，否则是未处理的 rejection
        this.$message.error(error?.message || '操作失败，请稍后重试')
      }).finally(() => {
        // 设置一个5秒的防抖
        this.scheduleLikeDebounceReset()
      })
    },
    /**
     * 分享
     */
    toggleShareMenu() {
      this.showShareMenu = !this.showShareMenu
    },
    /**
     * 关闭分享
     */
    closeShareMenu() {
      this.showShareMenu = false
    },
    /**
     * 分享到QQ
     */
    shareToQQ() {
      const url = encodeURIComponent(this.currentUrl)
      const title = encodeURIComponent(this.article.title)
      const summary = encodeURIComponent(this.article.summary || '')
      const pic = encodeURIComponent(this.article.avatar || '')
      window.open(
        `https://connect.qq.com/widget/shareqq/index.html?url=${url}&title=${title}&summary=${summary}&pics=${pic}`,
        "renren-share", "width=490,height=700");
      this.closeShareMenu()
    },
    /**
     * 分享到QQ空间
     */
    shareToQzone() {
      const url = encodeURIComponent(this.currentUrl)
      const title = encodeURIComponent(this.article.title)
      const summary = encodeURIComponent(this.article.summary || '')
      const pic = encodeURIComponent(this.article.avatar || '')
      window.open(
        `https://sns.qzone.qq.com/cgi-bin/qzshare/cgi_qzshare_onekey?url=${url}&title=${title}&summary=${summary}&pics=${pic}`,
        "renren-share", "width=490,height=700"
      )

      this.closeShareMenu()
    },
    /**
     * 分享到微博
     */
    shareToWeibo() {
      const url = encodeURIComponent(this.currentUrl)
      const title = encodeURIComponent(`${this.article.title} - doma's blog`)
      window.open(
        `http://service.weibo.com/share/share.php?url=${url}&title=${title}`,
        "renren-share", "width=490,height=700")
      this.closeShareMenu()
    },
    /**
     * 分享到微信
     */
    shareToWechat() {
      // 由于微信分享需要使用微信SDK，这样简单处理
      window.open(
        `https://api.pwmqr.com/qrcode/create/?url=${encodeURIComponent(this.currentUrl)}`,
        "renren-share", "width=490,height=700");
      this.closeShareMenu()
    },
    /**
     * 复制链接
     */
    async copyLink() {
      try {
        await navigator.clipboard.writeText(this.currentUrl)
        this.$message.success('链接已复制到剪贴板')
      } catch (err) {
        this.$message.error('复制失败，请手动复制')
      }
      this.closeShareMenu()
    },
    /**
     * 判断链接是否为当前站点以外的 HTTP(S) 链接
     */
    isExternalLink(href) {
      if (!href || /^(#|mailto:|tel:|javascript:)/i.test(href)) {
        return false
      }

      try {
        const url = new URL(href, window.location.href)
        return ['http:', 'https:'].includes(url.protocol) && url.origin !== window.location.origin
      } catch (error) {
        return false
      }
    },
    /**
     * 拦截文章详情页中的外部链接
     */
    handleExternalLinkClick(event) {
      const link = event.target.closest && event.target.closest('a')
      if (!link || !this.$el.contains(link)) {
        return
      }

      const href = link.getAttribute('href')
      if (!href) {
        return
      }

      // 伪协议必须在这里拦死：isExternalLink 对它们返回 false，直接 return 就是放行默认行为，
      // 浏览器会照常执行 javascript: 里的脚本。协议名中间可以插空白/控制字符（如 "java\tscript:"），
      // 所以先剥掉这些字符再判断；mailto:/tel:/相对路径/# 锚点不在拦截范围内，保持原行为。
      const normalizedHref = href.replace(/[\u0000-\u0020]/g, '').toLowerCase()
      if (/^(javascript|data|vbscript|file|blob):/.test(normalizedHref)) {
        event.preventDefault()
        event.stopPropagation()
        this.$message.warning('该链接使用了不安全的协议，已阻止打开')
        return
      }

      if (!this.isExternalLink(href)) {
        return
      }

      event.preventDefault()
      event.stopPropagation()
      this.externalLinkUrl = new URL(href, window.location.href).href
      this.externalLinkDialogVisible = true
    },
    /**
     * 确认打开外部链接
     */
    confirmExternalLink() {
      const url = this.externalLinkUrl
      this.cancelExternalLink()

      if (!url) {
        return
      }

      window.open(url, '_blank', 'noopener,noreferrer')
    },
    /**
     * 取消打开外部链接
     */
    cancelExternalLink(done) {
      this.externalLinkDialogVisible = false
      this.externalLinkUrl = ''
      if (typeof done === 'function') {
        done()
      }
    },
    /**
     * 跳转到文章
     */
    goToArticle(id) {
      this.$router.push(`/article/${id}`)
    },
    /**
     * 更新阅读进度
     */
    handleScroll() {
      const docEl = document.documentElement
      const winHeight = window.innerHeight
      const docHeight = docEl.scrollHeight - winHeight
      const scrollTop = window.scrollY || docEl.scrollTop
      // 短页面 docHeight 为 0，除法会得到 NaN
      this.readProgress = docHeight > 0 ? Math.min(100, Math.round((scrollTop / docHeight) * 100)) : 0
    },
    /**
     * 滚动到标题
     */
    scrollToHeading(id) {
      const element = document.getElementById(id)
      if (element) {
        const header = document.querySelector('.site-header')
        const headerHeight = header ? header.offsetHeight : 0
        const targetPosition = element.offsetTop - headerHeight - 20

        window.scrollTo({
          top: targetPosition,
          behavior: 'smooth'
        })
      }
    },
    /**
     * 更新激活标题
     */
    updateActiveHeading() {
      this.handleScroll()
      const headings = this.tocItems.map(item => document.getElementById(item.id))
      const header = document.querySelector('.site-header')
      const headerHeight = header ? header.offsetHeight : 0

      for (let i = headings.length - 1; i >= 0; i--) {
        const heading = headings[i]
        if (heading && heading.getBoundingClientRect().top <= headerHeight + 100) {
          this.activeHeading = heading.id
          break
        }
      }
    },
    /**
     * 把代码块语言名写到 pre 的 data-lang 上，供标题栏显示。
     * 必须在 hljs.highlightElement 之后调用：语言类名（language-xxx）由它写入，
     * 之前调用拿不到任何语言信息。
     */
    addCodeLangLabels() {
      const blocks = document.querySelectorAll('.article-content pre code')
      blocks.forEach((code) => {
        const pre = code.parentElement
        // hljs 会把 language-xxx 与 hljs 一起挂在 code 的 class 上
        const langClass = Array.from(code.classList).find((c) => c.startsWith('language-'))
        if (!langClass) return
        const lang = langClass.replace('language-', '')
        // 无语言或 hljs 未能识别时是 plaintext，标题栏留空即可
        if (!lang || lang === 'plaintext' || lang === 'nohighlight') return
        pre.setAttribute('data-lang', lang)
      })
    },
    /**
     * 添加复制按钮
     */
    addCopyButtons() {
      const codeBlocks = document.querySelectorAll('.article-content pre')
      if (!codeBlocks.length) return

      codeBlocks.forEach(pre => {
        // 检查是否已经添加过复制按钮
        if (pre.querySelector('.code-header')) return

        // 创建复制按钮容器
        const buttonWrapper = document.createElement('div')
        buttonWrapper.className = 'code-header'

        // 语言名（由 addCodeLangLabels 写在 pre 的 data-lang 上，无则留空占位）
        const langLabel = document.createElement('span')
        langLabel.className = 'code-lang'
        langLabel.textContent = pre.getAttribute('data-lang') || ''
        buttonWrapper.appendChild(langLabel)

        // 创建复制按钮
        const copyButton = document.createElement('button')
        copyButton.className = 'copy-button'
        copyButton.innerHTML = '<i class="fas fa-copy"></i> 复制'
        copyButton.title = '复制代码'

        // 添加点击事件
        copyButton.addEventListener('click', async () => {
          try {
            const code = pre.querySelector('code')
            await navigator.clipboard.writeText(code.textContent)
            copyButton.innerHTML = '<i class="fas fa-check"></i> 已复制'
            copyButton.classList.add('copied')
            setTimeout(() => {
              copyButton.innerHTML = '<i class="fas fa-copy"></i> 复制'
              copyButton.classList.remove('copied')
            }, 2000)
            this.$message.success('复制成功')
          } catch (err) {
            this.$message.error('复制失败，请手动复制')
          }
        })

        // 将按钮添加到代码块
        buttonWrapper.appendChild(copyButton)
        pre.appendChild(buttonWrapper)
      })
    },
    /**
     * 初始化图片预览
     */
    initImagePreview() {
      // 重复调用时先断开旧观察器，否则每进一次文章页就多留一个观察器 + 一份组件实例
      if (this.imageObserver) {
        this.imageObserver.disconnect()
        this.imageObserver = null
      }
      if (this.imageObserverTimer) {
        clearTimeout(this.imageObserverTimer)
      }
      // 使用 IntersectionObserver 监听图片
      const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            const img = entry.target
            const actualSrc = img.getAttribute('data-src')
            if (actualSrc) {
              // 创建一个新的图片对象来预加载
              const tempImg = new Image()
              tempImg.onload = () => {
                img.src = actualSrc
                img.classList.add('loaded')
              }
              tempImg.onerror = () => {
                img.src = 'https://img.shiyit.com/base/mojian/img-error.jpg'
                img.classList.add('error')
              }
              tempImg.src = actualSrc
              img.removeAttribute('data-src')
              observer.unobserve(img)
            }
          }
        })
      }, {
        rootMargin: '50px 0px',
        threshold: 0.1
      })

      this.imageObserver = observer

      // 监听所有带有 lazy-image 类的图片
      this.imageObserverTimer = setTimeout(() => {
        this.imageObserverTimer = null
        const lazyImages = document.querySelectorAll('.lazy-image')
        lazyImages.forEach(img => {
          observer.observe(img)
        })

        // 收集所有图片URL用于预览
        this.images = Array.from(document.querySelectorAll('.article-content img')).map(img =>
          img.getAttribute('data-src') || img.getAttribute('src')
        )

        // 为图片添加点击事件
        document.querySelectorAll('.article-content img').forEach(img => {
          img.style.cursor = 'zoom-in'
          img.addEventListener('click', this.handleImageClick)
        })
      }, 200)
    },
    /**
     * 清掉本页建立的观察器与定时器。
     * 换文章和卸载都要调用：观察器回调闭包持有组件实例，不清理会连同整篇文章一起泄漏。
     */
    resetDomState() {
      if (this.initTimer) {
        clearTimeout(this.initTimer)
        this.initTimer = null
      }
      if (this.imageObserverTimer) {
        clearTimeout(this.imageObserverTimer)
        this.imageObserverTimer = null
      }
      if (this.imageObserver) {
        this.imageObserver.disconnect()
        this.imageObserver = null
      }
    },
    /**
     * 处理图片点击
     */
    handleImageClick(e) {
      const img = e.target
      if (img.tagName === 'IMG') {
        this.$refs.imagePreview.show(this.images, this.images.indexOf(img.src))
      }
    },
    toggleDislike() {
      // 实现点踩功能
      if (this.likeDebounce) {
        this.$message.warning('请于 5 秒后再试')
        return
      }
      // TODO: 调用点踩 API
      this.$message.success(this.article.isDislike ? '取消点踩成功' : '点踩成功')
      if (this.article.isDislike) {
        this.article.dislikeNum--
      } else {
        this.article.dislikeNum++
      }
      this.article.isDislike = !this.article.isDislike
      this.likeDebounce = true
      this.scheduleLikeDebounceReset()
    },
    /**
     * 5 秒后解除点赞/点踩的防抖锁。
     * 定时器 id 挂在实例上，卸载时由 beforeUnmount 清理，避免回调在组件销毁后仍持有 this。
     */
    scheduleLikeDebounceReset() {
      if (this.likeDebounceTimer) {
        clearTimeout(this.likeDebounceTimer)
      }
      this.likeDebounceTimer = setTimeout(() => {
        this.likeDebounceTimer = null
        this.likeDebounce = false
      }, 5000)
    },
    toggleFavorite() {
      this.$message.warning('暂未开放')
      return
      // 实现收藏功能
      this.$message.success(this.article.isFavorite ? '取消收藏成功' : '收藏成功')
      if (this.article.isFavorite) {
        this.article.favoriteNum--
      } else {
        this.article.favoriteNum++
      }
      this.article.isFavorite = !this.article.isFavorite
    },
    toggleSidebar() {
      this.showSidebar = !this.showSidebar
      // 等待 DOM 更新后重新计算操作栏位置
      this.$nextTick(() => {
        this.updateActionBarPosition()
      })
    },
    scrollToComments() {
      const commentsSection = document.querySelector('.comment-section')
      if (commentsSection) {
        commentsSection.scrollIntoView({ behavior: 'smooth' })
      }
    },
    updateActionBarPosition() {
      const articleBox = document.getElementById("articleBox")
      if (articleBox) {
        const rect = articleBox.getBoundingClientRect()
        this.actionBarLeft = (rect.left - 95) + 'px'
      }
    },
    /**
     * 初始化代码块的展开/折叠功能
     */
    initializeCodeBlocks() {
      const codeBlocks = document.querySelectorAll('.article-content pre')
      codeBlocks.forEach((pre, index) => {
        // 已经处理过的代码块直接跳过：重复调用会重建按钮并把用户展开过的块重新折叠
        if (this.collapsedCodeBlocks.has(index) || pre.querySelector('.expand-button')) return

        // 获取代码块的实际高度
        const actualHeight = pre.scrollHeight

        if (actualHeight > 500) {
          // 添加折叠类
          pre.classList.add('collapsed')

          // 创建展开按钮
          const expandButton = document.createElement('button')
          expandButton.className = 'expand-button'
          expandButton.innerHTML = '<i class="fas fa-chevron-down"></i>展开代码'

          // 添加点击事件
          expandButton.onclick = (e) => {
            e.stopPropagation()
            const isCollapsed = pre.classList.contains('collapsed')
            if (isCollapsed) {
              pre.classList.remove('collapsed')
              expandButton.innerHTML = '<i class="fas fa-chevron-up"></i>收起代码'
              this.collapsedCodeBlocks.delete(index)
            } else {
              pre.classList.add('collapsed')
              expandButton.innerHTML = '<i class="fas fa-chevron-down"></i>展开代码'
              this.collapsedCodeBlocks.add(index)
            }
          }

          pre.appendChild(expandButton)
        }
      })
    },
    /**
     * 添加行号
     */
    addLineNumbers() {
      const codeBlocks = document.querySelectorAll('.article-content pre code')
      codeBlocks.forEach((code) => {
        const pre = code.parentElement

        // 检查是否已添加行号
        if (!pre.querySelector('.line-numbers')) {
          const lines = code.textContent.split('\n').length
          const lineNumbers = document.createElement('div')
          lineNumbers.className = 'line-numbers'

          for (let i = 1; i <= lines; i++) {
            const span = document.createElement('span')
            span.textContent = i
            lineNumbers.appendChild(span)
          }

          pre.insertBefore(lineNumbers, code)
        }
      })
    },
    /**
     * 获取预览内容
     */
    getPreviewContent(content) {
      // 返回内容的前300个字符，并确保HTML标签完整
      const div = document.createElement('div')
      div.innerHTML = this.$sanitize(content)
      const text = div.textContent || div.innerText
      return escapeHtml(text.substring(0, 300)) + '...'
    },
    /**
     * 处理会员升级
     */
    handleUpgrade() {
      if (!this.$store.state.userInfo) {
        this.$message.warning('请先登录')
        return
      }
      this.showMembershipDialog = true
    },
    /**
     * 处理付费购买
     */
    handlePurchase() {
      if (!this.$store.state.userInfo) {
        this.$message.warning('请先登录')
        return
      }
      this.showPaymentDialog = true
    },
    /**
     * 处理支付成功
     */
    handlePaymentSuccess() {
      // 重新获取文章信息
      this.getArticle()
    },
    /**
     * 处理会员开通成功
     */
    handleMembershipSuccess() {
      // 重新获取文章信息
      this.getArticle()
    },
    /**
     * 处理评论添加
     */
    handleCommentAdded() {
      this.article.commentNum = (this.article.commentNum || 0) + 1;
    },

    /**
     * 处理评论删除
     */
    handleCommentDeleted() {
      this.article.commentNum = Math.max((this.article.commentNum || 0) - 1, 0);
    },

    startTransition(element) {
      element.style.height = 'auto'
      const height = element.scrollHeight
      element.style.height = '0px'
      // 触发回流
      element.offsetHeight
      element.style.height = height + 'px'
    },
    endTransition(element) {
      element.style.height = element.scrollHeight + 'px'
      // 触发回流
      element.offsetHeight
      element.style.height = '0px'
    }
  },
  async created() {
    await this.getArticle()
    window.addEventListener('resize', this.updateActionBarPosition)
  },
  mounted() {
    window.addEventListener('scroll', this.updateActiveHeading)
    this.$el.addEventListener('click', this.handleExternalLinkClick)
  },
  beforeUnmount() {
    this.resetDomState()
    if (this.likeDebounceTimer) {
      clearTimeout(this.likeDebounceTimer)
      this.likeDebounceTimer = null
    }
    window.removeEventListener('scroll', this.updateActiveHeading)
    window.removeEventListener('resize', this.updateActionBarPosition)
    this.$el.removeEventListener('click', this.handleExternalLinkClick)
    // 清理图片点击事件监听器
    const images = document.querySelectorAll('.article-content img')
    images.forEach(img => {
      img.removeEventListener('click', this.handleImageClick)
    })
  },
  watch: {
    // 监听路由参数变化
    '$route'(to, from) {
      if (to.params.id !== from.params.id) {
        // 重新获取文章数据（getArticle 内部会先 resetDomState 清掉旧观察器/定时器）
        this.getArticle()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.article-page {
  max-width: 1300px;
  margin: 0 auto;
  padding: $spacing-lg;
  @include responsive(lg) {
    padding: $spacing-sm;
  }
}

.external-link-dialog-content {
  color: var(--text-secondary);
  line-height: 1.7;

  p {
    margin: 0 0 $spacing-sm;
  }
}

.external-link-url {
  padding: $spacing-sm;
  overflow-wrap: anywhere;
  word-break: break-all;
  color: var(--text-primary);
  background: var(--hover-bg);
  border-radius: $border-radius-sm;
  font-family: monospace;
  font-size: 13px;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: $spacing-md * 2;
  transition: all 0.3s ease;

  &.center {
    grid-template-columns: 1fr;
    max-width: 1100px;
    margin: 0 auto;
  }

  @include responsive(lg) {
    grid-template-columns: 1fr;
    gap: $spacing-lg;
    padding: 0;
  }
}

.article-main {
  background: var(--surface);
  border: 1px solid var(--rule);
  border-radius: $border-radius-md;
  overflow: hidden;
}

.article-header {
  padding: $spacing-lg $spacing-xl;
  position: relative;
  border-bottom: 1px solid var(--border-color);
  background: var(--card-bg);

  .article-title {
    font-family: $font-serif;
    color: var(--text-primary);
    font-size: clamp(1.5rem, 3vw, 1.9rem);
    line-height: 1.4;
    margin: 0 0 $spacing-lg;
    text-align: left;
  }

  .article-info {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: $spacing-md;

    .author-info {
      display: flex;
      align-items: center;
      gap: $spacing-md;

      .author-avatar {
        width: 42px;
        height: 42px;
        border-radius: 50%;
        object-fit: cover;
        border: 1px solid var(--rule);
        background: var(--surface);
      }

      .author-meta {
        display: flex;
        flex-direction: column;
        gap: $spacing-xs;

        .author-name {
          color: $primary;
          font-weight: 600;
          font-size: 1.1em;
        }

        .post-meta {
          display: flex;
          align-items: center;
          gap: $spacing-sm;
          color: var(--text-secondary);
          font-size: 0.9em;

          i {
            color: $primary;
            margin-right: 4px;
          }

          .meta-divider {
            color: var(--text-secondary);
            opacity: 0.5;
          }

          .category {
            color: $primary;
          }
        }
      }
    }

    .article-stats {
      display: flex;
      align-items: center;
      gap: $spacing-lg;

      .stat-item {
        display: flex;
        align-items: center;
        gap: $spacing-xs;
        color: var(--text-secondary);
        font-size: 0.95em;

        i {
          color: $primary;
          font-size: 1.1em;
        }
      }

      .edit-item {
        cursor: pointer;
        transition: color 0.2s;

        &:hover {
          color: $primary;
        }
      }
    }
  }
}

.reprint-notice {
  margin: $spacing-md $spacing-md * 2;
  padding: $spacing-base $spacing-lg;
  border-left: 2px solid var(--accent);
  background: var(--row-hover);
  color: var(--text-secondary);
  font-size: 0.9em;
  line-height: 1.7;
  border-radius: $border-radius-sm;

  a {
    color: $primary;
    word-break: break-all;
  }
}

.article-content {
  padding: $spacing-md * 2;
  line-height: 1.9;
  color: var(--text-primary);
  font-size: 1.05em;
  font-feature-settings: 'palt' 1;
  text-align: justify;

  h1, h2, h3, h4, h5, h6 {
    color: var(--text-primary);
    font-weight: 600;
    letter-spacing: 0.01em;
    text-align: left;
  }

  :deep(h1) {
    font-size: 2.1em;
    margin: $spacing-xl 0 $spacing-lg;
    line-height: 1.3;
  }

  :deep(h2) {
    font-family: $font-serif;
    font-size: 1.7em;
    margin: $spacing-xl * 1.4 0 $spacing-lg;
    padding-bottom: $spacing-sm;
    border-bottom: 1px solid var(--rule);
    position: relative;
    color: var(--text-primary);
    padding: $spacing-md 0;

    &::after {
      content: '';
      position: absolute;
      bottom: -1px;
      left: 0;
      width: 42px;
      height: 2px;
      background: var(--accent);
    }
  }

  :deep(h3) {
    font-family: $font-serif;
    font-size: 1.35em;
    margin: $spacing-xl 0 $spacing-md;
    color: var(--text-primary);
    position: relative;
    padding-left: $spacing-lg;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0.35em;
      width: 2px;
      height: 1em;
      background: var(--rule-strong);
    }
  }

  :deep(h4) {
    font-size: 1.15em;
    margin: $spacing-lg 0 $spacing-sm;
  }

  :deep(p) {
    margin: $spacing-md 0;
    color: var(--text-secondary);
    line-height: 1.9;
  }

  :deep(a) {
    color: $primary;
    text-decoration: none;
    border-bottom: 1px dashed rgba($primary, 0.5);
    transition: all 0.2s ease;
    padding-bottom: 1px;

    &:hover {
      color: var(--primary-dark, $primary);
      border-bottom-style: solid;
      border-bottom-color: $primary;
    }
  }

  :deep(blockquote) {
    margin: $spacing-lg 0;
    padding: $spacing-md $spacing-lg;
    background: var(--surface);
    border-left: 2px solid var(--rule-strong);
    border-radius: 0 $border-radius-sm $border-radius-sm 0;
    color: var(--text-secondary);
    font-style: italic;
    position: relative;

    &::before {
      content: '\201C';
      position: absolute;
      top: $spacing-sm;
      left: $spacing-md;
      font-size: 2.4em;
      line-height: 1;
      color: rgba($primary, 0.25);
      font-style: normal;
      font-family: Georgia, serif;
    }

    p {
      margin: 0;
    }
  }

  :deep(ul),
  :deep(ol) {
    margin: $spacing-md 0;
    padding-left: $spacing-xl;
    color: var(--text-secondary);

    li {
      margin-bottom: $spacing-sm;
      position: relative;

      &::marker {
        color: $primary;
      }
    }
  }

  :deep(code:not([class])) {
    font-size: 0.9em;
    line-height: 1.5;
    position: relative;
    color: #e05d6d;
    background: rgba($primary, 0.08);
    border-radius: $border-radius-sm;
    padding: 0.15em 0.4em;
    margin: 0 0.2em;
    font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  }

  /* 代码窗口是全文唯一的「深色器皿」，底色不跟主题翻白；但夜里正文就铺在
     --surface #161a20 上，窗底若是 #1b1f24 只差 5 阶，窗口会糊进页面。
     所以只把「窗底 / 边线 / 投影」交给主题令牌，高亮配色（atom-one-dark）不动。 */
  --code-window-bg: #1b1f24;
  --code-window-border: rgba(255, 255, 255, 0.08);
  --code-window-shadow: 0 12px 30px rgba(17, 20, 26, 0.16);
  // atom-one-dark 里唯一低于 AA 的一档：注释/引用 #5c6370 在窗底上只有 2.4–2.7:1，
  // 还带斜体，中文注释尤其糊。按窗底色分别提亮（浅窗 4.38:1 / 深窗 4.90:1），其余 token 不动。
  --code-comment: #7a8494;

  :root[data-theme='dark'] & {
    --code-window-bg: #23282f;
    --code-window-border: rgba(255, 255, 255, 0.14);
    --code-window-shadow: 0 14px 34px rgba(0, 0, 0, 0.55);
    --code-comment: #8b95a5;
  }

  // hljs 的 token span 在代码块深处，scoped 下要 :deep 才穿得进去
  :deep(.hljs-comment),
  :deep(.hljs-quote) {
    color: var(--code-comment);
  }

  :deep(pre) {
    margin: $spacing-lg 0;
    position: relative;
    // 深墨灰窗口底，配全站冷灰；比 atom-one-dark 的蓝紫 #282c34 更中性
    background: var(--code-window-bg);
    border: 1px solid var(--code-window-border);
    border-radius: 12px;
    box-shadow: var(--code-window-shadow);
    // 顶部为仿 macOS 标题栏预留高度
    padding-top: 2.75em;
    overflow: hidden;
    max-height: 2000px;
    transition: max-height 0.4s ease-in-out;

    &.collapsed {
      max-height: 300px;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        height: 60px;
        background: linear-gradient(transparent, var(--code-window-bg));
        pointer-events: none;
        z-index: 2;
      }

      .expand-button {
        display: flex !important;
      }
    }

    // ── 仿 macOS 标题栏 ──
    // 标题栏底走 ::before（::after 被折叠态的渐变遮罩占用，两者不能共用）
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 2.75em;
      background: rgba(255, 255, 255, 0.035);
      border-bottom: 1px solid rgba(255, 255, 255, 0.07);
      pointer-events: none;
      z-index: 1;
    }

    .expand-button {
      position: absolute;
      bottom: 15px;
      left: 50%;
      transform: translateX(-50%);
      padding: 7px 18px;
      background: rgba(255, 255, 255, 0.09);
      border: 1px solid rgba(255, 255, 255, 0.16);
      border-radius: 999px;
      color: #c8ced8;
      cursor: pointer;
      z-index: 3;
      font-size: 0.85em;
      align-items: center;
      gap: 6px;
      transition: background 0.2s ease, border-color 0.2s ease,
        transform 0.2s ease, color 0.2s ease;
      white-space: nowrap;
      backdrop-filter: blur(6px) saturate(1.3);
      -webkit-backdrop-filter: blur(6px) saturate(1.3);

      &:hover {
        background: rgba(255, 255, 255, 0.16);
        color: #fff;
        border-color: rgba(255, 255, 255, 0.3);
        transform: translateX(-50%) translateY(-1px);
      }

      i {
        font-size: 14px;
      }
    }

    /* 行号列：与代码区同底，只留一条极淡分隔，读成一块完整窗口 */
    .line-numbers {
      position: absolute;
      left: 0;
      top: 2.75em;
      bottom: 0;
      font-size: 14px;
      padding: 1em 0;
      text-align: right;
      color: rgba(255, 255, 255, 0.26);
      border-right: 1px solid rgba(255, 255, 255, 0.06);
      background: transparent;
      user-select: none;
      z-index: 1;

      span {
        display: block;
        padding: 0 0.7em;
        min-width: 2.6em;
        line-height: 1.6;
      }
    }

    /* 调整代码内容的样式 */
    code {
      display: block;
      padding: 1em;
      padding-left: 4.2em;
      margin-left: 0;
      overflow-x: auto;
      // 细滚动条：代码横滚时别甩出一条系统粗条压在窗口底
      scrollbar-width: thin;
      scrollbar-color: rgba(255, 255, 255, 0.24) transparent;

      &::-webkit-scrollbar {
        height: 8px;
      }

      &::-webkit-scrollbar-track {
        background: transparent;
      }

      &::-webkit-scrollbar-thumb {
        background: rgba(255, 255, 255, 0.18);
        background-clip: content-box;
        border: 2px solid transparent;
        border-radius: 999px;

        &:hover {
          background: rgba(255, 255, 255, 0.3);
          background-clip: content-box;
        }
      }

      font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
      font-size: 14px;
      line-height: 1.6;
      position: relative;

      // hljs 主题（atom-one-dark）自带 #282c34 背景，会盖住窗口底色；
      // 这里必须清掉，窗口底统一由 pre 提供。
      &.hljs {
        background: transparent;
      }
    }

    // 用 & 展开成 pre code.hljs，压过主题里的 pre code.hljs{padding:1em}
    // （那条 specificity 比 code.hljs 高，不这么写行号会压到代码上）
    & code.hljs {
      padding: 1em 1em 1em 4.2em;
    }

    // 标题栏：左圆点 + 中语言名 + 右复制键，三者同一行
    .code-header {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 2.75em;
      z-index: 3;
      display: flex;
      align-items: center;
      padding: 0 12px 0 14px;
      pointer-events: none;
    }

    // 三个 macOS 圆点：一处伪元素 + 三色 radial-gradient 画，不额外加 DOM
    .code-header::before {
      content: '';
      flex: none;
      width: 42px;
      height: 12px;
      background:
        radial-gradient(circle at 6px 6px, #ff5f57 5.5px, transparent 6px),
        radial-gradient(circle at 21px 6px, #febc2e 5.5px, transparent 6px),
        radial-gradient(circle at 36px 6px, #28c840 5.5px, transparent 6px);
      opacity: 0.85;
      transition: opacity 0.2s ease;
    }

    &:hover .code-header::before {
      opacity: 1;
    }

    // 语言名：绝对居中，不受左右两侧宽度影响
    .code-header .code-lang {
      position: absolute;
      left: 50%;
      transform: translateX(-50%);
      font-family: $font-mono;
      font-size: 0.72rem;
      letter-spacing: 0.04em;
      color: rgba(255, 255, 255, 0.42);
      text-transform: lowercase;
      pointer-events: none;
      white-space: nowrap;
    }

    .copy-button {
      margin-left: auto;
      padding: 4px 12px;
      background: rgba(255, 255, 255, 0.08);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 999px;
      color: #c8ced8;
      cursor: pointer;
      font-size: 12px;
      transition: background 0.2s ease, color 0.2s ease, border-color 0.2s ease,
        opacity 0.2s ease;
      display: flex;
      align-items: center;
      gap: 5px;
      font-family: inherit;
      opacity: 0;
      pointer-events: auto;

      i {
        font-size: 12px;
      }

      &:hover {
        background: rgba(255, 255, 255, 0.16);
        border-color: rgba(255, 255, 255, 0.24);
        color: #fff;
      }

      &.copied {
        background: rgba(40, 200, 64, 0.9);
        border-color: transparent;
        color: #fff;
      }
    }

    &:hover .copy-button {
      opacity: 1;
    }

    // 触屏没有 hover，复制键常显，否则永远点不到
    @media (hover: none) {
      .copy-button {
        opacity: 1;
      }
    }
  }

  :deep(img.lazy-image) {
    opacity: 0;

    &.loaded {
      opacity: 1;
    }

    &.error {
      opacity: 0.5;
    }
  }

  :deep(img) {
    max-width: 100%;
    border-radius: $border-radius-sm;
    border: 1px solid var(--rule);
    margin: $spacing-lg auto;
    display: block;
    cursor: zoom-in;
  }

  :deep(table) {
    width: 100%;
    margin: $spacing-lg 0;
    border-collapse: collapse;

    th,
    td {
      padding: $spacing-sm $spacing-md;
      border: 1px solid var(--border-color);
    }

    th {
      background: var(--hover-bg);
      color: var(--text-primary);
      font-weight: 600;
      text-align: left;
    }

    tr:nth-child(even) {
      background: var(--hover-bg);
    }

    tr:hover {
      background: var(--row-hover);
    }
  }

  :deep(hr) {
    margin: $spacing-xl * 1.2 0;
    border: none;
    height: 1px;
    background: var(--rule);
  }
.locked-content {
    position: relative;

    .preview-content {
      max-height: 300px;
      overflow: hidden;
      position: relative;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        height: 100px;
        background: linear-gradient(transparent, var(--surface));
        pointer-events: none;
      }
    }

    .content-locker {
      position: relative;
      margin-top: -60px;
      padding: $spacing-xl;
      text-align: center;
      background: var(--surface);
      border: 1px solid var(--rule);
      border-radius: $border-radius-md;
      z-index: 1;

      .locker-icon {
        width: 52px;
        height: 52px;
        margin: 0 auto $spacing-lg;
        background: var(--accent-soft);
        border-radius: $border-radius-sm;
        display: flex;
        align-items: center;
        justify-content: center;

        i {
          font-size: 1.5em;
          color: var(--accent);
        }
      }

      h3 {
        color: var(--text-primary);
        font-size: 1.4em;
        margin-bottom: $spacing-sm;
      }

      p {
        color: var(--text-secondary);
        margin-bottom: $spacing-lg;
      }

      .el-button {
        padding: $spacing-sm $spacing-xl;
        font-size: 1.1em;
      }
    }

    &.member .locker-icon {
      background: rgba(#FFD700, 0.1);

      i {
        color: #FFD700;
      }
    }

    &.paid .locker-icon {
      background: rgba(#E6162D, 0.1);

      i {
        color: #E6162D;
      }
    }
  }
}

.article-footer {
  padding: $spacing-md * 2;
  border-top: 1px solid var(--border-color);

  @include responsive(lg) {
    padding: $spacing-sm;
  }

  .copyright-notice {
    margin-bottom: $spacing-xl;
    padding: $spacing-md $spacing-lg;
    background: var(--hover-bg);
    border-left: 2px solid var(--accent);
    border-radius: $border-radius-sm;

    .notice-content {
      padding: 0;
      color: var(--text-secondary);
      font-size: 0.9em;
      line-height: 2;

      .notice-item {
        display: flex;
        align-items: center;
        gap: $spacing-sm;
        padding: 0;

        a {
          color: $primary;
          text-decoration: none;
          border-bottom: 1px solid transparent;
          transition: all 0.2s ease;
          word-break: break-all;

          &:hover {
            border-bottom-color: currentColor;
          }
        }
      }
    }
  }

  .tags-section {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    margin-bottom: $spacing-xl;

    i {
      color: $primary;
    }

    .tags-list {
      display: flex;
      flex-wrap: wrap;
      gap: $spacing-sm;
    }

    .tag-item {
      padding: 3px 10px;
      background: transparent;
      border: 1px solid var(--rule);
      color: var(--text-secondary);
      border-radius: $border-radius-sm;
      font-size: 0.85em;
      text-decoration: none;
      transition: color 0.2s ease, border-color 0.2s ease;

      &:hover {
        border-color: var(--accent);
        color: var(--accent);
      }
    }
  }

  .article-actions {
    display: flex;
    justify-content: center;
    gap: $spacing-lg;
    margin-bottom: $spacing-xl;

    .action-btn {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      padding: 8px 20px;
      border: 1px solid var(--rule);
      border-radius: $border-radius-md;
      background: transparent;
      color: var(--text-secondary);
      font-size: 0.9rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

      &:hover {
        transform: translateY(-1px);
      }

      &:active {
        transform: translateY(0);
      }

      &.like {
        &.active {
          border-color: var(--accent);
          color: var(--accent);
          background: var(--accent-soft);
        }

        &:hover {
          border-color: var(--accent);
          color: var(--accent);
        }
      }

      &.share {
        background: var(--accent);
        border-color: var(--accent);
        color: #fff;
        box-shadow: 0 4px 14px rgba(168, 106, 16, 0.2);

        &:hover {
          background: var(--accent);
          filter: brightness(1.08);
          box-shadow: 0 6px 18px rgba(168, 106, 16, 0.28);
          color: #fff;
        }

        &:active {
          filter: brightness(0.95);
        }
      }
    }

    :deep(.share-dropdown) {
      position: relative;

      .share-menu {
        position: absolute;
        bottom: calc(100% + 8px);
        right: 0;
        background: var(--surface);
        border-radius: $border-radius-md;
        box-shadow: $shadow-md;
        padding: $spacing-xs;
        min-width: 180px;
        z-index: 100;
        border: 1px solid var(--border-color);
        transform-origin: bottom right;
        animation: shareMenuIn 0.2s ease;

        &::before {
          content: '';
          position: absolute;
          bottom: -5px;
          right: 20px;
          width: 10px;
          height: 10px;
          background: var(--card-bg);
          border-left: 1px solid var(--border-color);
          border-top: 1px solid var(--border-color);
          transform: rotate(225deg);
        }

        .share-item {
          width: 100%;
          padding: $spacing-sm $spacing-lg;
          border: none;
          background: none;
          color: var(--text-secondary);
          font-size: 0.95em;
          display: flex;
          align-items: center;
          gap: $spacing-md;
          cursor: pointer;
          transition: all 0.3s ease;
          border-radius: $border-radius-sm;

          i {
            width: 16px;
            text-align: center;
            font-size: 1.1em;

            &.fa-qq {
              color: #12B7F5;
            }

            &.fa-star {
              color: #FFD700;
            }

            &.fa-weibo {
              color: #E6162D;
            }

            &.fa-weixin {
              color: #07C160;
            }

            &.fa-link {
              color: $primary;
            }
          }

          &:hover {
            background: var(--row-hover);
          }
        }
      }
    }
  }
}

.article-sidebar {
  .toc-container {
    position: sticky;
    top: 90px;
    background: var(--surface);
    border-radius: $border-radius-md;
    overflow: hidden;
    border: 1px solid var(--rule);

    .toc-header {
      padding: $spacing-md $spacing-lg;
      background: var(--surface);
      color: var(--text-primary);
      font-family: $font-mono;
      font-size: 0.78rem;
      letter-spacing: 0.1em;
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      position: relative;
      border-bottom: 1px solid var(--rule);
      justify-content: space-between;

      .title-wrapper {
        display: flex;
        align-items: center;
        gap: $spacing-sm;

        i {
          color: $primary;
          font-size: 1.1em;
          transform-origin: center;
        }
      }

      .header-actions {
        display: flex;
        align-items: center;
        gap: $spacing-md;
      }

      .progress-wrapper {
        font-size: 0.9em;
        color: var(--text-tertiary);
        display: flex;
        align-items: center;
        gap: $spacing-xs;

        i {
          color: var(--text-tertiary);
          font-size: 0.9em;
        }

        .progress-text {
          font-variant-numeric: tabular-nums;
          min-width: 3em;
          text-align: right;

          &::after {
            content: '%';
            margin-left: 2px;
            opacity: 0.7;
          }
        }
      }

      .toc-toggle {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        height: 24px;
        padding: 0;
        border: 1px solid var(--rule);
        border-radius: $border-radius-sm;
        background: transparent;
        color: var(--text-tertiary);
        cursor: pointer;
        transition: color 0.2s ease, border-color 0.2s ease;

        .el-icon {
          font-size: 13px;
        }

        &:hover {
          color: var(--accent);
          border-color: var(--accent);
        }
      }
    }

      .toc-content {
      padding: $spacing-lg;
      max-height: calc(100vh - 200px);
      overflow-y: auto;
      position: relative;

      .toc-item {
        padding: $spacing-sm $spacing-md;
        margin: 2px 0;
        cursor: pointer;
        border-radius: $border-radius-sm;
        color: var(--text-secondary);
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        font-size: 0.95em;
        line-height: 1.4;
        position: relative;
        display: flex;
        align-items: center;
        padding-left: 16px;
        text-indent: 0;

        &::before {
          content: '';
          position: absolute;
          left: 7px;
          top: 50%;
          transform: translateY(-50%);
          width: 5px;
          height: 5px;
          background: var(--rule-strong);
          border-radius: 50%;
        }

        &:hover {
          color: var(--text-primary);
          background: var(--row-hover);
        }

        &.active {
          color: var(--accent);
          background: var(--row-hover);
          font-weight: 500;
          padding-left: 22px;

          &::before {
            background: var(--accent);
            opacity: 1;
          }
        }

        &.depth-1 {
          margin-left: 18px;
          font-size: 0.93em;
          opacity: 0.94;

          &::before {
            width: 4px;
            height: 4px;
            background: transparent;
            border: 1px solid rgba($primary, 0.7);
          }
        }

        &.depth-2 {
          margin-left: 34px;
          font-size: 0.89em;
          opacity: 0.88;

          &::before {
            width: 3px;
            height: 3px;
            background: var(--text-secondary);
          }
        }

        &.depth-3 {
          margin-left: 48px;
          font-size: 0.85em;
          opacity: 0.82;

          &::before {
            width: 2px;
            height: 2px;
            border-radius: 0;
          }
        }

        .toc-item-text {
          min-width: 0;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        &.level-1 {
          font-weight: 600;
        }
      }
    }
  }
}


@keyframes shareMenuIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.toc-restore {
  position: fixed;
  top: 96px;
  right: 24px;
  z-index: 90;
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-md;
  border: 1px solid var(--rule);
  border-radius: $border-radius-md;
  background: var(--surface);
  color: var(--text-secondary);
  font-size: 0.85rem;
  cursor: pointer;
  transition: color 0.2s ease, border-color 0.2s ease;

  .el-icon {
    color: $primary;
    font-size: 15px;
  }

  &:hover {
    color: var(--text-primary);
    border-color: var(--accent);

    .el-icon {
      color: var(--accent);
    }
  }
}

.desktop-only {
  @include responsive(lg) {
    display: none;
  }
}

.floating-action-bar {
  position: fixed;
  top: 40%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: $spacing-md * 1.5;
  padding: $spacing-sm;
  border-radius: $border-radius-lg;
  z-index: 100;
  transition: left 0.3s ease;

  @include responsive(lg) {
    display: none;
  }

  .action-item {
    cursor: pointer;
    .action-button {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: $border-radius-sm;
      border: 1px solid var(--rule);
      background: var(--surface);
      transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

      i {
        font-size: 1.2em;
        color: var(--text-secondary);
        transition: all 0.3s ease;

        &.active {
          color: $primary;
        }
      }
    }

    .action-count {
      font-size: 0.8em;
      color: var(--text-secondary);
      min-width: 20px;
      text-align: center;
    }

    &:hover {
      .action-button {
        border-color: var(--accent);

        i {
          color: var(--accent);
        }
      }
    }
  }

  .reward-item {
    position: relative;

    .reward-popup {
      position: absolute;
      left: calc(100% + 16px);
      top: 50%;
      transform: translateY(-50%);
      background: var(--surface);
      border: 1px solid var(--rule);
      border-radius: $border-radius-md;
      padding: $spacing-md;
      box-shadow: $shadow-lg;
      opacity: 0;
      visibility: hidden;
      transition: opacity 0.25s ease, visibility 0.25s ease, transform 0.25s ease;
      pointer-events: none;
      width: 510px;

      &::before {
        content: '';
        position: absolute;
        left: -6px;
        top: 50%;
        transform: translateY(-50%) rotate(45deg);
        width: 12px;
        height: 12px;
        background: var(--card-bg);
        border-radius: 2px;
      }

      .reward-content {
        display: flex;
        gap: $spacing-md;
        margin-bottom: $spacing-sm;

        .reward-qr {
          width: 250px;
          height: 250px;
          border-radius: $border-radius-sm;
          object-fit: cover;
        }
      }

      .reward-text {
        text-align: center;
        color: var(--text-secondary);
        font-size: 0.9em;
      }
    }

    &:hover {
      .reward-popup {
        opacity: 1;
        visibility: visible;
        pointer-events: auto;
        transform: translateY(-50%) translateX(8px);
      }
    }
  }
}

.ai-description {
  margin: $spacing-md $spacing-xl;
  border-radius: $border-radius-md;
  // 旧版是粉紫→浅蓝渐变底 + 青色描边，新设计用中性面 + 强调色左线
  background: var(--surface);
  border: 1px solid var(--rule);
  border-left: 2px solid var(--accent);
  transition: all 0.3s ease;
  overflow: hidden;

  @include responsive(sm) {
    margin: $spacing-sm;
  }



  .ai-header {
    padding: $spacing-sm $spacing-md;
    font-family: $font-mono;
    font-size: 0.82rem;
    letter-spacing: 0.04em;
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    justify-content: flex-start;
    color: var(--accent);
    cursor: pointer;

    i {
      font-size: 1.1em;
    }
  }

  .ai-content {
    padding: $spacing-md;
    overflow: hidden;
  }
  .ai-description-text {
    margin-top: $spacing-sm;
    color: var(--text-secondary);
  }
}

.expand-enter-active,
.expand-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  height: 0;
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
}

</style>
