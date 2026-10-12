# Domerry 博客

一个前后端分离的开源个人博客系统：门户站点（`blog-web`）、管理后台（`blog-admin`）与 Spring Boot
后端（`blog/`）三部分组成，支持文章/说说/相册/资源/留言板/友链/评论/站内通知、第三方登录、多对象
存储、定时任务与站点 SEO。

- 后端：Spring Boot 2.7 + MyBatis-Plus + Sa-Token + MySQL + Redis，JDK 8
- 门户前端：Vue 2.7 + element-ui + Vite
- 后台前端：Vue 3 + element-plus + Pinia + Vite

> **微信小程序端已移除**：上游的 `uniapp-blog/` 目录已整包删除，本仓库不再包含任何移动端小程序
> 代码。

---

## 功能

### 门户站点（`blog-web`）

- 首页（轮播、推荐文章、最新文章、最新说说、公告、标签云、友情链接）
- 文章：详情、目录、代码高亮、Markdown 渲染、点赞、上下篇、相关文章
- 分类 / 标签 / 归档 / 热搜
- 说说（动态）、相册、资源分享
- 留言板、评论（含表情、多级回复）、反馈
- 友链申请、关于本站
- 站内通知（WebSocket 实时推送）、聊天室
- 文章搜索（按关键词检索）
- 第三方登录（QQ / 微博 / Gitee / GitHub）
- 个人中心（资料、修改密码、我的消息）
- 文章编辑器（mavon-editor / Vditor 双编辑器）
- SEO：`sitemap.xml` 生成、页面 title/keywords/description 注入

### 管理后台（`blog-admin`）

- 仪表盘（数据统计图表）
- 内容管理：文章、分类、标签、说说、相册、照片、资源
- 互动管理：评论、留言板、反馈、通知
- 站点管理：友情链接、公告、网站配置
- 系统管理：用户、角色、菜单、字典、参数配置、操作日志
- 系统监控：缓存、在线用户、服务器信息（基于 OSHI）
- 定时任务：Quartz 任务的增删改查与执行日志
- 代码生成器：读取表结构生成前后端 CRUD 代码
- AI 配置：AI 文章摘要与对话（火山引擎方舟）
- 文件存储配置：MinIO / 七牛 / 阿里云 OSS / 腾讯云 COS

### 平台能力

- 认证授权：Sa-Token（Token 有效期 7 天、Redis 持久化、同账号多端登录策略可配）
- 限流：`@AccessLimit` 注解 + 切面
- 操作日志：`@OperationLogger` 注解 + 切面
- 多数据源：`dynamic-datasource`（主库 + 第二数据源）
- IP 归属地查询：离线 `ip2region` 库（**数据文件已随仓库提供**，无需额外下载）
- 邮件通知：评论、友链、反馈等场景的邮件提醒（SMTP）
- 密钥加密：Jasypt 加解密（敏感配置可用 `ENC()` 形式存放）
- 接口文档：Knife4j / springdoc-openapi（`http://localhost:8900/doc.html`）
- 跨域白名单：`CORS_ALLOWED_ORIGINS` 配置化

---

## 技术栈（实际版本）

| 层次 | 技术 | 版本 |
| --- | --- | --- |
| 后端框架 | Spring Boot | 2.7.0 |
| 后端语言 | Java | JDK 8（`java.version=1.8`） |
| ORM | MyBatis-Plus | 3.5.2 |
| 数据库 | MySQL | 5.7 / 8.0（脚本由 MySQL 8.0.20 导出，兼容 5.7+） |
| 缓存 | Redis（Lettuce + commons-pool2 2.11.1） | 5.0+ |
| 认证 | Sa-Token（含 sa-token-redis-jackson） | 1.39.0 |
| 多数据源 | dynamic-datasource-spring-boot-starter | 4.3.0 |
| 接口文档 | knife4j-openapi2-spring-boot-starter | 4.4.0 |
| 工具库 | Hutool | 5.8.26 |
| JSON | fastjson2 | 2.0.26 |
| 定时任务 | Quartz | 2.3.2 |
| 系统监控 | OSHI | 6.4.0 |
| 对象存储 | MinIO SDK 8.0.3 / 8.5.2、七牛 7.12.1、阿里云 OSS 3.16.1、腾讯云 COS 5.6.137 | — |
| 第三方登录 | JustAuth 1.16.7、weixin-java-mp 3.4.0 | — |
| Markdown | flexmark-all 0.62.2（Markdown→HTML）、jsoup 1.14.3 | — |
| 门户前端 | Vue | 2.7.16 |
| 门户前端 | element-ui | 2.15.14 |
| 门户前端 | Vue Router 3.6.5 / Vuex 3.6.2 | — |
| 门户前端构建 | Vite | 5.1.4 |
| 后台前端 | Vue | 3.2.47 |
| 后台前端 | element-plus | 2.3.0 |
| 后台前端 | Pinia 2.0.33 / Vue Router 4.1.6 | — |
| 后台前端构建 | Vite | 4.2.0 |
| 后台前端语言 | TypeScript | 5.4.5 |

