-- 音乐接口代理数据库配置迁移
-- 说明：本文由人工在目标数据库执行；代码与部署流程不会自动执行。
-- 背景：门户前端原先把第三方音乐接口地址与 apiKey 硬编码在源码里，密钥会随构建产物发布；
--       改为由后端代理转发后，地址与密钥只存在于 sys_dict_data，前端不再持有。
-- 重复执行安全：全部写入都以「是否已存在」为条件，重复执行不会重复插入，也不会覆盖管理员已填写的值。
-- 注意：本脚本不包含任何真实密钥，apiKey 留空，由管理员在后台「字典管理」中填写。

-- 1. 写入字典类型 music_api（不指定 id，交给自增；已存在则跳过）
SET @music_dict_id := (SELECT id FROM sys_dict WHERE type = 'music_api' ORDER BY id LIMIT 1);

INSERT INTO sys_dict (name, type, status, remark, create_time, update_time, sort)
SELECT '音乐接口', 'music_api', 1, '第三方音乐接口代理配置（label 为配置键，value 为配置值）', NOW(), NOW(), 0
FROM DUAL
WHERE @music_dict_id IS NULL;

SET @music_dict_id := IFNULL(@music_dict_id, LAST_INSERT_ID());

-- 2. 写入三条字典数据：url（基础地址）/ apiKey（密钥）/ timeout（超时毫秒）
--    apiKey 留空，禁止提交到仓库；管理员在后台填写后即时生效，无需重启。

SET @has_url := (SELECT COUNT(*) FROM sys_dict_data WHERE dict_id = @music_dict_id AND label = 'url');
INSERT INTO sys_dict_data (dict_id, label, value, style, is_default, sort, remark, status)
SELECT @music_dict_id, 'url', '', NULL, 0, 1, '第三方音乐接口基础地址，例如 https://your-music-api.example/api', 1
FROM DUAL
WHERE @has_url = 0;

SET @has_api_key := (SELECT COUNT(*) FROM sys_dict_data WHERE dict_id = @music_dict_id AND label = 'apiKey');
INSERT INTO sys_dict_data (dict_id, label, value, style, is_default, sort, remark, status)
SELECT @music_dict_id, 'apiKey', '', NULL, 0, 2, '第三方音乐接口密钥，留空需管理员在后台填写；禁止提交到仓库', 1
FROM DUAL
WHERE @has_api_key = 0;

SET @has_timeout := (SELECT COUNT(*) FROM sys_dict_data WHERE dict_id = @music_dict_id AND label = 'timeout');
INSERT INTO sys_dict_data (dict_id, label, value, style, is_default, sort, remark, status)
SELECT @music_dict_id, 'timeout', '10000', NULL, 0, 3, '调用第三方音乐接口的超时时间（毫秒），缺省 10000', 1
FROM DUAL
WHERE @has_timeout = 0;
