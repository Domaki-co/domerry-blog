<template>
  <div class="home-page">
    <!-- 首屏：全屏封面图 + 居中站名，「向下」滚到文章列表 -->
    <Hero :carousels="carouselSlides" @scroll-to-posts="scrollToPosts" />

    <!--
      文章面板：一片圆角浅色板，从底部往上盖住 sticky 钉住的封面 —— 首页的「窗帘」。
      它必须不透明（盖住底下那张图），所以自己把全站的天空 / 星空重铺了一遍，
      见 .home-sheet 的样式注释。
    -->
    <div
      class="home-sheet"
      :class="{ 'is-revealing-nav': navRevealing }"
    >
      <div class="home">
        <div class="content-layout">
          <main class="home-main-content">
            <!-- 侧边栏在 <=1024px 时整体隐藏，这里给移动端补一个折叠式置顶入口 -->
            <MobileStickyBar :sticks="stickyPosts" />

            <!-- 分类筛选：macOS 胶囊分段选择器 (Segmented Control)。
                 激活态不是各按钮自己涂底色，而是由 .segment-thumb 这一个滑块承担，
                 切换分类时它平移过去，做出系统设置侧栏那种「滑行」的手感 -->
            <nav ref="filtersEl" class="filters-segmented-wrap">
              <div class="segmented-track" ref="segmentTrack">
                <span class="segment-thumb" :style="thumbStyle" aria-hidden="true"></span>
                <button
                  v-for="category in categories"
                  :key="category.id"
                  type="button"
                  class="segment-item"
                  :class="{ 'is-active': activeName === String(category.id) }"
                  @click="handleClick(category)"
                >
                  <span class="segment-name">{{ category.name }}</span>
                  <span v-if="categoryCount(category)" class="segment-count">{{ categoryCount(category) }}</span>
                </button>
                <router-link
                  to="/categories"
                  class="segment-item segment-more-btn"
                  title="查看全部详细分类"
                >
                  <span>更多</span>
                  <svg viewBox="0 0 24 24" width="10" height="10" fill="none" stroke="currentColor" stroke-width="2.5">
                    <path d="M9 18l6-6-6-6" />
                  </svg>
                </router-link>
              </div>
              <!-- 只在选中具体分类时出现：默认「全部」的篇数在侧栏作者卡的「文章篇数」里已经有一份 -->
              <div v-if="activeName !== 'all'" class="meta-status-capsule">
                <span class="status-dot"></span>
                <span class="total-text">共 {{ total }} 篇</span>
              </div>
            </nav>

            <!-- 列表只渲染一次,筛选器仅改查询条件,避免每个分类面板重复挂一份完整列表 -->
            <div ref="postsSection" class="posts-section">
              <ArticleList
                :articles="articleList"
                :loading="loading"
                :total="total"
                :params="params"
                :transition-phase="transitionPhase"
                :min-height="lockedMinHeight"
                @article-click="goToPost"
                @page-change="changePage"
                @stick-changed="getStickArticles"
                class="article-list"
              />
            </div>
          </main>
          <Sidebar :sticks="stickyPosts" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import ArticleList from "@/components/ArticleList/index.vue";
import Hero from "@/views/home/components/hero.vue";
import Sidebar from "@/components/Sidebar/index.vue";
import MobileStickyBar from "@/views/home/components/mobileStickyBar.vue";
import {
  getArticlesApi,
  getCarouselArticlesApi,
  getStickArticlesApi,
  getAllCategoriesApi,
  getCategoriesApi,
} from "@/api/article";

/**
 * 筛选器的入场类名挂多久后摘掉（毫秒）。
 * 要大于它自己的动画时长（样式里 0.9s），留点余量给慢设备。
 *
 * 文章行不再需要这个：它们用 translate 属性进场（hover 走的是 transform），
 * 两条互不覆盖，类名可以一直留着，不用等动画播完再摘。
 */
const REVEAL_HOLD_MS = 1300;

/**
 * 文章行进场类名挂多久后摘掉（毫秒）：0.9s 的过渡 + 一点余量。
 *
 * 必须摘 —— is-pending 上的 transition 声明（特异性比 .entry 高）会把
 * ArticleList 自己那条 background/transform 0.28s 的过渡整条顶掉，
 * 留在身上 hover 就变成硬切了。摘掉后回到组件默认样式，和动画终点一致，不跳。
 */
const ROW_REVEAL_HOLD_MS = 1000;

/**
 * 入场触发线：把视口下边往外撑 3%（一条「提前量」），元素顶边还差一点点进视野
 * 就开始播。
 *
 * 这个符号是试出来的，两个方向都试错才有结论：
 * 收进去（-8%）→ 触发时元素已经露了小半截在屏幕上，动画一挂它先跳成透明再淡入，
 * 反而闪一下；撑太多（+15%）→ 面板内容离折线只差 40px，等于一进页面就偷偷播完，
 * 等滚下去只剩静止画面。撑 3%（约 24px）刚好：动画开播时元素还在视口外，
 * 滚进来时正在浮起的中段。
 */