> `blog-web` **仍是 Vue 2.7**，尚未迁移到 Vue 3（见「已知限制」）。第三方组件的完整许可与版权
> 信息见 [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md)。

---

## 目录结构

```
domerry-blog/
├── blog/                        # 后端（Maven 多模块，groupId com.domerry）
│   ├── pom.xml                  # 父 POM：Spring Boot 2.7.0 / JDK 8 / finalName=domerry-blog
│   ├── ip2region.xdb            # 离线 IP 库数据文件（后端启动需要，随仓库提供）
│   ├── domerry-commom/          # 公共模块：通用工具、结果封装、异常、注解切面、DTO
│   ├── domerry-admin/           # 后台业务模块：控制器/服务/Mapper/实体（后台接口）
│   ├── domerry-api/             # 门户接口模块：门户端控制器/服务/Mapper
│   ├── domerry-server/          # 启动模块：主类 DomerryAdminApplication、application*.yml
│   ├── domerry-file/            # 文件存储模块：MinIO / 七牛 / OSS / COS
│   ├── domerry-quartz/          # 定时任务模块：Quartz 任务与执行日志
│   └── domerry-auth/            # 认证模块：第三方登录（QQ/微博/Gitee/GitHub）、微信
├── blog-web/                    # 门户前端（Vue 2.7 + element-ui + Vite）
│   ├── .env.example             # 环境变量模板（复制为 .env.development / .env.production）
│   ├── public/                  # 静态资源（占位图等）
│   └── src/                     # 源码（views / components / api / store / router / utils）
├── blog-admin/                  # 后台前端（Vue 3 + element-plus + Pinia + Vite + TS）
│   ├── .env.example             # 环境变量模板
│   ├── public/                  # 静态资源（占位图等）
│   └── src/                     # 源码（views / components / api / stores / router / utils）
├── deploy/                      # 部署示例
│   ├── conf/                    # Nginx 站点配置示例（blog.conf / admin.conf）
│   ├── docker/                  # Dockerfile 与构建脚本
│   └── mac/                     # 本机一键部署脚本 + deploy.env.example
├── docs/                        # 补充文档与数据库增量脚本
├── domerry-blog.sql             # 数据库结构与初始数据（已脱敏，不含建库语句）
├── LICENSE                      # Apache License 2.0
├── NOTICE                       # 本项目归属 + 上游归属与修改声明
├── THIRD-PARTY-NOTICES.md       # 第三方组件许可与版权
├── CONTRIBUTING.md              # 贡献指南
└── SECURITY.md                  # 安全策略与漏洞报告方式
```

> `blog/ip2region.xdb` 与 `blog/domerry-server/src/main/resources/ip2region.xdb` 是同一份离线 IP
> 归属地数据（**ip2region 项目，Apache-2.0**）：一份供工作目录相对路径读取，一份供 classpath
> 读取，**均已随仓库提供，无需额外下载**。

---

## 快速开始

### 环境要求

| 组件 | 版本要求 |
| --- | --- |
| JDK | 8（`java.version=1.8`；建议 JDK 8u301+） |
| Maven | 3.6+ |
| MySQL | 5.7+ / 8.0+ |
| Redis | 5.0+ |
| Node.js | 18 LTS 及以上（`blog-web` 使用 Vite 5，要求 Node ^18.0.0 或 >=20.0.0；推荐 20 LTS） |
| npm | 随 Node 安装（仓库内没有 `.nvmrc`，请自行统一团队 Node 版本） |

### 1. 数据库

`domerry-blog.sql` **只包含表结构与数据，不含 `CREATE DATABASE` 语句**，必须先手动建库：

