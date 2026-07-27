-- ============================================================================
-- 数据库结构脚本: schema.sql
-- 说明: 建库、建表、建触发器。全新环境请先执行本脚本，再按需执行 data.sql 插入示例数据。
-- ============================================================================









DROP TABLE IF EXISTS `article_moderation_submissions`;

CREATE TABLE `article_moderation_submissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `article_id` bigint NOT NULL,
  `active_article_id` bigint DEFAULT NULL,
  `submission_token` varchar(64) NOT NULL,
  `title` varchar(255) NOT NULL,
  `summary` varchar(500) DEFAULT NULL,
  `content` clob NOT NULL,
  `cover_image` varchar(500) DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `topic_id` bigint DEFAULT NULL,
  `allow_comment` tinyint DEFAULT 1,
  `submission_type` varchar(16) NOT NULL,
  `status` varchar(32) NOT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` timestamp DEFAULT NULL,
  `processing_started_at` timestamp DEFAULT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  `submitted_at` timestamp NOT NULL,
  `reviewed_at` timestamp DEFAULT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `review_reason` varchar(500) DEFAULT NULL,
  `manual_action_at` timestamp DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `uk_article_moderation_submission_token` UNIQUE (`submission_token`),
  CONSTRAINT `uk_article_moderation_active_article` UNIQUE (`active_article_id`)
);

-- ----------------------------------------------------------------------------
-- 表结构（按外键依赖顺序）
-- ----------------------------------------------------------------------------




CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) NOT NULL COMMENT '邮箱地址',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像URL',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-正常，2-禁用，3-删除',
  `role` tinyint NOT NULL DEFAULT '1' COMMENT '角色：1-普通用户，2-管理员，3-超级管理员',
  `token_version` int NOT NULL DEFAULT '0' COMMENT '认证令牌版本',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(45) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `phone` varchar(20) DEFAULT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `website` varchar(255) DEFAULT NULL,
  `follower_count` int NOT NULL DEFAULT '0',
  `following_count` int NOT NULL DEFAULT '0',
  `position` varchar(100) DEFAULT NULL COMMENT '职位',
  `company` varchar(100) DEFAULT NULL COMMENT '公司/单位/学校',
  `github_id` bigint DEFAULT NULL COMMENT 'GitHub用户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `users_uk_username` (`username`),
  UNIQUE KEY `users_uk_email` (`email`),
  UNIQUE KEY `users_uk_github_id` (`github_id`),
  KEY `users_idx_status` (`status`),
  KEY `users_idx_create_time` (`create_time`)
) COMMENT='用户表';

CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `description` varchar(200) DEFAULT NULL COMMENT '分类描述',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类ID，0表示顶级分类',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序序号',
  `article_count` int NOT NULL DEFAULT '0' COMMENT '文章数量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-正常，2-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `categories_uk_name_parent` (`name`,`parent_id`),
  KEY `categories_idx_parent_id` (`parent_id`),
  KEY `categories_idx_sort_order` (`sort_order`)
) COMMENT='分类表';

CREATE TABLE `articles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `content` longtext NOT NULL COMMENT '文章内容（Markdown格式）',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `cover_image` varchar(500) DEFAULT NULL COMMENT '封面图片URL',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-草稿，2-已发布，3-已删除',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览量',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论数',
  `favorite_count` int NOT NULL DEFAULT '0' COMMENT '收藏数',
  `is_top` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶：0-否，1-是',
  `is_recommend` tinyint NOT NULL DEFAULT '0' COMMENT '是否推荐：0-否，1-是',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `topic_id` bigint DEFAULT NULL COMMENT '话题ID',
  PRIMARY KEY (`id`),
  KEY `articles_idx_category_id` (`category_id`),
  KEY `articles_idx_author_id` (`author_id`),
  KEY `articles_idx_status` (`status`),
  KEY `articles_idx_publish_time` (`publish_time`),
  KEY `articles_idx_view_count` (`view_count`),
  KEY `articles_idx_like_count` (`like_count`),
  KEY `articles_idx_is_top_recommend` (`is_top`,`is_recommend`),
  KEY `articles_idx_category_status_publish` (`category_id`, `status`, `publish_time` DESC),
  KEY `articles_idx_author_status_publish` (`author_id`, `status`, `publish_time` DESC),
  KEY `articles_idx_status_top_recommend_publish` (`status`, `is_top`, `is_recommend`, `publish_time` DESC)
) COMMENT='文章表';

CREATE TABLE `article_views` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浏览记录ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（游客为NULL）',
  `ip_address` varchar(45) NOT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `referer` varchar(500) DEFAULT NULL COMMENT '来源页面',
  `view_date` date NOT NULL COMMENT '浏览日期',
  `view_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `article_views_idx_article_id` (`article_id`),
  KEY `article_views_idx_user_id` (`user_id`),
  KEY `article_views_idx_view_date` (`view_date`),
  KEY `article_views_idx_view_time` (`view_time`),
  KEY `article_views_idx_ip_article_date` (`ip_address`, `article_id`, `view_date`)
) COMMENT='文章浏览记录表';

CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父评论ID，0表示顶级评论',
  `reply_to_comment_id` bigint DEFAULT NULL COMMENT '回复的目标评论ID',
  `content` text NOT NULL COMMENT '评论内容',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-待审核，2-已通过，3-已拒绝，4-已删除',
  `ip_address` varchar(45) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `comments_idx_user_id` (`user_id`),
  KEY `comments_idx_parent_id` (`parent_id`),
  KEY `comments_idx_status` (`status`),
  KEY `comments_idx_create_time` (`create_time`),
  KEY `comments_idx_comments_article_created` (`article_id`,`create_time`),
  KEY `comments_idx_article_status_deleted` (`article_id`, `status`, `deleted`)
) COMMENT='评论表';


CREATE TABLE `comment_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `comment_likes_uk_comment_user` (`comment_id`,`user_id`),
  KEY `comment_likes_idx_user_id` (`user_id`)
) COMMENT='评论点赞表';

CREATE TABLE `file_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_name` varchar(255) DEFAULT NULL COMMENT '存储文件名',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件访问URL',
  `content_hash` char(64) DEFAULT NULL COMMENT '文件内容SHA-256',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `mime_type` varchar(100) DEFAULT NULL,
  `file_extension` varchar(20) DEFAULT NULL COMMENT '文件扩展名',
  `file_category` varchar(20) DEFAULT NULL COMMENT '文件分类：image/attachment',
  `upload_user_id` bigint DEFAULT NULL COMMENT '上传用户ID',
  `status` varchar(20) DEFAULT NULL COMMENT '文件状态：active/deleted',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `file_info_uk_user_hash` (`upload_user_id`,`content_hash`),
  KEY `file_info_fk_file_info_upload_user` (`upload_user_id`),
  KEY `file_info_idx_status_category` (`status`, `file_category`)
) COMMENT='文件信息表';

CREATE TABLE `file_cleanup_tasks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `object_key` varchar(500) NOT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_time` datetime NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `last_error` varchar(1000) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `file_cleanup_uk_object_key` (`object_key`),
  KEY `file_cleanup_idx_due` (`status`,`next_retry_time`)
) COMMENT='TOS对象清理补偿任务';

CREATE TABLE `upload_files` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_name` varchar(255) NOT NULL COMMENT '存储文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  `file_url` varchar(500) NOT NULL COMMENT '访问URL',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) NOT NULL COMMENT '文件类型',
  `mime_type` varchar(100) NOT NULL COMMENT 'MIME类型',
  `upload_user_id` bigint NOT NULL COMMENT '上传用户ID',
  `usage_type` tinyint NOT NULL DEFAULT '1' COMMENT '用途类型：1-文章图片，2-头像，3-其他',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-正常，2-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `upload_files_uk_file_path` (`file_path`),
  KEY `upload_files_idx_upload_user_id` (`upload_user_id`),
  KEY `upload_files_idx_status` (`status`),
  KEY `upload_files_idx_usage_type` (`usage_type`),
  KEY `upload_files_idx_create_time` (`create_time`)
) COMMENT='文件上传记录表';

CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收通知的用户ID',
  `sender_id` bigint NULL COMMENT '触发通知的用户ID（系统通知时为NULL）',
  `type` tinyint NOT NULL COMMENT '通知类型：1-文章点赞，2-文章评论，3-评论点赞，4-评论回复',
  `target_id` bigint NOT NULL COMMENT '目标ID（文章ID或评论ID）',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1-文章，2-评论',
  `content` varchar(500) DEFAULT NULL COMMENT '通知内容',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读：0-未读，1-已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `notifications_idx_sender_id` (`sender_id`),
  KEY `notifications_idx_type` (`type`),
  KEY `notifications_idx_target` (`target_id`,`target_type`),
  KEY `notifications_idx_is_read` (`is_read`),
  KEY `notifications_idx_create_time` (`create_time`),
  KEY `notifications_idx_user_read_time` (`user_id`,`is_read`,`create_time` DESC)
) COMMENT='消息通知表';

CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `config_type` varchar(20) NOT NULL DEFAULT 'string' COMMENT '配置类型：string/number/boolean/json',
  `description` varchar(200) DEFAULT NULL COMMENT '配置描述',
  `is_public` tinyint NOT NULL DEFAULT '0' COMMENT '是否公开：0-否，1-是',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `system_config_uk_config_key` (`config_key`)
) COMMENT='系统配置表';


