-- 增加评论回复目标字段，支持两层展示中的“X 回复 Y”
ALTER TABLE `comments`
  ADD COLUMN `reply_to_comment_id` BIGINT NULL AFTER `parent_id`;

-- 为回复目标添加索引以优化联查昵称与列表构建
CREATE INDEX `idx_reply_to_comment_id` ON `comments` (`reply_to_comment_id`);

-- 可选约束：顶层评论不应设置回复目标（保留为业务约束，不用DB约束）
-- 注意：线上执行前请先确认无锁表风险，并在低峰时段操作。
