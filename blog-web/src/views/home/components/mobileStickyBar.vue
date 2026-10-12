<template>
  <div class="mobile-sticky" v-if="sticks.length > 0">
    <div class="sticky-head" @click="toggle">
      <i class="fas fa-thumbtack"></i>
      <span class="head-title">置顶</span>
      <span class="head-count">{{ sticks.length }}</span>
      <i class="fas fa-chevron-down head-arrow" :class="{ 'is-open': expanded }"></i>
    </div>
    <transition name="sticky-slide">
      <div class="sticky-body" v-show="expanded">
        <router-link
          v-for="(post, index) in sticks"
          :key="post.id"
          :to="`/post/${post.id}`"
          class="sticky-item"
        >
          <span class="sticky-index">{{ index + 1 }}</span>
          <span class="sticky-title" :title="post.title">{{ post.title }}</span>
        </router-link>
      </div>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'MobileStickyBar',
  props: {
    sticks: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      //手机上默认折叠：只留一行标题栏，免得占掉本就紧张的竖向空间
      expanded: false
    }
  },
  methods: {
    toggle() {
      this.expanded = !this.expanded
    }
  }
}
</script>

<style lang="scss" scoped>
//侧边栏（作者卡 / 公告 / 推荐 / 置顶 / 标签云）在 <=1024px 时整体 display:none，
//这里补一个移动端专用的置顶入口，断点与 Sidebar 的 @include responsive(lg) 严格对齐，
//保证任何宽度下都是「要么侧边栏有置顶卡、要么这里有置顶栏」，不会同时出现或同时消失
.mobile-sticky {
  display: none;
  margin-bottom: $spacing-lg;
  background: var(--card-bg);
  border: 1px solid var(--border-color);
  border-radius: $border-radius-md;
  overflow: hidden;

  @include responsive(lg) {
    display: block;
  }
}

.sticky-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px $spacing-md;
  cursor: pointer;
  user-select: none;

  .fa-thumbtack {
    color: #8b5cf6;
    transform: rotate(45deg);
  }

  .head-title {
    font-weight: 600;
    color: var(--text-primary);
  }

  .head-count {
    min-width: 20px;
    height: 20px;
    padding: 0 6px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #fff;
    border-radius: 10px;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
  }

  .head-arrow {
    margin-left: auto;
    color: var(--text-secondary);
    transition: transform 0.3s ease;

    &.is-open {
      transform: rotate(180deg);
    }
  }
}

.sticky-body {
  padding: 0 $spacing-md $spacing-sm;
}

.sticky-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: $border-radius-sm;
  text-decoration: none;
  transition: all 0.3s ease;

  & + .sticky-item {
    margin-top: 4px;
  }

  &:hover,
  &:active {
    background: var(--hover-bg);
    transform: translateX(4px);
  }
}

.sticky-index {
  flex: 0 0 auto;
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #fff;
  border-radius: 6px;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
}

.sticky-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sticky-slide-enter-active,
.sticky-slide-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.sticky-slide-enter,
.sticky-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
