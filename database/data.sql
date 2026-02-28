-- ============================================================================
-- 示例数据脚本: data.sql
-- 说明: 插入系统配置、默认用户、分类、示例文章与评论等。需先执行 schema.sql 建库建表。
-- 默认管理员: admin / admin123  演示用户: demo_user / admin123
-- ============================================================================

USE `blog_db`;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. 系统配置
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

-- 2. 管理员与演示用户（密码: admin123）
INSERT INTO `users` (`username`, `email`, `password`, `nickname`, `avatar`, `status`, `role`, `phone`, `bio`, `website`, `follower_count`, `following_count`, `position`, `company`) VALUES
('admin', 'admin@blog.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLO.TAGxK6Lu', '系统管理员', NULL, 1, 3, NULL, '博客系统管理员', NULL, 0, 0, '管理员', '博客系统'),
('demo_user', 'demo@blog.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLO.TAGxK6Lu', '演示用户', NULL, 1, 1, NULL, '这是一个演示账户', NULL, 0, 0, NULL, NULL);

-- 3. 默认分类
INSERT INTO `categories` (`name`, `description`, `parent_id`, `sort_order`, `article_count`, `status`) VALUES
('技术分享', '技术相关的文章分享', 0, 1, 0, 1),
('生活随笔', '日常生活感悟和随笔', 0, 2, 0, 1),
('学习笔记', '学习过程中的笔记和总结', 0, 3, 0, 1),
('项目经验', '项目开发经验和总结', 0, 4, 0, 1),
('工具推荐', '好用工具和软件推荐', 0, 5, 0, 1),
('Java开发', 'Java相关技术文章', 1, 1, 0, 1),
('前端技术', '前端开发相关文章', 1, 2, 0, 1),
('数据库', '数据库相关技术文章', 1, 3, 0, 1),
('运维部署', '服务器运维和部署相关', 1, 4, 0, 1);

-- 4. 示例文章
INSERT INTO `articles` (`title`, `content`, `summary`, `cover_image`, `category_id`, `author_id`, `status`, `view_count`, `like_count`, `comment_count`, `favorite_count`, `is_top`, `is_recommend`, `publish_time`) VALUES
('欢迎来到我的博客',
'# 欢迎来到我的博客

这是我的第一篇博客文章，欢迎大家来到我的个人博客网站！

## 关于这个博客

这个博客是使用 **Spring Boot** + **Vue.js** 技术栈开发的现代化博客系统，具有以下特点：

- 🚀 现代化的技术栈
- 📱 响应式设计，支持移动端
- 🔍 全文搜索功能
- 💬 评论系统
- 👍 点赞和收藏功能
- 📊 访问统计
- 🔐 用户权限管理

## 主要功能

### 用户功能
- 用户注册和登录
- 个人资料管理
- 文章收藏和点赞
- 评论互动

### 内容管理
- 文章发布和编辑
- 分类管理
- 图片上传
- Markdown 编辑器

### 管理功能
- 用户管理
- 内容审核
- 数据统计
- 系统配置

希望大家喜欢这个博客系统！',
'欢迎来到我的个人博客，这里将分享技术文章、学习笔记和生活感悟。',
NULL, 1, 1, 2, 0, 0, 0, 0, 1, 1, NOW()),

('Spring Boot 快速入门指南',
'# Spring Boot 快速入门指南

Spring Boot 是一个基于 Spring 框架的快速开发框架，它简化了 Spring 应用的配置和部署。

## 什么是 Spring Boot

Spring Boot 是由 Pivotal 团队提供的全新框架，其设计目的是用来简化新 Spring 应用的初始搭建以及开发过程。

## 主要特性

1. **自动配置**: 根据项目依赖自动配置 Spring 应用
2. **起步依赖**: 简化依赖管理
3. **内嵌服务器**: 无需部署 WAR 文件
4. **生产就绪**: 提供监控、健康检查等功能

## 快速开始

### 1. 创建项目

使用 Spring Initializr 创建项目：

```bash
curl https://start.spring.io/starter.zip \\
  -d dependencies=web,data-jpa,mysql \\
  -d name=blog-demo \\
  -o blog-demo.zip
```

### 2. 编写第一个控制器

```java
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}
```

### 3. 运行应用

```bash
mvn spring-boot:run
```

## 总结

Spring Boot 大大简化了 Spring 应用的开发，是现代 Java Web 开发的首选框架。',
'Spring Boot 快速入门教程，介绍基本概念和使用方法。',
NULL, 6, 1, 2, 0, 0, 0, 0, 0, 1, NOW()),

('Vue 3 Composition API 详解',
'# Vue 3 Composition API 详解

Vue 3 引入了全新的 Composition API，它提供了一种更灵活的方式来组织组件逻辑。

## 什么是 Composition API

Composition API 是一种基于函数的 API，允许我们将相关功能组织在一起，而不是按照选项（data、methods、computed 等）分散。

## 核心概念

### setup 函数

setup 函数是 Composition API 的入口点：

```javascript
import { ref, computed } from ''vue''

export default {
  setup() {
    const count = ref(0)
    const doubled = computed(() => count.value * 2)

    function increment() {
      count.value++
    }

    return {
      count,
      doubled,
      increment
    }
  }
}
```

### ref 和 reactive

- ref: 用于创建响应式的基本类型
- reactive: 用于创建响应式对象

```javascript
const count = ref(0)
const state = reactive({
  name: ''Vue 3'',
  version: ''3.0''
})
```

## 总结

Composition API 让代码更加模块化和可维护，特别适合大型应用的开发。',
'深入讲解 Vue 3 Composition API 的使用方法和最佳实践。',
NULL, 7, 1, 2, 0, 0, 0, 0, 1, 1, NOW());

-- 5. 示例评论
INSERT INTO `comments` (`article_id`, `user_id`, `parent_id`, `content`, `like_count`, `status`, `deleted`) VALUES
(1, 2, 0, '欢迎博主！网站做得很棒，期待更多精彩内容！', 0, 2, 0),
(1, 1, 1, '谢谢支持！我会继续努力更新优质内容的。', 0, 2, 0),
(2, 2, 0, '这个Spring Boot教程写得很详细，对新手很友好！', 0, 2, 0),
(3, 2, 0, 'Composition API 确实是 Vue 3 的一大亮点，博主讲得很清楚。', 0, 2, 0);

-- 6. 更新统计数据
UPDATE `categories` SET `article_count` = (
    SELECT COUNT(*) FROM `articles` WHERE `category_id` = `categories`.`id` AND `status` = 2
);
UPDATE `articles` SET `comment_count` = (
    SELECT COUNT(*) FROM `comments` WHERE `article_id` = `articles`.`id` AND `status` = 2 AND `deleted` = 0
);

-- 7. 今日访问统计
INSERT INTO `visit_statistics` (`date`, `total_visits`, `unique_visitors`, `page_views`, `new_users`, `new_articles`, `new_comments`) VALUES
(CURDATE(), 0, 0, 0, 2, 3, 4)
ON DUPLICATE KEY UPDATE
  `new_users` = 2,
  `new_articles` = 3,
  `new_comments` = 4;

SET FOREIGN_KEY_CHECKS = 1;

SELECT '示例数据初始化完成。默认管理员: admin / admin123  演示用户: demo_user / admin123' AS message;
