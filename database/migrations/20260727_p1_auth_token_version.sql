ALTER TABLE users
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT '认证令牌版本';
