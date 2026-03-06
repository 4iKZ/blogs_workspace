# 安全与认证设计

[← 返回 Wiki 首页](./Home.md)

---

## 认证架构概览

Lumina 使用 **双层认证架构**，核心是 Spring Security + JWT 无状态认证：

```
请求进入
    │
    ▼
JwtAuthenticationFilter (OncePerRequestFilter)
    │  1. 从 Authorization 头提取 Bearer Token
    │  2. 验证签名、有效期、Token 类型
    │  3. 从 Token 解析 username
    │  4. 加载 UserDetails（CustomUserDetailsServiceImpl）
    │  5. 创建 Authentication，设置到 SecurityContext
    │  6. 设置 request.setAttribute("userId", userId)
    ▼
Spring Security 过滤链
    │  基于 SecurityContext 中的 Authentication 判断权限
    ▼
Controller
    │  AuthUtils.getCurrentUserId() 获取当前用户 ID
```

---

## JWT Token 机制

### Token 类型

| 类型 | 有效期 | 用途 |
|------|--------|------|
| **Access Token** | 7天（604800000ms） | API 请求认证 |
| **Refresh Token** | 更长 | 换取新的 Access Token |

### Token 载荷（Payload）

```json
{
  "userId": 1,
  "username": "admin",
  "role": "admin",
  "tokenType": "ACCESS",   // 或 "REFRESH"
  "iat": 1640995200,       // 签发时间
  "exp": 1641600000        // 过期时间
}
```

### 签名算法

使用 HMAC-SHA256（HS256），密钥通过环境变量 `JWT_SECRET` 注入（默认 32 字节）。

---

## Spring Security 配置

配置文件：`com.blog.config.SecurityConfig`

### 公开端点（无需认证）

```
POST /api/user/register
POST /api/user/login
POST /api/user/refresh-token
POST /api/user/token/refresh
GET  /api/user/top-authors
POST /api/captcha/**
GET  /api/article/list
GET  /api/article/{id}
GET  /api/article/hot
GET  /api/article/recommended
GET  /api/category/**
GET  /api/tag/**
GET  /api/search/**
GET  /api/comment/list
GET  /api/comment/article/*/count
GET  /api/comment/check-sensitive
GET  /api/comment/replace-sensitive
GET  /api/statistics/**
POST /api/user/avatar/upload
```

### 需要认证的端点

```
POST /api/comment
POST /api/comment/*/like
DELETE /api/comment/*/delete
GET  /api/user/info
GET  /api/user/profile
PUT  /api/user/password
GET  /api/notification/**
POST /api/article/publish
PUT  /api/article/edit/**
DELETE /api/article/delete/**
POST /api/user/like/**
POST /api/user/favorite/**
POST /api/user/follow/**
```

### 需要管理员权限的端点

```
/api/admin/**   → hasRole("admin")
```

---

## Token 刷新机制

当 Access Token 过期（收到 401 响应）时，前端自动刷新：

```
前端请求 → 401 响应
    │
    ▼
axios 拦截器
    │  判断是否已在刷新中？
    │
    ├─ 否：发起刷新请求
    │       POST /api/user/token/refresh { refreshToken }
    │       │
    │       ├─ 成功：更新 userStore 中的 token
    │       │         重试原请求
    │       │
    │       └─ 失败：清除用户信息，跳转 /login
    │
    └─ 是：将原请求加入等待队列
            刷新成功后，用新 token 重试所有排队请求
```

---

## 密码安全

- 使用 **BCrypt** 加密存储（Spring Security 内置，工作因子默认 10）
- 登录时 `passwordEncoder.matches(rawPassword, encodedPassword)` 校验
- 不存储明文密码，不可逆加密

---

## 内容安全策略（CSP）

SecurityConfig 中配置了 HTTP 响应头 CSP：

```
Content-Security-Policy:
  default-src 'self';
  script-src 'self';
  style-src 'self';
  img-src 'self' data:;
  connect-src 'self';
  frame-ancestors 'self';
  form-action 'self';
  base-uri 'self';
  object-src 'none'
```

---

## 权限角色体系

| 角色值（数据库 role 字段） | Spring Security 角色 | 权限范围 |
|--------------------------|---------------------|---------|
| `1` | `ROLE_user` | 普通用户，可创作、互动 |
| `2` | `ROLE_admin` | 管理员，可访问 `/api/admin/**` |
| `3` | `ROLE_superadmin` | 超级管理员（预留） |

---

## 敏感词过滤

评论内容在写入数据库前进行敏感词检测：

```java
// SensitiveWordFilter（基于 DFA 算法 / AC 自动机）
SensitiveWordFilter filter = new SensitiveWordFilter();

// 检测
boolean hasSensitive = filter.containsSensitiveWord(content);

// 替换（用 * 遮盖敏感词）
String filtered = filter.replaceSensitiveWord(content, '*');
```

敏感词列表存储在内存中，可通过管理后台动态更新（对应 `SensitiveWord` 实体）。

---

## 图形验证码

注册和登录流程集成图形验证码，防止机器人操作：

```
POST /api/captcha/generate → 返回 captchaKey
GET  /api/captcha/image?key=captchaKey → 返回验证码图片（Base64）
```

验证码存储在 **Redis** 中，有效期 **5分钟**，**使用一次即失效**。

---

## 分布式锁防并发

高频操作（点赞、收藏）使用 Redis 分布式锁防止并发重复操作：

```java
// RedisDistributedLock
String lockKey = "lock:comment:like:{userId}:{commentId}";
String lockValue = redisDistributedLock.tryLock(lockKey);

if (lockValue == null) {
    throw new BusinessException("操作频繁，请稍后重试");
}

try {
    // 执行业务逻辑
} finally {
    redisDistributedLock.unlock(lockKey, lockValue);
}
```

锁实现基于 Redis `SETNX`（SET if Not eXists），确保同一用户对同一目标的操作串行化。

---

## 前端认证状态管理

### Token 存储

Token 存储在 **localStorage** 中，通过 Pinia `userStore` 管理：

```typescript
// 登录后保存
userStore.setUserInfo(userInfo, accessToken, refreshToken)

// 退出时清空
userStore.clearUserInfo()  // 同时清除 localStorage

// 页面刷新时恢复
userStore.loadFromStorage()  // 在 main.ts 初始化时调用
```

### 401 错误处理

```typescript
// axios 响应拦截器
if (res.code === 401) {
    // 不显示错误提示（避免打断用户体验）
    return tryRefreshAndRetry(response.config)
}
```

401 错误不显示 toast 通知，直接静默触发 Token 刷新或跳转登录页。

---

## 安全最佳实践建议

> ⚠️ 以下为生产环境配置建议：

1. **通过环境变量注入敏感配置**，不要硬编码到 `application.yml`：
   ```bash
   export JWT_SECRET=your_random_32_byte_secret
   export SPRING_DATASOURCE_PASSWORD=your_db_password
   ```

2. **强制 HTTPS**，在 Nginx 中配置 SSL 证书并重定向 HTTP → HTTPS。

3. **修改默认管理员密码**（初始密码 `123456`）。

4. **Redis 密码配置**（不使用无密码 Redis）。

5. **定期轮换 JWT Secret**（轮换后所有 Token 失效，用户需重新登录）。

6. **TOS 密钥环境变量化**：`access-key-id` 和 `secret-access-key` 应通过环境变量注入。
