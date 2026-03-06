# 后端实现详解

[← 返回 Wiki 首页](./Home.md)

---

## 启动入口

```java
// src/main/java/com/blog/BlogBackendApplication.java
@SpringBootApplication
@EnableScheduling          // 启用定时任务
@EnableAsync               // 启用异步方法
@MapperScan("com.blog.mapper")
public class BlogBackendApplication { ... }
```

---

## 控制器（Controller）

所有控制器位于 `com.blog.controller`，路径前缀均为 `/api/`。

| 控制器 | 路径前缀 | 主要功能 |
|--------|---------|---------|
| `ArticleController` | `/api/article` | 文章 CRUD、列表、详情 |
| `ArticleSearchController` | `/api/article/search` | 文章搜索 |
| `ArticleStatisticsController` | `/api/statistics` | 文章统计数据 |
| `UserController` | `/api/user` | 用户注册/登录/信息/密码 |
| `UserLikeController` | `/api/user/like` | 文章/评论点赞 |
| `UserFavoriteController` | `/api/user/favorite` | 文章收藏 |
| `CommentController` | `/api/comment` | 评论 CRUD、点赞 |
| `CategoryController` | `/api/category` | 分类管理 |
| `NotificationController` | `/api/notification` | 通知查询与已读 |
| `AdminController` | `/api/admin` | 管理后台（需 admin 角色） |
| `FileUploadController` | `/api/upload` | 文件/图片上传 |
| `ImageController` | `/api/image` | 图片处理 |
| `SearchController` | `/api/search` | 全站搜索 |
| `CaptchaController` | `/api/captcha` | 验证码生成与校验 |
| `DataBackupController` | `/api/admin/backup` | 数据备份 |
| `SystemConfigController` | `/api/admin/config` | 系统动态配置 |
| `WebsiteStatisticsController` | `/api/admin/statistics` | 网站统计 |
| `WebsiteVisitController` | `/api/visit` | 访问记录上报 |

### 典型控制器示例

```java
// ArticleController.java
@RestController
@RequestMapping("/api/article")
@Tag(name = "文章管理接口")
public class ArticleController {

    @PostMapping("/publish")
    @Operation(summary = "发布文章")
    public Result<Long> publishArticle(@Valid @RequestBody ArticleCreateDTO dto) {
        Long currentUserId = AuthUtils.getCurrentUserId();
        return articleService.publishArticle(dto, currentUserId);
    }

    @GetMapping("/list")
    @Operation(summary = "获取文章列表")
    public Result<PageResult<ArticleDTO>> getArticleList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "latest") String sortBy) {
        return articleService.getArticleList(page, size, keyword, categoryId, null, null, null, sortBy);
    }
}
```

---

## 服务层（Service）

### 主要服务接口与实现

| 服务接口 | 实现类 | 核心职责 |
|---------|--------|---------|
| `ArticleService` | `ArticleServiceImpl` | 文章增删改查、发布流程 |
| `UserService` | `UserServiceImpl` | 用户注册、登录、JWT颁发 |
| `CommentService` | `CommentServiceImpl` | 评论树构建、点赞分布式锁 |
| `CategoryService` | `CategoryServiceImpl` | 分类 CRUD，文章数统计 |
| `NotificationService` | `NotificationServiceImpl` | 通知创建、已读管理 |
| `ArticleRankService` | `ArticleRankServiceImpl` | Redis ZSet 榜单操作 |
| `SearchService` | `SearchServiceImpl` | MySQL 全文搜索 |
| `FileUploadService` | `FileUploadServiceImpl` | 本地 / TOS 文件上传 |
| `TOSService` | `TOSServiceImpl` | 火山引擎 TOS SDK 封装 |
| `DataBackupService` | `DataBackupServiceImpl` | 数据库备份 |
| `SystemConfigService` | `SystemConfigServiceImpl` | 动态配置读写 |
| `ChunkedUploadService` | `ChunkedUploadServiceImpl` | 大文件分片上传 |
| `WebsiteStatisticsService` | `WebsiteStatisticsServiceImpl` | PV/UV 统计 |
| `CaptchaService` | `CaptchaServiceImpl` | 验证码生成与校验 |

