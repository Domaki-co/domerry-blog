<template>
  <div>
    <main class="app-content">
      <Announcement />
      <!-- 内容 -->
      <!-- vue-router 4 要求用插槽写法包 Transition，否则 dev 下会打印弃用警告 -->
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" :key="$route.fullPath" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script>
import Announcement from "@/components/Announcement/index.vue";
import { getToken } from "@/utils/cookie";
import { getSystemNoticeApi } from "@/api/message";
export default {
  components: {
    Announcement,
  },
  beforeRouteUpdate(to, from, next) {
    if (getToken()) {
      getSystemNoticeApi().then((res) => {
        this.$store.state.isUnread = res.data
      }).catch(() => {
        // 未读数是顶栏小红点，拉不到就维持上一次的值即可；
        // 这里在每次路由更新时都会执行，用 toast 会在每次导航时刷屏，故只留控制台告警
        console.warn('获取未读消息数失败');
      });
    }
    next();
  },
};
</script>
<style lang="scss">
// 页面过渡动画
.page-enter-active {
  animation: fade-in 0.3s;
}
.page-leave-active {
  animation: fade-out 0.3s;
}

@keyframes fade-in {
  0% {
    opacity: 0;
    transform: scale(0.95);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes fade-out {
  0% {
    opacity: 1;
    transform: scale(1);
  }
  100% {
    opacity: 0;
    transform: scale(1.05);
  }
}
</style>
