# AGENTS.md - Domerry 博客后台前端（blog-admin）

本目录是博客管理后台，面向已认证的管理员。Vue 3（3.2）+ TypeScript（strict）+ Vite 4 + Element Plus（2.3）+ Pinia（persistedstate 持久化）+ Vue Router 4（history 模式）。

## 常用命令

在本目录执行：

```bash
npm install
npm run dev      # 端口 30001（.env.development 的 VITE_APP_PORT）
npm run build
npm run preview
```

当前没有 lint / test / typecheck 脚本（`vue-tsc` 在 devDependencies 里但 build 不跑它）。提交前至少执行 `npm run build`；改动页面时在浏览器中验证关键流程。

部署：`deploy/mac/allinone.sh admin` 执行 `npm run build` 后将 `dist/*` 上传到 Nginx 站点目录（目标主机与目录由 `deploy/mac/deploy.env` 或同名环境变量指定，仓库内只有空占位模板），所以 **`npm run build` 通过是发布前的硬门槛**。部署细节见 `deploy/` 与 README 的「部署示例」。

## 目录职责

- `src/api/`：按业务域划分的后端请求封装（`article`、`system`、`site`、`message`、`monitor`、`file`、`tool`）。
- `src/views/`：后台页面（36 个），目录与业务域对应：`article/`、`dashboard/`、`message/`、`monitor/`、`site/`、`system/`、`tool/`、`login/`。
- `src/components/`：可复用组件，目录约定 `组件名/index.vue`；`ButtonGroup`、`SvgIcon` 全局注册，已有 `Upload/Image`、`VditorEditor`、`GlobalSearch` 等优先复用。
- `src/layouts/`：整体布局（Navbar / Sidebar / TagsView）。
- `src/router/index.ts`：**只有静态路由**（login、redirect、404、`/` → dashboard）。
- `src/store/modules/`：Pinia 模块（user、permission、settings、tagsView），每个模块同时导出 `useXxxStore` 和供非 setup 环境用的 `useXxxStoreHook()`。
- `src/plugins/permission.ts`：路由守卫；`src/directives/permission.ts`：`v-permission` 按钮级权限。
- `src/utils/`：`request.ts`（axios 封装）、`auth.ts`（token）、`validate.ts` 等。
- `src/styles/global.scss`：唯一的全局 SCSS；主题变量走 Element Plus CSS 变量。

## 请求与认证

- 所有请求必须走 `src/utils/request.ts`（baseURL = `VITE_APP_BASE_API`，timeout 50s），**页面里不要直接 import axios**。
- API 函数命名 `xxxApi`（如 `getArticleListApi`），按业务域放在 `src/api/<domain>/` 下，一个实体一个文件。
- 管理端接口路径为 `/sys/...`，认证为 `/auth/login`、`/auth/info?source=admin`、`/auth/getCaptcha`。
- token 存 cookie **`Domerry-Admin-Token`，有效期 1 小时**（`src/utils/auth.ts`）；请求拦截器把 token 放进 `Authorization` 头，**无 Bearer 前缀**。
- 响应约定 `{ code, message, data }`：`code !== 200` → `ElMessage.error` 并 reject；业务码 `401` → 弹窗后登出；HTTP 401 → 静默登出。

## 权限与路由（后端驱动）

- 动态菜单来自 `/sys/menu/routers`，`permissionStore.generateRoutes()` 用 `import.meta.glob('../../views/**/**.vue')` 把后端返回的组件字符串解析为路由：`'Layout'` → `layouts/index.vue`，`'ParentView'` → ParentView 组件。
- 按钮级权限用 `v-permission="['sys:user:add']"`，权限标识与后端一致，格式 `模块:资源:动作`。
- 路由 meta 字段：`title`、`icon`（Element Plus 图标名字符串）、`hidden`、`affix`、`keepAlive`、`alwaysShow`、`isExternal`。
- 新增管理功能要三处同步：前端页面 + 后端 `domerry-admin` 接口 + 后端菜单/权限表数据。

## 页面套路

标准列表页结构：`el-form` 搜索区 → `el-card`（header 里放 `ButtonGroup`，按钮用 `v-permission` 控制）→ `el-table` + 分页 → `el-dialog` 表单。图标用全局注册的 Element Plus 图标（字符串 prop，如 `icon="Search"`）或 `svg-icon`（SVG 源文件放 `src/icons/svg/`）。

全部 51 个 `.vue` 文件均为 `<script setup lang="ts">` + scoped 样式，新代码不要引入 Options API。`unplugin-auto-import` 已自动导入 Vue/Router/Pinia API，与同目录文件的风格保持一致（显式或自动导入均可，别混得突兀）。

## 配置与联调

- `.env.development`：`VITE_APP_PORT=30001`、`VITE_APP_BASE_API=/api`、`VITE_APP_API_URL=http://localhost:8900/`（后端地址，**后端端口是 8900**）。Vite 代理把 `/api` 转发到 `VITE_APP_API_URL` 并去掉 `/api` 前缀，开发时需先启动后端。
- `.env.production`：`VITE_APP_BASE_API=https://admin.your-domain.example/doma`（生产无代理，直连；仓库内是占位值，请替换为你自己的域名）。
- 后端位于仓库根目录 `blog/`，管理端接口由 `blog/domerry-admin` 提供；改接口契约时同步检查该模块与 `blog-web` 门户端。

## 已知坑

- `vite.config.ts` 里 AutoImport 的 `dirs: ['./src/composables', './src/stores']` 指向**不存在的目录**（陈旧配置），不要照着它建依赖；实际 store 在 `src/store/modules/`。
- `src/components.d.ts` 是手工维护的全局组件声明文件，不是插件生成的，新增全局组件后需手动补声明。
- `src/components/VditorEditor/index.vue` 是 Vditor 封装，上传走自定义 `uploadHandler`（文章页使用），改动它会影响文章编辑的图片上传回显逻辑。

## 验证范围

- 纯样式或页面改动：`npm run dev` 验证加载、交互、窄屏布局和错误提示。
- API、鉴权或路由改动：额外验证登录失效处理（token 1 小时过期）、权限菜单与受限页面跳转。
- 发布前：`npm run build` 必须通过。
