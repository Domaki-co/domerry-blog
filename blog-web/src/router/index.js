import { createRouter, createWebHistory } from 'vue-router'
// 所有页面组件一律走动态 import：静态 import 会把文章详情页的 highlight.js、
// 相册、编辑器等全部并入首屏模块图（dev 下首屏 JS 解压体积 8MB 主要来自这里）。
import store from '@/store';

const routes = [

    {
        path: "/",
        component: () => import('@/layout/index.vue'),
        meta: {
            title: "doma's blog - 一个专注于技术分享的博客平台",
            loading: true
        },
        children: [
            {
                path: '/',
                name: 'Home',
                component: () => import('@/views/home/index.vue'),
                meta: {
                    title: '首页',
                    transition: 'fade',
                    icon: 'fas fa-home',
                    loading: true
                 }
              },
              {
                path: '/archive',
                name: 'Archive',
                component: () => import('@/views/archives/index.vue'),
                meta: {
                  transition: 'fade',
                  title: "归档 - doma's blog",
                  icon: 'fas fa-archive'
                }
              },
              {
                path: '/categories',
                name: 'Categories',
                component: () => import('@/views/categories/index.vue'),
                meta: {
                    transition: 'fade',
                    title: "分类 - doma's blog",
                    icon: 'fas fa-folder'
                 }
              },
              {
                path: '/tags',
                name: 'Tags',
                component: () => import('@/views/tags/index.vue'),
                meta: {
                    transition: 'fade',
                    title: "标签 - doma's blog",
                    icon: 'fas fa-tags'
                }
              },
              {
                path: '/moments',
                name: 'Moments',
                component: () => import('@/views/moments/index.vue'),
                meta: {
                  title: "说说 - doma's blog",
                  icon: 'fas fa-comment-dots'
                }
              },
              {
                path: '/photos',
                name: 'Photos',
                component: () => import('@/views/photos/index.vue'),
                meta: {
                    transition: 'fade',
                    title: "相册 - doma's blog",
                    icon: 'fas fa-images'
                }
              },
              {
                path: '/photos/:id',
                name: 'PhotoDetail',
                component: () => import('@/views/photos/detail.vue'),
                meta: {
                    transition: 'fade',
                    title: "相册详情 - doma's blog",
                    icon: 'fas fa-images',
                    hidden: true
                }
              },
              {
                path: '/hotSearch',
                name: 'HotSearch',
                component: () => import(/* webpackPrefetch: true */ '@/views/hotSearch/index.vue'),
                meta: {
                  transition: 'fade',
                  title: "热搜 - doma's blog",
                  icon: 'fas fa-fire',
                  hidden: true
                }
              },
              {
                path: '/resources',
                name: 'Resources',
                component: () => import('@/views/resources/index.vue'),
                meta: {
                  title: "音乐 - doma's blog",
                  icon: 'fas fa-music'

                }
              },
              {
                path: '/messages',
                name: 'Messages',
                component: () => import('@/views/messages/index.vue'),
                meta: {
                  transition: 'fade',
                  title: "留言板 - doma's blog",
                  icon: 'fas fa-comments'
                }
              },
              {
                path: '/friends',
                name: 'Friends',
                component: () => import(/* webpackPrefetch: true */ '@/views/friends/index.vue'),
                meta: {
                  transition: 'fade',
                  title: "友情链接 - doma's blog",
                  icon: 'fas fa-user-friends'
                }
              },
              {
                path: '/about',
                name: 'About',
                component: () => import('@/views/about/index.vue'),
                meta: {
                  transition: 'fade',
                  title: "关于本站 - doma's blog",
                  icon: 'fas fa-info-circle'
                }
              },
              {
                path: '/post/:id',
                name: 'Post',
                component: () => import('@/views/article/index.vue'),
                props: true,
                meta: {
                  hidden: true
                }
              },
              {
                path: '/user/profile',
                name: 'Profile',
                component: () => import(/* webpackPrefetch: true */ '@/views/profile/index.vue'),
                meta: {
                  title: "个人主页 - doma's blog",
                  icon: 'fas fa-user',
                  hidden: true
                }
              },
              {
                path: '/editor',
                name: 'Editor',
                component: () => import(/* webpackPrefetch: true */ '@/views/editor/index.vue'),
                meta: {
                  title: "写文章 - doma's blog",
                  icon: 'fas fa-edit',
                  requireAuth: true,
                  hidden: true
                }
              },
              {
                path: '/login',
                name: 'Login',
                component: () => import('@/views/Login/index.vue'),
                meta: {
                  title: '登录',
                  hidden: true,
                  fullscreen: true
                }
              },
              {
                path: '/notifications',
                name: 'Notifications',
                component: () => import('@/views/notifications/index.vue'),
                meta: {
                  title: '消息通知',
                  requiresAuth: true,
                  hidden: true
                }
              },
              {
                path: '/:pathMatch(.*)*',
                name: 'NotFound',
                component: () => import('@/views/404/404.vue'),
                meta: {
                  hidden: true
                }
              }
        ]
    }
]

const router = createRouter({
  history: createWebHistory('/'),
  routes,
  scrollBehavior(to, from, savedPosition) {
    return { left: 0, top: 0 }
  }
})


// 解决重复点击导航时，控制台出现报错
// vue-router 4 的实例方法直接挂在 router 上（原型链上是空对象，没有可打补丁的
// VueRouter.prototype.push），所以改成在实例上包装一次，统一吞掉 push 的 rejection。
// 注：v4 里"重复导航"已经改成 resolve 出 NavigationFailure 而不是 reject，
// 这里主要是兜住守卫里抛出的错误，行为与 Vue 2 版保持一致。
const routerPush = router.push
router.push = function push(to) {
  return routerPush.call(this, to).catch(err => err)
}


router.beforeEach((to) => {
  if (to.meta.title) {
    document.title = to.meta.title
  }
  //关闭搜索框
  store.commit('SET_SEARCH_VISIBLE', false)
})

export default router