```sql
CREATE DATABASE domerry_blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

然后导入（库名固定为 `domerry_blog`，后端配置中的连接串即指向它）：

```bash
mysql -uroot -p domerry_blog < domerry-blog.sql
```

> ⚠️ **首次部署前务必修改默认管理员密码**：脚本中预置了 `admin` 账号，其密码散列是**公开可见的
> 演示数据**（BCrypt 散列，任何人都能在仓库里读到）。请在首次登录后立即在「个人中心 / 用户管理」
> 中修改密码。
>
> 脚本已做脱敏：保留表结构、字典与菜单等基础数据，用户、文章、评论等内容数据为空表或仅含演示行。

### 2. Redis

启动 Redis 并记住连接信息（默认端口 6379，可设密码）。后端使用 Redis 存放登录态、缓存等。

### 3. 后端配置与启动

后端配置通过环境变量注入；未设置时使用
`blog/domerry-server/src/main/resources/application-dev.yml` 中的占位默认值（`192.168.1.100`、
`your-cdn.example` 等，**必须替换为你自己的值**）。

```bash
cd blog

# 最小可用配置（示例：连接本机 MySQL / Redis）
export MYSQL_HOST=127.0.0.1
export MYSQL_PORT=3306
export MYSQL_USERNAME=root
export MYSQL_PASSWORD='your-mysql-password'
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_PASSWORD=''

# 构建（跳过测试）
mvn clean package -DskipTests

# 启动（默认 profile = dev，端口 8900）
java -jar domerry-server/target/domerry-blog.jar
```

- 启动类：`com.domerry.DomerryAdminApplication`
- 默认端口：`8900`（见 `application-dev.yml` 的 `server.port`）
- 接口文档：<http://localhost:8900/doc.html>（Knife4j）与 <http://localhost:8900/swagger-ui.html>
- 切换环境：`--spring.profiles.active=dev|test`（仓库内提供 `application-dev.yml` 与
  `application-test.yml`；生产环境建议自行新增 `application-prod.yml`）
- 敏感配置加密：Jasypt 支持 `ENC(...)` 形式，需设置 `JASYPT_ENCRYPTOR_PASSWORD`；也可用
  `APP_LOCAL_SECRET_FILE` 指向一个仅存在于服务器上的 `application-local-secret.yml` 覆盖敏感项
  （该文件名已被 `.gitignore` 忽略）

### 4. 门户前端（`blog-web`）

```bash
cd blog-web
cp .env.example .env.development     # 按需修改其中的占位值
npm install
npm run dev                          # 默认端口 3001
npm run build                        # 产物在 blog-web/dist
```

开发环境下 `VITE_APP_BASE_API=/dev-api`，由 Vite 代理到 `VITE_APP_API_URL`（默认
`http://localhost:8900`），因此**后端必须先启动**。

### 5. 后台前端（`blog-admin`）

```bash
cd blog-admin
cp .env.example .env.development
npm install
npm run dev                          # 默认端口 30001
npm run build                        # 产物在 blog-admin/dist
```

开发环境下 `VITE_APP_BASE_API=/api`，代理到 `VITE_APP_API_URL`（默认 `http://localhost:8900/`）。

### 6. 部署示例

`deploy/` 下提供可直接参考的示例（均为占位值，请自行替换）：

- `deploy/conf/blog.conf`、`deploy/conf/admin.conf`：Nginx 站点配置（静态资源缓存、SPA 回退、
  `/api`、`/doma` 与 WebSocket 反向代理）
- `deploy/docker/Dockerfile`：基于 `openjdk:8-jre-alpine` 的后端镜像
- `deploy/mac/*.sh` + `deploy/mac/deploy.env.example`：本机一键构建/上传脚本（复制为 `deploy.env`
  后填写服务器地址，该文件已被 `.gitignore` 忽略）

---

## 配置说明

仓库内的所有配置**只含占位值**，没有任何生产密钥；请在使用前全部替换为你自己的值。

### 前端环境变量

| 文件 | 变量 | 说明 |
| --- | --- | --- |
| `blog-web/.env.example` | `VITE_APP_PORT` | 开发服务器端口（默认 3001） |
| | `VITE_APP_API_URL` | 开发环境后端地址（默认 `http://localhost:8900`） |
| | `VITE_APP_BASE_API` | 接口基础路径（生产示例：`https://your-domain.example/doma`） |
| | `VITE_APP_WEBSOCKET_API` | WebSocket 地址（生产示例：`wss://your-domain.example/doma/websocket/`） |
| | `VITE_APP_ADMIN_URL` | 后台登录地址（门户跳转后台用） |
| | `VITE_APP_LOGO` | 站点 Logo 图片地址（对象存储/CDN） |
| | `VITE_MUSIC_API_BASE` / `VITE_MUSIC_API_KEY` | 第三方音乐接口地址与 Key（**需自备**） |
| | `VITE_APP_TITLE` / `VITE_APP_KEYWORDS` / `VITE_APP_DESCRIPTION` | 站点标题、关键词、描述 |
| `blog-admin/.env.example` | `VITE_APP_PORT` | 开发服务器端口（默认 30001） |
| | `VITE_APP_API_URL` | 开发环境后端地址 |
| | `VITE_APP_BASE_API` | 接口基础路径（生产示例：`https://admin.your-domain.example/doma`） |
| | `VITE_APP_LOGO` | 站点 Logo 图片地址 |
| | `VITE_APP_TITLE` / `VITE_APP_KEYWORDS` / `VITE_APP_DESCRIPTION` | 站点标题、关键词、描述 |

