-- AI 文章摘要数据库配置迁移
-- 说明：本文由人工在目标数据库执行；代码与部署流程不会自动执行。
-- 建议先备份 sys_config，再按顺序执行。

-- 1. 检查 config_key 是否已有重复数据
SELECT config_key, COUNT(*) AS cnt, GROUP_CONCAT(id ORDER BY id) AS ids
FROM sys_config
GROUP BY config_key
HAVING COUNT(*) > 1;

-- 2. 如果第 1 步确认有重复，并且决定只保留最小 id，再取消下面注释执行。
--    有重复键时，第 3 步的唯一索引会失败。
-- DELETE c1 FROM sys_config c1
-- INNER JOIN sys_config c2
--   ON c1.config_key = c2.config_key AND c1.id > c2.id;

-- 3. 增加 config_key 唯一索引；重复执行不会报“索引已存在”
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_config'
    AND index_name = 'uk_sys_config_config_key'
);

SET @ddl := IF(@idx_exists = 0,
  'ALTER TABLE sys_config ADD UNIQUE KEY uk_sys_config_config_key (config_key)',
  'SELECT ''uk_sys_config_config_key already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 写入 AI 摘要配置。
--    ai.api.key 先留空，后续由管理员在后台编辑该行填写。
--    ON DUPLICATE KEY UPDATE 不会覆盖已填写的 config_value，方便重复执行本脚本。
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_time, update_time, remark)
VALUES
  ('AI 接口地址', 'ai.api.url', 'http://your-ai-gateway.example/v1/responses', 'Y', NOW(), NOW(), 'OpenAI 兼容 Responses API 完整地址'),
  ('AI 接口密钥', 'ai.api.key', '', 'Y', NOW(), NOW(), 'Bearer Token；禁止提交到仓库，管理员可在后台修改'),
  ('AI 模型', 'ai.model', 'glm-5.2', 'Y', NOW(), NOW(), 'Responses API 模型名'),
  ('AI 摘要开关', 'ai.summary.enabled', 'Y', 'Y', NOW(), NOW(), 'Y 开启，N 关闭；只控制文章摘要生成'),
  ('AI 超时秒数', 'ai.timeout.seconds', '60', 'Y', NOW(), NOW(), '连接与读取超时'),
  ('AI 最大输入字符', 'ai.max.input.chars', '12000', 'Y', NOW(), NOW(), '文章正文截断长度')
ON DUPLICATE KEY UPDATE
  config_name = VALUES(config_name),
  config_type = VALUES(config_type),
  remark = VALUES(remark),
  update_time = NOW();

