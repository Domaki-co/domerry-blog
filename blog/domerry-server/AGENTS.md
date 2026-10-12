# AGENTS.md — domerry-server（启动模块）

后端聚合与启动模块：包含 Spring Boot 主类、全部配置文件和 Mapper XML，各业务模块在此被聚合与打包。

## 职责

- **入口**：`DomerryAdminApplication`（启动类）与 `TestApplication`
- **配置**：`application.yml`、`application-dev.yml`、`application-test.yml`（端口 `8900`，含动态数据源、Redis、x-file-storage、knife4j、Jasypt 等）
- **MyBatis Mapper XML**：`src/main/resources/mapper/`，存放 `Sys*Mapper.xml` 等 SQL
- **代码生成模板**：`src/main/resources/templates/`（Velocity `.vm` 模板，供 `GenController` 生成代码）
- **监控资源**：`src/main/resources/ip2region.xdb`、`sensitive-words.txt`

## 改动须知

- **新增/修改 SQL**：优先在此模块的 `resources/mapper/` 下新增或调整对应 `Sys*Mapper.xml`，并保证 namespace 与 `com.domerry` 下的 Mapper 接口一致
- **Mapper 扫描**：`@MapperScan("com.domerry.mapper")` 已开启，实体事务/Mapper 均在 `com.domerry.mapper` 与 `com.domerry.entity`
- **启用能力**：主类已开启 `@EnableScheduling`、`@EnableCaching`、`@EnableFileStorage`，新增相关能力时保持在此统一开关
- **打包产物**：`finalName = domerry-blog`，fatjar 由 spring-boot-maven-plugin 生成
- **本模块自身无业务 Controller/Service**，业务逻辑位于其依赖的 `domerry-admin`、`domerry-api`、`domerry-auth`、`domerry-file`、`domerry-quartz`