### 后端环境变量

| 变量 | 用途 | 默认值 |
| --- | --- | --- |
| `MYSQL_HOST` / `MYSQL_PORT` | MySQL 主机与端口 | `192.168.1.100` / `3306` |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | MySQL 账号与密码 | 空 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 主机、端口与密码 | `192.168.1.100` / `6379` / 空 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP 发信账号与密码/授权码 | 空 |
| `JASYPT_ENCRYPTOR_PASSWORD` | Jasypt 加解密口令（使用 `ENC()` 时必须设置） | 无 |
| `APP_LOCAL_SECRET_FILE` | 服务器本地密钥覆盖文件路径 | `/etc/domerry/application-local-secret.yml` |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | MinIO 访问密钥 | 无 |
| `MINIO_ENDPOINT` / `MINIO_BUCKET` / `MINIO_DOMAIN` | MinIO 端点、桶名、访问域名 | `https://your-cdn.example/` / `blog` / `https://your-cdn.example/blog/` |
| `QINIU_ACCESS_KEY` / `QINIU_SECRET_KEY` / `QINIU_BUCKET` / `QINIU_DOMAIN` | 七牛密钥、桶名与访问域名 | 无 / 无 / `your-bucket` / `https://your-cdn.example/` |
| `QQ_APP_ID` / `QQ_APP_SECRET` / `QQ_REDIRECT_URL` | QQ 登录 | 空 / 空 / `https://your-domain.example/blog/api/auth/callback/qq` |
| `WEIBO_APP_ID` / `WEIBO_APP_SECRET` / `WEIBO_REDIRECT_URL` | 微博登录 | 空 / 空 / `https://your-domain.example/blog/api/auth/callback/weibo` |
| `GITEE_APP_ID` / `GITEE_APP_SECRET` / `GITEE_REDIRECT_URL` | Gitee 登录 | 空 / 空 / `https://your-domain.example/blog/api/auth/callback/gitee` |
| `GITHUB_APP_ID` / `GITHUB_APP_SECRET` / `GITHUB_REDIRECT_URL` | GitHub 登录 | 空 / 空 / `https://your-domain.example/blog/doma/api/auth/callback/github` |
| `CORS_ALLOWED_ORIGINS` | 跨域白名单（英文逗号分隔） | `http://localhost:3001,http://localhost:30001,https://your-domain.example,https://admin.your-domain.example` |
| `SITE_DOMAIN` | `sitemap.xml` 中使用的站点地址 | `https://your-domain.example` |

> 阿里云 OSS 与腾讯云 COS 的密钥同样请以环境变量或本地密钥文件方式提供，**不要提交进仓库**。
>
> `.env` / `.env.development` / `.env.production` 中**没有任何生产密钥**，全部为本地开发用的
> 占位值（`your-domain.example` / `your-cdn.example` / `your-music-api.example`）；请自行替换。
>
> 站点备案号（ICP）已随上游信息一并移除，部署者需按所在地法规**自行填写/备案**。

---

## 第三方资源与外部接口

以下是运行时依赖的**外部服务/外链**，请知悉哪些需要自备、哪些需要自行托管：

1. **前端 CDN 外链**：`blog-web/src/App.vue` 通过 `@import` 引入 `cdnjs.cloudflare.com` 上的
   Font Awesome 5.15.4 图标字体；表情图片走 `baidubox-emoji.cdn.bcebos.com`
   （`blog-web/src/assets/emoji.json`）。两者均为**第三方公开 CDN、不含任何凭证**；
   **离线部署需自行托管**这两份资源（或移除相应引用）。
2. **随机图 API（后端）**：`HomeServiceImpl` 调用第三方公开接口
   `https://www.coderutil.com/api/resou/v1/{type}` 获取随机图（无需 Key，用于音视频/随机封面占位）。
   该接口由第三方提供，**可用性不受本项目控制**，可自行替换为本地图库。
3. **火山引擎方舟（AI 能力）**：AI 文章摘要与 AI 对话调用 `ark.cn-beijing.volces.com`，
   **需自备 API Key 与模型接入点**（配置键见后台「网站配置 / AI 配置」中的 `ai.*` 项）。
