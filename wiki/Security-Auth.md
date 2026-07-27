# 安全与认证设计

[← 返回 Wiki 首页](./Home.md)

---

## 当前安全契约

本页描述当前生产契约；接口细节以 [API 文档](../docs/API接口文档.md) 为准。认证与分块上传均已完成破坏性升级，旧客户端必须在维护窗口内升级，不提供不安全的兼容路径。

Lumina 使用 Spring Security 与 JWT 认证。Access Token 用于 API 的 `Authorization: Bearer <token>` 请求头；Refresh Token 仅用于换取新的 Access Token，浏览器脚本不能读取它。

## Token 生命周期和载荷

| 类型 | 有效期 | 保存位置 | 用途 |
|------|--------|----------|------|
| Access Token | 900 秒 | Pinia 内存 | 已认证 API 请求 |
| Refresh Token | 604800 秒 | `HttpOnly` Cookie | 换取新的 Access Token |

两个 JWT 均使用 HS256，分别由 `JWT_SECRET` 与 `JWT_REFRESH_SECRET` 签名。密钥必须通过部署环境注入，轮换任一密钥会使相应旧令牌失效。

Access Token 的关键载荷为 `jti`、`userId`、`username`、`tokenVersion`、`tokenType=ACCESS`、`iat` 和 `exp`。Refresh Token 还包含 `familyId` 与递增的 `generation`，且 `tokenType=REFRESH`。服务端验证签名、类型、过期时间、`jti` 和当前用户的 `token_version`；已登出 Access Token 的 `jti` 会进入黑名单。

`token_version` 保存在 `users` 表。密码重置会在更新密码的同一事务中递增它，因此该用户此前签发的 Access 和 Refresh Token 都会失效。

## 登录、刷新和登出

登录与 GitHub 登录响应只返回 Access Token 和用户资料，不返回刷新令牌。服务端通过 `Set-Cookie` 下发刷新令牌，属性固定为：

```
HttpOnly; Secure; SameSite=Strict; Path=/api/user
```

开发或测试环境仅可通过 `REFRESH_COOKIE_SECURE=false` 关闭 `Secure`；生产环境必须保持启用。

刷新端点为 `POST /api/user/token/refresh`。它没有请求体，服务端从 Cookie 读取刷新令牌，成功后：

1. 验证刷新令牌的签名、`jti`、`tokenVersion`、family 和用户状态。
2. 原子地消费当前 family generation，并写入下一 generation。
3. 通过 `Set-Cookie` 轮换 Refresh Token，JSON 只返回 `{ token }`。

已消费令牌再次出现属于重放：服务端撤销整个 Refresh Token family，客户端必须重新登录。登出会读取 Authorization 与 Cookie，黑名单当前 Access Token、撤销 Refresh Token family，并清除 Cookie。

## 前端会话

Pinia 仅在内存保存 Access Token。刷新页面时，应用调用无请求体的刷新端点恢复会话；失败则进入匿名状态。Axios 启用 `withCredentials`，以便仅对受限 Cookie 路径携带刷新 Cookie。

浏览器持久化存储不得保存 Access Token 或 Refresh Token。启动迁移会清理旧版本留下的令牌；安全版本发布和密钥轮换后，旧会话需要重新登录。用户资料可以作为非敏感缓存保存，但不得被视为已认证状态。

## Spring Security 边界

`JwtAuthenticationFilter` 在控制器前解析 Bearer Access Token，核验当前 `token_version` 和 Access Token 黑名单后才建立 `SecurityContext` 并写入 `userId` 请求属性。

公开端点仅包括注册/登录、Cookie 刷新与登出、密码重置、验证码、GitHub OAuth 回调、公开文章读取、公开分类/标签/搜索/统计和公开评论读取。文章发布、编辑、删除，上传会话，用户资料修改、互动与通知均要求认证；`/api/admin/**` 需要管理员角色。早期文档中的公开头像直传和旧文章上传示例已经废止，不应被集成或放行。

## 密码重置与验证码防护

密码使用 BCrypt 单向散列。密码重置发送接口要求 `email`、`captchaKey` 和 `captcha`；无论邮箱是否存在，成功响应保持一致，以减少枚举风险。

邮件验证码由 `SecureRandom` 生成，Redis 仅保存邮箱与验证码的 HMAC-SHA256 摘要，不保存明文。发送受以下限制：每邮箱 60 秒一次、每邮箱每小时 5 次、每 IP 每小时 20 次。验证码有效期为 10 分钟。

验证与消费使用 Redis Lua 原子执行：成功时一次性删除验证码和尝试计数；最多失败 5 次，随后锁定 15 分钟。密码更新成功后，事务提交后撤销该用户的 Refresh Token 会话。

## 内容安全策略（CSP）

应用返回以下 CSP 语义：

```
default-src 'self';
script-src 'self';
object-src 'none';
base-uri 'self';
frame-ancestors 'none';
style-src 'self' 'unsafe-inline' https://fonts.googleapis.com;
font-src 'self' https://fonts.gstatic.com;
img-src 'self' data: https://syhaox.tos-cn-beijing.volces.com;
connect-src 'self' https://syhaox.tos-cn-beijing.volces.com;
worker-src 'self' blob:
```

`script-src` 不允许内联脚本，`object-src` 禁用插件对象，`frame-ancestors` 禁止嵌入。`style-src` 的内联样式例外仅为现有 UI 框架所需；Google Fonts 是唯一外部字体来源。`data:` 仅允许用于图片，TOS 域名仅允许用于图片和连接；`worker-src blob:` 保留给前端图片压缩 Worker。文章 Markdown 仍经 DOMPurify 清洗，不因这些资源例外而允许事件属性、危险 URL 或内联脚本。

## 迁移和运维

上线安全版本前应在维护窗口内执行认证与审核迁移、轮换 JWT 密钥并清理刷新会话，再同时发布前后端。不得让旧版认证或上传客户端与新后端混跑。回滚应用代码时保留 `users.token_version` 和安全迁移数据，避免恢复已失效的会话。

生产环境还应：

1. 强制 HTTPS，并保持 Refresh Cookie 的 `Secure` 属性。
2. 配置精确 CORS 允许源；携带凭据时禁止通配源。
3. 将数据库、Redis、TOS、JWT 和密码重置 HMAC 密钥全部置于环境变量或受管密钥系统。
4. 修改初始管理员密码，并审计管理员审核与认证异常日志。