CREATE TABLE `user_favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_favorites_uk_user_article` (`user_id`,`article_id`),
  KEY `user_favorites_idx_article_id` (`article_id`),
  KEY `user_favorites_idx_create_time` (`create_time`),
  KEY `user_favorites_idx_user_create_time` (`user_id`, `create_time` DESC)
) COMMENT='用户收藏表';

CREATE TABLE `user_follows` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `follower_id` bigint NOT NULL COMMENT '关注者ID',
  `following_id` bigint NOT NULL COMMENT '被关注者ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_follows_uk_follower_following` (`follower_id`,`following_id`),
  KEY `user_follows_idx_follower` (`follower_id`),
  KEY `user_follows_idx_following` (`following_id`),
  KEY `user_follows_idx_follower_deleted` (`follower_id`, `deleted`),
  KEY `user_follows_idx_following_deleted` (`following_id`, `deleted`)
) COMMENT='用户关注关系表';

CREATE TABLE `user_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_id` bigint NOT NULL COMMENT '目标ID（文章ID或评论ID）',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1-文章，2-评论',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_likes_uk_user_target` (`user_id`,`target_id`,`target_type`),
  KEY `user_likes_idx_target` (`target_id`,`target_type`),
  KEY `user_likes_idx_create_time` (`create_time`),
  KEY `user_likes_idx_user_type_create_time` (`user_id`, `target_type`, `create_time` DESC)
) COMMENT='用户点赞表';

CREATE TABLE `visit_statistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `date` date NOT NULL COMMENT '统计日期',
  `total_visits` int NOT NULL DEFAULT '0' COMMENT '总访问量',
  `unique_visitors` int NOT NULL DEFAULT '0' COMMENT '独立访客数',
  `page_views` int NOT NULL DEFAULT '0' COMMENT '页面浏览量',
  `new_users` int NOT NULL DEFAULT '0' COMMENT '新用户数',
  `new_articles` int NOT NULL DEFAULT '0' COMMENT '新文章数',
  `new_comments` int NOT NULL DEFAULT '0' COMMENT '新评论数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `visit_statistics_uk_date` (`date`),
  KEY `visit_statistics_idx_create_time` (`create_time`)
) COMMENT='访问统计表';

CREATE TABLE `website_access_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问日志ID',
  `access_date` varchar(10) DEFAULT NULL COMMENT '访问日期',
  `access_time` datetime NOT NULL COMMENT '访问时间',
  `ip_address` varchar(45) NOT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `page_url` varchar(500) DEFAULT NULL COMMENT '页面URL',
  `request_method` varchar(10) DEFAULT NULL COMMENT '请求方法',
  `response_status` int DEFAULT NULL COMMENT '响应状态码',
  `response_time` bigint DEFAULT NULL COMMENT '响应时间（毫秒）',
  `referer` varchar(500) DEFAULT NULL COMMENT '页面来源',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（游客为NULL）',
  `session_id` varchar(100) DEFAULT NULL COMMENT '会话ID',
  `country` varchar(50) DEFAULT NULL COMMENT '国家',
  `province` varchar(50) DEFAULT NULL COMMENT '省份',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `device_type` varchar(20) DEFAULT NULL COMMENT '设备类型',
  `browser` varchar(50) DEFAULT NULL COMMENT '浏览器',
  `operating_system` varchar(50) DEFAULT NULL COMMENT '操作系统',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `website_access_log_idx_access_date` (`access_date`),
  KEY `website_access_log_idx_access_time` (`access_time`),
  KEY `website_access_log_idx_ip_address` (`ip_address`),
  KEY `website_access_log_idx_user_id` (`user_id`),
  KEY `website_access_log_idx_session_id` (`session_id`),
  KEY `website_access_log_idx_access_time_page` (`access_time`, `page_url`),
  KEY `website_access_log_idx_user_access_time` (`user_id`, `access_time`),
  KEY `website_access_log_idx_session_access_time` (`session_id`, `access_time`)
) COMMENT='网站访问日志表';

-- auto-generated definition
create table sensitive_words
(
    id          bigint auto_increment
        primary key,
    word        varchar(50)                           not null,
    category    varchar(20) default 'default'         null,
    level       tinyint     default 1                 null comment '1-警告，2-禁止',
    create_time datetime    default CURRENT_TIMESTAMP null,
    update_time datetime    default CURRENT_TIMESTAMP null,
    constraint word
        unique (word),
    key `sensitive_words_idx_category_level` (`category`, `level`)
);


-- ----------------------------------------------------------------------------
