# AGENTS.md — domerry-admin（后台管理模块）

后台管理系统接口层，提供管理端 Controller 与 Service。依赖 `domerry-commom`。

## 职责

- **controller/system**：用户、角色、菜单、字典、配置、操作日志等系统管理接口
- **controller/article**：文章、分类、标签、动态（moments）管理接口
- **controller/site**：公告、站点配置、相册、照片、资源、友链
- **controller/message**：评论、留言、反馈
- **controller/monitor**：在线用户、缓存、服务器监控
- **controller/dashboard**：后台首页看板
- **controller/tool**：`GenController` 代码生成工具
- **service / service/impl**：对应业务逻辑（如 `SysArticleServiceImpl`、`GenTableServiceImpl`、`BusinessEmailService` 等）

## 改动须知

- 本模块为**管理端面**，接口面向后台，注意与 `domerry-api`（门户面）区分职责
- Service 以接口 + `impl` 分包组织，新增业务保持该结构
- 代码生成模块依赖 `domerry-server` 的 Velocity 模板与 `GenTable`/`GenTableColumn` 相关实体
- 操作日志、访问限流等能力由 `domerry-commom` 的注解/切面提供，直接复用
