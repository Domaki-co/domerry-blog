# AGENTS.md — domerry-quartz（定时任务模块）

基于 Quartz 的定时任务模块，提供任务与执行日志的控制器、调度封装与服务。依赖 `domerry-commom`。

## 职责

- **quartz**：调度核心，含 `AbstractQuartzJob`、`QuartzJobExecution`、`QuartzDisallowConcurrentExecution`、`TaskQuartz`、`ScheduleConstants`、`TaskException`、`VelocityInitializer`
- **controller**：`SysJobController`、`SysJobLogController`
- **service / service/impl**：`SysJobService`、`SysJobLogService`
- **utils**：`ScheduleUtils`、`CronUtils`、`JobInvokeUtils`

## 改动须知

- Quartz 相关 Mapper XML 在 `domerry-server/src/main/resources/mapper/`（`SysJobMapper.xml`、`SysJobLogMapper.xml`）
- 任务执行封装在 `quartz/` 包内，新增任务类型参考 `TaskQuartz` 的任务分发逻辑
- cron 表达式校验使用 `CronUtils`
- 其他模块自身的定时任务（如 `domerry-file` 的 `DataSyncSchedule`）使用 Spring 的 `@Scheduled`，与这里的管理化 Quartz 任务区分
