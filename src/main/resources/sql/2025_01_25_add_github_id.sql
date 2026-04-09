-- 为 users 表添加 github_id 字段
ALTER TABLE users ADD COLUMN github_id bigint DEFAULT NULL COMMENT 'GitHub用户ID' AFTER company;

-- 为 github_id 添加唯一索引
ALTER TABLE users ADD UNIQUE KEY uk_github_id (github_id);