4. **需自备账号的外部服务**：对象存储（MinIO / 七牛 / 阿里云 OSS / 腾讯云 COS）、SMTP 邮件、
   第三方登录 OAuth（QQ / 微博 / Gitee / GitHub）。对应配置项见上文「配置说明」与各
   `.env.example`。
5. **已本地化的中性占位素材**：登录页滑块背景 `src/assets/img/slider-bg.jpg`、锁屏背景
   `src/assets/lock-bg.jpg`、侧栏背景 `public/static/img/beijing.jpg`、微信二维码占位
   `public/static/img/wechat.jpg`、默认封面 `public/static/img/book.png` 与加载图
   `public/static/img/lazy.gif` 均为**项目自带的中性占位图**，请替换为你自己的素材（并自行确认
   替换素材的授权）。
6. **站点图标**：`blog-web/public/` 与 `blog-admin/public/` 下的 `logo.svg`、`favicon.ico`、
   `favicon-32x32.png`、`apple-touch-icon.png`、`logo-512.png` 是本项目自带的**自制中性图标**，
   **不含任何上游品牌元素**，可自由替换。两端 `.env*` 的 `VITE_APP_LOGO` 默认指向 `/logo.svg`
   （供 `index.html` 的 `<link rel="icon">` 使用）；后台「网站配置」里的站点 logo / 图标
   （`sys_web_config.logo` / `favicon`）与 `domerry-blog.sql` 种子数据中的头像 URL 默认指向第三方
   公开生成接口 `api.dicebear.com`，**请替换为你自己的图片**。
7. **第三方商标与图标**：`blog-web/src/assets/icons/` 下的 `github.svg`、`gitee.svg`、`baidu.svg`、
   `chrome.svg`、`zip.svg`、`pdf.svg`、`doc.svg`、`excel.svg` 是第三方**商标/品牌标识**，本项目仅在
   「标识所指向的服务或文件格式」的指名性用途（nominative use）中使用，**不代表任何背书**；
   相关商标归各自所有者（详见 `THIRD-PARTY-NOTICES.md` 的「第三方商标与图标」一节）。

---

## 已知限制

1. **门户前端仍是 Vue 2.7，Vue 3 迁移尚未进行**。`blog-web` 使用 Vue 2.7 + element-ui + Vite
   （element-ui 只支持 Vue 2）；Vue 3 迁移属于后续计划（v1.1 目标），当前没有迁移分支。
2. **后端接口路径前缀 `/doma/` 是历史遗留的功能路径**，与品牌无关：它出现在 `deploy/conf/*.conf`
   的 Nginx `location` 与前端 `.env` 的 `VITE_APP_BASE_API` 中。为保持已有部署与客户端兼容，
   本项目**未改动**该前缀；如需改名，请同时调整 Nginx、前端环境变量与 OAuth 回调地址。
3. **微信小程序端已移除**：上游 `uniapp-blog/` 已整包删除，本仓库不提供小程序端。
4. **部分演示数据为空表**：`domerry-blog.sql` 已脱敏，用户、文章、评论等内容表为空或仅含演示行，
   首次使用时需自行创建内容。
5. **没有内置测试与 lint**：`blog-web` / `blog-admin` 的 `package.json` 只提供 `dev` / `build` /
   `preview` 三个脚本（无 `lint`、无 `test`）；后端 `blog/pom.xml` 的 surefire 配置为
   `<skipTests>true</skipTests>`。
6. **`.env` 系列文件随仓库提供**，但只含公开占位值；请勿把你的真实密钥写进这些文件后再提交。

---

## 开源协议

本项目以 **Apache License 2.0** 授权，全文见 [`LICENSE`](LICENSE)。

- 第三方组件的许可与版权信息见 [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md)。
- 本项目的归属声明、上游归属声明与相对上游的修改清单见 [`NOTICE`](NOTICE)。

---

## 致谢

本项目基于开源项目 **拾壹博客**（Apache License 2.0）二次开发，感谢原作者及其贡献者：
<https://gitee.com/quequnlong/shiyi-blog>。上游归属与修改声明见 [`NOTICE`](NOTICE)。

同时感谢 `blog/`、`blog-web/`、`blog-admin/` 所依赖的众多开源项目与社区。

---

## 贡献与安全

- 贡献指南（开发环境、代码风格、提交与 PR 流程、请不要提交密钥）：[`CONTRIBUTING.md`](CONTRIBUTING.md)
- 安全策略与漏洞报告方式（请**不要**公开提 issue 报告漏洞）：[`SECURITY.md`](SECURITY.md)
