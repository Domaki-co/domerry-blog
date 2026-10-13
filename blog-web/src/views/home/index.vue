<template>
  <div class="home-page">
    <!-- 首屏整屏大图：轮播文章封面自动切换，点向下箭头进入文章列表 -->
    <Hero :carousels="carouselSlides" @scroll-down="scrollToPosts" />

    <div class="home" ref="homeContent">
      <div class="content-layout">
        <main class="home-main-content">
          <!-- 侧边栏在 <=1024px 时整体隐藏，这里给移动端补一个折叠式置顶入口 -->
          <MobileStickyBar :sticks="stickyPosts" />

          <!-- 分类筛选：文字 + 当前项下划线，不用 Element Plus 的标签页（样式太重） -->
          <nav class="filters">
            <a
              v-for="category in categories"
              :key="category.id"
              href="javascript:void(0)"
              :class="{ 'is-current': activeName === String(category.id) }"
              @click="handleClick(category)"
            >
              {{ category.name }}
            </a>
            <span class="total">共 {{ total }} 篇</span>
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
     * 获取所有分类
     */
    getAllCategories() {
      getAllCategoriesApi().then((res) => {
        this.categories.push(...res.data);
      });
    },
  },
  created() {
    this.getArticleList();
    this.getCarouselArticles();
    this.getStickArticles();
    this.getAllCategories();
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
  grid-template-columns: minmax(0, 1fr) 286px;
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

// ── 分类筛选行 ──
.filters {
  display: flex;
  align-items: baseline;
  gap: clamp(14px, 1.6vw, 24px);
  padding-bottom: 14px;
  border-bottom: 1px solid var(--rule-strong);
  font-size: 0.9rem;
  flex-wrap: wrap;

  a {
    color: var(--text-secondary);
    text-decoration: none;
    padding-bottom: 2px;
    border-bottom: 1px solid transparent;
    transition: color 0.2s ease, border-color 0.2s ease;
    white-space: nowrap;

    &:hover {
      color: var(--text-primary);
    }

    &.is-current {
      color: var(--text-primary);
      border-bottom-color: var(--accent);
    }
  }

  .total {
    margin-left: auto;
    font-family: $font-mono;
    font-size: 0.74rem;
    color: var(--text-tertiary);
    letter-spacing: 0.03em;
  }
}

@include responsive(md) {
  .filters {
    // 窄屏横向滚动，避免分类多了换行把列表推下去
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 12px;

    &::-webkit-scrollbar {
      display: none;
    }

    .total {
      display: none;
    }
  }
}
</style>
