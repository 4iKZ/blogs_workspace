# 系统架构设计

[← 返回 Wiki 首页](./Home.md)

---

## 整体架构

Lumina 采用经典的**前后端分离**架构，前端通过 RESTful API 与后端通信，后端连接 MySQL、Redis 和火山引擎 TOS 三大存储层。

```
┌─────────────────────────────────────────────────────────────┐
│                      用户浏览器                              │
│              Vue 3 SPA (port 3000 / Nginx)                  │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP / REST API
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Nginx 反向代理                            │
│  /          → frontend/dist (静态文件)                      │
│  /api/      → localhost:8080 (后端)                         │
│  /uploads/  → /data/uploads/blog/ (本地上传文件)            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              Spring Boot 后端 (port 8080)                   │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────┐  │
│  │Controller│→ │ Service  │→ │  Mapper  │→ │  Entity   │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────┘  │
│       ↑              ↑                                      │
│  Spring Security  事件驱动                                  │
│  JWT Filter       定时任务                                  │
└──────┬────────────────┬───────────────┬─────────────────────┘
       │                │               │
       ▼                ▼               ▼
┌───────────┐  ┌─────────────┐  ┌─────────────────┐
│  MySQL    │  │   Redis     │  │  火山引擎 TOS   │
│  主存储   │  │  缓存/锁    │  │  文件/图片 CDN │
└───────────┘  └─────────────┘  └─────────────────┘
```

---

## 后端模块结构

后端代码位于 `src/main/java/com/blog/`，按职责分为以下包：

```
com/blog/
├── BlogBackendApplication.java     # 应用入口
│
├── controller/     # REST API 控制层（请求路由与参数校验）
├── service/        # 业务逻辑层
│   └── impl/       # 服务实现类
├── mapper/         # 数据访问层（MyBatis Plus）
├── entity/         # JPA 实体（数据库表映射）
├── dto/            # 数据传输对象（请求/响应 DTO）
│
├── config/         # Spring 配置类
├── security/       # JWT 认证过滤器 & 认证入口点
├── event/          # 领域事件（浏览量/点赞变化、缓存失效等）
├── schedule/       # 定时任务（榜单重置、关注数校正）
├── exception/      # 全局异常处理
├── common/         # 通用类（Result、PageResult、ResultCode）
├── interceptor/    # 请求拦截器
└── utils/          # 工具类（JWT、Redis、AES、图片处理等）
```

### 各层职责

| 层次 | 职责 | 示例 |
|------|------|------|
| **Controller** | 接收 HTTP 请求，参数校验，调用 Service | `ArticleController` |
| **Service** | 核心业务逻辑，事务管理，缓存策略 | `ArticleServiceImpl` |
| **Mapper** | SQL 操作，MyBatis Plus CRUD | `ArticleMapper` |
| **Entity** | 数据库表的 Java 对象映射 | `Article` |
| **DTO** | 前后端数据传输模型（不含敏感字段） | `ArticleCreateDTO` |

---

## 前端模块结构

前端代码位于 `frontend/src/`：

```
frontend/src/
├── main.ts            # 应用入口，注册插件
├── App.vue            # 根组件（主题切换、路由视图）
│
├── views/             # 页面级组件
│   ├── HomeView.vue           # 首页（文章列表）
│   ├── ArticleDetailView.vue  # 文章详情
│   ├── ArticleEditView.vue    # 文章编辑（Markdown编辑器）
│   ├── ProfileView.vue        # 个人主页
│   ├── NotificationView.vue   # 通知中心
│   ├── SearchView.vue         # 搜索结果
│   ├── CategoryView.vue       # 分类浏览
│   ├── LoginView.vue          # 登录
│   ├── RegisterView.vue       # 注册
│   ├── ResetPasswordView.vue  # 密码重置
│   ├── UserProfileView.vue    # 他人主页
│   └── admin/                 # 管理后台页面
│
├── components/        # 可复用组件
│   ├── Layout.vue            # 整体布局
│   ├── Header.vue            # 顶部导航
│   ├── Footer.vue            # 页脚
│   ├── ArticleCard.vue       # 文章卡片
│   ├── LuminaToast.vue       # 全局提示框
│   ├── article/              # 文章相关组件
│   │   ├── LikeButton.vue    # 点赞按钮
│   │   ├── FavoriteButton.vue# 收藏按钮
│   │   ├── PublishDrawer.vue # 发布抽屉
│   │   └── StatisticsCard.vue# 统计卡片
│   └── comment/              # 评论相关组件
│       ├── CommentSection.vue# 评论区
│       ├── CommentItem.vue   # 单条评论
│       └── CommentForm.vue   # 评论输入框
│
├── services/          # API 请求服务层
├── store/             # Pinia 状态管理
├── router/            # Vue Router 路由配置
├── types/             # TypeScript 类型定义
├── composables/       # 组合式函数（usePageTitle, useLuminaToast）
├── utils/             # 工具函数（axios、图片压缩、上传）
└── workers/           # Web Worker（图片压缩任务）
```

