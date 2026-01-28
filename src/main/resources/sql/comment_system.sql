-- 文章评论系统数据库迁移脚本

-- 1. 为comments表添加索引
ALTER TABLE comments ADD INDEX idx_article_parent_status (article_id, parent_id, status);
ALTER TABLE comments ADD INDEX idx_user_status (user_id, status);
ALTER TABLE comments ADD INDEX idx_create_time (create_time);

-- 2. 新增评论点赞表
CREATE TABLE IF NOT EXISTS comment_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    KEY idx_user_id (user_id),
    CONSTRAINT fk_comment_likes_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_likes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 新增评论通知表
CREATE TABLE IF NOT EXISTS comment_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    type TINYINT NOT NULL COMMENT '1-评论通知，2-回复通知，3-点赞通知',
    read_status TINYINT DEFAULT 0 COMMENT '0-未读，1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_receiver_status (receiver_id, read_status),
    CONSTRAINT fk_notifications_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_user FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 新增敏感词表
CREATE TABLE IF NOT EXISTS sensitive_words (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(50) NOT NULL UNIQUE,
    category VARCHAR(20) DEFAULT 'default',
    level TINYINT DEFAULT 1 COMMENT '1-警告，2-禁止',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 初始化敏感词数据
INSERT INTO sensitive_words (word, category, level) VALUES
('敏感词1', 'default', 1),
('敏感词2', 'default', 1),
('敏感词3', 'default', 2);
