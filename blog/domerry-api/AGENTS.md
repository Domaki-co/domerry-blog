# AGENTS.md — domerry-api（门户/前台模块）

门户（前台/博客端）接口层，提供面向访客的 Controller 与 Service。依赖 `domerry-admin`、`domerry-commom`。

## 职责

- **controller/home、article、comment、tag**：首页、文章、评论、标签等门户接口
- **controller/chat、moment、message、friend、reward/sign、notifications、user、resource、album**：聊天、动态、留言、友链、签到、通知、用户、资源、相册等
- **service / service/impl**：对应门户业务逻辑（如 `ArticleServiceImpl`、`HomeServiceImpl`、`UserServiceImpl` 等）

## 改动须知

- 本模块为**门户面**，接口面向访问者，注意与 `domerry-admin`（管理端面）区分职责
- Service 以接口 + `impl` 分包组织，新增业务保持该结构
- 门户接口通常无需管理端权限，权限控制由 `domerry-auth` 的 Sa-Token 配置决定
- 通用的实体、Mapper、工具在 `domerry-commom` 中，优先复用，避免重复实现