### 通用响应封装

所有 Service 方法返回 `Result<T>` 对象：

```java
// com/blog/common/Result.java
@Data
public class Result<T> {
    private Integer code;      // 状态码：200 成功，其他失败
    private String message;    // 提示信息
    private T data;            // 响应数据
    private Long timestamp;    // 时间戳

    public static <T> Result<T> success(T data) { ... }
    public static <T> Result<T> success(String message, T data) { ... }
    public static <T> Result<T> error(String message) { ... }
    public static <T> Result<T> error(Integer code, String message) { ... }
}
```

### 分页结果封装

```java
// com/blog/common/PageResult.java
@Data
public class PageResult<T> {
    private List<T> records;   // 数据列表
    private Long total;        // 总条数
    private Integer page;      // 当前页
    private Integer size;      // 每页大小
    private Integer pages;     // 总页数
}
```

---

## 实体类（Entity）

所有实体位于 `com.blog.entity`，使用 MyBatis Plus 注解。

| 实体类 | 数据库表 | 说明 |
|--------|---------|------|
| `User` | `users` | 用户信息 |
| `Article` | `articles` | 文章 |
| `Category` | `categories` | 分类 |
| `Comment` | `comments` | 评论（支持逻辑删除） |
| `CommentLike` | `comment_likes` | 评论点赞 |
| `Notification` | `notifications` | 消息通知 |
| `UserFavorite` | `user_favorites` | 用户收藏 |
| `UserFollow` | `user_follows` | 用户关注（支持逻辑删除） |
| `UserLike` | `user_likes` | 用户点赞 |
| `ArticleView` | `article_views` | 文章浏览记录 |
| `FileInfo` | `file_info` | 文件信息 |
| `UploadFile` | `upload_files` | 上传文件记录 |
| `SystemConfig` | `system_config` | 系统配置 |
| `SensitiveWord` | - | 敏感词（内存过滤） |

### BaseEntity 公共字段

```java
// com/blog/entity/BaseEntity.java
@Data
public abstract class BaseEntity {
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

---

## DTO（数据传输对象）

DTO 位于 `com.blog.dto`，用于接收请求和返回响应，避免直接暴露 Entity。

### 主要请求 DTO

| DTO | 用途 |
|-----|------|
| `ArticleCreateDTO` | 创建/编辑文章 |
| `UserRegisterDTO` | 用户注册（含验证码） |
| `UserLoginDTO` | 用户登录 |
| `UserUpdateDTO` | 更新用户信息 |
| `ChangePasswordDTO` | 修改密码 |
| `CommentCreateDTO` | 发表评论 |
| `CategoryCreateDTO` | 创建分类 |
| `SearchRequestDTO` | 搜索请求 |
| `SystemConfigDTO` | 系统配置更新 |

### 主要响应 DTO

| DTO | 用途 |
|-----|------|
| `ArticleDTO` | 文章详情（含作者信息） |
| `ArticleSummaryDTO` | 文章摘要（列表用） |
| `UserDTO` | 用户信息（含 Token） |
| `CommentDTO` | 评论（含子评论树） |
| `NotificationDTO` | 通知消息 |
| `TokenRefreshResponseDTO` | 刷新 Token 响应 |
| `WebsiteStatisticsDTO` | 网站统计数据 |
| `SearchResultDTO` | 搜索结果 |

---

## 配置类（Config）

| 配置类 | 说明 |
|--------|------|
| `SecurityConfig` | Spring Security 过滤链、公开/私有端点配置 |
| `RedisConfig` | Lettuce 连接池、Redis 序列化配置 |
| `CacheConfig` | Spring Cache 缓存管理器配置 |
| `CacheConsistencyConfig` | 缓存双删策略参数 |
| `MyBatisPlusConfig` | 分页插件、逻辑删除 |
| `SwaggerConfig` | SpringDoc OpenAPI 标题/描述 |
| `CorsConfig` | 跨域配置（开发环境允许所有来源） |
| `AsyncConfig` | 异步线程池配置 |
| `AuditConfig` | MyBatis Plus 审计（自动填充时间字段） |
| `TOSConfig` | 火山引擎 TOS 客户端初始化 |
| `ArticleRankInitializer` | 启动时从 MySQL 加载排行榜到 Redis |
| `WebConfig` | 静态资源、拦截器注册 |

---

## 工具类（Utils）

| 工具类 | 功能 |
|--------|------|
| `JWTUtils` | JWT 生成、解析、校验（Access Token + Refresh Token） |
| `RedisUtils` / `RedisCacheUtils` | Redis 通用操作封装 |
| `RedisDistributedLock` | 基于 Redis SETNX 的分布式锁 |
| `AuthUtils` | 从 SecurityContext 获取当前用户 ID/名 |
| `SensitiveWordFilter` | AC 自动机敏感词过滤（评论内容检测） |
| `ImageProcessor` | 图片压缩、格式转换（基于 Java AWT） |
| `AESUtils` | AES 加解密工具 |
| `HotArticleCacheEvictionService` | 清除 Spring Cache 热门文章结果缓存 |
| `IpUtils` | 获取客户端真实 IP |

### JWTUtils 核心方法

```java
// 生成 Access Token（有效期 7 天）
String generateAccessToken(Long userId, String username, String role)