const REVEAL_ROOT_MARGIN = '0px 0px 3% 0px';

export default {
  name: "Home",
  components: {
    ArticleList,
    Hero,
    Sidebar,
    MobileStickyBar,
  },
  data() {
    return {
      total: 0,
      params: {
        pageNum: 1,
        pageSize: 10,
        categoryId: null,
      },
      // 当前生效的查询参数快照（用于“是否产生新查询”的纯等价判断）
      activeParams: {
        pageNum: 1,
        pageSize: 10,
        categoryId: null,
      },
      // 成功数据的快照（用于请求失败时的数据和页码回滚）
      backupData: {
        articles: [],
        total: 0,
        params: null,
      },
      articleList: [],
      carouselSlides: [],
      stickyPosts: [],
      loading: false,
      activeName: "all",
      // 过渡生命周期状态：'idle' | 'leaving' | 'loading' | 'entering'
      transitionPhase: "idle",
      // 容器锁定的最小高度，防止布局抖动
      lockedMinHeight: "",
      // 竞态计数器：快速连续翻页时只认最后一次请求
      querySeq: 0,
      // 骨架屏延时定时器引用
      skeletonTimer: null,
      // 进场动画结束后复位过渡态的定时器引用
      transitionTimer: null,
      // 分段选择器滑块的内联样式。首帧是 0 宽 + 透明，
      // 第一次 placeSegmentThumb() 量到真实位置后走进场动效
      thumbStyle: {
        width: "0px",
        transform: "translateX(0px)",
        opacity: 0,
      },
      // 第二屏接力入场的两个开关：各自盯着筛选器 / 台账，快进视野时置 true，
      // 播完摘掉。不常驻类名，是为了让翻页 / 切分类时只剩 ledger 自己的 is-entering
      navRevealing: false,
      categories: [
        {
          id: "all",
          name: "全部",
        },
      ],
    };
  },
  methods: {
    /**
     * 判断目标参数是否导致发起新的查询请求
     * 翻页定义：只要用户操作导致发起新的查询请求，都算翻页。
     * 包括：点击页码、上一页、下一页、筛选、排序、搜索、下拉加载。
     * 相同参数则不发起查询请求，亦不触发翻页动画。
     *
     * @param {object} nextParams 待执行的查询参数
     * @returns {boolean} 是否需要发起新查询
     */
    isQueryNeeded(nextParams) {
      if (!this.activeParams) return true;
      const keys = ["pageNum", "pageSize", "categoryId", "keyword", "sortField", "sortOrder"];
      for (const key of keys) {
        const curVal = this.activeParams[key] ?? null;
        const nextVal = nextParams[key] ?? null;
        if (curVal !== nextVal) {
          return true;
        }
      }
      return false;
    },
    /**
     * 锁定列表容器当前高度，避免翻页过渡时因内容清空/骨架屏高度差异导致页面跳动
     */
    lockContainerHeight() {
      const el = this.$refs.postsSection;
      if (el && el.offsetHeight > 0) {
        this.lockedMinHeight = `${el.offsetHeight}px`;
      }
    },
    /**
     * 解锁容器最小高度
     */
    unlockContainerHeight() {
      this.lockedMinHeight = "";
    },
    /**
     * 统一文章查询协调器（翻页/筛选/排序/搜索/加载统一入口）
     * 
     * 负责：
     * 1. 平滑过渡：旧内容淡出或轻微下移 -> 显示骨架屏/loading -> 新数据淡入、轻微上移复位
     * 2. 防抖动：容器最小高度锁定与解除
     * 3. 竞态处理：维护 querySeq，快速连续翻页时只认最后一次请求，避免动画闪烁和旧覆盖新
     * 4. 容错回滚：请求失败时保留/恢复旧内容与页码，并弹出错误提示
     * 
     * @param {object} nextParams 查询参数
     * @param {object} options 选项 { scrollToTop: boolean, force: boolean }
     */
    async executeArticleQuery(nextParams, options = {}) {
      const { scrollToTop = false, force = false } = options;

      // 判断逻辑：没有查询请求时不触发翻页动画
      if (!force && !this.isQueryNeeded(nextParams)) {
        return;
      }

      // 1. 生成本次请求唯一序列号
      const thisSeq = ++this.querySeq;

      // 清理之前的骨架屏延时器
      if (this.skeletonTimer) {
        clearTimeout(this.skeletonTimer);
        this.skeletonTimer = null;
      }

      // 2. 锁定容器最小高度，避免布局塌陷抖动
      this.lockContainerHeight();

      // 3. 记录失败回退备份（仅在从正常显示态触发时备份，避免覆盖最初有效内容）
      if (this.transitionPhase === "idle") {
        this.backupData = {
          articles: [...this.articleList],
          total: this.total,
          params: { ...this.activeParams },
        };
      }

      // 4. 预先更新 params 以让分页指示器即时响应
      this.params = { ...nextParams };
      this.loading = true;

      // 5. 过渡状态推进：
      // 如果处于 idle，进入 leaving（旧内容淡出、轻微下移）
      // 如果已经处于 leaving/loading（用户快速连续翻页），保持当前态，防止画面剧烈闪烁
      if (this.transitionPhase === "idle") {
        this.transitionPhase = "leaving";
      }

      // 若网络请求超过 180ms，无缝进入 loading 态展示骨架屏
      this.skeletonTimer = setTimeout(() => {
        if (thisSeq === this.querySeq && this.transitionPhase === "leaving") {
          this.transitionPhase = "loading";
        }
      }, 180);

      // 保证旧内容至少有 160ms 优雅淡出下移
      const minFadeOutPromise = new Promise((resolve) => setTimeout(resolve, 160));

      try {
        // 6. 发起查询请求（保持现有接口与入参协议不变）
        const requestPromise = getArticlesApi(this.params);
        const [res] = await Promise.all([requestPromise, minFadeOutPromise]);

        // 7. 快速连续翻页判断：只认最后一次请求，过期的立即丢弃
        if (thisSeq !== this.querySeq) {
          return;
        }

        if (this.skeletonTimer) {
          clearTimeout(this.skeletonTimer);
          this.skeletonTimer = null;
        }

        // 8. 请求成功：新数据渲染
        this.articleList = res.data?.records || [];
        this.total = res.data?.total || 0;
        this.activeParams = { ...this.params };

        // 9. 列表淡入、轻微上移复位
        this.transitionPhase = "entering";

        if (scrollToTop && this.$refs.postsSection) {
          const top = Math.max(0, this.$refs.postsSection.offsetTop - 80);
          window.scrollTo({ top, behavior: "smooth" });
        }

        this.$nextTick(() => {
          this.placeSegmentThumb();

          // 等待 CSS entering 进场动画 (320ms) 播放完毕后重置为 idle，并解锁高度
          this.transitionTimer = setTimeout(() => {
            if (thisSeq === this.querySeq) {
              this.transitionPhase = "idle";
              this.loading = false;
              this.unlockContainerHeight();
            }
          }, 320);
        });

      } catch (error) {
        // 过期的请求失败亦忽略
        if (thisSeq !== this.querySeq) {
          return;
        }

        if (this.skeletonTimer) {
          clearTimeout(this.skeletonTimer);
          this.skeletonTimer = null;
        }

        console.error("Failed to fetch articles:", error);

        // 10. 请求失败：保留/恢复旧内容与页码
        if (this.backupData.articles) {
          this.articleList = [...this.backupData.articles];
          this.total = this.backupData.total;
          this.params = { ...this.backupData.params };
          this.activeParams = { ...this.backupData.params };
        }

        this.transitionPhase = "idle";
        this.loading = false;
        this.unlockContainerHeight();

        // 显示友好错误提示
        this.$message.error(error.message || "获取文章列表失败，已恢复原页面");
        this.$nextTick(() => this.placeSegmentThumb());
      }
    },
    /**
     * 切换分类（筛选翻页）
     * @param {object} category 分类对象
     */
    handleClick(category) {
      const nextCategoryId = category.id === "all" ? null : category.id;
      const nextParams = {
        ...this.params,
        categoryId: nextCategoryId,
        pageNum: 1,
      };

      // 翻页判断：如果分类与页码未变，不触发查询请求，亦不触发翻页过渡
      if (!this.isQueryNeeded(nextParams)) {
        return;
      }

      this.activeName = String(category.id);
      this.executeArticleQuery(nextParams, { scrollToTop: false });
      // 等 is-active 落到新按钮上再量宽度
      this.$nextTick(() => this.placeSegmentThumb());
    },
    /**
     * 分段选择器上某个分类的数字微标。
     * 分类接口只给了「全部 + 文章数前 8」，全部的数量取自列表接口的 total；
     * 拿不到数量（fallback 接口没有 posts）或为 0 时不渲染气泡
     * @param {object} category 分类对象
     * @returns {number|null}
     */
    categoryCount(category) {
      if (category.id === "all") {
        return this.total || null;
      }
      return category.articleCount || null;
    },
    /**
     * 把激活滑块对齐到当前分类按钮。
     *
     * 位置只能量不能算：按钮宽度随文字、字重、数字气泡变化。
     *
     * 用 offsetLeft / offsetWidth 而不是 getBoundingClientRect，是因为按钮按下时
     * 有 `transform: scale(0.97)`，那 0.15s 的过渡在 click 之后的 nextTick 里还没收完，
     * rect 量到的是缩小后的盒子（宽度差 ~3px、左边缘偏 ~1.3px），滑块会永久错位。
     * offset* 是布局值，不受 transform 影响。
     *
     * 底槽的 1px 描边因此写成 inset box-shadow 而不是 border：没有 border 时
     * 「相对 padding edge」和「相对 border edge」两种 offsetLeft 实现完全重合，
     * 也正好等于滑块 left:0 所依托的绝对定位包含块原点。
     */
    placeSegmentThumb() {
      const track = this.$refs.segmentTrack;
      if (!track) return;
      // 「更多」入口不是分类，量的时候必须排除
      const items = track.querySelectorAll(".segment-item:not(.segment-more-btn)");
      const index = this.categories.findIndex(
        (item) => String(item.id) === this.activeName
      );
      const el = index >= 0 ? items[index] : null;

      if (!el) {
        this.thumbStyle = { width: "0px", transform: "translateX(0px)", opacity: 0 };
        return;
      }

      this.thumbStyle = {
        width: `${el.offsetWidth}px`,
        transform: `translateX(${el.offsetLeft}px)`,
        opacity: 1,
      };
    },
    /**
     * 视口变化时重量一次（用 rAF 节流，resize 一秒能触发几十次）
     */
    handleViewportResize() {
      if (this.thumbRaf) return;
      this.thumbRaf = window.requestAnimationFrame(() => {
        this.thumbRaf = null;
        this.placeSegmentThumb();
      });
    },
    /**
     * 首屏「向下」：滚到文章列表。
     * 列表容器已有 ref，不再另挂 id；系统开了「减少动态效果」就直接跳，不做平滑滚动
     */
    scrollToPosts() {
      const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
      this.$refs.postsSection?.scrollIntoView({
        behavior: reduce ? 'auto' : 'smooth',
        block: 'start',
      });
    },
    /**
     * 第二屏接力入场：筛选器先到位，之后文章「出现一行就动一行」。
     *
     * 两块用两套触发方式，因为它们要的不是同一件事：
     * · 筛选器是一整块导航，视线一扫就过去，盯它一个元素、进场时挂一次类名就够。
     * · 文章行是「一条一条被读到」的，必须每行各盯各的 —— 只有轮到它的顶边进入
     *   视口下缘，这一行才开始淡入上浮。参考 demo 就是这么写的：每个 .post 自己
     *   挂一个 IntersectionObserver，触发后 unobserve 掉。
     *
     * 早先那版是盯着整个 .posts-section 挂一个类名、靠 nth-child 排队延迟：十行
     * 同时开始、第 4 行以后连延迟都一样，看着像整块闪一下；而真正滚下去的时候，
     * 后半截的行早就演完了，静止躺在那里 —— 这就是「往下划动画不行」的根源。
     *
     * 为什么用 IntersectionObserver 而不是跟着 --hero-p 算：这一段发生时首屏已经
     * 滚完，--hero-p 停在 1 不再变化，没有可用的进度源；入场本身是「一次性事件」，
     * 不是随滚动连续变化的量，观察器正好表达这件事。
     */
    setupSheetReveal() {
      // 系统开了「减少动态效果」：整段不参与，直接保持静止态
      if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        return;
      }
      // 已经滚在页面中间（刷新保留了滚动位置、从详情页返回）就别再演一遍：
      // 这时候内容多半整块在视口里，补一次入场反而像闪一下
      if (window.pageYOffset > 8) return;

      this.revealObservers = [];
      this.revealTimers = [];
      this.revealEnabled = true;
      this.armReveal(this.$refs.filtersEl, "navRevealing");
      // 首屏渲染时列表还没回来，先认领已有的行，后续由 articleList 的 watcher 补
      this.observeRows();
    },
    /**
     * 认领文章行：给每行打上 is-pending（CSS 里就是「透明 + 下沉 26px」），
     * 交给共用的观察器；谁的顶边进视野谁被放出来，放出来一次就不再管它。
     *
     * 用打标记而不是让 CSS 默认隐藏，是为了「JS 没跑 / 没有观察器」时列表仍然
     * 正常可见 —— 不该显示的东西不能变成默认状态。切分类、翻页、首屏数据回来
     * （articleList 一变）都要重新认领一次，新行才有人看着。
     */
    observeRows() {
      if (!this.revealEnabled || typeof IntersectionObserver === "undefined") return;
      const section = this.$refs.postsSection;
      if (!section) return;
      if (!this.rowObserver) {
        this.rowObserver = new IntersectionObserver(
          (entries) => {
            entries.forEach((entry) => {
              if (!entry.isIntersecting) return;
              const row = entry.target;
              row.classList.add("is-revealed");
              this.rowObserver.unobserve(row);
              // 播完把两个类名一起摘掉，把 transition 让回给 .entry 自己
              this.revealTimers.push(
                window.setTimeout(() => {
                  row.classList.remove("is-pending", "is-revealed");
                }, ROW_REVEAL_HOLD_MS)
              );
            });
          },
          // 阈值和边距照抄参考 demo：行露出 15% 才开始、再往里 40px 才认，
          // 免得刚碰到视口下缘就急着演（那会儿只看得见一条边）
          { threshold: 0.15, rootMargin: "0px 0px -40px 0px" }
        );
        this.revealObservers.push(this.rowObserver);
      }
      section.querySelectorAll(".entry").forEach((row) => {
        // 认领过就不再认领：切分类时 Vue 会按键复用旧行，重复认领会让它
        // 再消失一次。标记放 dataset 而不是靠 is-pending —— 那个类名播完要摘掉
        if (row.dataset.revealClaimed === "1") return;
        row.dataset.revealClaimed = "1";
        row.classList.add("is-pending");
        this.rowObserver.observe(row);
      });
    },
    /**
     * 盯着一个元素，等它的顶边快进视野时把开关打开，播完再关掉
     * @param {Element} el 目标元素
     * @param {string} flag 数据里的开关名（navRevealing）
     */
    armReveal(el, flag) {
      if (!el || typeof IntersectionObserver === "undefined") return;
      const observer = new IntersectionObserver(
        (entries) => {
          if (!entries.some((entry) => entry.isIntersecting)) return;
          observer.disconnect();
          this[flag] = true;
          // 播完摘掉类名：动画是 both 填充，类名在的时候 .entry 的 transform
          // 归动画管，hover 的右移会失效，所以不能常驻
          this.revealTimers.push(
            window.setTimeout(() => {
              this[flag] = false;
            }, REVEAL_HOLD_MS)
          );
        },
        { threshold: 0, rootMargin: REVEAL_ROOT_MARGIN }
      );
      observer.observe(el);
      this.revealObservers.push(observer);
    },
    teardownSheetReveal() {
      (this.revealObservers || []).forEach((observer) => observer.disconnect());
      this.revealObservers = [];
      this.rowObserver = null;
      this.revealEnabled = false;
      (this.revealTimers || []).forEach((timer) => clearTimeout(timer));
      this.revealTimers = [];
    },
    /**
     * 跳转到文章详情
     * @param {number} id 文章id
     */
    goToPost(id) {
      this.$router.push(`/post/${id}`);
    },
    /**
     * 切换页码（页码、上一页、下一页翻页）
     * @param {number} page 页码
     */
    changePage(page) {
      const nextParams = {
        ...this.params,
        pageNum: page,
      };

      // 翻页判断：点击相同页码不发起请求，亦不触发过渡动画
      if (!this.isQueryNeeded(nextParams)) {
        return;
      }

      this.executeArticleQuery(nextParams, { scrollToTop: true });
    },
    /**
     * 排序（保留排序翻页支持）
     */
    handleSort(sortField, sortOrder = 'desc') {
      const nextParams = {
        ...this.params,
        sortField,
        sortOrder,
        pageNum: 1,
      };
      if (!this.isQueryNeeded(nextParams)) return;
      this.executeArticleQuery(nextParams, { scrollToTop: true });
    },
    /**
     * 搜索（保留搜索翻页支持）
     */
    handleSearch(keyword) {
      const nextParams = {
        ...this.params,
        keyword: (keyword || '').trim(),
        pageNum: 1,
      };
      if (!this.isQueryNeeded(nextParams)) return;
      this.executeArticleQuery(nextParams, { scrollToTop: true });
    },
    /**
     * 获取文章列表
     */
    getArticleList() {
      this.executeArticleQuery(this.params, { force: true, scrollToTop: false });
    },
    /**
     * 获取轮播和推荐文章
     */
    getCarouselArticles() {
      getCarouselArticlesApi()
        .then((res) => {
          this.carouselSlides = res.data;
        })
        .catch((error) => {
          console.error("Failed to fetch carousel articles:", error);
          this.$message.error("获取轮播图失败");
        });
    },
    /**
     * 获取置顶文章（传给右侧边栏的置顶卡片，不受分类切换影响）
     */
    getStickArticles() {
      getStickArticlesApi()
        .then((res) => {
          this.stickyPosts = res.data || [];
        })
        .catch((error) => {
          console.error("Failed to fetch sticky articles:", error);
        });
    },
    /**
     * 获取分类（按文章数量排序，取最多的前 8 个，避免分类栏拥挤）
     */
    async getAllCategories() {
      try {
        const res = await getCategoriesApi();
        const raw = res.data || [];
        // 统计文章数并倒序排列
        const list = raw
          .map((cat) => ({
            id: cat.id,
            name: cat.name,
            articleCount: (cat.posts || []).filter((p) => p && p.id).length,
          }))
          .sort((a, b) => b.articleCount - a.articleCount);

        // 取文章数排名前 8 的热门分类
        const top8 = list.slice(0, 8);
        this.categories = [{ id: "all", name: "全部" }, ...top8];
        this.$nextTick(() => this.placeSegmentThumb());
      } catch (err) {
        console.error("Failed to fetch top categories:", err);
        getAllCategoriesApi()
          .then((fallbackRes) => {
            const fallbackList = (fallbackRes.data || []).slice(0, 8);
            this.categories = [{ id: "all", name: "全部" }, ...fallbackList];
            this.$nextTick(() => this.placeSegmentThumb());
          })
          .catch((retryErr) => {
            // 两次都失败：分段选择器只剩「全部」，至少让用户知道筛选没加载出来
            console.error("Failed to fetch categories on retry:", retryErr);
            this.$message.error("获取分类失败");
          });
      }
    },
  },
  watch: {
    /**
     * 列表数据一换（首屏返回、切分类、翻页）就重新认领文章行：
     * 复用到的旧行带着 is-revealed 直接可见，只有新行才置为待进场。
     * 必须等 DOM 打完补丁（nextTick），否则摸到的还是上一批 <li>。
     */
    articleList() {
      this.$nextTick(() => this.observeRows());
    },
  },
  created() {
    // 首次进入页面，直接进入 loading 骨架屏态，请求返回后平滑淡入
    this.transitionPhase = "loading";
    this.getArticleList();
    this.getCarouselArticles();
    this.getStickArticles();
    this.getAllCategories();
  },
  mounted() {
    this.placeSegmentThumb();
    this.setupSheetReveal();
    window.addEventListener("resize", this.handleViewportResize);
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.handleViewportResize);
    // 观察器和收尾定时器都会写实例状态，离开页面必须先断掉
    this.teardownSheetReveal();
    if (this.thumbRaf) {
      window.cancelAnimationFrame(this.thumbRaf);
      this.thumbRaf = null;
    }
    if (this.skeletonTimer) {
      clearTimeout(this.skeletonTimer);
      this.skeletonTimer = null;
    }
    // 进场动画收尾定时器：回调会写 transitionPhase / loading / lockedMinHeight，
    // 卸载后再执行就是在给一个已经下线的实例改状态
    if (this.transitionTimer) {
      clearTimeout(this.transitionTimer);
      this.transitionTimer = null;
    }
  },
};
</script>

