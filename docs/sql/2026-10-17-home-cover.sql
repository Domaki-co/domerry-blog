-- 网站配置-首页首屏封面字段迁移脚本
-- 说明：用于在 sys_web_config 表中增加首页首屏封面字段 home_cover，供门户首页 hero 首屏封面从管理后台配置
-- 重复执行安全：先查 information_schema.columns，列已存在时只输出提示，不重复 ALTER

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'sys_web_config'
    AND column_name = 'home_cover'
);

SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `sys_web_config` ADD COLUMN `home_cover` VARCHAR(255) NULL COMMENT ''首页首屏封面''',
  'SELECT ''column home_cover already exists'''
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
