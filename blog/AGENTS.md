# AGENTS.md — Domerry 博客后端（blog）

本目录是博客的**后端项目**，基于 Spring Boot（2.7.0）+ MyBatis-Plus（3.5.2）+ Sa-Token（1.39.0），Maven 多模块结构（基包 `com.domerry`），Java 8。

## 模块索引

| 模块 | 职责 | 详细说明 |
| --- | --- | --- |
| `domerry-server` | 启动模块：主类、配置、Mapper XML | [AGENTS.md](domerry-server/AGENTS.md) |
| `domerry-commom` | 通用模块：实体、Mapper、工具、公共配置（注意目录名就是这个拼写） | [AGENTS.md](domerry-commom/AGENTS.md) |
| `domerry-admin` | 后台管理端接口（Controller/Service） | [AGENTS.md](domerry-admin/AGENTS.md) |
| `domerry-api` | 门户/前台接口（Controller/Service） | [AGENTS.md](domerry-api/AGENTS.md) |
| `domerry-auth` | 认证登录、第三方授权、Sa-Token 权限 | [AGENTS.md](domerry-auth/AGENTS.md) |
| `domerry-file` | 文件/图片存储、第三方平台、定时刷新 | [AGENTS.md](domerry-file/AGENTS.md) |
| `domerry-quartz` | 定时任务（Quartz）模块 | [AGENTS.md](domerry-quartz/AGENTS.md) |

## 依赖关系

```
domerry-server(启动) ──> domerry-api ─> domerry-admin ─> domerry-commom
                    └─> domerry-admin ─┘
                    └─> domerry-file     └─> domerry-commom
                    └─> domerry-quartz
                    └─> domerry-auth ─> domerry-admin, domerry-commom
```

各业务模块最终都依赖 `domerry-commom`。`domerry-server` 聚合所有模块并打包为 `domerry-blog`。

## 运行与部署

- **端口 8900**，无 context-path（门户路由自带 `/api/**` 前缀，管理端为 `/sys/**`）
- 构建：`blog/` 下执行 `mvn clean package`，产物固定为 `domerry-blog.jar`，主类 `com.domerry.DomerryAdminApplication`
- Docker 部署：`deploy/docker/Dockerfile` + `docker_build.sh`（openjdk:8-jre-alpine，EXPOSE 8900，-Xmx600m）
- 接口文档：`http://127.0.0.1:8900/doc.html`（knife4j；`springdoc.swagger-ui.path=/swagger-ui.html`、`springdoc.api-docs.path=/v3/api-docs`；dev 环境 `knife4j.enable: true` 且 `knife4j.basic.enable: false`，不需要登录）
- **没有任何测试**：surefire 配置了 `skipTests=true`，各模块无 `src/test`，不要指望 `mvn test` 做验证
- `domerry-server` 下的 `TestApplication.java` 不是测试类，是 jasypt 加解密工具类

## 配置

- 所有配置在 `domerry-server/src/main/resources/`：`application.yml`（默认激活 `dev`）、`application-dev.yml`、`application-test.yml`
- 中间件：MySQL（动态数据源 master/slave）、Redis（同时存 Sa-Token 会话）；主机/端口/账号/密码一律通过环境变量（`MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`）或 `${APP_LOCAL_SECRET_FILE}` 指向的外部 `application-local-secret.yml` 注入，支持 jasypt `ENC(...)`；仓库内的默认值均为占位值，不要写死真实凭据
- 其他外部依赖：MinIO 对象存储（x-file-storage）、QQ SMTP 邮件、豆包 AI（火山方舟）、QQ/微博/Gitee/GitHub OAuth（JustAuth）、微信公众号
- 放行路径在 `domerry-auth` 的 `SaTokenConfigure` 维护：`/api/**`（门户公开）、`/auth/login|logout|verify`、swagger、`/wechat/**`、`/localFile/**`、`/third/**`

## 编码约定（全模块适用）

- **统一返回**：`com.domerry.common.Result<T>`（`Result.success(data)` / `Result.error(message)`，成功 200，业务失败 500）
- **异常**：业务错误在 service 中抛 `ServiceException`，由 `domerry-commom` 的 `GlobalException`（`@RestControllerAdvice`）统一兜底；Sa-Token 未登录→401、无权限→403
- **认证**：Sa-Token（不是 JWT），token-name `Authorization`，超时 7 天；接口级用 `@SaCheckLogin`、`@SaCheckPermission("sys:article:list")`，权限标识格式 `模块:资源:动作`
- **分层**：`controller → service(impl) → mapper → entity`；Controller 用 `@RestController + @RequestMapping`，只做参数接收和 `Result` 包装
- **分页**：Controller 接收 `pageNum`/`pageSize` 原始参数，Service 里用 `PageUtil.getPage()` 构造分页，返回 `IPage<Vo>`
- **Mapper**：接口继承 `BaseMapper<T>` 并集中在 `domerry-commom` 的 `com.domerry.mapper`（`@MapperScan` 已扫描）；自定义 SQL 统一写在 `domerry-server/src/main/resources/mapper/*Mapper.xml`（XML 也可放 mapper 接口同包，pom 已配置打包 `src/main/java` 下的 XML）
- **注入**：Lombok `@RequiredArgsConstructor` 构造注入 + `@Slf4j` + `@Data`，不要用 `@Autowired` 字段注入
- **横切**：限流用 `@AccessLimit`，操作日志用 `@OperationLogger`（注解和切面都在 `domerry-commom`）
- 审计字段（创建/更新时间人）由 `MyMetaObjectHandler` 自动填充，不要手动赋值

> 前端项目不在本目录内：`blog-admin` 为后台前端，`blog-web` 为门户前端，均位于仓库根目录。

> 验证策略以仓库根目录 [AGENTS.md](../AGENTS.md) 的「验证与测试约束」为准：本项目无测试，`mvn clean package` 全量打包昂贵，除非必要（交付 jar、改 pom）不要反复执行，优先用 `mvn -pl <模块> -am compile` 增量编译。
