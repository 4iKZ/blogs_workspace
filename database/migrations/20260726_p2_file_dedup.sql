-- P2-05 additive migration. Safe to keep when rolling application code back.
ALTER TABLE `file_info`
  ADD COLUMN `content_hash` CHAR(64) NULL COMMENT '文件内容SHA-256' AFTER `file_url`,
  ADD UNIQUE KEY `uk_file_info_user_hash` (`upload_user_id`, `content_hash`);

CREATE TABLE IF NOT EXISTS `file_cleanup_tasks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `object_key` VARCHAR(500) NOT NULL,
  `retry_count` INT NOT NULL DEFAULT 0,
  `next_retry_time` DATETIME NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `last_error` VARCHAR(1000) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_cleanup_object_key` (`object_key`),
  KEY `idx_file_cleanup_due` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='TOS对象清理补偿任务';
