-- 2026-10-09 P3：站点默认名统一为 Lumina；备案号改由系统配置 site_icp 提供（页脚不再硬编码）。
-- 1. 只改仍为旧默认值"我的博客"的库，已自定义站点名的库不受影响。
-- 2. 仅在缺失时写入备案号当前值，保证升级后页脚显示不变。
-- 可重复执行。
UPDATE system_config
SET config_value = 'Lumina'
WHERE config_key = 'site_name' AND config_value = '我的博客';

INSERT INTO system_config (config_key, config_value, config_type, description, is_public)
SELECT 'site_icp', '京ICP备2026045342号-1', 'string', '网站备案号', 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM system_config WHERE config_key = 'site_icp');
