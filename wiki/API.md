# API 接口参考

[← 返回 Wiki 首页](./Home.md)

---

## 接口规范

### 基本信息

| 项目 | 值 |
|------|-----|
| **Base URL** | `http://localhost:8080/api` |
| **认证方式** | JWT Bearer Token |
| **数据格式** | JSON (`Content-Type: application/json`) |
| **字符编码** | UTF-8 |
| **时区** | Asia/Shanghai (GMT+8) |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` |

### 通用响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1640995200000
}
```

### 状态码说明

| code | 说明 |
|------|------|
| `200` | 成功 |
| `400` | 请求参数错误 |
| `401` | 未认证（需要登录） |
| `403` | 权限不足 |
| `404` | 资源不存在 |
| `409` | 资源冲突（如用户名已存在） |
| `500` | 服务器内部错误 |

### 认证头

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

🔒 标志表示该接口需要 JWT 认证；🔑 表示需要管理员权限。

---

## 1. 用户管理 `/api/user`

### POST `/api/user/register` — 用户注册

**请求体：**
```json
{
  "username": "string",       // 用户名，3-20字符，必填
  "email": "string",          // 邮箱，必填
  "password": "string",       // 密码，6-20字符，必填
  "confirmPassword": "string",// 确认密码，必填
  "nickname": "string",       // 昵称，可选
  "captcha": "string",        // 验证码，必填
  "captchaKey": "string"      // 验证码 Key，必填
}
```

**响应：** 同登录响应，包含 `accessToken` 和 `refreshToken`。

---

### POST `/api/user/login` — 用户登录

**请求体：**
```json
{
  "username": "string",   // 用户名或邮箱
  "password": "string"    // 密码
}
```

