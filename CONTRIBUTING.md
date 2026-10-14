# 贡献指南（Contributing）

感谢你愿意为 `domerry-blog` 做贡献。本文说明开发环境、代码风格、提交与 PR 流程。开始之前，
请先阅读 [`README.md`](README.md) 的「快速开始」把本地环境跑起来。

- 本项目的许可为 Apache License 2.0（见 [`LICENSE`](LICENSE)）；提交 PR 即表示你同意你的贡献
  以同一许可授权。
- 上游归属与相对上游的修改清单见 [`NOTICE`](NOTICE)；第三方组件许可见
  [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md)。

---

## 1. 开发环境

| 组件 | 版本 |
| --- | --- |
| JDK | 8（`java.version=1.8`） |
| Maven | 3.6+ |
| MySQL | 5.7+ / 8.0+ |
| Redis | 5.0+ |
| Node.js | 18 LTS 及以上（推荐 20 LTS） |

后端（`blog/`）：

```bash
cd blog
mvn clean package -DskipTests          # 构建
java -jar domerry-server/target/domerry-blog.jar   # 启动，默认端口 8900，profile=dev
```

前端（`blog-web/` 门户、`blog-admin/` 后台）：

```bash
npm install
npm run dev      # blog-web 默认 3001，blog-admin 默认 30001
npm run build    # 提交前必须通过
npm run preview
```

- 接口文档（后端启动后）：<http://localhost:8900/doc.html>
- 前端开发环境通过 Vite 代理访问后端，**必须先启动后端**（`blog-web` 的 `/dev-api`、
  `blog-admin` 的 `/api` 都会去掉前缀转发到 `VITE_APP_API_URL`，默认 `http://localhost:8900`）。
- 仓库内没有 `.nvmrc`，请自行统一团队 Node 版本。

## 2. 项目结构

- `blog/`：Maven 多模块后端（基包 `com.domerry`）
  - `domerry-commom` 通用模块（实体、Mapper 接口、工具、公共配置、注解切面）
  - `domerry-admin` 管理端接口，`domerry-api` 门户端接口
  - `domerry-auth` 认证与第三方登录，`domerry-file` 文件存储与第三方平台
  - `domerry-quartz` 定时任务，`domerry-server` 启动模块（主类、配置文件、Mapper XML）
- `blog-web/`：门户前端（Vue 3.5 + Element Plus + Vuex + Vue Router + Vite 5，已完成 Vue 2.7 → Vue 3 迁移）
- `blog-admin/`：管理后台（Vue 3 + element-plus + Pinia + Vite + TypeScript）

每个模块目录下都有 `AGENTS.md`，写明了该模块的职责、约定与已知坑；改动某个模块前请先读它
（对人和 AI 编码代理同样适用）。

## 3. 代码风格

以下是项目现有代码的实际约定，请保持一致（不要求全量重构旧代码）：

### 后端（Java）

- 统一返回 `com.domerry.common.Result<T>`：`Result.success(data)` / `Result.error(message)`；
  业务失败在 service 层抛 `ServiceException`，由 `domerry-commom` 的全局异常处理器兜底。
- 分层为 `controller → service(impl) → mapper → entity`；Controller 只做参数接收与 `Result` 包装。
- 分页在 Service 中用 `PageUtil.getPage()` 构造，返回 `IPage<Vo>`。
- Mapper 接口继承 `BaseMapper<T>` 并集中在 `domerry-commom` 的 `com.domerry.mapper`；自定义 SQL 写在
  `domerry-server/src/main/resources/mapper/*Mapper.xml`。
- 依赖注入使用 Lombok `@RequiredArgsConstructor` 构造注入，日志用 `@Slf4j`，**不要用
  `@Autowired` 字段注入**。
- 权限用 Sa-Token：`@SaCheckLogin`、`@SaCheckPermission("模块:资源:动作")`；限流用 `@AccessLimit`，
  操作日志用 `@OperationLogger`；审计字段由 `MyMetaObjectHandler` 自动填充，不要手动赋值。
- 缩进 4 空格（与现有代码一致）；类/方法保留必要的中文注释风格即可，但**不要**写与代码重复的
  注释。

### 前端

- `blog-web`：Options API（`export default {}`）是**存量主体**，改动旧文件时沿用原风格；新增或整体
  重写的组件可以用 `<script setup>`。UI 库是 **Element Plus**（不要再用 element-ui / Vue 2 的 API），
  状态管理是 **Vuex**，请**不要**引入 Pinia；请求统一走 `src/utils/request.js`，
  不要在视图组件里自建 axios 实例；样式使用全局注入的 SCSS 变量与 CSS 变量，不要写死颜色。
