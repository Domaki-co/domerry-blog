<template>
  <div class="home-page">
    <!-- 首屏：全屏封面图 + 居中站名，「向下」滚到文章列表 -->
    <Hero :carousels="carouselSlides" @scroll-to-posts="scrollToPosts" />

    <!--
      文章面板：一片圆角浅色板，从底部往上盖住 sticky 钉住的封面 —— 首页的「窗帘」。
      它必须不透明（盖住底下那张图），所以自己把全站的天空 / 星空重铺了一遍，
      见 .home-sheet 的样式注释。
    -->
    <div class="home-sheet">
      <div class="home">
        <div class="content-layout">
          <main class="home-main-content">
            <!-- 侧边栏在 <=1024px 时整体隐藏，这里给移动端补一个折叠式置顶入口 -->
            <MobileStickyBar :sticks="stickyPosts" />

            <!-- 分类筛选：macOS 胶囊分段选择器 (Segmented Control)。
                 激活态不是各按钮自己涂底色，而是由 .segment-thumb 这一个滑块承担，
                 切换分类时它平移过去，做出系统设置侧栏那种「滑行」的手感 -->
            <nav class="filters-segmented-wrap">
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
    window.addEventListener("resize", this.handleViewportResize);
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.handleViewportResize);
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

// 文章面板：首页的「窗帘」。
// 大图（hero.vue 的 .hero）sticky 钉在视口顶上、z-index: 1；这一层 z-index: 2，
// 整块从底部往上盖住它 —— 往下滚时图不动、面板滑上来，接缝处一道圆角 + 一道投影。
// 大图因此比视口矮 --sheet-peek（84px），首屏露出的那条边就是「下面还有内容」的提示。
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
