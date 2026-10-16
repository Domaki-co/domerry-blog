<template>
  <div class="home-page">
    <!-- 首屏整屏大图：轮播文章封面自动切换，点向下箭头进入文章列表 -->
    <Hero :carousels="carouselSlides" @scroll-down="scrollToPosts" />

    <div class="home" ref="homeContent">
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
            <div class="meta-status-capsule">
              <span class="status-dot"></span>
              <span class="total-text">共 {{ total }} 篇</span>
            </div>
          </nav>

          <!-- 列表只渲染一次,筛选器仅改查询条件,避免每个分类面板重复挂一份完整列表 -->
          <div ref="postsSection">
            <ArticleList
              :articles="articleList"
              :loading="loading"
              :total="total"
              :params="params"
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
      },
      articleList: [],
      carouselSlides: [],
      stickyPosts: [],
      loading: false,
      activeName: "all",
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
     * 切换分类
     * @param {object} category 分类对象
     */
    handleClick(category) {
      this.activeName = String(category.id);
      this.params.categoryId = category.id === "all" ? null : category.id;
      this.params.pageNum = 1;
      this.getArticleList();
      // 等 is-active 落到新按钮上再量宽度：激活态字重更重，宽度和常态不同
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
     * 跳转到文章详情
     * @param {number} id 文章id
     */
    goToPost(id) {
      this.$router.push(`/post/${id}`);
    },
    /**
     * 从首屏大图平滑滚动到文章列表（用 rect+scrollY 而不是 offsetTop，
     * 免得被 #app 的 padding 和 hero 的负边距影响）
     */
    scrollToPosts() {
      const el = this.$refs.homeContent;
      if (!el) return;
      // 顶栏是 fixed 且高 64px，减去它列表第一行才不会被压住
      const top = el.getBoundingClientRect().top + window.scrollY - 64;
      window.scrollTo({ top: Math.max(top, 0), behavior: "smooth" });
    },
    /**
     * 切换页码
     * @param {number} page 页码
     */
    changePage(page) {
      this.params.pageNum = page;
      this.getArticleList();
      window.scrollTo({
        top: this.$refs.postsSection?.offsetTop - 80,
        behavior: "smooth",
      });
    },
    /**
     * 获取文章列表
     */
    getArticleList() {
      this.loading = true;
      getArticlesApi(this.params)
        .then((res) => {
          this.articleList = res.data.records;
          this.total = res.data.total;
        })
        .catch((error) => {
          console.error("Failed to fetch articles:", error);
        })
        .finally(() => {
          this.loading = false;
          // total 决定「全部」后面的数字微标，量完宽度滑块才不会错位
          this.$nextTick(() => this.placeSegmentThumb());
        });
    },
    /**
     * 获取轮播和推荐文章
     */
    getCarouselArticles() {
      getCarouselArticlesApi().then((res) => {
        this.carouselSlides = res.data;
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
        getAllCategoriesApi().then((fallbackRes) => {
          const fallbackList = (fallbackRes.data || []).slice(0, 8);
          this.categories = [{ id: "all", name: "全部" }, ...fallbackList];
          this.$nextTick(() => this.placeSegmentThumb());
        });
      }
    },
  },
  created() {
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
  },
};
</script>

<style lang="scss" scoped>
// 整屏大图要通栏，所以顶掉 .home 的 max-width/padding，单独放在外层。
// 注意：这里不能加 overflow:hidden —— 大图靠 -64px 负边距顶到页面最顶部，
// 一旦这里变成裁剪容器，负边距那段就会被切掉，露出一条页面底色
.home-page {
  width: 100%;
}

.home {
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  // 左右内边距必须和 Hero 的 .hero-inner 用同一个 clamp，
  // 否则大图上的站名和下方台账的左边缘对不齐（差 24px 很显眼）
  padding: clamp(36px, 5vh, 60px) clamp(20px, 4vw, 48px) $spacing-xl;

  @include responsive(lg) {
    padding: $spacing-lg $spacing-md;
  }
}

.content-layout {
  display: grid;
  // 侧栏卡片自带 16px 内边距，列宽从 286px 提到 300px，让卡内文字宽度和改造前持平
  grid-template-columns: minmax(0, 1fr) 300px;
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
      color: var(--text-tertiary);
      font-family: $font-mono;
      font-variant-numeric: tabular-nums;
      font-size: 0.66rem;
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
      color: var(--text-tertiary);
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
      color: var(--text-tertiary);
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
