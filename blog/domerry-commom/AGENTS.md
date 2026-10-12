# AGENTS.md — domerry-commom（通用模块）

通用基础模块，几乎被所有业务模块依赖。此处定义共享的实体、枚举、Mapper、公共配置与工具。

## 职责

- **entity（实体）**：`com.domerry.entity`，如 `SysArticle`、`SysUser`、`SysFileOss`、`GenTable` 等数据库实体
- **mapper**: `com.domerry.mapper`，MyBatis-Plus Mapper 接口（对应 XML 在 `domerry-server/src/main/resources/mapper/`）
- **common**：统一返回 `Result`、`ResultCode`、`PageQuery`、`Constants`、`RedisConstants`
- **config**：`RedisConfig`、`WebMvcConfig`、`JsonNullSerializer`、`filter`、`interceptor`、`mybatisplus` 配置
- **annotation / aspect**：`AccessLimit`、`OperationLogger` 自定义注解及切面
- **exception**：`ServiceException`、`GlobalException` 全局异常处理
- **utils**：`RedisUtil`、`EmailUtil`、`SensitiveUtil`、`CaptchaUtil`、`IpUtil`、`DateUtil`、`SpringUtil`、`PageUtil` 等
- **vo / dto**：`com.domerry.vo` 与 `com.domerry.dto`，对外视图对象与入参对象
- **websocket**：WebSocket 支持

## 改动须知

- 该模块的实体/Mapper/公共配置会被多个模块引用，**变更前先确认不影响 `domerry-admin`、`domerry-api`、`domerry-auth`、`domerry-file`、`domerry-quartz`**
- 新增 Mapper 接口请在 `com.domerry.mapper`，对应 SQL 写在 `domerry-server` 的 `resources/mapper/` 下，两者命名保持一致
- 新增公共工具/常量放 `com.domerry.utils` / `com.domerry.common`，避免在业务模块重复实现
- 新增实体后如需生成代码，可参考 `domerry-server/src/main/resources/templates/` 下的 Velocity 模板