// 生成 Refresh Token（有效期更长）
String generateRefreshToken(Long userId, String username)

// 从 Token 中获取用户 ID
Long getUserIdFromToken(String token)

// 从 Token 中获取用户名
String getUsernameFromToken(String token)

// 验证 Token 有效性
boolean validateToken(String token)

// 判断是否为 Access Token
boolean isAccessToken(String token)
```

---

## 异常处理

### 全局异常处理器

```java
// com/blog/exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常 → 返回具体错误信息
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) { ... }

    // 参数校验异常 → 返回字段错误信息
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(...) { ... }

    // 未认证异常 → 返回 401
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) { ... }

    // 通用异常 → 返回 500
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) { ... }
}
```

### ResultCode 枚举

```java
// com/blog/common/ResultCode.java
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证，请先登录"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源已存在"),
    SERVER_ERROR(500, "服务器内部错误"),
    // ... 更多业务码
}
```

---

## 敏感词过滤

评论发布时自动过滤敏感词，基于 AC 自动机（DFA 算法）实现高性能匹配：

```java
// 使用示例
SensitiveWordFilter filter = new SensitiveWordFilter();
boolean hasSensitive = filter.containsSensitiveWord(content);
String filtered = filter.replaceSensitiveWord(content, '*');
```

暴露的公开 API：
- `GET /api/comment/check-sensitive?content=...` - 检测是否含敏感词  
- `GET /api/comment/replace-sensitive?content=...` - 返回脱敏后内容

---

## 文件上传

### 上传流程

```
前端选择文件
    │
    ├─ 图片自动压缩（Web Worker + Canvas API）
    │
    ▼
POST /api/upload/image (或 /api/article/upload-cover)
    │
    ├─ 文件类型校验（jpg/jpeg/png/gif/bmp/webp）
    ├─ 文件大小校验（≤ 10MB）
    │
    ├─ 存储策略选择：
    │   ├─ 火山引擎 TOS（生产环境，CDN 加速）
    │   └─ 本地存储（/data/uploads/blog/）
    │
    └─ 写入 file_info / upload_files 表
       返回文件访问 URL
```

### TOS 配置

```yaml
tos:
  access-key-id: ...
  secret-access-key: ...
  endpoint: https://tos-cn-beijing.volces.com
  region: cn-beijing
  bucket-name: syhaox
  base-folder: old_book_system/
  acl: public-read
  default-image-style: lumina  # TOS 控制台创建的图片处理样式
```

---

## MyBatis Plus 配置

```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true   # 下划线转驼峰
    cache-enabled: false                  # 关闭 MyBatis 二级缓存（使用 Redis）
  global-config:
    db-config:
      id-type: ASSIGN_ID                  # 雪花算法 ID
      logic-delete-field: deleted         # 逻辑删除字段
      logic-delete-value: 1               # 1 = 已删除
      logic-not-delete-value: 0           # 0 = 未删除
  mapper-locations: classpath*:mapper/*.xml
```

涉及逻辑删除的表：`comments`、`user_follows`
