# AGENTS.md - 拾壹博客门户前端（blog-web）

本目录是博客的公开门户，也包含登录、个人中心、投稿、音乐和聊天等用户功能。Vue **2.7.16**（不是 Vue 3）+ JavaScript + Vite 5（`@vitejs/plugin-vue2`）+ Element UI（2.15，不是 Element Plus）+ Vuex 3（单 store，无模块）+ Vue Router 3（history 模式）。

## 常用命令

在本目录执行：

```bash
npm install
npm run dev      # 端口 3001
npm run build
npm run preview
```

没有 lint / test 脚本。提交前至少执行 `npm run build`；涉及交互、响应式布局或登录态时，在浏览器中验证对应用户流程。

## 目录职责

- `src/api/`：门户接口封装（article、auth、member、music、chat、site 等 17 个模块），导出函数统一 `xxxApi` 命名。
- `src/views/`：路由页面：首页、article（文章详情）、archives、categories、tags、moments（说说）、photos（相册）、resources（**音乐页**）、messages（留言板）、friends、about、editor（写文章）、chat（群聊）、notifications、profile、Login、404。
- `src/layout/`：站点框架（Header / Footer / MobileMenu）；`player.vue` 是 APlayer 音乐播放器，在 **App.vue** 里引入，全站常驻。
- `src/components/`：业务组件；`common/` 放全局注册的基础组件（`mj-emoji`、`mj-image-preview`）。
- `src/router/index.js`：所有路由是 Layout 的 children；守卫里设置 `document.title`。
- `src/store/index.js`：Vuex 全局状态（userInfo 持久化到 sessionStorage `"user"`，token 存 cookie）。
- `src/directives/`：`v-click-outside`、`v-loading`、`v-animate-on-scroll`（均全局注册）。
- `src/styles/`：`variables.scss`、`mixins.scss`、`global.scss`、`elmentui.scss`（文件名就是这个拼写，别"修正"）。

## 请求与认证

- 所有 HTTP 请求走 `src/utils/request.js`（baseURL = `VITE_APP_BASE_API`，timeout 30s），**视图组件里不要自建 axios 实例**。门户接口路径以 `/api/` 开头（如 `/api/article/list`）。
- 请求拦截器从 cookie 读 token 放进 `Authorization` 头；响应拦截器：`code === 200` 放行，`401` → 清 token、清 userInfo、跳 `/login`，其他 reject `message`。
- 聊天页（`views/chat/index.vue`）自己建 WebSocket 连接，地址来自 `VITE_APP_WEBSOCKET_API`。
- **已知例外**：`src/api/music.js` 用原生 `fetch` 直连第三方网易云代理（`api.chksz.com`，key 硬编码在文件里）——这是历史遗留，不要在新代码中复制这种模式（key 不该进源码）。

## 音乐功能

- `/resources` 路由（`views/resources/`）是音乐页：网易云歌曲搜索、歌单链接导入、播放队列。
- 全站底部播放器在 `layout/player.vue`，歌单来自后端 `/third/getSongList`（`src/api/file.js`），歌词解码用 `src/utils/encoding.js` 的字符集探测。
- 后端对应实现在 `blog/mojian-file`（ThirdPartyController 等），改歌曲相关接口时两边同步。

## 编码约定

- **Options API everywhere**：现有 48 个 `.vue` 全部是 `export default {}`（data/methods/computed），不要引入 `<script setup>`、Composition API、Pinia 或 Element Plus。
- 路由 `meta` 字段：`title`、`hidden`、`fullscreen`、`transition`、`requireAuth`。**注意 `requireAuth` 和 `requiresAuth` 两种拼写在 router 里并存**，新代码统一用 `requireAuth`。
- 深色模式：`src/utils/theme.js` 切换 `documentElement` 的 `data-theme="dark"`，偏好存 localStorage `theme-mode`，回退 `prefers-color-scheme`；组件样式应使用 `var(--line)`、`var(--hover-bg)` 等 CSS 变量和 `$dark` map，不要写死颜色。
- SCSS 变量/混入已通过 vite `additionalData` 全局注入每个组件，无需 import；断点 `$breakpoint-sm 640 / md 768 / lg 1024 / xl 1280`。
- 全局注册可复用：`svg-icon`（SVG 源文件放 `src/assets/icons/`，symbolId `icon-[dir]-[name]`）、`mj-emoji`、`mj-image-preview`、`mavon-editor`、vue-lazyload。
- Markdown 能力已有三套：`mavon-editor`（全局组件）、`vditor`（editor 页）、`marked + highlight.js`（文章渲染，atom-one-dark）。改动编辑/渲染时同步考虑 XSS 防护和历史文章兼容。

## 配置与联调

- `.env.development`：`VITE_APP_BASE_API=/dev-api`、`VITE_APP_API_URL=http://localhost:8900`（**后端端口是 8900，需先启动后端**）、`VITE_APP_WEBSOCKET_API=ws://localhost:8900/websocket/`。Vite 代理把 `/dev-api` 转发到后端并去掉前缀。
- `.env.production`：`VITE_APP_BASE_API=https://blog.doma.cool/doma`、`VITE_APP_WEBSOCKET_API=wss://blog.doma.cool/doma/websocket/`、`VITE_APP_ADMIN_URL=https://admin.doma.cool/login`。
- `index.html` 的 title/logo/SEO 从 `VITE_APP_*` 环境变量注入；本地插件 `vite-plugin-dev-utility.js` 会在 dev 模式移除 `/static/js/index.js` 引用（生产才有），调试 HTML 壳时注意这一差异。

## 已知坑

- **大小写坑**：`router/index.js` 里 `import('@/views/login/index.vue')` 是小写，但目录实际是 `Login/`（大写 L）。macOS 大小写不敏感所以没事，**Linux 上构建会挂**。新增 import 必须与磁盘上的目录大小写完全一致。
- 后端配置由后端动态加载（站点配置、菜单等），本地调试前端时很多内容依赖后端返回，纯前端起服务看不到完整页面。

## 验证范围

- 页面改动：验证桌面端与移动端的排版、懒加载/滚动行为和 404 回退。
- 登录、个人中心、编辑器或聊天改动：额外验证登录过期跳转、Vuex 状态更新和 WebSocket/上传失败状态。
- 音乐/播放器改动：验证全站播放器与音乐页的联动及歌词解码。
- 发布前：`npm run build` 必须通过。
