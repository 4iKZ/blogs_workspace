-- ============================================================================
-- 数据库初始化脚本: init_database.sql
-- 创建日期: 2026-01-28
-- 作者: Trae AI Assistant
-- 说明: 此脚本整合了表结构创建 (create_tables.sql)、初始数据插入 (init_data.sql) 
--       以及通知功能触发器 (notifications.sql)，用于全新环境的数据库初始化部署。
-- ============================================================================

-- 1. 环境配置
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- 创建并使用数据库
CREATE DATABASE IF NOT EXISTS `blog_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `blog_db`;

-- 2. 表结构创建 (按照依赖顺序)
-- 来自: create_tables.sql

-- [1] 用户表 (users)
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `email` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱地址',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '头像URL',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-正常，2-禁用，3-删除',
  `role` tinyint NOT NULL DEFAULT '1' COMMENT '角色：1-普通用户，2-管理员，3-超级管理员',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bio` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `website` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `follower_count` int NOT NULL DEFAULT '0',
  `following_count` int NOT NULL DEFAULT '0',
  `position` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '职位',
  `company` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公司/单位/学校',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- [2] 分类表 (categories)
DROP TABLE IF EXISTS `categories`;
CREATE TABLE `categories` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类描述',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类ID，0表示顶级分类',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序序号',
  `article_count` int NOT NULL DEFAULT '0' COMMENT '文章数量',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-正常，2-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name_parent` (`name`,`parent_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- [3] 文章表 (articles)
DROP TABLE IF EXISTS `articles`;
CREATE TABLE `articles` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章标题',
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文章内容（Markdown格式）',
  `summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文章摘要',
  `cover_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图片URL',
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
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `topic_id` bigint DEFAULT NULL COMMENT '话题ID',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_status` (`status`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_view_count` (`view_count`),
  KEY `idx_like_count` (`like_count`),
  KEY `idx_is_top_recommend` (`is_top`,`is_recommend`),
  FULLTEXT KEY `ft_title_content` (`title`,`content`),
  CONSTRAINT `fk_articles_author` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_articles_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- [4] 评论表 (comments)
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父评论ID，0表示顶级评论',
  `reply_to_comment_id` bigint DEFAULT NULL COMMENT '回复的目标评论ID',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-待审核，2-已通过，3-已拒绝，4-已删除',
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户代理',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_comments_article_created` (`article_id`,`create_time`),
  CONSTRAINT `fk_comments_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- [5] 文章浏览记录表 (article_views)
DROP TABLE IF EXISTS `article_views`;
CREATE TABLE `article_views` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '浏览记录ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（游客为NULL）',
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户代理',
  `referer` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '来源页面',
  `view_date` date NOT NULL COMMENT '浏览日期',
  `view_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_view_date` (`view_date`),
  KEY `idx_view_time` (`view_time`),
  KEY `idx_ip_article_date` (`ip_address`,`article_id`,`view_date`),
  CONSTRAINT `fk_article_views_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_article_views_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章浏览记录表';

-- [6] 评论点赞表 (comment_likes)
DROP TABLE IF EXISTS `comment_likes`;
CREATE TABLE `comment_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `comment_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_comment_likes_comment` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- [7] 文件信息表 (file_info)
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '原始文件名',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '存储文件名',
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件路径',
  `file_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件访问URL',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件类型',
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_extension` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件扩展名',
  `file_category` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件分类：image/attachment',
  `upload_user_id` bigint DEFAULT NULL COMMENT '上传用户ID',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '文件状态：active/deleted',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `fk_file_info_upload_user` (`upload_user_id`),
  CONSTRAINT `fk_file_info_upload_user` FOREIGN KEY (`upload_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';

