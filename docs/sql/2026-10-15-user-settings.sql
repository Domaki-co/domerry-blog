-- 用户个性化偏好配置字段迁移脚本
-- 说明：用于在 sys_user 表中增加通用的 JSON 扩展字段 user_settings，用于存储音乐开关、主题偏好等用户维度设置

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_user'
    AND column_name = 'user_settings'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `sys_user` ADD COLUMN `user_settings` JSON NULL COMMENT ''用户个性化偏好配置(JSON)''',
  'SELECT ''column user_settings already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
