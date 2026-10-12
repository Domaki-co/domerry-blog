# AGENTS.md — domerry-file（文件模块）

文件存储与第三方平台模块，基于 `x-file-storage-spring`，支持本地、MinIO、七牛、阿里 OSS、腾讯 COS 等。依赖 `domerry-commom`。

## 职责

- **controller**：`FileController`（文件上传/管理）、`ThirdPartyController`
- **service / service/impl**：`FileDetailService`、`ThirdPartyService` 等文件与第三方业务
- **init**：`FileStorageInit` 启动时从 `sys_file_oss` 表动态注册各存储平台
- **schedule**：`DataSyncSchedule` 定时刷新 MinIO 预签名 URL（默认注释掉 `@Component`）
- **entity**：`OpenSongRespVo` 等文件相关 VO

## 改动须知

- 存储平台见 `domerry-server/src/main/resources/application*.yml` 的 `x-file-storage:` 段，默认平台为 `minio-1`
- 动态存储平台可在数据库表 `sys_file_oss` 中维护，`FileStorageInit` 启动时加载（阿里云/七牛/腾讯/MINIO）
- 定时任务使用 `@Scheduled`，如需启用 `DataSyncSchedule` 需放开 `@Component` 或将类纳入扫描；注意其数据源为 `@DS("slave")`，且目前仅处理 `blog` 桶，`myfile` 桶的 URL 会命中 `extractObjectName` 的未知路径分支而被跳过
- `@EnableFileStorage` 已在 `DomerryAdminApplication` 开启
- `ThirdPartyService.getSongList()` 不查库，通过 Hutool 调用第三方歌单接口（GET，请求头 `X-Api-Key`）；接口地址、apiKey、超时时间配置在字典表 `sys_dict_data`（`dictType = third_song_api`，label 存配置键），后台改字典即时生效
