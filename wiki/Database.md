# 数据库设计文档

[← 返回 Wiki 首页](./Home.md)

---

## 概览

- **数据库名称**: `blog_db`
- **字符集**: `utf8mb4`
- **排序规则**: `utf8mb4_unicode_ci`
- **数据库引擎**: InnoDB
- **建库脚本**: `database/schema.sql`
- **初始化数据**: `database/data.sql`

---

## 表结构关系图

```
users (用户)
  ├── articles (文章) [author_id → users.id]
  │     ├── comments (评论) [article_id → articles.id]
  │     │     └── comment_likes (评论点赞)
  │     ├── article_views (浏览记录)
  │     ├── user_favorites (收藏) [user_id + article_id]
  │     └── user_likes (文章点赞) [target_type=1]
  ├── notifications (通知) [user_id + sender_id → users.id]
  ├── user_follows (关注关系) [follower_id + following_id → users.id]
  ├── file_info (文件信息)
  └── upload_files (上传记录)
  
categories (分类)
  └── articles [category_id → categories.id]

system_config (系统配置，独立)
visit_statistics (访问统计，独立)
website_access_log (访问日志，独立)
```

---

## 详细表结构

### users（用户表）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | bigint | ✓ | AUTO_INCREMENT | 用户ID，主键 |
| `username` | varchar(50) | ✓ | - | 用户名（唯一） |
| `email` | varchar(100) | ✓ | - | 邮箱（唯一） |
| `password` | varchar(255) | ✓ | - | BCrypt 加密密码 |
| `nickname` | varchar(50) | - | NULL | 昵称 |
| `avatar` | varchar(500) | - | NULL | 头像 URL |
| `status` | tinyint | ✓ | 1 | 1-正常，2-禁用，3-删除 |
| `role` | tinyint | ✓ | 1 | 1-普通用户，2-管理员，3-超级管理员 |
| `phone` | varchar(20) | - | NULL | 手机号 |
| `bio` | varchar(255) | - | NULL | 个人简介 |
| `website` | varchar(255) | - | NULL | 个人网站 |
| `position` | varchar(100) | - | NULL | 职位 |
| `company` | varchar(100) | - | NULL | 公司/单位/学校 |
| `follower_count` | int | ✓ | 0 | 粉丝数 |
| `following_count` | int | ✓ | 0 | 关注数 |
| `last_login_time` | datetime | - | NULL | 最后登录时间 |
| `last_login_ip` | varchar(45) | - | NULL | 最后登录 IP |
| `create_time` | datetime | ✓ | CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | datetime | ✓ | ON UPDATE | 更新时间 |

**索引**: `uk_username`（唯一）、`uk_email`（唯一）、`idx_status`、`idx_create_time`

---

### categories（分类表）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | bigint | ✓ | AUTO_INCREMENT | 分类ID |
| `name` | varchar(50) | ✓ | - | 分类名称 |
| `description` | varchar(200) | - | NULL | 分类描述 |
| `parent_id` | bigint | ✓ | 0 | 父分类ID，0=顶级分类 |
| `sort_order` | int | ✓ | 0 | 排序序号 |
| `article_count` | int | ✓ | 0 | 文章数量（统计缓存） |
| `status` | tinyint | ✓ | 1 | 1-正常，2-禁用 |
| `create_time` | datetime | ✓ | CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | datetime | ✓ | ON UPDATE | 更新时间 |

**索引**: `uk_name_parent`（name + parent_id 联合唯一）、`idx_parent_id`、`idx_sort_order`

---

### articles（文章表）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | bigint | ✓ | AUTO_INCREMENT | 文章ID |
| `title` | varchar(200) | ✓ | - | 文章标题 |
| `content` | longtext | ✓ | - | 文章内容（Markdown） |
| `summary` | varchar(500) | - | NULL | 文章摘要 |
| `cover_image` | varchar(500) | - | NULL | 封面图片 URL |
| `category_id` | bigint | ✓ | - | 分类ID |
| `author_id` | bigint | ✓ | - | 作者用户ID |
| `status` | tinyint | ✓ | 1 | 1-草稿，2-已发布，3-已删除 |
| `view_count` | int | ✓ | 0 | 浏览量 |
| `like_count` | int | ✓ | 0 | 点赞数 |
| `comment_count` | int | ✓ | 0 | 评论数 |
| `favorite_count` | int | ✓ | 0 | 收藏数 |
| `is_top` | tinyint | ✓ | 0 | 是否置顶（0-否，1-是） |
| `is_recommend` | tinyint | ✓ | 0 | 是否推荐（0-否，1-是） |
| `publish_time` | datetime | - | NULL | 发布时间 |
| `topic_id` | bigint | - | NULL | 话题ID（预留） |
| `create_time` | datetime | ✓ | CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | datetime | ✓ | ON UPDATE | 更新时间 |

**索引**:
- `idx_category_id`、`idx_author_id`、`idx_status`、`idx_publish_time`
- `idx_view_count`、`idx_like_count`
- `idx_is_top_recommend`（置顶+推荐联合索引）
- `ft_title_content`（**全文索引**，支持全文搜索）

**外键**: `fk_articles_author`（→ users.id）、`fk_articles_category`（→ categories.id）

---