- `blog-admin`：全部为 `<script setup lang="ts">` + scoped 样式，请**不要**引入 Options API；
  请求统一走 `src/utils/request.ts`；API 函数以 `xxxApi` 命名并按业务域放在 `src/api/<domain>/`。
- 缩进 2 空格；组件目录约定 `组件名/index.vue`。
- **注意大小写**：Linux 对文件名大小写敏感，`import` 路径必须与磁盘上的实际大小写完全一致
  （历史上 `blog-web` 的 `views/Login/` 与部分 import 的大小写不一致，macOS 上不出问题但 Linux 上
  会构建失败）。

### 项目里没有的东西

- 没有 lint / test 脚本（`blog-web`、`blog-admin` 的 `package.json` 只有 `dev` / `build` / `preview`），
  后端 surefire 配置为 `skipTests=true`，各模块没有 `src/test`。
- 因此提交前的最低门槛是：后端能 `mvn clean package`，前端能 `npm run build`，并且**手动验证**
  受影响的页面/接口流程。

## 4. 提交规范

项目目前**没有**强制提交信息格式（没有 commitlint / commit-msg 钩子）。以下是通用建议：

- 一次提交只做一件事，提交信息用一句话说清「做了什么、为什么」。
- 推荐（非强制）使用 Conventional Commits 前缀：`feat:`、`fix:`、`docs:`、`refactor:`、`chore:`、
  `style:`、`test:`，例如 `fix(blog-web): 修复登录页在移动端的排版错位`。
- 提交信息中请使用中文或英文均可，保持同一次 PR 内的语言一致。

## 5. 分支与 PR 流程

1. Fork 本仓库（或在本仓库新建分支），从默认分支切出功能分支，例如
   `feat/article-export`、`fix/login-redirect`。
2. 只改与本次目的相关的文件，避免顺带格式化整个文件（会产生难以审查的 diff）。
3. 提交前自测：后端 `mvn clean package`、前端 `npm run build` 通过，并手动验证受影响功能。
4. 提 PR 时说明：**做了什么 / 为什么 / 如何验证**；涉及界面改动请附截图；涉及数据库改动请附
   增量 SQL 脚本（**不要**直接改 `domerry-blog.sql` 的历史数据）。注意 `docs/` 属于本地/内部资产、
   已被 `.gitignore` 忽略，脚本本身要随 PR 提交。
5. PR 描述里请关联相关 issue（如有）。
6. 改动后端接口契约时，请同步检查 `blog-admin` 与 `blog-web` 两侧调用方。

## 6. 安全与密钥（重要）

- **禁止**把任何真实密钥、口令、Token、私钥、证书、生产域名、账号密码、内网 IP、个人邮箱与
  手机号提交进仓库；包括但不限于 `.env`、`application*.yml`、`deploy/**`、测试脚本与截图。
- 仓库内的配置**只允许**是占位值：`your-domain.example`、`your-cdn.example`、
  `your-music-api.example`、`change-me` 之类。
- 敏感配置请通过环境变量注入，或使用 `APP_LOCAL_SECRET_FILE` 指向一个仅存在于服务器的
  `application-local-secret.yml`（该文件名已被 `.gitignore` 忽略）；也可使用 Jasypt 的 `ENC(...)`
  形式，密钥通过 `JASYPT_ENCRYPTOR_PASSWORD` 提供。
- 提交前请自查：`git diff --cached` 通读一遍，或用类似
  `grep -rIn "password\|secret\|apikey\|access-key" .` 粗筛一次改动。
- 如果**不小心**提交了密钥：立即在对应平台轮换/作废该密钥，然后联系维护者清理历史（仅删除
  最新提交中的文件是不够的）。安全漏洞请按 [`SECURITY.md`](SECURITY.md) 私下报告。

## 7. 报告问题与提需求

- 普通 Bug 与功能建议请提 issue；请附上**环境信息**（JDK / MySQL / Redis / Node 版本）、复现步骤、
  期望行为与实际行为、以及关键日志或截图。
- 报 Bug 前请先确认你已按 README「快速开始」完成建库（推荐库名 `domerry_blog`，并让后端 profile
  与库名一致）、导入 `domerry-blog.sql`、并正确设置了环境变量（后端默认值是 `192.168.1.100` /
  `192.168.196.130` 之类的占位值，未替换会直接连不上）。
- **安全问题请勿公开提 issue**，见 [`SECURITY.md`](SECURITY.md)。

## 8. 不得提交的内容（清单）

- 上游品牌素材与文案：任何「拾壹博客 / shiyi / shiyit / Neat Admin」相关的名称、Logo、截图、
  链接与域名。
- 商业字体文件与来源不可考证的图片素材。
- 含个人信息的二维码、头像、合影等。
- 任何真实凭证（见第 6 节）。