<style lang="scss" scoped>
// 固定顶栏高度。它是 Header 自己的 scoped 变量，跨文件拿不到，只能在这里重写一份：
// 对应 src/styles/global.scss:17 的 #app padding-top 与
// src/layout/Header/index.vue 的 $header-height，三处必须同步修改
$header-offset: 64px;

// 整屏大图要通栏，所以顶掉 .home 的 max-width/padding，单独放在外层。
// 注意：这里不能加 overflow:hidden —— 大图靠 -64px 负边距顶到页面最顶部，
// 一旦这里变成裁剪容器，负边距那段就会被切掉，露出一条页面底色
.home-page {
  width: 100%;
}

// 首屏「向下」把列表滚到 y=0，列表顶端会被固定顶栏盖住：
// 留出顶栏高度 + 一小段呼吸距离，落点正好在顶栏下缘往下一点点
.posts-section {
  scroll-margin-top: calc(#{$header-offset} + #{$spacing-sm});
}

// ── 第二屏接力入场 ──
// 首屏滚走之后，接力的是面板里的内容：筛选器先到位，文章行「出现一行动一行」——
// 谁的顶边撞进视口下缘，谁才开始淡入上浮（参考 demo 里每个 .post 各挂一个
// IntersectionObserver 的做法，见 setupSheetReveal / observeRows）。
//
// 缓动不用 0.16/1/0.3/1（那是「弹射」曲线：头 150ms 就冲完 90% 的位移，剩下
// 700ms 几乎不动，看着极快又没有余韵）。0.2/0.7/0.2/1 起步柔、中段匀速、
// 尾段慢慢收，时长拉长之后才看得出「在浮起来」而不是「闪一下」。
$reveal-ease: cubic-bezier(0.2, 0.7, 0.2, 1);

.home-sheet {
  // 筛选器：只走 18px，轻一点 —— 它是「导航」，不该抢文章的戏
  &.is-revealing-nav .filters-segmented-wrap {
    animation: sheetRise 0.9s $reveal-ease both;
  }

  // 文章行：ArticleList 是子组件，scoped 样式必须走 :deep()
  // 进场用 translate 属性、hover 用 transform，两条互不覆盖 —— 所以类名可以
  // 一直留着，不像早先那版还得在动画播完摘掉才能让 .entry:hover 生效。
  :deep(.entry.is-pending) {
    opacity: 0;
    translate: 0 26px;
    // 过渡写在起始态：下一帧打上 .is-revealed 才会补间，否则瞬间到位
    transition: opacity 0.9s $reveal-ease, translate 0.9s $reveal-ease;
  }

  :deep(.entry.is-pending.is-revealed) {
    opacity: 1;
    translate: 0 0;
  }
}

@keyframes sheetRise {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

// 系统开了「减少动态效果」：这段纯装饰，整段不播
// （JS 那边也不会挂类名，这里是二道保险，比如系统设置在运行中改掉）
@media (prefers-reduced-motion: reduce) {
  .home-sheet {
    &.is-revealing-nav .filters-segmented-wrap {
      animation: none;
    }

    // 文章行：JS 那边不会打 is-pending，这里再兜一道
    :deep(.entry.is-pending) {
      opacity: 1;
      translate: none;
      transition: none;
    }
  }
}

// 文章面板：首页的「窗帘」。
// 大图（hero.vue 的 .hero）sticky 钉在视口顶上、z-index: 1；这一层 z-index: 2，
// 整块从底部往上盖住它 —— 往下滚时图不动、面板滑上来，接缝处一道圆角 + 一道投影。
// 大图正好一屏（--sheet-peek 为 0），面板从折线以下切进来。
// 面板高度和三处判据都读 global.scss 里同一个变量。
.home-sheet {
  position: relative;
  z-index: 2;
  // 至少一屏：滑上来之后必须把底下那张图完全盖住
  min-height: 100vh;
  min-height: 100svh;
  // 只有顶边是圆角，下边接着页面往下走
  border-radius: 32px 32px 0 0;
  // 投影往上打：面板滑上来时先看到一道阴影压到图上，层次才立得住。
  // 第二道 1px 是分隔线，取主题相关的 --sheet-edge（深色档下黑投影不起作用，
  // 全靠这道亮线），否则夜里面板边和底色糊成一片，看不出「滑上来一层」
  box-shadow: 0 -24px 60px rgba(9, 12, 16, 0.28), 0 -1px 0 var(--sheet-edge);
  // 兜底底色。面板必须不透明才盖得住图，而整页底色本来是 html::before 那层
  // fixed 的天空 + html::after 的星空（见 styles/global.scss）—— 一层不透明的底
  // 会把它们整块挡掉，所以下面把同一套画法在自己身上重铺一遍。
  background-color: var(--background);

  &::before,
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    // -1：贴在面板内容下面、面板自身之上。.home-sheet 有 z-index，
    // 已经是个层叠上下文，所以 -1 不会掉到 sticky 大图的后面
    z-index: -1;
    border-radius: inherit;
    pointer-events: none;
    // 关键：渐变按视口定位而不是按这块很高的面板定位，才会和 html 那层对得
    // 严丝合缝 —— 面板滑到哪儿，底色都和周围一模一样，看不出「换了一层」
    background-attachment: fixed;
    background-repeat: no-repeat;
    // 已知降级：Safari（尤其 iOS）对 fixed 定位背景支持不稳，真机上可能退回按
    // 面板自身盒子定位，天空会被拉长成一块很淡的色带。不会有硬接缝（面板顶边
    // 上面一律是图，碰不到 html 那层），只是观感偏淡，可以接受
  }

  // 天空：与 html::before 同源（styles/mixins.scss 的 sky-paint）。
  // 这里不接 160s 的漂移：面板刚露头时接缝处只有图、没有底色，看不出相位差；
  // 等它盖满视口，html 那层本来也整个被挡掉了
  &::before {
    @include sky-paint;
  }

  // 星空：与 html::after 同源，只在天黑时淡入（靠 --sky-stars）
  &::after {
    @include star-paint;
    background-repeat: repeat;
    opacity: var(--sky-stars, 0);
  }
}

.home {
  max-width: $container-max;
  margin: 0 auto;
  width: 100%;
  // 左右内边距走版心令牌：和 Hero 的 .hero-inner 共用同一条边界，
  // 否则首屏的站名和下方台账的左边缘对不齐（差一截很显眼）
  padding: clamp(36px, 5vh, 60px) $container-gutter $spacing-xl;

  @include responsive(lg) {
    // 窄屏这一档也要跟着版心令牌走：原来的 16px 会和 .hero-inner 的 gutter 错开
    padding: $spacing-lg $container-gutter;
  }
}

.content-layout {
  display: grid;
  // 侧栏卡片自带 16px 内边距，列宽从 286px 提到 300px，让卡内文字宽度和改造前持平
  // 宽度走版心令牌：首屏的整屏图不再和它对位，但列宽本身仍是内容列的一部分
  grid-template-columns: minmax(0, 1fr) $sidebar-w;
  gap: clamp(36px, 4vw, 64px);
  align-items: start;

  @include responsive(lg) {
    grid-template-columns: 1fr;
    gap: $spacing-xl;
  }
}

.home-main-content {
  min-width: 0;
  width: 100%;
}

// ── macOS 胶囊分段选择器 (Segmented Control) ──
.filters-segmented-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--rule);

  .segmented-track {
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: 3px;
    padding: 3px;
    background: rgba(0, 0, 0, 0.04);
    // 描边走 inset 阴影而不是 border。滑块靠 getBoundingClientRect 求差定位，
    // 没有 border 时底槽的 padding box 就是 border box，基准才唯一
    box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.04);
    border-radius: 999px;
    backdrop-filter: blur(12px) saturate(160%);
    -webkit-backdrop-filter: blur(12px) saturate(160%);
    transition: background 0.25s ease, box-shadow 0.25s ease;
  }

  // 唯一的激活滑块：位置与宽度由 placeSegmentThumb() 内联写入，
  // 切换分类时它一路滑过去，而不是每个按钮各自涂一块底色
  .segment-thumb {
    position: absolute;
    top: 3px;
    bottom: 3px;
    left: 0;
    z-index: 0;
    width: 0;
    opacity: 0;
    border-radius: 999px;
    background: #ffffff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
    pointer-events: none;
    will-change: transform, width;
    // Apple 阻尼曲线：滑行不是匀速到位，末端有个「吸附」的减速
    transition: transform 0.38s cubic-bezier(0.16, 1, 0.3, 1),
      width 0.38s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.2s ease,
      background 0.25s ease, box-shadow 0.25s ease;
  }

  @media (prefers-reduced-motion: reduce) {
    .segment-thumb {
      transition: opacity 0.2s ease, background 0.25s ease, box-shadow 0.25s ease;
    }
  }

  .segment-item {
    // 必须显式压在滑块之上：滑块是定位元素，不设 z-index 会盖住按钮文字
    position: relative;
    z-index: 1;
    display: inline-flex;
    align-items: center;
    padding: 5px 15px;
    border: none;
    border-radius: 999px;
    background: transparent;
    color: var(--text-secondary);
    font-size: 0.84rem;
    font-weight: 450;
    cursor: pointer;
    white-space: nowrap;
    transition: color 0.2s cubic-bezier(0.16, 1, 0.3, 1),
      background 0.2s cubic-bezier(0.16, 1, 0.3, 1), transform 0.15s ease;

    &:hover:not(.is-active) {
      color: var(--text-primary);
      background: rgba(0, 0, 0, 0.035);
    }

    &:active {
      transform: scale(0.97);
    }

    &.is-active {
      color: var(--text-primary);
      font-weight: 550;
      // 白底卡片交给 .segment-thumb，这里只留文字
    }

    // 文章计数微标：等宽数字 + 小圆角气泡
    .segment-count {
      margin-left: 6px;
      padding: 2px 5px;
      border-radius: 999px;
      background: rgba(0, 0, 0, 0.05);
      // tertiary #8b949f 在纸白底上只有 2.84:1，够不到 AA 正文的 4.5:1，用 secondary（≈6.4:1）
      color: var(--text-secondary);
      font-family: $font-mono;
      font-variant-numeric: tabular-nums;
      font-size: 0.72rem;
      line-height: 1;
      transition: background 0.2s ease, color 0.2s ease;
    }

    &.is-active .segment-count {
      background: var(--accent-soft);
      color: var(--accent);
    }

    &.segment-more-btn {
      text-decoration: none;
      gap: 4px;
      font-size: 0.76rem;
      color: var(--text-secondary);
      padding: 5px 12px;

      svg {
        transition: transform 0.2s ease;
      }

      &:hover {
        color: var(--accent);

        svg {
          transform: translateX(1.5px);
        }
      }
    }
  }

  .meta-status-capsule {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    padding: 4px 11px;
    border-radius: 999px;
    background: rgba(0, 0, 0, 0.03);
    border: 1px solid var(--rule);
    flex: none;

    .status-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--accent);
      box-shadow: 0 0 6px var(--accent-soft);
    }

    .total-text {
      font-family: $font-mono;
      font-size: 0.72rem;
      color: var(--text-secondary);
      letter-spacing: 0.02em;
    }
  }
}

// 深色模式适配
:root[data-theme='dark'] .filters-segmented-wrap {
  .segmented-track {
    background: rgba(255, 255, 255, 0.06);
    box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
  }

  // 微光灰玻滑块：半透明 + 顶边内发光高光线
  .segment-thumb {
    background: rgba(255, 255, 255, 0.16);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.12);
  }

  .segment-item {
    &:hover:not(.is-active) {
      background: rgba(255, 255, 255, 0.05);
    }

    .segment-count {
      background: rgba(255, 255, 255, 0.07);
    }
  }

  .meta-status-capsule {
    background: rgba(255, 255, 255, 0.04);
  }
}

@include responsive(md) {
  .filters-segmented-wrap {
    overflow-x: auto;
    padding-bottom: 12px;
    gap: 10px;
    -webkit-overflow-scrolling: touch;

    &::-webkit-scrollbar {
      display: none;
    }

    .segmented-track {
      flex: none;
    }

    .meta-status-capsule {
      display: none;
    }
  }
}
</style>