**响应数据：**
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "nickname": "管理员",
  "avatar": null,
  "bio": null,
  "website": null,
  "status": 1,
  "role": "admin",
  "createTime": "2025-01-01T00:00:00",
  "lastLoginTime": "2025-01-01T00:00:00",
  "lastLoginIp": "127.0.0.1",
  "articleCount": 0,
  "commentCount": 0,
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci..."
}
```

---

### POST `/api/user/logout` 🔒 — 用户登出

请求头：`X-Refresh-Token: <refreshToken>`（可选，用于使 Refresh Token 失效）

---

### POST `/api/user/token/refresh` — 刷新 Token

**请求体：**
```json
{ "refreshToken": "eyJhbGci..." }
```

**响应：**
```json
{ "token": "eyJhbGci...", "refreshToken": "eyJhbGci..." }
```

---

### GET `/api/user/info` 🔒 — 获取当前用户信息

### PUT `/api/user/info` 🔒 — 更新用户信息

**请求体（均可选）：**
```json
{
  "nickname": "string",
  "email": "string",
  "avatar": "string",
  "bio": "string",
  "website": "string",
  "position": "string",
  "company": "string"
}
```

### PUT `/api/user/password` 🔒 — 修改密码

**查询参数：** `oldPassword`、`newPassword`

### POST `/api/user/avatar/upload` — 上传头像

**请求体：** `multipart/form-data`，字段名 `file`

---

### POST `/api/user/password/reset/send` — 发送密码重置验证码

**请求体：**
```json
{ "email": "user@example.com" }
```

### POST `/api/user/password/reset` — 通过验证码重置密码

**请求体：**
```json
{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "newpass123"
}
```

---

### GET `/api/user/top-authors` — 获取热门作者

**查询参数：** `limit`（默认 10）

### GET `/api/user/:id` — 获取指定用户信息

---

## 2. 验证码 `/api/captcha`

### POST `/api/captcha/generate` — 生成验证码

**响应：**
```json
{
  "code": 200,
  "data": "captcha_key_123456"  // captchaKey，用于注册时提交
}
```

返回的 Base64 图片通过单独接口获取，或直接在前端显示。

---

## 3. 文章管理 `/api/article`

### POST `/api/article/publish` 🔒 — 发布文章

**请求体：**
```json
{
  "title": "string",         // 标题，必填
  "content": "string",       // Markdown 内容，必填
  "summary": "string",       // 摘要，可选
  "coverImage": "string",    // 封面图片 URL，可选
  "categoryId": 1,           // 分类ID，必填
  "status": 2,               // 1-草稿，2-发布
  "isTop": 0,                // 是否置顶
  "isRecommend": 0           // 是否推荐
}
```

**响应：** `data` 为新文章 ID（Long）

### PUT `/api/article/:id` 🔒 — 编辑文章

请求体同上。

### DELETE `/api/article/:id` 🔒 — 删除文章

### GET `/api/article/list` — 获取文章列表

**查询参数：**

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `page` | int | 1 | 页码 |
| `size` | int | 10 | 每页数量 |
| `keyword` | string | - | 搜索关键词 |
| `categoryId` | long | - | 分类ID过滤 |
| `tagId` | long | - | 标签ID过滤 |
| `status` | int | - | 状态过滤 |
| `authorId` | long | - | 作者ID过滤 |
| `sortBy` | string | `latest` | `popular`-热度，`latest`-最新 |

**响应：** `PageResult<ArticleDTO>`

### GET `/api/article/:id` — 获取文章详情

**响应 ArticleDTO：**
```json
{
  "id": 1,
  "title": "文章标题",
  "content": "# Markdown 内容...",
  "summary": "摘要",
  "coverImage": "https://...",
  "categoryId": 1,
  "categoryName": "技术",
  "authorId": 1,
  "authorName": "4iKZ",
  "authorAvatar": "https://...",
  "status": 2,
  "viewCount": 1000,
  "likeCount": 50,
  "commentCount": 20,
  "favoriteCount": 30,
  "isTop": 0,
  "isRecommend": 1,
  "publishTime": "2025-01-01T00:00:00",
  "createTime": "2025-01-01T00:00:00"
}
```

### GET `/api/article/hot` — 获取热门文章

### GET `/api/article/recommended` — 获取推荐文章

---

## 4. 评论管理 `/api/comment`

### GET `/api/comment/list` — 获取评论列表

**查询参数：** `articleId`（必填）、`page`、`size`、`sortBy`（`latest`/`hot`）

**响应：** 返回评论树结构（顶级评论 + 子评论嵌套）

### POST `/api/comment` 🔒 — 发表评论

**请求体：**
```json
{
  "articleId": 1,
  "content": "评论内容",
  "parentId": 0,               // 0 表示顶级评论
  "replyToCommentId": null     // 回复的目标评论ID（楼中楼）
}
```

### DELETE `/api/comment/:id/delete` 🔒 — 删除评论

### POST `/api/comment/:id/like` 🔒 — 点赞评论

**响应：**
```json
{
  "liked": true,        // true=点赞成功，false=取消点赞
  "likeCount": 10       // 当前点赞数
}
```

### GET `/api/comment/:id/like-status` — 获取评论点赞状态（需传 token）

### GET `/api/comment/check-sensitive` — 检测敏感词

**查询参数：** `content=文本内容`

### GET `/api/comment/replace-sensitive` — 替换敏感词（返回脱敏文本）

---

## 5. 分类管理 `/api/category`

### GET `/api/category/list` — 获取分类列表

**查询参数：** `includeCount=true`（是否包含文章数统计）

### GET `/api/category/:id` — 获取分类详情

### POST `/api/category` 🔑 — 创建分类

### PUT `/api/category/:id` 🔑 — 编辑分类

### DELETE `/api/category/:id` 🔑 — 删除分类

---

## 6. 用户互动

### POST `/api/user/like/:targetId` 🔒 — 点赞文章

**查询参数：** `targetType=1`（文章）或 `2`（评论）

### DELETE `/api/user/like/:targetId` 🔒 — 取消点赞

### POST `/api/user/favorite/:articleId` 🔒 — 收藏文章

### DELETE `/api/user/favorite/:articleId` 🔒 — 取消收藏

### GET `/api/user/favorite/list` 🔒 — 获取收藏列表

### POST `/api/user/follow/:userId` 🔒 — 关注用户

### DELETE `/api/user/follow/:userId` 🔒 — 取关用户

---

## 7. 通知中心 `/api/notification`

### GET `/api/notification/list` 🔒 — 获取通知列表

**查询参数：** `page`、`size`、`type`（通知类型过滤）、`isRead`（0/1）

### PUT `/api/notification/:id/read` 🔒 — 标记单条已读

### PUT `/api/notification/read-all` 🔒 — 全部已读

### GET `/api/notification/unread-count` 🔒 — 获取未读数量

---

## 8. 搜索 `/api/search`

### GET `/api/search` — 全站搜索

**查询参数：**
```
keyword=关键词&type=article&page=1&size=10
```

`type` 可选值：`article`（文章）、`user`（用户）、`all`（全部）

---

## 9. 统计 `/api/statistics`

### GET `/api/statistics/hot-articles` — 热门文章排行

**查询参数：** `rankType=day|week`、`limit=10`

### GET `/api/statistics/recommended` — 推荐文章

### GET `/api/statistics/top-articles` — 置顶文章

---

## 10. 文件上传 `/api/upload`

### POST `/api/upload/image` 🔒 — 上传图片

**请求体：** `multipart/form-data`，字段名 `file`

支持格式：`jpg`、`jpeg`、`png`、`gif`、`bmp`、`webp`  
最大大小：10MB

**响应：**
```json
{ "url": "https://tos.example.com/path/to/image.jpg" }
```

---

## 11. 管理后台 `/api/admin` 🔑

### GET `/api/admin/statistics` — 全站统计数据

### GET `/api/admin/users` — 用户列表（支持分页/搜索）

### PUT `/api/admin/users/:id/status` — 修改用户状态（启用/禁用）

### DELETE `/api/admin/users/:id` — 删除用户

### GET `/api/admin/articles` — 文章列表（管理视图，含草稿）

### DELETE `/api/admin/articles/:id` — 管理员删除文章

### GET `/api/admin/comments` — 评论列表（管理视图）

### PUT `/api/admin/comments/:id/status` — 修改评论状态

### DELETE `/api/admin/comments/:id` — 删除评论

### GET `/api/admin/config` — 获取系统配置

### PUT `/api/admin/config` — 更新系统配置

### GET `/api/admin/backup/list` — 获取备份列表

### POST `/api/admin/backup/create` — 创建备份

### POST `/api/admin/backup/restore` — 恢复备份

---

## 分页参数规范

所有列表接口均支持以下分页参数：

| 参数 | 类型 | 默认值 | 最大值 |
|------|------|--------|--------|
| `page` | int | 1 | - |
| `size` | int | 10 | 100 |

**分页响应格式：**
```json
{
  "records": [...],
  "total": 100,
  "page": 1,
  "size": 10,
  "pages": 10
}
```