---

## 请求处理流程

### 完整请求链路

```
前端 Vue 组件
    │  调用 services/ 中的 API 函数
    ▼
axios 实例 (utils/axios.ts)
    │  自动附加 JWT Token
    │  10秒错误提示冷却
    │  401 → 自动刷新 Token
    ▼
Nginx 反向代理
    │  /api/* → 8080
    ▼
Spring Security 过滤链
    │  JwtAuthenticationFilter 验证 Token
    │  设置 SecurityContext + request 属性 (userId, username)
    ▼
Controller
    │  @Valid 参数校验
    │  调用 AuthUtils.getCurrentUserId()
    ▼
Service
    │  业务逻辑 + 事务
    │  Redis 缓存读写
    │  事件发布
    ▼
Mapper (MyBatis Plus)
    │  SQL 执行
    ▼
MySQL
```

---

## 数据流向

### 文章浏览量计数流程

```
用户访问文章
    │
    ▼
ArticleController.getArticleDetail()
    │
    ├─ 异步发布 ArticleViewCountChangeEvent
    │       │
    │       └─ ArticleEventListener 监听
    │               └─ 清除热门文章 Spring Cache
    │
    └─ ArticleRankService.incrementViewCount()
            └─ Redis ZSet: article:rank:day / article:rank:week
                    (score += 1.0)
```

### 文章点赞并发控制流程

```
用户点赞
    │
    ▼
CommentServiceImpl / ArticleServiceImpl
    │
    ├─ RedisDistributedLock.tryLock(lockKey)  // 获取分布式锁
    │
    ├─ 检查 Redis 是否已点赞 (user:comment:like:{userId}:{commentId})
    │
    ├─ 数据库写入 (user_likes / comment_likes 表)
    │
    └─ TransactionSynchronizationManager.registerSynchronization()
            └─ afterCommit() → 更新 Redis 缓存
    
    └─ RedisDistributedLock.unlock(lockKey, lockValue)
```

---

## 缓存架构

| 缓存类型 | 用途 | TTL |
|---------|------|-----|
| Redis String | JWT 黑名单、验证码、限流 | 按需 |
| Redis ZSet | 文章日榜/周榜排行 | 永久（定时重置） |
| Redis Hash | 用户点赞/收藏状态 | 7天 |
| Spring Cache (`@Cacheable`) | 热门文章列表查询结果 | 事件驱动失效 |

---

## 异步与事件驱动

| 事件 | 触发时机 | 处理逻辑 |
|------|---------|---------|
| `ArticleViewCountChangeEvent` | 文章被浏览 | 清除热门文章 Cache |
| `ArticleLikeCountChangeEvent` | 文章被点赞/取消 | 清除热门文章 Cache |
| `CacheInvalidationEvent` | 缓存双删策略 | 延迟删除 Redis Key |
| `NotificationEvent` | 评论/点赞触发 | 异步写入通知表 |

---

## 定时任务

| 任务类 | Cron | 功能 |
|--------|------|------|
| `RankResetSchedule` | `0 0 0 * * ?` (每天零点) | 重置文章日榜 |
| `RankResetSchedule` | `0 0 0 ? * MON` (每周一零点) | 重置文章周榜 |
| `FollowCountCorrectionSchedule` | 定期 | 校正用户关注/粉丝计数 |
| `CacheConsistencyVerifier` | 每5分钟 | 验证缓存一致性（采样100条） |

---

## 技术决策说明

### 为什么选择 MyBatis Plus 而非 JPA?
MyBatis Plus 提供更灵活的 SQL 控制，同时具备逻辑删除、自动填充等便利特性，适合对 SQL 性能有要求的博客场景。

### 为什么使用 Redis 分布式锁而非数据库乐观锁?
点赞、计数等高频操作并发量大，Redis 分布式锁性能更好，同时避免数据库版本号字段的频繁更新。

### 为什么选择 JWT 无状态认证?
无状态 JWT 天然支持水平扩展，无需共享 Session 存储，适合将来分布式部署。Token 7天有效期 + Refresh Token 机制保障安全性。