-- [8] 消息通知表 (notifications)
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` bigint NOT NULL COMMENT '接收通知的用户ID',
  `sender_id` bigint NOT NULL COMMENT '触发通知的用户ID',
  `type` tinyint NOT NULL COMMENT '通知类型：1-文章点赞，2-文章评论，3-评论点赞，4-评论回复',
  `target_id` bigint NOT NULL COMMENT '目标ID（文章ID或评论ID）',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1-文章，2-评论',
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知内容',
  `is_read` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读：0-未读，1-已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_type` (`type`),
  KEY `idx_target` (`target_id`,`target_type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_user_read` (`user_id`,`is_read`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_user_read_time` (`user_id`,`is_read`,`create_time` DESC),
  CONSTRAINT `fk_notifications_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知表';

-- [9] 系统配置表 (system_config)
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text COLLATE utf8mb4_unicode_ci COMMENT '配置值',
  `config_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'string' COMMENT '配置类型：string/number/boolean/json',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '配置描述',
  `is_public` tinyint NOT NULL DEFAULT '0' COMMENT '是否公开：0-否，1-是',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- [10] 文件上传记录表 (upload_files)
DROP TABLE IF EXISTS `upload_files`;
CREATE TABLE `upload_files` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储文件名',
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件路径',
  `file_url` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '访问URL',
  `file_size` bigint NOT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件类型',
  `mime_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MIME类型',
  `upload_user_id` bigint NOT NULL COMMENT '上传用户ID',
  `usage_type` tinyint NOT NULL DEFAULT '1' COMMENT '用途类型：1-文章图片，2-头像，3-其他',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1-正常，2-已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_path` (`file_path`),
  KEY `idx_upload_user_id` (`upload_user_id`),
  KEY `idx_usage_type` (`usage_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_upload_files_user` FOREIGN KEY (`upload_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传记录表';

-- [11] 用户收藏表 (user_favorites)
DROP TABLE IF EXISTS `user_favorites`;
CREATE TABLE `user_favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_article` (`user_id`,`article_id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_favorites_article` FOREIGN KEY (`article_id`) REFERENCES `articles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorites_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

-- [12] 用户关注关系表 (user_follows)
DROP TABLE IF EXISTS `user_follows`;
CREATE TABLE `user_follows` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `follower_id` bigint NOT NULL COMMENT '关注者ID',
  `following_id` bigint NOT NULL COMMENT '被关注者ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标志：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follower_following` (`follower_id`,`following_id`),
  KEY `idx_follower` (`follower_id`),
  KEY `idx_following` (`following_id`),
  CONSTRAINT `fk_follower` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_following` FOREIGN KEY (`following_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注关系表';

-- [13] 用户点赞表 (user_likes)
DROP TABLE IF EXISTS `user_likes`;
CREATE TABLE `user_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_id` bigint NOT NULL COMMENT '目标ID（文章ID或评论ID）',
  `target_type` tinyint NOT NULL COMMENT '目标类型：1-文章，2-评论',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`,`target_id`,`target_type`),
  KEY `idx_target` (`target_id`,`target_type`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_likes_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户点赞表';

-- [14] 访问统计表 (visit_statistics)
DROP TABLE IF EXISTS `visit_statistics`;
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
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_date` (`date`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问统计表';

-- [15] 网站访问日志表 (website_access_log)
DROP TABLE IF EXISTS `website_access_log`;
CREATE TABLE `website_access_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访问日志ID',
  `access_date` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '访问日期',
  `access_time` datetime NOT NULL COMMENT '访问时间',
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户代理',
  `request_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求URL',
  `page_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '页面URL',
  `request_method` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法',
  `response_status` int DEFAULT NULL COMMENT '响应状态码',
  `response_time` bigint DEFAULT NULL COMMENT '响应时间（毫秒）',
  `referer` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '页面来源',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID（游客为NULL）',
  `session_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会话ID',
  `country` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国家',
  `province` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '省份',
  `city` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '城市',
  `device_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备类型',
  `browser` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器',
  `operating_system` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作系统',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_access_date` (`access_date`),
  KEY `idx_access_time` (`access_time`),
  KEY `idx_ip_address` (`ip_address`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网站访问日志表';


-- 3. 初始数据插入
-- 来自: init_data.sql

-- [1] 系统配置数据
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`, `is_public`) VALUES
('site_name', '我的博客', 'string', '网站名称', 1),
('site_description', '一个优秀的个人博客网站', 'string', '网站描述', 1),
('site_keywords', '博客,技术,分享,学习', 'string', '网站关键词', 1),
('site_logo', '', 'string', '网站Logo URL', 1),
('site_favicon', '', 'string', '网站图标 URL', 1),
('allow_register', 'true', 'boolean', '是否允许用户注册', 0),
('comment_audit', 'false', 'boolean', '评论是否需要审核', 0),
('max_file_size', '10485760', 'number', '文件上传最大大小（字节）', 0),
('upload_allowed_types', 'jpg,jpeg,png,gif,webp', 'string', '允许上传的文件类型', 0),
('articles_per_page', '10', 'number', '每页文章数量', 1),
('comments_per_page', '20', 'number', '每页评论数量', 1),
('hot_article_threshold', '10', 'number', '热门文章点赞阈值', 0),
('enable_notification', 'true', 'boolean', '是否启用通知功能', 0),
('enable_visit_statistics', 'true', 'boolean', '是否启用访问统计', 0);

-- [2] 管理员与演示用户
-- 密码: admin123 (BCrypt加密后的值)
INSERT INTO `users` (`username`, `email`, `password`, `nickname`, `avatar`, `status`, `role`, `phone`, `bio`, `website`, `follower_count`, `following_count`, `position`, `company`) VALUES
('admin', 'admin@blog.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLO.TAGxK6Lu', '系统管理员', NULL, 1, 3, NULL, '博客系统管理员', NULL, 0, 0, '管理员', '博客系统'),
('demo_user', 'demo@blog.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLO.TAGxK6Lu', '演示用户', NULL, 1, 1, NULL, '这是一个演示账户', NULL, 0, 0, NULL, NULL);

-- [3] 默认分类
INSERT INTO `categories` (`name`, `description`, `parent_id`, `sort_order`, `article_count`, `status`) VALUES
('技术分享', '技术相关的文章分享', 0, 1, 0, 1),
('生活随笔', '日常生活感悟和随笔', 0, 2, 0, 1),
('学习笔记', '学习过程中的笔记和总结', 0, 3, 0, 1),
('项目经验', '项目开发经验和总结', 0, 4, 0, 1),
('工具推荐', '好用工具和软件推荐', 0, 5, 0, 1);

-- 子分类
INSERT INTO `categories` (`name`, `description`, `parent_id`, `sort_order`, `article_count`, `status`) VALUES
('Java开发', 'Java相关技术文章', 1, 1, 0, 1),
('前端技术', '前端开发相关文章', 1, 2, 0, 1),
('数据库', '数据库相关技术文章', 1, 3, 0, 1),
('运维部署', '服务器运维和部署相关', 1, 4, 0, 1);

-- [4] 示例文章
INSERT INTO `articles` (`title`, `content`, `summary`, `cover_image`, `category_id`, `author_id`, `status`, `view_count`, `like_count`, `comment_count`, `favorite_count`, `is_top`, `is_recommend`, `publish_time`) VALUES
('欢迎来到我的博客', '# 欢迎来到我的博客\n\n这是我的第一篇博客文章，欢迎大家来到我的个人博客网站！\n\n## 关于这个博客\n\n这个博客是使用 **Spring Boot** + **Vue.js** 技术栈开发的现代化博客系统，具有以下特点：\n\n- 🚀 现代化的技术栈\n- 📱 响应式设计，支持移动端\n- 🔍 全文搜索功能\n- 💬 评论系统\n- 👍 点赞和收藏功能\n- 📊 访问统计\n- 🔐 用户权限管理\n\n## 主要功能\n\n### 用户功能\n- 用户注册和登录\n- 个人资料管理\n- 文章收藏和点赞\n- 评论互动\n\n### 内容管理\n- 文章发布和编辑\n- 分类管理\n- 图片上传\n- Markdown 编辑器\n\n### 管理功能\n- 用户管理\n- 内容审核\n- 数据统计\n- 系统配置\n\n希望大家喜欢这个博客系统！', '欢迎来到我的个人博客，这里将分享技术文章、学习笔记和生活感悟。', NULL, 1, 1, 2, 0, 0, 0, 0, 1, 1, NOW()),
('Spring Boot 快速入门指南', '# Spring Boot 快速入门指南\n\nSpring Boot 是一个基于 Spring 框架的快速开发框架，它简化了 Spring 应用的配置和部署。\n\n## 什么是 Spring Boot\n\nSpring Boot 是由 Pivotal 团队提供的全新框架，其设计目的是用来简化新 Spring 应用的初始搭建以及开发过程。\n\n## 主要特性\n\n1. **自动配置**: 根据项目依赖自动配置 Spring 应用\n2. **起步依赖**: 简化依赖管理\n3. **内嵌服务器**: 无需部署 WAR 文件\n4. **生产就绪**: 提供监控、健康检查等功能\n\n## 快速开始\n\n### 1. 创建项目\n\n使用 Spring Initializr 创建项目：\n\n```bash\ncurl https://start.spring.io/starter.zip \\\n  -d dependencies=web,data-jpa,mysql \\\n  -d name=blog-demo \\\n  -o blog-demo.zip\n```\n\n### 2. 编写第一个控制器\n\n```java\n@RestController\npublic class HelloController {\n\n    @GetMapping(\"/hello\")\n    public String hello() {\n        return \"Hello, Spring Boot!\";\n    }\n}\n```\n\n### 3. 运行应用\n\n```bash\nmvn spring-boot:run\n```\n\n## 总结\n\nSpring Boot 大大简化了 Spring 应用的开发，是现代 Java Web 开发的首选框架。', 'Spring Boot 快速入门教程，介绍基本概念和使用方法。', NULL, 6, 1, 2, 0, 0, 0, 0, 0, 1, NOW()),
('Vue 3 Composition API 详解', '# Vue 3 Composition API 详解\n\nVue 3 引入了全新的 Composition API，它提供了一种更灵活的方式来组织组件逻辑。\n\n## 什么是 Composition API\n\nComposition API 是一种基于函数的 API，允许我们将相关功能组织在一起，而不是按照选项（data、methods、computed 等）分散。\n\n## 核心概念\n\n### setup 函数\n\nsetup 函数是 Composition API 的入口点：\n\n```javascript\nimport { ref, computed } from ''vue''\n\nexport default {\n  setup() {\n    const count = ref(0)\n    const doubled = computed(() => count.value * 2)\n\n    function increment() {\n      count.value++\n    }\n\n    return {\n      count,\n      doubled,\n      increment\n    }\n  }\n}\n```\n\n### ref 和 reactive\n\n- ref: 用于创建响应式的基本类型\n- reactive: 用于创建响应式对象\n\n```javascript\nconst count = ref(0)\nconst state = reactive({\n  name: ''Vue 3'',\n  version: ''3.0''\n})\n```\n\n## 总结\n\nComposition API 让代码更加模块化和可维护，特别适合大型应用的开发。', '深入讲解 Vue 3 Composition API 的使用方法 and 最佳实践。', NULL, 7, 1, 2, 0, 0, 0, 0, 1, 1, NOW());

-- [5] 示例评论
INSERT INTO `comments` (`article_id`, `user_id`, `parent_id`, `content`, `like_count`, `status`, `deleted`) VALUES
(1, 2, 0, '欢迎博主！网站做得很棒，期待更多精彩内容！', 0, 2, 0),
(1, 1, 1, '谢谢支持！我会继续努力更新优质内容的。', 0, 2, 0),
(2, 2, 0, '这个Spring Boot教程写得很详细，对新手很友好！', 0, 2, 0),
(3, 2, 0, 'Composition API 确实是 Vue 3 的一大亮点，博主讲得很清楚。', 0, 2, 0);

-- [6] 更新统计数据
UPDATE `categories` SET `article_count` = (
    SELECT COUNT(*) FROM `articles` WHERE `category_id` = `categories`.`id` AND `status` = 2
);
UPDATE `articles` SET `comment_count` = (
    SELECT COUNT(*) FROM `comments` WHERE `article_id` = `articles`.`id` AND `status` = 2 AND `deleted` = 0
);

-- [7] 今日访问统计
INSERT INTO `visit_statistics` (`date`, `total_visits`, `unique_visitors`, `page_views`, `new_users`, `new_articles`, `new_comments`) VALUES
(CURDATE(), 0, 0, 0, 2, 3, 4)
ON DUPLICATE KEY UPDATE
  `new_users` = 2,
  `new_articles` = 3,
  `new_comments` = 4;


-- 4. 存储过程、函数与触发器
-- 来自: create_tables.sql & notifications.sql

DELIMITER ;;

-- [1] 文章删除后的清理触发器 (来自 create_tables.sql)
DROP TRIGGER IF EXISTS `articles_after_delete`;;
CREATE TRIGGER `articles_after_delete` AFTER DELETE ON `articles` FOR EACH ROW BEGIN
  DELETE FROM user_likes WHERE target_type = 1 AND target_id = OLD.id;
END;;

-- [2] 点赞校验触发器 (来自 notifications.sql)
DROP TRIGGER IF EXISTS `user_likes_before_insert`;;
CREATE TRIGGER `user_likes_before_insert` BEFORE INSERT ON `user_likes`
FOR EACH ROW
BEGIN
    DECLARE article_exists INT;
    DECLARE comment_exists INT;

    IF NEW.target_type = 1 THEN
        -- 验证文章是否存在且已发布
        SELECT COUNT(*) INTO article_exists
        FROM `articles`
        WHERE `id` = NEW.target_id AND `status` = 2;

        IF article_exists = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot like article: article does not exist or is not published';
        END IF;
    ELSEIF NEW.target_type = 2 THEN
        -- 验证评论是否存在
        SELECT COUNT(*) INTO comment_exists
        FROM `comments`
        WHERE `id` = NEW.target_id;

        IF comment_exists = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot like comment: comment does not exist';
        END IF;
    END IF;
END;;

DROP TRIGGER IF EXISTS `user_likes_before_update`;;
CREATE TRIGGER `user_likes_before_update` BEFORE UPDATE ON `user_likes`
FOR EACH ROW
BEGIN
    DECLARE article_exists INT;
    DECLARE comment_exists INT;

    IF NEW.target_type = 1 THEN
        -- 验证文章是否存在且已发布
        SELECT COUNT(*) INTO article_exists
        FROM `articles`
        WHERE `id` = NEW.target_id AND `status` = 2;

        IF article_exists = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot update like: article does not exist or is not published';
        END IF;
    ELSEIF NEW.target_type = 2 THEN
        -- 验证评论是否存在
        SELECT COUNT(*) INTO comment_exists
        FROM `comments`
        WHERE `id` = NEW.target_id;

        IF comment_exists = 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Cannot update like: comment does not exist';
        END IF;
    END IF;
END;;

DELIMITER ;

-- 5. 收尾工作
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- 初始化完成标记
SELECT '========================================' as '';
SELECT '数据库初始化完成！' as '状态';
SELECT '默认管理员账号: admin / admin123' as '登录信息';
SELECT '========================================' as '';
