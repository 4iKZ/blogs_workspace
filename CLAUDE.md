# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

博客网站后端（Spring Boot）+ 前端（Vue 3）全栈项目，支持文章发布、评论互动、用户关注、通知系统、GitHub OAuth 登录、AI 内容审核、访问统计等完整功能。

## 常用命令

### 后端（项目根目录）

```bash
# 运行Spring Boot应用
mvn spring-boot:run

# 编译打包
mvn clean package -DskipTests

# 运行单个测试
mvn test -Dtest=ClassName#methodName

# 仅编译
mvn compile
```

### 前端（frontend 目录）

```bash
cd frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 生产构建
npm run build

# 预览构建结果
npm run preview
```

### 数据库

```bash
# 全新数据库：先 schema（建表），后 data（示例数据）
database/schema.sql
database/data.sql

# 既有数据库升级：在维护窗口内按顺序执行一次加法迁移
database/migrations/20260726_p2_file_dedup.sql
database/migrations/20260727_p1_auth_token_version.sql
database/migrations/20260727_p1_article_moderation_submissions.sql
```

## 架构概览

### 后端：Controller - Service - Mapper 三层架构

- **Controller**：处理请求/响应，参数校验，调用 Service
- **Service**：业务逻辑处理，事务管理
- **Mapper**：数据访问层，使用 MyBatis Plus

关键配置类：

- `config/SecurityConfig.java` — Spring Security 过滤器链、精确 CORS 与 CSP 配置
- `config/MyBatisPlusConfig.java` — MyBatis Plus 全局配置（含逻辑删除）
- `config/SwaggerConfig.java` — OpenAPI 3（SpringDoc）文档配置
- `interceptor/JwtInterceptor.java` — JWT 拦截器

认证机制：Spring Security + JWT。Access Token 有效期为 900 秒，只保存在 Pinia 内存；
Refresh Token 有效期为 604800 秒，仅通过 `HttpOnly; Secure; SameSite=Strict; Path=/api/user`
Cookie 下发和轮换。JWT 含 `jti`、`tokenVersion` 与令牌族信息；旧客户端必须重新登录。

### 前端：Vue 3 + Vue Router + Pinia

路由配置：`frontend/src/router/index.ts`

- 路由守卫：检查 `requiresAuth` / `requiresAdmin` 元数据
- 路由懒加载：所有页面组件使用动态导入

状态管理（Pinia）：

- `store/user.ts` — 用户登录状态、角色信息
- `store/article.ts` — 文章相关状态
- `store/notification.ts` — 通知状态
- `store/siteConfig.ts` — 站点配置

### 缓存策略：Redis + Caffeine 二级缓存

- Redis：分布式缓存（文章浏览量、热点数据）
- Caffeine：本地缓存（减少 Redis 交互）
- 文章浏览量：应用关闭时强制同步到数据库

### 事件驱动

Spring Event 异步处理：

- `ArticleViewCountChangeEvent` — 文章浏览量变化
- `ArticleLikeCountChangeEvent` — 文章点赞变化
- `NotificationEvent` — 通知创建

### 文件存储：火山云 TOS 对象存储

- 头像、封面图、Markdown 图片等均上传至 TOS
- 通过 `tos.base-folder` 配置区分不同用途路径
- 上传前按当前用户流式计算 SHA-256 查重
- 数据库写入失败时立即删除新对象；删除失败进入 `file_cleanup_tasks` 定时重试

### AI 内容审核

集成 DeepSeek API（`spring.ai.openai` 配置项），对文章/评论内容进行审核。文章审核使用持久化快照和
失败关闭状态机：AI 失败按 1/5/15 分钟重试，耗尽后进入人工审核；已发布文章编辑在审核通过前继续展示旧版本。

## 关键数据库表

| 表名                         | 用途                                                           |
| ---------------------------- | -------------------------------------------------------------- |
| `users`                      | 用户（status: 1正常 2禁用 3删除；role: 1普通 2管理员 3超管）   |
| `articles`                   | 文章（status: 1草稿 2已发布 3已删除），含全文索引              |
| `categories`                 | 分类（支持层级 parent_id）                                     |
| `comments`                   | 评论（status: 1待审核 2通过 3拒绝 4删除）                      |
| `user_likes`                 | 点赞（target_type: 1文章 2评论），触发器保证只能点赞已发布内容 |
| `user_favorites`             | 收藏                                                           |
| `user_follows`               | 关注关系（逻辑删除）                                           |
| `notifications`              | 通知（type: 1文章点赞 2文章评论 3评论点赞 4评论回复）          |
| `article_views`              | 文章浏览记录                                                   |
| `website_access_log`         | 访问日志（异步批量写入）                                       |
| `visit_statistics`           | 每日访问统计                                                   |
| `system_config`              | 系统配置（KV 存储）                                            |
| `sensitive_words`            | 敏感词库                                                       |
| `file_info` / `upload_files` | 文件上传记录；`file_info.content_hash` 用于用户级 SHA-256 查重 |
| `file_cleanup_tasks`         | TOS 对象删除补偿任务，最多重试 5 次                            |
| `article_moderation_submissions` | 文章审核快照、重试状态和人工审核审计；每篇文章仅允许一个活动任务 |

## 前端项目特殊说明

- Markdown 编辑器：`md-editor-v3`
- 图片压缩：前端使用 Web Worker 压缩后再上传至 TOS
- 主题支持：CSS 变量驱动，`frontend/public/css/theme/light.css` 和 `dark.css`
- API 请求：`frontend/src/utils/axios.ts` 配置请求拦截器，自动附加内存中的 JWT，并开启 Cookie 凭据
- 首次导航通过 `store/user.ts::initializeSession()` 刷新服务端角色；并发 401 共享一次 Token 刷新
- Blob 下载保留原始 Axios 响应，用服务端 `Content-Disposition` 文件名保存

## 技术栈版本

- Java：21
- Spring Boot：3.5.6
- MyBatis Plus：3.5.5
- Vue：3.4+
- Element Plus：2.7+
- Vite：5.2+

## Health Stack

- frontend: `cd frontend && npm ci && npm run check`
- backend focused: `mvn -Dtest="SecurityConfigTest,UserServiceImplSecurityTest,ArticleServiceImplUnitTest,FileUploadServiceImplSecurityTest,FileUploadDeduplicationTest,*FileCleanup*Test,ArticleControllerPrivacyTest" test`
- backend package: `mvn -DskipTests package`
- backend full suite: `mvn test`（存在历史失败，不得在未修复前声明全量通过；见 `docs/全栈代码审计缺陷报告_20260725.md`）

## Skill routing

When the user's request matches an available skill, invoke it via the Skill tool. When in doubt, invoke the skill.

Key routing rules:
- Product ideas/brainstorming → invoke /office-hours
- Strategy/scope → invoke /plan-ceo-review
- Architecture → invoke /plan-eng-review
- Design system/plan review → invoke /design-consultation or /plan-design-review
- Full review pipeline → invoke /autoplan
- Bugs/errors → invoke /investigate
- QA/testing site behavior → invoke /qa or /qa-only
- Code review/diff check → invoke /review
- Visual polish → invoke /design-review
- Ship/deploy/PR → invoke /ship or /land-and-deploy
- Save progress → invoke /context-save
- Resume context → invoke /context-restore
- Author a backlog-ready spec/issue → invoke /spec
