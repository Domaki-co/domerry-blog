# AGENTS.md — domerry-auth（认证模块）

登录认证与第三方授权模块，提供账号登录、第三方 OAuth 登录与 Sa-Token 权限装配。依赖 `domerry-admin`、`domerry-commom`。

## 职责

- **controller**：`AuthController`（登录/注册）、`WeChatController`（公众号）
- **service / service/impl**：`AuthService`，登录与第三方登录业务
- **dto**：`LoginDTO`、`EmailRegisterDto` 登录/注册入参
- **config/satoken**：`SaTokenConfigure`、`StpInterfaceImpl`（权限加载）、`MySaTokenListener`（会话监听）
- **config/properties**：`GithubConfigProperties`、`QqConfigProperties`、`WechatProperties`、`GiteeConfigProperties`、`WeiboConfigProperties` 等第三方配置项
- **config**：`WechatConfig`

## 改动须知

- Sa-Token 全局配置（token 名称/有效期/风格）在 `domerry-server/src/main/resources/application.yml` 的 `sa-token:` 段，变更权限逻辑先看 `config/satoken`
- 权限/角色数据由 `StpInterfaceImpl` 加载，改动权限模型需同步该实现
- 第三方登录在 `config/properties` 中按平台声明配置项，并读取 `com.domerry` 对应配置
- 涉及用户/角色/菜单数据时，复用 `domerry-commom` 的实体与 Mapper