### comments（评论表）

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `id` | bigint | ✓ | AUTO_INCREMENT | 评论ID |
| `article_id` | bigint | ✓ | - | 所属文章ID |
| `user_id` | bigint | ✓ | - | 评论用户ID |
| `parent_id` | bigint | ✓ | 0 | 父评论ID，0=顶级评论 |
| `reply_to_comment_id` | bigint | - | NULL | 回复的目标评论ID（楼中楼） |
| `content` | text | ✓ | - | 评论内容 |
| `like_count` | int | ✓ | 0 | 点赞数 |
| `status` | tinyint | ✓ | 1 | 1-待审核，2-已通过，3-已拒绝，4-已删除 |
| `ip_address` | varchar(45) | - | NULL | IP地址 |
| `user_agent` | varchar(500) | - | NULL | 用户代理 |
| `deleted` | tinyint | ✓ | 0 | 逻辑删除（0-未删，1-已删） |
| `create_time` | datetime | ✓ | CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | datetime | ✓ | ON UPDATE | 更新时间 |

**索引**: `idx_article_id`、`idx_user_id`、`idx_parent_id`、`idx_status`、`idx_comments_article_created`（文章ID+创建时间联合）

**外键**: `fk_comments_article`（→ articles.id，**CASCADE 删除**）、`fk_comments_user`（→ users.id）

---

### comment_likes（评论点赞表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `comment_id` | bigint | 评论ID |
| `user_id` | bigint | 用户ID |
| `create_time` | datetime | 点赞时间 |
| `update_time` | datetime | 更新时间 |

**约束**: `uk_comment_user`（comment_id + user_id 联合唯一，防重复点赞）

---

### user_likes（用户点赞表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户ID |
| `target_id` | bigint | 目标ID（文章ID 或 评论ID） |
| `target_type` | tinyint | 目标类型：1-文章，2-评论 |
| `create_time` | datetime | 点赞时间 |

**约束**: `uk_user_target`（user_id + target_id + target_type 联合唯一）

---

### user_favorites（用户收藏表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `user_id` | bigint | 用户ID |
| `article_id` | bigint | 文章ID |
| `create_time` | datetime | 收藏时间 |

**约束**: `uk_user_article`（user_id + article_id 联合唯一）

---

### user_follows（用户关注关系表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `follower_id` | bigint | 关注者ID |
| `following_id` | bigint | 被关注者ID |
| `deleted` | tinyint | 逻辑删除（0-有效，1-已取关） |
| `create_time` | datetime | 关注时间 |
| `update_time` | datetime | 更新时间 |

**约束**: `uk_follower_following`（联合唯一）

---

### notifications（消息通知表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `user_id` | bigint | 接收通知的用户ID |
| `sender_id` | bigint | 触发通知的用户ID |
| `type` | tinyint | 类型：1-文章点赞，2-文章评论，3-评论点赞，4-评论回复 |
| `target_id` | bigint | 目标ID（文章ID 或 评论ID） |
| `target_type` | tinyint | 目标类型：1-文章，2-评论 |
| `content` | varchar(500) | 通知内容 |
| `is_read` | tinyint | 是否已读：0-未读，1-已读 |
| `create_time` | datetime | 创建时间 |
| `update_time` | datetime | 更新时间 |

**索引**: `idx_user_read`（user_id + is_read）、`idx_user_read_time`（user_id + is_read + create_time DESC，优化未读通知查询）

---

### article_views（文章浏览记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `article_id` | bigint | 文章ID |
| `user_id` | bigint | 用户ID（游客为 NULL） |
| `ip_address` | varchar(45) | IP地址 |
| `user_agent` | varchar(500) | 用户代理 |
| `referer` | varchar(500) | 来源页面 |
| `view_date` | date | 浏览日期（用于去重统计） |
| `view_time` | datetime | 精确浏览时间 |

**索引**: `idx_ip_article_date`（IP + 文章ID + 日期，用于每日去重计数）

---

### file_info（文件信息表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `original_name` | varchar(255) | 原始文件名 |
| `file_name` | varchar(255) | 存储文件名 |
| `file_path` | varchar(500) | 文件路径 |
| `file_url` | varchar(500) | 访问 URL |
| `file_size` | bigint | 文件大小（字节） |
| `file_type` | varchar(50) | 文件类型 |
| `mime_type` | varchar(100) | MIME 类型 |
| `file_extension` | varchar(20) | 扩展名 |
| `file_category` | varchar(20) | 分类：image/attachment |
| `upload_user_id` | bigint | 上传用户ID |
| `status` | varchar(20) | 状态：active/deleted |
| `create_time` | datetime | 创建时间 |
| `update_time` | datetime | 更新时间 |

---

### upload_files（文件上传记录表）

与 `file_info` 类似，`usage_type` 字段标识用途：1-文章图片，2-头像，3-其他。

---

### system_config（系统配置表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint | 主键 |
| `config_key` | varchar(100) | 配置键（唯一） |
| `config_value` | text | 配置值 |
| `config_type` | varchar(20) | 类型：string/number/boolean/json |
| `description` | varchar(200) | 配置描述 |
| `is_public` | tinyint | 是否公开：0-否，1-是 |

---

### visit_statistics（访问统计表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `date` | date | 统计日期（唯一） |
| `total_visits` | int | 总访问量 |
| `unique_visitors` | int | 独立访客数（UV） |
| `page_views` | int | 页面浏览量（PV） |
| `new_users` | int | 当日新用户数 |
| `new_articles` | int | 当日新文章数 |
| `new_comments` | int | 当日新评论数 |

---

## 索引设计原则

1. **高频查询字段**加索引：`status`、`category_id`、`author_id`、`create_time`
2. **联合唯一约束**防重复数据：点赞、收藏、关注关系表
3. **全文索引**支持搜索：`articles.ft_title_content`
4. **排序优化**：`idx_user_read_time` 支持「按时间倒序查未读通知」

---

## 快速初始化

```bash
# 1. 建库建表
mysql -u root -p < database/schema.sql

# 2. 插入初始数据（默认管理员账号 admin/123456）
mysql -u root -p blog_db < database/data.sql
```
