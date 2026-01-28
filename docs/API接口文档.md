# 博客网站 API 接口文档

## 接口概述

### 基础信息
- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Token
- **数据格式**: JSON
- **字符编码**: UTF-8

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
- `200`: 成功
- `400`: 请求参数错误
- `401`: 未认证
- `403`: 权限不足
- `404`: 资源不存在
- `500`: 服务器内部错误

### 认证说明
需要认证的接口在请求头中添加：
```
Authorization: Bearer {token}
```

---

## 1. 用户管理模块

### 1.1 用户注册
**POST** `/api/user/register`

**请求参数:**
```json
{
  "username": "string",     // 用户名，3-20字符
  "email": "string",        // 邮箱地址
  "password": "string",     // 密码，6-20字符
  "confirmPassword": "string", // 确认密码，必须与密码一致
  "nickname": "string",     // 昵称，可选
  "captcha": "string",      // 验证码，必填
  "captchaKey": "string"    // 验证码key，必填
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "nickname": "测试用户",
    "phone": null,
    "bio": null,
    "website": null,
    "avatar": null,
    "status": 1,
    "role": "user",
    "createTime": "2025-01-01T00:00:00",
    "lastLoginTime": null,
    "lastLoginIp": null,
    "articleCount": 0,
    "commentCount": 0,
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

### 1.12 生成验证码
**POST** `/api/captcha/generate`

**功能说明:**
- 生成验证码，用于用户注册、登录等场景
- 无需登录认证
- 验证码有效期为5分钟，且只能使用一次

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "captcha_key_123456",
  "timestamp": 1764924000000
}
```

**错误响应:**
```json
{
  "code": 500,
  "message": "生成验证码失败",
  "data": null,
  "timestamp": 1764924000000
}
```

### 1.2 用户登录
**POST** `/api/user/login`

**请求参数:**
```json
{
  "username": "string",     // 用户名或邮箱
  "password": "string"      // 密码
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "phone": null,
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
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": 1761035724179
}
```

### 1.3 获取用户信息
**GET** `/api/user/info` 🔒

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "phone": null,
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
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "timestamp": 1761035724179
}
```

### 1.4 更新用户信息
**PUT** `/api/user/info` 🔒

**请求参数:**
```json
{
  "nickname": "string",     // 昵称
  "email": "string",        // 邮箱
  "avatar": "string"        // 头像URL
}
```

### 1.5 修改密码
**PUT** `/api/user/password` 🔒

**请求参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| oldPassword | String | 是 | 原密码（查询参数） |
| newPassword | String | 是 | 新密码（查询参数） |

**调用示例:**
```bash
curl -X PUT "http://localhost:8080/api/user/password?oldPassword=old123&newPassword=new123" \
  -H "Authorization: Bearer {token}"
```

### 1.6 用户登出
**POST** `/api/user/logout` 🔒

**功能说明:**
- 用户退出登录，使当前JWT令牌失效
- 需要用户登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1761035724179
}
```

### 1.7 刷新Token
**POST** `/api/user/refresh-token` 🔒

**请求参数:**
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| refreshToken | String | Query | 是 | 刷新令牌 |

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "timestamp": 1761035724179
}
```

### 1.8 重置密码
**POST** `/api/user/reset-password`

**功能说明:**
- 通过邮箱重置用户密码
- 无需登录认证

**请求参数:**
| 参数名 | 类型 | 位置 | 必填 | 说明 |
|--------|------|------|------|------|
| email | String | Query | 是 | 邮箱地址 |
| newPassword | String | Query | 是 | 新密码 |

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1761035724179
}
```

### 1.9 获取用户列表（管理员）
**GET** `/api/user/admin/list` 🔒

**查询参数:**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| size | Integer | 否 | 10 | 每页数量 |
| keyword | String | 否 | null | 搜索关键词 |

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "username": "admin",
      "nickname": "管理员",
      "email": "admin@example.com",
      "status": 1,
      "createTime": "2025-01-01T00:00:00",
      "lastLoginTime": "2025-10-21T08:30:00"
    },
    {
      "id": 2,
      "username": "demo_user",
      "nickname": "演示用户",
      "email": "demo@example.com",
      "status": 0,
      "createTime": "2025-01-02T00:00:00",
      "lastLoginTime": null
    }
  ],
  "timestamp": 1761035837904
}
```

### 1.10 更新用户状态（管理员）
**PUT** `/api/user/admin/status/{userId}` 🔒

**路径参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**查询参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| status | Integer | 是 | 状态：1-启用，0-禁用 |

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1761035850168
}
```

### 1.11 删除用户（管理员）
**DELETE** `/api/user/admin/{userId}` 🔒

**路径参数:**
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Long | 是 | 用户ID |

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1761035822151
}
```

**错误响应:**
```json
{
  "code": 500,
  "message": "用户不存在",
  "data": null,
  "timestamp": 1761035822151
}
```

---

## 2. 文章管理模块

### 2.1 获取文章列表
**GET** `/api/article/list`

#### 功能描述
获取系统中的文章列表，支持多维度筛选和分页查询，为用户提供灵活的文章检索功能。

#### 业务场景
- 前端首页文章列表展示
- 分类页面文章列表
- 搜索结果页面展示
- 用户个人文章管理
- 管理后台文章审核

#### 权限要求
- **角色要求**: 所有用户（游客、普通用户、管理员）
- **权限级别**: 读取权限
- **认证要求**: 无需认证（游客可访问）

#### 调用限制
- **频率限制**: 100次/分钟/IP
- **数据量限制**: 单次查询最多返回100条记录
- **缓存策略**: 列表数据缓存5分钟

#### 请求参数
| 参数名 | 类型 | 位置 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|------|--------|------|------|
| page | Integer | Query | 否 | 1 | >=1 | 页码，从1开始 |
| size | Integer | Query | 否 | 10 | 1-100 | 每页数量，最大100 |
| keyword | String | Query | 否 | null | 最大50字符 | 搜索关键词，支持标题和内容搜索 |
| categoryId | Long | Query | 否 | null | >0 | 分类ID，用于筛选指定分类的文章 |
| tagId | Long | Query | 否 | null | >0 | 标签ID，用于筛选指定标签的文章 |
| status | Integer | Query | 否 | null | 0,1 | 文章状态：0-草稿，1-已发布 |
| authorId | Long | Query | 否 | null | >0 | 作者ID，用于筛选指定作者的文章 |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "Spring Boot快速入门指南",
      "content": "# Spring Boot快速入门指南\n\nSpring Boot是基于Spring框架的快速开发框架...",
      "summary": "Spring Boot快速入门教程，介绍基本概念和使用方法。",
      "coverImage": "https://example.com/images/spring-boot-cover.jpg",
      "status": 1,
      "allowComment": 1,
      "viewCount": 156,
      "likeCount": 23,
      "commentCount": 8,
      "favoriteCount": 12,
      "authorId": 1,
      "authorNickname": "系统管理员",
      "authorAvatar": "https://example.com/avatars/admin.jpg",
      "categoryId": 6,
      "categoryName": "技术分享",
      "tags": [
        {
          "id": 1,
          "name": "Java",
          "description": "Java相关技术",
          "color": "#ff6b6b"
        },
        {
          "id": 2,
          "name": "Spring Boot",
          "description": "Spring Boot框架",
          "color": "#4CAF50"
        }
      ],
      "category": {
        "id": 6,
        "name": "技术分享",
        "description": "技术相关的文章分享",
        "sortOrder": 1,
        "icon": null
      },
      "liked": false,
      "favorited": false,
      "createTime": "2024-01-15T10:30:00",
      "updateTime": "2024-01-20T14:25:00",
      "publishTime": "2024-01-15T10:30:00"
    }
  ],
  "timestamp": 1640995200000
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码，200表示成功 |
| message | String | 响应消息 |
| data | Array | 文章列表数据数组 |
| data[].id | Long | 文章唯一标识ID |
| data[].title | String | 文章标题 |
| data[].content | String | 文章内容（Markdown格式） |
| data[].summary | String | 文章摘要 |
| data[].coverImage | String | 封面图片URL |
| data[].status | Integer | 文章状态：0-草稿，1-已发布 |
| data[].allowComment | Integer | 是否允许评论：0-不允许，1-允许 |
| data[].viewCount | Integer | 浏览次数 |
| data[].likeCount | Integer | 点赞次数 |
| data[].commentCount | Integer | 评论次数 |
| data[].favoriteCount | Integer | 收藏次数 |
| data[].authorId | Long | 作者ID |
| data[].authorNickname | String | 作者昵称 |
| data[].authorAvatar | String | 作者头像URL |
| data[].categoryId | Long | 分类ID |
| data[].categoryName | String | 分类名称 |
| data[].tags | Array | 标签列表 |
| data[].category | Object | 分类详细信息 |
| data[].liked | Boolean | 当前用户是否已点赞 |
| data[].favorited | Boolean | 当前用户是否已收藏 |
| data[].createTime | String | 创建时间（ISO 8601格式，例如：2024-01-01T00:00:00） |
| data[].updateTime | String | 更新时间（ISO 8601格式，例如：2024-01-01T00:00:00） |
| data[].publishTime | String | 发布时间（ISO 8601格式，例如：2024-01-01T00:00:00） |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功 |

#### 错误响应

**参数验证错误 (HTTP 400)**
```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": null,
  "timestamp": 1640995200000,
}
```

**数据验证错误 (HTTP 422)**
```json
{
  "code": 422,
  "message": "请求参数验证失败",
  "data": {
    "errors": [
      {
        "field": "page",
        "message": "页码必须大于0"
      },
      {
        "field": "size",
        "message": "每页数量必须在1-100之间"
      }
    ]
  },
  "timestamp": 1640995200000,
}
```

**服务器错误 (HTTP 500)**
```json
{
  "code": 500,
  "message": "服务器内部错误",
  "data": null,
  "timestamp": 1640995200000,
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 400 | 请求参数错误 | 检查请求参数格式和类型 |
| 422 | 请求参数验证失败 | 检查参数值是否满足约束条件 |
| 429 | 请求过于频繁 | 降低请求频率，遵守限流规则 |
| 500 | 服务器内部错误 | 联系系统管理员或稍后重试 |
| 10001 | 数据库错误 | 检查数据库连接状态 |
| 10002 | 缓存错误 | 检查缓存服务状态 |

#### 调用示例
```bash
# 基础查询 - 获取第1页，每页10条
curl -X GET "http://localhost:8080/api/article/list?page=1&size=10"

# 按分类筛选 - 获取分类ID为6的文章
curl -X GET "http://localhost:8080/api/article/list?categoryId=6"

# 搜索文章 - 搜索包含"Spring"关键词的文章
curl -X GET "http://localhost:8080/api/article/list?keyword=Spring"

# 按作者筛选 - 获取作者ID为1的文章
curl -X GET "http://localhost:8080/api/article/list?authorId=1"

# 组合查询 - 按分类和状态筛选
curl -X GET "http://localhost:8080/api/article/list?categoryId=6&status=2&page=1&size=20"
```

### 2.2 获取文章详情
**GET** `/api/article/{articleId}`

#### 功能描述
根据文章ID获取指定文章的详细信息，包括完整的文章内容、作者信息、分类标签、统计数据等。

#### 业务场景
- 文章详情页面展示
- 文章编辑页面数据预填充
- 文章分享链接生成
- 移动端文章阅读
- 文章打印功能

#### 权限要求
- **角色要求**: 所有用户（游客、普通用户、管理员）
- **权限级别**: 读取权限
- **认证要求**: 无需认证（游客可访问已发布文章）

#### 调用限制
- **频率限制**: 200次/分钟/IP
- **数据量限制**: 单次查询返回单篇文章
- **缓存策略**: 文章详情缓存10分钟
- **访问统计**: 每次访问会自动更新文章浏览量

#### 请求参数
| 参数名 | 类型 | 位置 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|------|--------|------|------|
| articleId | Long | Path | 是 | - | >0 | 文章唯一标识ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "Spring Boot快速入门指南",
    "content": "# Spring Boot快速入门指南\n\n## 1. 什么是Spring Boot\n\nSpring Boot是基于Spring框架的快速开发框架，它简化了基于Spring的应用开发...\n\n## 2. 核心特性\n\n- 自动配置\n- 起步依赖\n- 嵌入式服务器\n- 生产就绪功能\n\n## 3. 快速开始\n\n### 3.1 创建项目\n\n使用Spring Initializr快速创建项目...\n\n### 3.2 编写代码\n\n```java\n@RestController\npublic class HelloController {\n    @GetMapping(\"/hello\")\n    public String hello() {\n        return \"Hello, Spring Boot!\";\n    }\n}\n```",
    "summary": "Spring Boot快速入门教程，详细介绍Spring Boot的核心特性和使用方法，包含完整的代码示例。",
    "coverImage": "https://example.com/images/spring-boot-detail-cover.jpg",
    "status": 1,
    "allowComment": 1,
    "viewCount": 157,
    "likeCount": 23,
    "commentCount": 8,
    "favoriteCount": 12,
    "authorId": 1,
    "authorNickname": "系统管理员",
    "authorAvatar": "https://example.com/avatars/admin-detail.jpg",
    "categoryId": 6,
    "categoryName": "技术分享",
    "tags": [
      {
        "id": 1,
        "name": "Java",
        "description": "Java相关技术",
        "color": "#ff6b6b"
      },
      {
        "id": 2,
        "name": "Spring Boot",
        "description": "Spring Boot框架",
        "color": "#4CAF50"
      },
      {
        "id": 3,
        "name": "后端开发",
        "description": "后端开发技术",
        "color": "#2196F3"
      }
    ],
    "category": {
      "id": 6,
      "name": "技术分享",
      "description": "技术相关的文章分享",
      "sortOrder": 1,
      "icon": "https://example.com/icons/tech-share.png"
    },
    "liked": false,
    "favorited": false,
    "createTime": "2024-01-15T10:30:00",
    "updateTime": "2024-01-20T14:25:00",
    "publishTime": "2024-01-15T10:30:00"
  },
  "timestamp": 1640995200000,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码，200表示成功 |
| message | String | 响应消息 |
| data | Object | 文章详情数据对象 |
| data.id | Long | 文章唯一标识ID |
| data.title | String | 文章标题 |
| data.content | String | 文章完整内容（Markdown格式） |
| data.summary | String | 文章摘要 |
| data.coverImage | String | 封面图片URL |
| data.status | Integer | 文章状态：0-草稿，1-已发布 |
| data.allowComment | Integer | 是否允许评论：0-不允许，1-允许 |
| data.viewCount | Integer | 浏览次数（含当前访问） |
| data.likeCount | Integer | 点赞次数 |
| data.commentCount | Integer | 评论次数 |
| data.favoriteCount | Integer | 收藏次数 |
| data.authorId | Long | 作者ID |
| data.authorNickname | String | 作者昵称 |
| data.authorAvatar | String | 作者头像URL |
| data.categoryId | Long | 分类ID |
| data.categoryName | String | 分类名称 |
| data.tags | Array | 标签列表数组 |
| data.tags[].id | Long | 标签ID |
| data.tags[].name | String | 标签名称 |
| data.tags[].description | String | 标签描述 |
| data.tags[].color | String | 标签颜色（十六进制） |
| data.category | Object | 分类详细信息对象 |
| data.category.id | Long | 分类ID |
| data.category.name | String | 分类名称 |
| data.category.description | String | 分类描述 |
| data.category.sortOrder | Integer | 分类排序序号 |
| data.category.icon | String | 分类图标URL |
| data.liked | Boolean | 当前用户是否已点赞（需登录） |
| data.favorited | Boolean | 当前用户是否已收藏（需登录） |
| data.createTime | String | 创建时间（ISO 8601格式，例如：2024-01-01T00:00:00） |
| data.updateTime | String | 更新时间（ISO 8601格式，例如：2024-01-01T00:00:00） |
| data.publishTime | String | 发布时间（ISO 8601格式，例如：2024-01-01T00:00:00） |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功 |

#### 错误响应

**文章不存在 (HTTP 404)**
```json
{
  "code": 2001,
  "message": "文章不存在",
  "data": null,
  "timestamp": 1640995200000,
}
```

**文章未发布 (HTTP 403)**
```json
{
  "code": 2002,
  "message": "文章未发布",
  "data": null,
  "timestamp": 1640995200000,
}
```

**参数验证错误 (HTTP 400)**
```json
{
  "code": 400,
  "message": "请求参数错误",
  "data": null,
  "timestamp": 1640995200000,
}
```

**服务器错误 (HTTP 500)**
```json
{
  "code": 500,
  "message": "服务器内部错误",
  "data": null,
  "timestamp": 1640995200000,
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 400 | 请求参数错误 | 检查文章ID格式是否正确 |
| 401 | 未认证 | 尝试访问私有文章需要先登录 |
| 403 | 无权限访问 | 文章未发布或用户无权访问 |
| 2001 | 文章不存在 | 检查文章ID是否正确 |
| 2002 | 文章未发布 | 联系作者发布文章或等待发布 |
| 2003 | 文章状态错误 | 检查文章当前状态 |
| 500 | 服务器内部错误 | 联系系统管理员或稍后重试 |
| 10001 | 数据库错误 | 检查数据库连接状态 |
| 10002 | 缓存错误 | 检查缓存服务状态 |

#### 业务规则说明
1. **访问权限控制**
   - 游客只能访问已发布的文章（status=2）
   - 登录用户可以访问自己的草稿文章（status=1）
   - 管理员可以访问所有状态的文章

2. **浏览量统计**
   - 每次成功访问文章详情都会自动增加浏览量
   - 同一IP用户5分钟内重复访问不计入浏览量
   - 文章作者访问自己的文章不计入浏览量

3. **内容展示**
   - Markdown格式的内容需要前端进行渲染
   - 支持代码高亮、数学公式等扩展语法
   - 图片链接需要处理相对路径和绝对路径

4. **缓存策略**
   - 已发布文章缓存10分钟
   - 草稿文章不缓存
   - 文章更新或删除时自动清除缓存

#### 调用示例
```bash
# 获取文章详情 - 基础调用
curl -X GET "http://localhost:8080/api/article/1"

# 带认证头获取文章详情（可以获取liked和favorited状态）
curl -X GET "http://localhost:8080/api/article/1" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# 获取不存在的文章
curl -X GET "http://localhost:8080/api/article/99999"
# 响应：{"code": 2001, "message": "文章不存在", ...}

# 获取未发布的文章（游客访问）
curl -X GET "http://localhost:8080/api/article/2"
# 响应：{"code": 2002, "message": "文章未发布", ...}
```

#### 前端集成示例
```javascript
// 获取文章详情
async function getArticleDetail(articleId, token = null) {
  const headers = {
    'Content-Type': 'application/json'
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(`/api/article/${articleId}`, {
      method: 'GET',
      headers: headers
    });

    const result = await response.json();

    if (result.success) {
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('获取文章详情失败:', error);
    throw error;
  }
}

// 使用示例
getArticleDetail(1)
  .then(article => {
    console.log('文章详情:', article);
    // 渲染文章内容
    renderArticle(article);
  })
  .catch(error => {
    console.error('错误:', error);
  });
```

### 2.3 创建文章
**POST** `/api/article/publish` 🔒

**请求参数:**
```json
{
  "title": "string",        // 文章标题
  "content": "string",      // 文章内容（Markdown）
  "summary": "string",      // 文章摘要，可选
  "coverImage": "string",   // 封面图片URL，可选
  "categoryId": 1,          // 分类ID
  "tagIds": "1,2,3",        // 标签ID列表，用逗号分隔
  "status": 1,              // 状态：0-草稿，1-已发布
  "allowComment": 1         // 是否允许评论：0-不允许，1-允许
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 5,                // 新创建文章的ID
  "timestamp": 1761040461369,
}
```

### 2.4 更新文章
**PUT** `/api/article/{articleId}` 🔒

**请求参数:**
```json
{
  "title": "string",        // 文章标题
  "content": "string",      // 文章内容（Markdown）
  "summary": "string",      // 文章摘要，可选
  "coverImage": "string",   // 封面图片URL，可选
  "categoryId": 1,          // 分类ID
  "tagIds": "1,2,3",        // 标签ID列表，用逗号分隔
  "status": 1,              // 状态：0-草稿，1-已发布
  "allowComment": 1         // 是否允许评论：0-不允许，1-允许
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1761040461369,
}
```

### 2.5 删除文章
**DELETE** `/api/article/{articleId}` 🔒

**路径参数:**
- `articleId` (Long): 文章ID

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1761040634730
}
```

### 2.6.2 检查文章是否已点赞
**GET** `/api/user/like/{articleId}/check` 🔒

**注意:** 此接口在UserLikeController中实现

**路径参数:**
- `articleId`: 文章ID (Long)

**功能说明:**
- 检查当前用户是否已点赞指定文章
- 需要用户登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": true,
  "timestamp": 1761040634730,
}
```

### 2.6.3 获取用户点赞列表
**GET** `/api/user/like/list` 🔒

**查询参数:**
- `page`: 页码，默认1
- `size`: 每页数量，默认10

**功能说明:**
- 获取当前用户的点赞文章列表
- 返回分页的点赞记录，每条记录包含完整的文章信息
- 需要用户登录认证

**响应字段说明:**
- `id`: 点赞记录ID
- `userId`: 点赞用户ID
- `articleId`: 被点赞的文章ID
- `createdAt`: 点赞时间（Unix时间戳字符串格式）
- `article`: 完整的文章信息对象
  - `id`: 文章ID
  - `title`: 文章标题
  - `content`: 文章内容
  - `summary`: 文章摘要
  - `status`: 文章状态（0-草稿，1-已发布）
  - `viewCount`: 浏览量
  - `likeCount`: 点赞数
  - `commentCount`: 评论数
  - `favoriteCount`: 收藏数
  - `authorId`: 作者ID
  - `categoryId`: 分类ID
  - `createTime`: 文章创建时间
  - `updateTime`: 文章更新时间
  - `publishTime`: 文章发布时间

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 2,
      "userId": 1,
      "articleId": 2,
      "createdAt": "1761042796",
      "article": {
        "id": 2,
        "title": "Spring Boot 快速入门指南",
        "content": "# Spring Boot 快速入门指南\n\nSpring Boot 是一个基于 Spring 框架的快速开发框架...",
        "summary": "Spring Boot 快速入门教程，介绍基本概念和使用方法。",
        "status": 1,
        "viewCount": 4,
        "likeCount": 1,
        "commentCount": 1,
        "favoriteCount": 0,
        "authorId": 1,
        "categoryId": 6,
        "createTime": "2025-10-17T04:03:01",
        "updateTime": "2025-10-21T10:55:41",
        "publishTime": "2025-10-17T04:03:01"
      }
    }
  ],
  "timestamp": 1761046044608,
}
```

### 2.6.4 获取用户点赞数量
**GET** `/api/user/like/count` 🔒

**功能说明:**
- 获取当前用户的总点赞数量
- 需要用户登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 15,
  "timestamp": 1761040634730,
}
```

### 2.6.5 获取用户点赞的文章列表
**GET** `/api/article/user/{userId}/liked` 🔒

**路径参数:**
- `userId`: 用户ID (Long)

**查询参数:**
- `page`: 页码，默认1
- `size`: 每页数量，默认10

**功能说明:**
- 获取指定用户点赞的文章列表
- 需要管理员权限

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 2,
      "title": "Spring Boot 快速入门指南",
      "summary": "Spring Boot 快速入门教程，介绍基本概念和使用方法。",
      "viewCount": 100,
      "likeCount": 50,
      "commentCount": 10,
      "favoriteCount": 5,
      "authorNickname": "系统管理员",
      "publishTime": "2024-01-01T00:00:00"
    }
  ],
  "timestamp": 1761040634730,
}
```

### 2.6.6 评论点赞
**POST** `/api/comment/{commentId}/like` 🔒

**路径参数:**
- `commentId`: 评论ID (Long)

**功能说明:**
- 为指定评论添加点赞
- 每个用户对同一条评论只能点赞一次
- 点赞后会自动更新评论的点赞计数
- 需要用户登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1761040634730,
}
```

### 2.6.7 取消评论点赞
**DELETE** `/api/comment/{commentId}/like` 🔒

**路径参数:**
- `commentId`: 评论ID (Long)

**功能说明:**
- 取消对指定评论的点赞
- 只有已点赞的用户才能取消点赞
- 取消点赞后会自动更新评论的点赞计数
- 需要用户登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1761040634730,
}
```

### 2.7 文章收藏
**POST** `/api/user/favorite/{articleId}` 🔒

**响应示例:**
```json
{
  "code": 200,
  "message": "收藏成功",
  "data": {
    "favorited": true
  }
}
```

### 2.8 获取用户收藏列表
**GET** `/api/user/favorite/list` 🔒

**查询参数:**
- `page`: 页码，默认1
- `size`: 每页数量，默认10

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 20,
    "pages": 2,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "title": "文章标题",
        "summary": "文章摘要",
        "author": {
          "id": 1,
          "username": "author",
          "nickname": "作者昵称"
        },
        "favoriteTime": "2024-01-01T00:00:00"
      }
    ]
  }
}
```

### 2.9 获取用户发布的文章
**GET** `/api/article/user/{userId}` 🔒

**查询参数:**
- `page`: 页码，默认1
- `size`: 每页数量，默认10
- `status`: 文章状态，可选（published/draft/all）

### 2.10 文章浏览量统计
**POST** `/api/article/{id}/view` 🔒

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "viewCount": 101
  }
}
```

### 2.11 获取置顶文章统计
**GET** `/api/statistics/article/top` 🔒

**查询参数:**
- `limit`: 返回数量限制，默认10

**功能说明:**
- 获取置顶文章的统计数据
- 需要用户登录认证
- 返回文章ID、浏览量、点赞数、评论数和收藏数

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "articleId": 2,
      "viewCount": 4,
      "likeCount": 1,
      "commentCount": 1,
      "favoriteCount": 0
    }
  ],
  "timestamp": 1761044332383,
}
```

### 2.12 获取推荐文章统计
**GET** `/api/statistics/article/recommended` 🔒

**查询参数:**
- `limit`: 返回数量限制，默认10

**功能说明:**
- 获取推荐文章的统计数据
- 需要用户登录认证
- 返回文章ID、浏览量、点赞数、评论数和收藏数

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "articleId": 6,
      "viewCount": 1,
      "likeCount": 1,
      "commentCount": 1,
      "favoriteCount": 1
    }
  ],
  "timestamp": 1761044373164,
}
```

### 2.13 获取热门文章统计
**GET** `/api/statistics/article/hot` 🔒

**查询参数:**
- `limit`: 返回数量限制，默认10

**功能说明:**
- 获取热门文章的统计数据
- 需要用户登录认证
- 返回文章ID、浏览量、点赞数、评论数和收藏数

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "articleId": 2,
      "viewCount": 4,
      "likeCount": 1,
      "commentCount": 1,
      "favoriteCount": 0
    },
    {
      "articleId": 6,
      "viewCount": 1,
      "likeCount": 1,
      "commentCount": 1,
      "favoriteCount": 1
    }
  ],
  "timestamp": 1761044378552,
}
```

### 2.14 获取热门文章
**GET** `/api/article/hot`

**查询参数:**
- `limit`: 返回数量限制，默认10

**功能说明:**
- 获取热门文章列表
- 无需登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "热门文章标题",
      "summary": "文章摘要",
      "viewCount": 100,
      "likeCount": 50,
      "commentCount": 10,
      "favoriteCount": 5,
      "authorNickname": "作者昵称",
      "publishTime": "2024-01-01T00:00:00"
    }
  ],
  "timestamp": 1761044378552,
}
```

### 2.15 获取推荐文章
**GET** `/api/article/recommended`

**查询参数:**
- `limit`: 返回数量限制，默认10

**功能说明:**
- 获取推荐文章列表
- 无需登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 2,
      "title": "推荐文章标题",
      "summary": "文章摘要",
      "viewCount": 50,
      "likeCount": 25,
      "commentCount": 5,
      "favoriteCount": 3,
      "authorNickname": "作者昵称",
      "publishTime": "2024-01-01T00:00:00"
    }
  ],
  "timestamp": 1761044378552,
}
```

### 2.16 上传文章封面图片
**POST** `/api/article/upload-cover` 🔒

**请求参数:**
- `file`: 图片文件（multipart/form-data）

**功能说明:**
- 上传文章封面图片
- 需要用户登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "上传成功",
  "data": "http://example.com/uploads/cover.jpg",
  "timestamp": 1761044378552,
}
```

### 2.17 搜索文章
**GET** `/api/article/search`

**查询参数:**
- `keyword`: 搜索关键词
- `page`: 页码，默认1
- `size`: 每页数量，默认10

**功能说明:**
- 根据关键词搜索文章
- 无需登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "搜索到的文章标题",
      "summary": "文章摘要",
      "viewCount": 100,
      "likeCount": 50,
      "commentCount": 10,
      "favoriteCount": 5,
      "authorNickname": "作者昵称",
      "publishTime": "2024-01-01T00:00:00"
    }
  ],
  "timestamp": 1761044378552,
}
```

### 2.18 按分类获取文章列表
**GET** `/api/article/category/{categoryId}`

**路径参数:**
- `categoryId`: 分类ID

**查询参数:**
- `page`: 页码，默认1
- `size`: 每页数量，默认10

**功能说明:**
- 根据分类ID获取文章列表
- 无需登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "分类下的文章标题",
      "summary": "文章摘要",
      "viewCount": 100,
      "likeCount": 50,
      "commentCount": 10,
      "favoriteCount": 5,
      "authorNickname": "作者昵称",
      "publishTime": "2024-01-01T00:00:00"
    }
  ],
  "timestamp": 1761044378552,
}
```

### 2.19 按标签获取文章列表
**GET** `/api/article/tag/{tagId}`

**路径参数:**
- `tagId`: 标签ID

**查询参数:**
- `page`: 页码，默认1
- `size`: 每页数量，默认10

**功能说明:**
- 根据标签ID获取文章列表
- 无需登录认证

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "标签下的文章标题",
      "summary": "文章摘要",
      "viewCount": 100,
      "likeCount": 50,
      "commentCount": 10,
      "favoriteCount": 5,
      "authorNickname": "作者昵称",
      "publishTime": "2024-01-01T00:00:00"
    }
  ],
  "timestamp": 1761044378552,
}
```

---

## 3. 分类管理模块

### 3.1 获取分类列表
**GET** `/api/category/list`

#### 功能描述
获取系统中所有可用的分类列表，支持博客文章按分类进行组织和管理。

#### 业务场景
- 前端展示分类导航菜单
- 后台管理系统分类管理页面
- 文章发布时选择分类

#### 权限要求
- **角色要求**: 所有用户（包括未登录用户）
- **权限级别**: 读取权限

#### 调用限制
- **频率限制**: 60次/分钟
- **数据量限制**: 无限制

#### 请求参数
无请求参数

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "技术分享",
      "description": "技术相关的文章分享",
      "sortOrder": 1,
      "icon": null,
      "articleCount": 0
    },
    {
      "id": 2,
      "name": "生活随笔",
      "description": "日常生活感悟和随笔",
      "sortOrder": 2,
      "icon": null,
      "articleCount": 0
    }
  ],
  "timestamp": 1762853546828,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Array | 分类列表数据 |
| data[].id | Long | 分类唯一标识 |
| data[].name | String | 分类名称 |
| data[].description | String | 分类描述 |
| data[].sortOrder | Integer | 排序序号，数值越小排序越靠前 |
| data[].icon | String | 分类图标URL |
| data[].articleCount | Long | 关联文章数量 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功（根据code自动计算） |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "服务器内部错误",
  "data": null,
  "timestamp": 1762853546828,
}
```

---

### 3.2 获取分类详情
**GET** `/api/category/{categoryId}`

#### 功能描述
根据分类ID获取指定分类的详细信息。

#### 业务场景
- 分类编辑页面预填充数据
- 分类详情展示页面
- 分类关联文章列表页面

#### 权限要求
- **角色要求**: 所有用户（包括未登录用户）
- **权限级别**: 读取权限

#### 调用限制
- **频率限制**: 60次/分钟
- **数据量限制**: 无限制

#### 请求参数
| 参数名 | 类型 | 位置 | 必填 | 默认值 | 说明 |
|--------|------|------|------|--------|------|
| categoryId | Long | 路径参数 | 是 | - | 分类唯一标识ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "技术分享",
    "description": "技术相关的文章分享",
    "sortOrder": 1,
    "icon": null,
    "articleCount": 0
  },
  "timestamp": 1762853558596,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Object | 分类详情数据 |
| data.id | Long | 分类唯一标识 |
| data.name | String | 分类名称 |
| data.description | String | 分类描述 |
| data.sortOrder | Integer | 排序序号，数值越小排序越靠前 |
| data.icon | String | 分类图标URL |
| data.articleCount | Long | 关联文章数量 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功 |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "分类不存在",
  "data": null,
  "timestamp": 1762853583573,
}
```

---

### 3.3 创建分类
**POST** `/api/category` 🔒

#### 功能描述
在系统中创建新的文章分类。

#### 业务场景
- 后台管理系统添加新分类
- 批量导入分类数据
- 系统初始化时创建默认分类

#### 权限要求
- **读取权限**: 所有用户（包括未登录用户）
- **写入权限**: 仅管理员用户

#### 调用限制
- **频率限制**: 10次/分钟
- **数据量限制**: 单次请求最大50个分类

#### 请求参数
**请求体 (application/json)**
```json
{
  "name": "技术分享",
  "description": "技术相关的文章分享",
  "sortOrder": 1,
  "icon": "https://example.com/icon.png"
}
```

**参数说明**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| name | String | 是 | - | 长度1-50字符 | 分类名称，不能为空 |
| description | String | 否 | null | 长度0-500字符 | 分类描述信息 |
| sortOrder | Integer | 否 | 0 | 0-999 | 排序序号，数值越小排序越靠前 |
| icon | String | 否 | null | 长度0-200字符 | 分类图标URL地址 |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 10,
  "timestamp": 1762852809707,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Long | 新创建的分类ID |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功 |

**错误响应 (HTTP 400)**
```json
{
  "code": 400,
  "message": "分类名称不能为空",
  "data": null,
  "timestamp": 1762852809707,
}
```

---

### 3.4 更新分类
**PUT** `/api/category/{categoryId}` 🔒

#### 功能描述
更新指定分类的详细信息。

#### 业务场景
- 后台管理系统编辑分类信息
- 批量更新分类排序
- 分类图标或描述信息更新

#### 权限要求
- **读取权限**: 所有用户（包括未登录用户）
- **写入权限**: 仅管理员用户

#### 调用限制
- **频率限制**: 30次/分钟
- **数据量限制**: 无限制

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| categoryId | Long | 是 | - | 要更新的分类ID |

**请求体 (application/json)**
```json
{
  "name": "技术分享",
  "description": "技术相关的文章分享",
  "sortOrder": 1,
  "icon": "https://example.com/icon.png"
}
```

**参数说明**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| name | String | 是 | - | 长度1-50字符 | 分类名称，不能为空 |
| description | String | 否 | null | 长度0-500字符 | 分类描述信息 |
| sortOrder | Integer | 否 | 0 | 0-999 | 排序序号，数值越小排序越靠前 |
| icon | String | 否 | null | 长度0-200字符 | 分类图标URL地址 |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1762852809707,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | null | 无返回数据 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功 |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "分类不存在",
  "data": null,
  "timestamp": 1762852809707,
}
```

---

### 3.5 删除分类
**DELETE** `/api/category/{categoryId}` 🔒

#### 功能描述
删除指定的分类。注意：如果分类下存在文章，则无法删除。

#### 业务场景
- 后台管理系统删除无效分类
- 系统维护时清理废弃分类
- 分类合并时删除重复分类

#### 权限要求
- **读取权限**: 所有用户（包括未登录用户）
- **写入权限**: 仅管理员用户

#### 调用限制
- **频率限制**: 10次/分钟
- **数据量限制**: 无限制

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| categoryId | Long | 是 | - | 要删除的分类ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1762852809707,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | null | 无返回数据 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功 |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "分类下存在文章，无法删除",
  "data": null,
  "timestamp": 1762852809707,
}
```

---

### 3.6 获取分类文章数量
**GET** `/api/category/{categoryId}/count`

#### 功能描述
获取指定分类下的文章数量统计。

#### 业务场景
- 分类管理页面显示文章数量
- 分类导航菜单显示文章统计
- 系统统计报表生成

#### 权限要求
- **角色要求**: 所有用户（包括未登录用户）
- **权限级别**: 读取权限

#### 调用限制
- **频率限制**: 60次/分钟
- **数据量限制**: 无限制

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| categoryId | Long | 是 | - | 分类唯一标识ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 0,
  "timestamp": 1762853572520,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Integer | 分类下的文章数量 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功 |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "分类不存在",
  "data": null,
  "timestamp": 1762853572520,
}
```

---

### 3.7 错误码列表

#### 通用错误码
| 错误码 | 错误信息 | 解决方案 |
|-------|---------|----------|
| 200 | 操作成功 | - |
| 400 | 请求参数错误 | 检查请求参数格式和必填项 |
| 401 | 未认证 | 检查JWT令牌是否有效 |
| 403 | 无权限访问 | 检查用户角色和权限 |
| 404 | 资源不存在 | 检查请求的资源ID是否正确 |
| 405 | 请求方法不允许 | 检查请求方法是否正确 |
| 409 | 资源冲突 | 检查请求资源是否冲突 |
| 422 | 请求参数验证失败 | 检查参数格式和约束条件 |
| 429 | 请求过于频繁 | 稍后再试 |
| 500 | 服务器内部错误 | 联系系统管理员 |
| 503 | 服务暂不可用 | 稍后再试 |

#### 用户管理错误码
| 错误码 | 错误信息 | 解决方案 |
|-------|---------|----------|
| 1001 | 用户不存在 | 检查用户ID是否正确 |
| 1002 | 用户已被禁用 | 联系管理员启用账户 |
| 1003 | 密码错误 | 检查密码是否正确 |
| 1004 | 用户名已存在 | 使用其他用户名 |
| 1005 | 邮箱已存在 | 使用其他邮箱地址 |
| 1006 | 手机号已存在 | 使用其他手机号 |
| 1007 | 原密码错误 | 检查原密码是否正确 |
| 1008 | 两次密码输入不一致 | 确认两次密码输入一致 |

#### 文章管理错误码
| 错误码 | 错误信息 | 解决方案 |
|-------|---------|----------|
| 2001 | 文章不存在 | 检查文章ID是否正确 |
| 2002 | 文章未发布 | 文章需要先发布才能进行此操作 |
| 2003 | 文章状态错误 | 检查文章当前状态 |

#### 评论管理错误码
| 错误码 | 错误信息 | 解决方案 |
|-------|---------|----------|
| 2004 | 评论不存在 | 检查评论ID是否正确 |
| 2005 | 评论状态错误 | 检查评论当前状态 |

#### 分类管理错误码
| 错误码 | 错误信息 | 解决方案 |
|-------|---------|----------|
| 3001 | 分类不存在 | 检查分类ID是否正确 |
| 3002 | 分类下存在文章，无法删除 | 先删除分类下的文章再删除分类 |

#### 标签管理错误码
| 错误码 | 错误信息 | 解决方案 |
|-------|---------|----------|
| 4001 | 标签不存在 | 检查标签ID是否正确 |
| 4002 | 标签下存在文章，无法删除 | 先删除标签下的文章再删除标签 |

#### 文件管理错误码
| 错误码 | 错误信息 | 解决方案 |
|-------|---------|----------|
| 5001 | 文件不存在 | 检查文件ID是否正确 |
| 5002 | 文件上传失败 | 检查网络连接或稍后再试 |
| 5003 | 文件类型不支持 | 使用支持的文件类型 |
| 5004 | 文件大小超出限制 | 使用较小的文件 |
| 5005 | 文件删除失败 | 联系系统管理员 |

#### 系统配置错误码
| 错误码 | 错误信息 | 解决方案 |
|-------|---------|----------|
| 6001 | 配置不存在 | 检查配置键是否正确 |
| 6002 | 配置键已存在 | 使用其他配置键 |

#### 收藏点赞错误码
| 错误码 | 错误信息 | 解决方案 |
|-------|---------|----------|
| 7001 | 已收藏 | 无需重复收藏 |
| 7002 | 已点赞 | 无需重复点赞 |

#### 系统错误码
| 错误码 | 错误信息 | 解决方案 |
|-------|---------|----------|
| 10000 | 系统错误 | 联系系统管理员 |
| 10001 | 数据库错误 | 联系系统管理员 |
| 10002 | 缓存错误 | 联系系统管理员 |
| 10003 | 消息发送失败 | 检查网络连接或稍后再试 |
| 10004 | 接口调用失败 | 检查网络连接或稍后再试 |
| 10005 | 权限不足 | 联系管理员提升权限 |

---

### 3.8 安全注意事项

1. **认证要求**: 所有接口都需要有效的JWT令牌
2. **权限控制**: 不同角色具有不同的操作权限
3. **数据验证**: 所有输入参数都经过严格验证
4. **SQL注入防护**: 使用MyBatis Plus防止SQL注入
5. **XSS防护**: 前端对用户输入进行转义处理

### 3.9 性能优化建议

1. **缓存策略**: 分类列表可考虑使用Redis缓存
2. **分页查询**: 大量数据时建议使用分页查询
3. **索引优化**: 确保数据库表有合适的索引
4. **连接池**: 使用数据库连接池提高性能

### 3.10 版本历史

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|----------|--------|
| 1.0 | 2025-11-11 | 初始版本 | 系统管理员 |
| 1.1 | 2025-11-11 | 完善错误码和响应格式 | 系统管理员 |

**请求参数:**
- `categoryId` (路径参数): 分类ID

---

## 4. 标签管理模块

### 4.1 获取标签列表
**GET** `/api/tag/list`

#### 功能描述
获取系统中所有标签的列表信息，支持分页查询和条件筛选。

#### 业务场景
- 文章编辑页面选择标签
- 标签管理页面展示标签列表
- 前端标签云组件数据源
- 搜索功能中的标签筛选

#### 权限要求
- **角色要求**: 管理员、编辑、普通用户
- **权限级别**: 读取权限

#### 调用限制
- **频率限制**: 60次/分钟
- **数据量限制**: 单次请求最多返回100条记录

#### 请求参数
**查询参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| page | Integer | 否 | 1 | 最小值1 | 页码，从1开始 |
| size | Integer | 否 | 10 | 1-100 | 每页记录数 |
| keyword | String | 否 | null | 长度0-50字符 | 标签名称关键词搜索，支持模糊匹配 |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "name": "Java",
      "description": "Java编程语言相关技术",
      "color": "#FF6B35",
      "articleCount": 15
    },
    {
      "id": 2,
      "name": "Spring Boot",
      "description": "Spring Boot框架相关",
      "color": "#6B8E23",
      "articleCount": 8
    }
  ],
  "timestamp": 1762852809707,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Array | 标签列表数据 |
| data[].id | Long | 标签唯一标识 |
| data[].name | String | 标签名称 |
| data[].description | String | 标签描述信息 |
| data[].color | String | 标签颜色代码 |
| data[].articleCount | Long | 关联文章数量 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功（根据code自动计算） |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "获取标签列表失败",
  "data": null,
  "timestamp": 1762852809707,
}
```

---

### 4.2 获取标签详情
**GET** `/api/tag/{tagId}`

#### 功能描述
根据标签ID获取标签的详细信息。

#### 业务场景
- 标签管理页面查看标签详情
- 文章详情页面显示标签信息
- 标签编辑前的信息确认

#### 权限要求
- **角色要求**: 管理员、编辑、普通用户
- **权限级别**: 读取权限

#### 调用限制
- **频率限制**: 30次/分钟
- **数据量限制**: 无限制

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| tagId | Long | 是 | - | 标签唯一标识ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "name": "Java",
    "description": "Java编程语言相关技术",
    "color": "#FF6B35",
    "articleCount": 15
  },
  "timestamp": 1762852809707,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Object | 标签详情数据 |
| data.id | Long | 标签唯一标识 |
| data.name | String | 标签名称 |
| data.description | String | 标签描述信息 |
| data.color | String | 标签颜色代码 |
| data.articleCount | Long | 关联文章数量 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功（根据code自动计算） |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "标签不存在",
  "data": null,
  "timestamp": 1762852809707,
}
```

---

### 4.3 创建标签
**POST** `/api/tag` 🔒

#### 功能描述
在系统中创建新的标签。

#### 业务场景
- 后台管理系统添加新标签
- 文章发布时创建新标签
- 批量导入标签数据

#### 权限要求
- **角色要求**: 管理员、编辑
- **权限级别**: 写入权限

#### 调用限制
- **频率限制**: 20次/分钟
- **数据量限制**: 单次请求最大创建10个标签

#### 请求参数
**请求体 (application/json)**
```json
{
  "name": "Java",
  "description": "Java编程语言相关技术",
  "color": "#FF6B35"
}
```

**参数说明**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| name | String | 是 | - | 长度1-50字符 | 标签名称，不能为空 |
| description | String | 否 | null | 长度0-500字符 | 标签描述信息 |
| color | String | 否 | "#666666" | 长度0-7字符 | 标签颜色代码，格式为#RRGGBB |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 10,
  "timestamp": 1762852809707,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Long | 新创建的标签ID |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功（根据code自动计算） |

**错误响应 (HTTP 400)**
```json
{
  "code": 400,
  "message": "标签名称不能为空",
  "data": null,
  "timestamp": 1762852809707,
}
```

---

### 4.4 更新标签
**PUT** `/api/tag/{tagId}` 🔒

#### 功能描述
更新指定标签的详细信息。

#### 业务场景
- 后台管理系统编辑标签信息
- 标签信息纠错和优化
- 批量更新标签属性

#### 权限要求
- **角色要求**: 管理员、编辑
- **权限级别**: 写入权限

#### 调用限制
- **频率限制**: 30次/分钟
- **数据量限制**: 无限制

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| tagId | Long | 是 | - | 要更新的标签ID |

**请求体 (application/json)**
```json
{
  "name": "Java",
  "description": "Java编程语言相关技术",
  "color": "#FF6B35"
}
```

**参数说明**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| name | String | 是 | - | 长度1-50字符 | 标签名称，不能为空 |
| description | String | 否 | null | 长度0-500字符 | 标签描述信息 |
| color | String | 否 | "#666666" | 长度0-7字符 | 标签颜色代码，格式为#RRGGBB |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1762852809707,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | null | 无返回数据 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功（根据code自动计算） |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "标签不存在",
  "data": null,
  "timestamp": 1762852809707,
}
```

---

### 4.5 删除标签
**DELETE** `/api/tag/{tagId}` 🔒

#### 功能描述
删除指定的标签。注意：如果标签下存在文章，则无法删除。

#### 业务场景
- 后台管理系统删除无效标签
- 系统维护时清理废弃标签
- 标签合并时删除重复标签

#### 权限要求
- **角色要求**: 管理员
- **权限级别**: 删除权限

#### 调用限制
- **频率限制**: 10次/分钟
- **数据量限制**: 无限制

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| tagId | Long | 是 | - | 要删除的标签ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1762852809707,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | null | 无返回数据 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功（根据code自动计算） |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "标签下存在文章，无法删除",
  "data": null,
  "timestamp": 1762852809707,
}
```

---

### 4.6 获取标签文章数量
**GET** `/api/tag/{tagId}/count`

#### 功能描述
获取指定标签下的文章数量统计。

#### 业务场景
- 标签管理页面显示文章数量
- 标签导航菜单显示文章统计
- 系统统计报表生成

#### 权限要求
- **角色要求**: 管理员、编辑、普通用户
- **权限级别**: 读取权限

#### 调用限制
- **频率限制**: 60次/分钟
- **数据量限制**: 无限制

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| tagId | Long | 是 | - | 标签唯一标识ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 15,
  "timestamp": 1762852809707,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Integer | 标签下的文章数量 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功（根据code自动计算） |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "标签不存在",
  "data": null,
  "timestamp": 1762852809707,
}
```

---

### 4.7 批量添加标签
**POST** `/api/tag/batch` 🔒

#### 功能描述
批量添加多个标签到系统中。

#### 业务场景
- 批量导入标签数据
- 系统初始化时创建默认标签
- 文章批量发布时创建多个标签

#### 权限要求
- **角色要求**: 管理员、编辑
- **权限级别**: 写入权限

#### 调用限制
- **频率限制**: 5次/分钟
- **数据量限制**: 单次请求最多添加50个标签

#### 请求参数
**请求体 (application/json)**
```json
[
  {
    "name": "Java",
    "description": "Java编程语言相关技术",
    "color": "#FF6B35"
  },
  {
    "name": "Spring Boot",
    "description": "Spring Boot框架相关",
    "color": "#6B8E23"
  }
]
```

**参数说明**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| name | String | 是 | - | 长度1-30字符 | 标签名称，不能为空 |
| description | String | 否 | null | 长度0-500字符 | 标签描述信息 |
| color | String | 否 | "#666666" | 长度0-7字符 | 标签颜色代码，格式为#RRGGBB |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [10, 11],
  "timestamp": 1762852809707,
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|-------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Array | 新创建的标签ID列表 |
| timestamp | Long | 响应时间戳 |
| success | Boolean | 操作是否成功（根据code自动计算） |

**错误响应 (HTTP 400)**
```json
{
  "code": 400,
  "message": "标签名称不能为空",
  "data": null,
  "timestamp": 1762852809707,
}
```

---

### 4.8 错误码列表

标签管理模块使用以下错误码，所有错误码均遵循统一的响应格式：

| 错误码 | HTTP状态码 | 错误信息 | 触发场景 | 解决方案 |
|--------|------------|----------|----------|----------|
| 4001 | 500 | 标签不存在 | 查询、更新、删除不存在的标签ID | 检查标签ID是否正确，或重新创建标签 |
| 4002 | 500 | 标签下存在文章，无法删除 | 删除包含文章的标签 | 先移除标签下的所有文章，或使用标签合并功能 |
| 400 | 400 | 标签名称不能为空 | 创建或更新标签时名称为空 | 提供有效的标签名称 |
| 400 | 400 | 标签名称长度超过限制 | 标签名称超过50个字符 | 缩短标签名称至50字符以内 |
| 400 | 400 | 标签描述长度超过限制 | 标签描述超过500个字符 | 缩短标签描述至500字符以内 |
| 400 | 400 | 标签颜色格式错误 | 颜色格式不符合#RRGGBB格式 | 使用正确的十六进制颜色格式 |
| 400 | 400 | 标签名称已存在 | 创建或更新标签时名称重复 | 使用不同的标签名称 |
| 500 | 500 | 获取标签列表失败 | 数据库查询异常或服务异常 | 检查数据库连接，重试操作 |
| 500 | 500 | 添加标签失败 | 数据库插入异常或服务异常 | 检查标签名称是否重复，重试操作 |
| 500 | 500 | 更新标签失败 | 数据库更新异常或服务异常 | 检查标签是否存在，重试操作 |
| 500 | 500 | 删除标签失败 | 数据库删除异常或服务异常 | 检查标签是否被引用，重试操作 |
| 500 | 500 | 获取标签详情失败 | 数据库查询异常或服务异常 | 检查标签ID是否正确，重试操作 |
| 500 | 500 | 获取标签文章数量失败 | 数据库查询异常或服务异常 | 检查标签ID是否正确，重试操作 |
| 500 | 500 | 批量添加标签失败 | 数据库插入异常或服务异常 | 检查标签数据格式，重试操作 |

#### 错误处理最佳实践
1. **客户端处理**: 根据错误码显示友好的错误提示信息
2. **重试机制**: 对于500错误，建议实现指数退避重试策略
3. **数据验证**: 在客户端进行数据格式验证，减少400错误
4. **监控告警**: 监控500错误频率，及时发现系统异常

#### 安全注意事项
- 所有标签操作都需要身份验证和授权检查
- 标签名称和描述内容需要进行XSS过滤
- 批量操作需要限制数据量，防止DoS攻击
- 敏感操作需要记录审计日志

#### 性能优化建议
- 标签列表接口支持分页查询，避免大数据量传输
- 频繁访问的标签数据建议使用缓存
- 批量操作建议使用异步处理
- 标签统计信息可以预计算存储

---

### 4.8 文档修正说明

#### 最新修正 (2025-11-11)
1. **标签名称长度限制修正**:
   - 原文档: 1-50字符
   - 修正为: 1-50字符 (与TagCreateDTO验证注解一致)
   - 位置: 4.1、4.3、4.4节的参数说明表

2. **字段说明修正**:
   - 删除status字段说明: 标签状态，1-正常，2-禁用（TagDTO中不包含该字段）
   - 删除deleted字段说明: 逻辑删除标识，0-未删除，1-已删除（TagDTO中不包含该字段）
   - 位置: 4.1节响应字段说明表

#### 修正原因
- 确保文档与数据库表结构一致，避免运行时错误
- 提供完整的数据模型说明，便于开发者理解
- 提高接口文档的准确性和实用性

---

## 5. 评论管理模块

### 5.1 发表评论
**POST** `/api/comment` 🔒

#### 功能描述
用户发表新评论，支持对文章或回复进行评论。系统会自动处理父评论关系，实现评论层级结构。

#### 业务场景
- 文章详情页面发表评论
- 回复其他用户评论
- 未登录用户通过昵称和邮箱发表评论
- 防止恶意刷评论保护机制

#### 权限要求
- **角色要求**: 所有用户（游客、普通用户、管理员）
- **权限级别**: 写入权限（需登录认证）
- **认证要求**: 需要JWT Token认证

#### 调用限制
- **频率限制**: 30次/小时/IP
- **数据量限制**: 单条评论内容最大1000字符
- **时间间隔限制**: 同一IP连续发表评论需间隔至少30秒
- **内容过滤**: 启用敏感词过滤机制，过滤违规内容

#### 接口版本控制
- **当前版本**: v1
- **API路径**: `/api/v1/comment` (未来版本)

#### 请求参数
**请求体 (application/json)**
```json
{
  "articleId": 1,
  "parentId": 0,
  "content": "这是评论内容",
  "nickname": "游客",
  "email": "visitor@example.com",
  "website": "https://example.com"
}
```

**参数说明**
| 参数名 | 类型 | 位置 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|------|--------|------|------|
| articleId | Long | Body | 是 | - | > 0 | 评论所属文章ID |
| parentId | Long | Body | 否 | 0 | ≥ 0 | 父评论ID，0表示顶级评论 |
| content | String | Body | 是 | - | 长度1-1000字符 | 评论内容，不能为空 |
| nickname | String | Body | 否 | null | 长度0-50字符 | 游客模式时的昵称 |
| email | String | Body | 否 | null | 长度0-100字符 | 游客模式时的邮箱 |
| website | String | Body | 否 | null | 长度0-200字符 | 游客模式时的个人网站 |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 15,
  "timestamp": 1762852809707
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Long | 新创建评论的ID |
| timestamp | Long | 响应时间戳 |

**错误响应 (HTTP 400)**
```json
{
  "code": 400,
  "message": "评论内容不能为空",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "发表评论失败",
  "data": null,
  "timestamp": 1762852809707
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 400 | 评论内容不能为空 | 提供有效的评论内容 |
| 400 | 评论内容不能超过1000个字符 | 缩短评论内容至1000字符以内 |
| 400 | 昵称不能超过50个字符 | 缩短昵称至50字符以内 |
| 400 | 邮箱不能超过100个字符 | 缩短邮箱至100字符以内 |
| 400 | 网站地址不能超过200个字符 | 缩短网站地址至200字符以内 |
| 401 | 未认证 | 检查JWT令牌是否有效 |
| 403 | 无权限访问 | 检查用户角色和权限 |
| 500 | 发表评论失败 | 检查数据库连接，重试操作 |

#### 业务规则说明
1. **评论审核流程**
   - 新评论默认状态为"待审核"（status=1）
   - 审核通过后状态变为"已通过"（status=2）
   - 管理员可在后台审核评论

2. **评论层级结构**
   - 支持无限层级评论（理论上）
   - parentId=0表示顶级评论
   - 其他值表示回复该评论ID

3. **游客模式支持**
   - 未登录用户可使用昵称、邮箱发表评论
   - 系统会临时保存用户信息
   - 下次登录可关联评论到用户账户

#### 调用示例
```bash
# 已登录用户发表评论
curl -X POST "http://localhost:8080/api/comment" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "articleId": 1,
    "parentId": 0,
    "content": "这是一条很棒的文章！"
  }'

# 游客模式发表评论
curl -X POST "http://localhost:8080/api/comment" \
  -H "Content-Type: application/json" \
  -d '{
    "articleId": 1,
    "parentId": 0,
    "content": "感谢分享，很有帮助！",
    "nickname": "访客用户",
    "email": "visitor@example.com",
    "website": "https://visitor.com"
  }'

# 回复评论
curl -X POST "http://localhost:8080/api/comment" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "articleId": 1,
    "parentId": 5,
    "content": "回复楼上：你说得对，我也这么认为。"
  }'
```

#### 前端集成示例
```javascript
// 发表评论
async function createComment(commentData, token = null) {
  const headers = {
    'Content-Type': 'application/json'
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch('/api/comment', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(commentData)
    });

    const result = await response.json();

    if (result.code === 200) {
      console.log('评论发表成功，评论ID:', result.data);
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('发表评论失败:', error);
    throw error;
  }
}

// 使用示例
createComment({
  articleId: 1,
  parentId: 0,
  content: "这是一个测试评论",
  nickname: "测试用户",
  email: "test@example.com"
}).then(commentId => {
  console.log('评论发表成功:', commentId);
}).catch(error => {
  console.error('错误:', error);
});
```

### 5.2 获取评论列表
**GET** `/api/comment/list`

#### 功能描述
获取指定文章的评论列表，支持分页查询和评论状态筛选。返回树形结构的评论列表，包括顶级评论及其子评论。

#### 业务场景
- 文章详情页面显示评论列表
- 文章评论分页加载
- 评论审核页面筛选评论
- 评论回复展示层级结构

#### 权限要求
- **角色要求**: 所有用户（游客、普通用户、管理员）
- **权限级别**: 读取权限
- **认证要求**: 无需认证（公开访问）

#### 调用限制
- **频率限制**: 60次/分钟/IP
- **数据量限制**: 单页最多返回100条记录
- **缓存策略**: 评论列表缓存5分钟，提高访问性能
- **内容过滤**: 过滤敏感内容，只显示已通过审核的评论

#### 接口版本控制
- **当前版本**: v1
- **API路径**: `/api/v1/comment/list` (未来版本)

#### 请求参数（更新）
**查询参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| articleId | Long | 是 | - | > 0 | 文章唯一标识ID |
| page | Integer | 否 | 1 | ≥ 1 | 页码，从1开始 |
| size | Integer | 否 | 10 | 1-100 | 每页记录数，最大100 |
| status | Integer | 否 | 2 | 1,2,3,4 | 评论状态：1-待审核，2-已通过，3-已拒绝，4-已删除 |
| sort | String | 否 | create_time | 仅支持`create_time` | 排序字段（仅支持固定字段） |
| order | String | 否 | asc | 仅支持`asc` | 排序方向（仅支持固定方向） |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "articleId": 1,
      "parentId": 0,
      "content": "这是顶级评论内容",
      "userId": 2,
      "nickname": "用户昵称",
      "email": null,
      "website": null,
      "avatar": "https://example.com/avatar.jpg",
      "status": 2,
      "likeCount": 5,
      "replyCount": 2,
      "createTime": "2024-01-01T10:30:00",
      "updateTime": "2024-01-01T10:30:00",
      "children": [
        {
          "id": 2,
          "articleId": 1,
          "parentId": 1,
          "content": "这是回复内容",
          "userId": 3,
          "nickname": "回复用户",
          "email": null,
          "website": null,
          "avatar": "https://example.com/avatar2.jpg",
          "status": 2,
          "likeCount": 2,
          "replyCount": 0,
          "createTime": "2024-01-01T11:30:00",
          "updateTime": "2024-01-01T11:30:00",
          "children": []
        }
      ]
    }
  ],
  "timestamp": 1762852809707
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Array | 评论列表数组（顶级评论） |
| data[].id | Long | 评论唯一标识ID |
| data[].articleId | Long | 所属文章ID |
| data[].parentId | Long | 父评论ID，0表示顶级评论 |
| data[].content | String | 评论内容 |
| data[].userId | Long | 评论者用户ID |
| data[].nickname | String | 评论者昵称 |
| data[].email | String | 评论者邮箱 |
| data[].website | String | 评论者个人网站 |
| data[].avatar | String | 评论者头像URL |
| data[].status | Integer | 评论状态：1-待审核，2-已通过，3-已拒绝 |
| data[].likeCount | Integer | 点赞数量 |
| data[].replyCount | Integer | 回复数量 |
| data[].createTime | String | 创建时间（ISO 8601格式） |
| data[].updateTime | String | 更新时间（ISO 8601格式） |
| data[].children | Array | 子评论列表（递归结构） |
| data[].children[].* | * | 子评论对象，结构与父评论相同 |
| timestamp | Long | 响应时间戳 |

**错误响应 (HTTP 400)**
```json
{
  "code": 400,
  "message": "参数验证失败",
  "data": {
    "errors": [
      {
        "field": "articleId",
        "message": "文章ID不能为空"
      }
    ]
  },
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "获取评论列表失败",
  "data": null,
  "timestamp": 1762852809707
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 400 | 文章ID不能为空 | 提供有效的文章ID |
| 400 | 页码必须大于0 | 使用大于0的页码值 |
| 400 | 每页数量必须在1-100之间 | 将每页数量设置在1-100范围内 |
| 500 | 获取评论列表失败 | 检查数据库连接，重试操作 |

#### 业务规则说明
1. **评论状态控制**
   - 默认只返回已通过审核的评论（status=2）
   - 只有登录用户才能看到待审核评论（status=1）
   - 管理员可以看到所有状态的评论
2. **排序支持说明**
   - 当前仅支持按`create_time`升序（`asc`）排列；传入其他排序字段或方向将返回错误提示：
     - 暂不支持该排序字段：`{sort}`，仅支持：`create_time`
     - 暂不支持该排序方向：`{order}`，仅支持：`asc`

2. **树形结构实现**
   - 返回顶级评论（parentId=0或null）
   - 每个顶级评论包含其所有子评论
   - 子评论按时间顺序排列

3. **数据过滤规则**
   - 仅返回非删除状态的评论
   - 隐藏被标记为拒绝的评论
   - 支持按状态筛选

#### 调用示例
```bash
# 获取文章评论列表（默认参数）
curl -X GET "http://localhost:8080/api/comment/list?articleId=1"

# 获取指定页码的评论列表
curl -X GET "http://localhost:8080/api/comment/list?articleId=1&page=2&size=20"

# 获取特定状态的评论
curl -X GET "http://localhost:8080/api/comment/list?articleId=1&status=2"

# 获取文章的所有评论
curl -X GET "http://localhost:8080/api/comment/list?articleId=1&page=1&size=100"
```

#### 前端集成示例
```javascript
// 获取评论列表
async function getCommentList(articleId, page = 1, size = 10, status = 2, token = null) {
  const params = new URLSearchParams({
    articleId,
    page,
    size
  });

  if (status) {
    params.append('status', status);
  }

  const headers = {
    'Content-Type': 'application/json'
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(`/api/comment/list?${params}`, {
      method: 'GET',
      headers: headers
    });

    const result = await response.json();

    if (result.code === 200) {
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('获取评论列表失败:', error);
    throw error;
  }
}

// 递归渲染评论树
function renderCommentTree(comments, level = 0) {
  const indentation = '  '.repeat(level);

  return comments.map(comment => {
    let commentHTML = `${indentation}<div class="comment-item" data-id="${comment.id}">`;
    commentHTML += `${indentation}  <div class="comment-header">`;
    commentHTML += `${indentation}    <img src="${comment.avatar}" alt="头像" class="avatar">`;
    commentHTML += `${indentation}    <span class="nickname">${comment.nickname}</span>`;
    commentHTML += `${indentation}    <span class="time">${comment.createTime}</span>`;
    commentHTML += `${indentation}  </div>`;
    commentHTML += `${indentation}  <div class="comment-content">${comment.content}</div>`;
    commentHTML += `${indentation}  <div class="comment-actions">`;
    commentHTML += `${indentation}    <button onclick="likeComment(${comment.id})">赞(${comment.likeCount})</button>`;
    commentHTML += `${indentation}    <button onclick="replyToComment(${comment.id})">回复</button>`;
    commentHTML += `${indentation}  </div>`;

    if (comment.children && comment.children.length > 0) {
      commentHTML += `${indentation}  <div class="comment-children">`;
      commentHTML += renderCommentTree(comment.children, level + 1);
      commentHTML += `${indentation}  </div>`;
    }

    commentHTML += `${indentation}</div>`;

    return commentHTML;
  }).join('\n');
}

// 使用示例
getCommentList(1, 1, 10, 2)
  .then(comments => {
    const commentHTML = renderCommentTree(comments);
    document.getElementById('comments-container').innerHTML = commentHTML;
  })
  .catch(error => {
    console.error('错误:', error);
  });
```

### 5.3 获取评论详情
**GET** `/api/comment/{commentId}`

#### 功能描述
根据评论ID获取指定评论的详细信息，包括评论内容、作者信息、状态等。

#### 业务场景
- 管理员审核评论详情
- 评论内容验证和展示
- 评论状态查询
- 评论引用和回复功能

#### 权限要求
- **角色要求**: 所有用户（游客、普通用户、管理员）
- **权限级别**: 读取权限
- **认证要求**: 需要JWT Token认证

#### 调用限制
- **频率限制**: 100次/分钟/IP
- **数据量限制**: 单次返回单条评论数据
- **缓存策略**: 评论详情缓存10分钟
- **访问统计**: 每次访问会记录日志

#### 接口版本控制
- **当前版本**: v1
- **API路径**: `/api/v1/comment/{commentId}` (未来版本)

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| commentId | Long | 是 | - | > 0 | 评论唯一标识ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "articleId": 1,
    "parentId": 0,
    "content": "评论内容",
    "userId": 2,
    "nickname": "用户名",
    "email": "user@example.com",
    "website": "https://example.com",
    "avatar": "https://example.com/avatar.jpg",
    "status": 2,
    "likeCount": 5,
    "replyCount": 2,
    "createTime": "2024-01-01T10:30:00",
    "updateTime": "2024-01-01T10:30:00",
    "children": []
  },
  "timestamp": 1762852809707
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Object | 评论详细信息对象 |
| data.id | Long | 评论唯一标识ID |
| data.articleId | Long | 所属文章ID |
| data.parentId | Long | 父评论ID，0表示顶级评论 |
| data.content | String | 评论内容 |
| data.userId | Long | 评论者用户ID |
| data.nickname | String | 评论者昵称 |
| data.email | String | 评论者邮箱 |
| data.website | String | 评论者个人网站 |
| data.avatar | String | 评论者头像URL |
| data.status | Integer | 评论状态：1-待审核，2-已通过，3-已拒绝，4-已删除 |
| data.likeCount | Integer | 点赞数量 |
| data.replyCount | Integer | 回复数量 |
| data.createTime | String | 创建时间（ISO 8601格式） |
| data.updateTime | String | 更新时间（ISO 8601格式） |
| data.children | Array | 子评论列表 |
| timestamp | Long | 响应时间戳 |

**错误响应 (HTTP 404)**
```json
{
  "code": 2004,
  "message": "评论不存在",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "获取评论详情失败",
  "data": null,
  "timestamp": 1762852809707
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 2004 | 评论不存在 | 检查评论ID是否正确 |
| 400 | 评论ID格式错误 | 检查评论ID格式是否为数字 |
| 500 | 获取评论详情失败 | 检查数据库连接，重试操作 |

#### 业务规则说明
1. **权限控制**
   - 未登录用户只能查看已通过审核的评论（status=2）
   - 登录用户可以查看自己的评论（包括待审核）
   - 管理员可以查看所有评论

2. **数据过滤**
   - 返回的评论数据会过滤敏感内容
   - 隐藏被删除的评论
   - 根据用户权限返回相应状态的评论

3. **关联信息**
   - 返回评论作者的基本信息
   - 包含评论的点赞和回复统计
   - 提供评论层级关系信息

#### 调用示例
```bash
# 获取评论详情
curl -X GET "http://localhost:8080/api/comment/1"

# 带认证头获取评论详情
curl -X GET "http://localhost:8080/api/comment/1" \
  -H "Authorization: Bearer {token}"
```

#### 前端集成示例
```javascript
// 获取评论详情
async function getCommentDetail(commentId, token = null) {
  const headers = {
    'Content-Type': 'application/json'
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(`/api/comment/${commentId}`, {
      method: 'GET',
      headers: headers
    });

    const result = await response.json();

    if (result.code === 200) {
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('获取评论详情失败:', error);
    throw error;
  }
}

// 使用示例
getCommentDetail(1)
  .then(comment => {
    console.log('评论详情:', comment);
    // 渲染评论详情
    renderCommentDetail(comment);
  })
  .catch(error => {
    console.error('错误:', error);
  });
}
```

### 5.4 删除评论
**DELETE** `/api/comment/{commentId}` 🔒

#### 功能描述
根据评论ID删除指定评论，支持物理删除和逻辑删除。只有评论作者或管理员有权删除评论。

#### 业务场景
- 用户删除自己的评论
- 管理员删除违规评论
- 文章作者删除不当评论
- 游客删除自己的评论（需要验证）

#### 权限要求
- **角色要求**: 评论作者、管理员
- **权限级别**: 删除权限
- **认证要求**: 需要JWT Token认证
- **权限验证**: 普通用户只能删除自己的评论，管理员可以删除任何评论

#### 调用限制
- **频率限制**: 10次/小时/用户
- **数据量限制**: 单次删除一条评论
- **操作验证**: 删除前需要权限验证
- **安全机制**: 防止恶意删除操作

#### 接口版本控制
- **当前版本**: v1
- **API路径**: `/api/v1/comment/{commentId}` (未来版本)

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| commentId | Long | 是 | - | > 0 | 评论唯一标识ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1762852809707
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | null | 无返回数据 |
| timestamp | Long | 响应时间戳 |

**错误响应 (HTTP 404)**
```json
{
  "code": 2004,
  "message": "评论不存在",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 403)**
```json
{
  "code": 403,
  "message": "无权限删除此评论",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "删除评论失败",
  "data": null,
  "timestamp": 1762852809707
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 2004 | 评论不存在 | 检查评论ID是否正确 |
| 400 | 评论ID格式错误 | 检查评论ID格式是否为数字 |
| 401 | 未认证 | 检查JWT令牌是否有效 |
| 403 | 无权限删除此评论 | 只能删除自己的评论或需要管理员权限 |
| 500 | 删除评论失败 | 检查数据库连接，重试操作 |

#### 业务规则说明
1. **权限控制**
   - 普通用户只能删除自己发表的评论
   - 管理员可以删除任何评论
   - 评论删除后，其子评论也会被删除
   - 已删除的评论不可恢复

2. **关联数据处理**
   - 删除评论后会更新文章的评论统计
   - 评论的点赞记录会被同时删除
   - 系统会记录删除操作日志

3. **安全验证**
   - 验证用户身份和权限
   - 防止并发删除操作
   - 验证评论状态（已删除的评论不能再次删除）

#### 调用示例
```bash
# 删除评论
curl -X DELETE "http://localhost:8080/api/comment/1" \
  -H "Authorization: Bearer {token}"
```

#### 前端集成示例
```javascript
// 删除评论
async function deleteComment(commentId, token) {
  if (!token) {
    throw new Error('未登录，无法删除评论');
  }

  try {
    const response = await fetch(`/api/comment/${commentId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    const result = await response.json();

    if (result.code === 200) {
      console.log('评论删除成功');
      // 从页面移除评论元素
      removeCommentElement(commentId);
      return true;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('删除评论失败:', error);
    throw error;
  }
}

// 使用示例
deleteComment(1, userToken)
  .then(success => {
    if (success) {
      console.log('评论删除成功');
    }
  })
  .catch(error => {
    console.error('错误:', error.message);
    alert('删除评论失败: ' + error.message);
  });
}
```

### 5.5 审核评论
**PUT** `/api/comment/{commentId}/review` 🔒

#### 功能描述
审核评论状态，支持将评论设置为通过或拒绝状态。只有管理员有权执行此操作。

#### 业务场景
- 管理员审核待审核的评论
- 拒绝违规评论
- 通过合规评论
- 评论内容质量控制

#### 权限要求
- **角色要求**: 管理员、超级管理员
- **权限级别**: 管理权限
- **认证要求**: 需要JWT Token认证
- **权限验证**: 只有管理员及以上角色可操作

#### 调用限制
- **频率限制**: 100次/小时/管理员
- **数据量限制**: 单次审核一条评论
- **操作验证**: 审核前需要验证管理员权限
- **安全机制**: 防止恶意审核操作

#### 接口版本控制
- **当前版本**: v1
- **API路径**: `/api/v1/comment/{commentId}/review` (未来版本)

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| commentId | Long | 是 | - | > 0 | 评论唯一标识ID |

**查询参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| status | Integer | 是 | - | 1,2,3 | 评论状态：1-待审核，2-已通过，3-已拒绝 |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1762852809707
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | null | 无返回数据 |
| timestamp | Long | 响应时间戳 |

**错误响应 (HTTP 404)**
```json
{
  "code": 2004,
  "message": "评论不存在",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 403)**
```json
{
  "code": 403,
  "message": "无权限审核此评论",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "审核评论失败",
  "data": null,
  "timestamp": 1762852809707
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 2004 | 评论不存在 | 检查评论ID是否正确 |
| 400 | 评论ID格式错误 | 检查评论ID格式是否为数字 |
| 400 | 评论状态值无效 | 状态值建议为1(待审核)、2(已通过)或3(已拒绝)，但目前系统未对状态值进行严格验证 |
| 401 | 未认证 | 检查JWT令牌是否有效 |
| 403 | 无权限审核此评论 | 需要管理员权限 |
| 500 | 审核评论失败 | 检查数据库连接，重试操作 |

#### 业务规则说明
1. **权限控制**
   - 只有管理员及以上角色可以审核评论
   - 不能审核自己的评论（防止自审）
   - 审核操作会记录审核员信息

2. **状态流转**
   - 评论状态可以从任意状态切换到任意状态（系统目前未对状态值进行严格验证，允许设置非标准状态值）
   - 待审核评论(1) → 已通过(2)：评论变为可见
   - 待审核评论(1) → 已拒绝(3)：评论变为不可见
   - 已通过评论(2) → 已拒绝(3)：评论变为不可见
   - 已拒绝评论(3) → 已通过(2)：评论重新变为可见

3. **数据处理**
   - 评论通过后会更新文章的评论统计
   - 审核操作会更新评论的更新时间
   - 系统会记录审核操作日志

#### 调用示例
```bash
# 审核评论为已通过
curl -X PUT "http://localhost:8080/api/comment/1/review?status=2" \
  -H "Authorization: Bearer {token}"

# 审核评论为已拒绝
curl -X PUT "http://localhost:8080/api/comment/1/review?status=3" \
  -H "Authorization: Bearer {token}"

# 审核评论为待审核（重新审核）
curl -X PUT "http://localhost:8080/api/comment/1/review?status=1" \
  -H "Authorization: Bearer {token}"
```

#### 前端集成示例
```javascript
// 审核评论
async function reviewComment(commentId, status, token) {
  if (!token) {
    throw new Error('未登录，无法审核评论');
  }

  try {
    const response = await fetch(`/api/comment/${commentId}/review?status=${status}`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    const result = await response.json();

    if (result.code === 200) {
      console.log('评论审核成功');
      // 更新页面上的评论状态
      updateCommentStatus(commentId, status);
      return true;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('审核评论失败:', error);
    throw error;
  }
}

// 使用示例
reviewComment(1, 2, adminToken) // 审核通过
  .then(success => {
    if (success) {
      console.log('评论审核通过');
    }
  })
  .catch(error => {
    console.error('错误:', error.message);
    alert('审核评论失败: ' + error.message);
  });

reviewComment(2, 3, adminToken) // 拒绝评论
  .then(success => {
    if (success) {
      console.log('评论已拒绝');
    }
  })
  .catch(error => {
    console.error('错误:', error.message);
    alert('审核评论失败: ' + error.message);
  });
}
```

### 5.6 获取文章评论数量
**GET** `/api/comment/article/{articleId}/count`

#### 功能描述
获取指定文章的评论总数量，用于在文章列表页面显示评论数统计。

#### 业务场景
- 文章列表页面显示评论数量
- 文章详情页面显示评论统计
- 热门文章排行统计
- 文章内容管理统计

#### 权限要求
- **角色要求**: 所有用户（游客、普通用户、管理员）
- **权限级别**: 读取权限
- **认证要求**: 需要JWT Token认证

#### 调用限制
- **频率限制**: 100次/分钟/IP
- **数据量限制**: 单次返回单一整数值
- **缓存策略**: 评论数量缓存10分钟，提高访问性能
- **统计规则**: 统计所有未删除的评论（包括待审核、已通过、已拒绝状态）

#### 接口版本控制
- **当前版本**: v1
- **API路径**: `/api/v1/comment/article/{articleId}/count` (未来版本)

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| articleId | Long | 是 | - | > 0 | 文章唯一标识ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": 25,
  "timestamp": 1762852809707
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Integer | 评论数量 |
| timestamp | Long | 响应时间戳 |

**错误响应 (HTTP 400)**
```json
{
  "code": 400,
  "message": "文章ID格式错误",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "获取评论数量失败",
  "data": null,
  "timestamp": 1762852809707
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 400 | 文章ID格式错误 | 检查文章ID格式是否为数字 |
| 500 | 获取评论数量失败 | 检查数据库连接，重试操作 |

#### 业务规则说明
1. **统计规则**
   - 只统计已通过审核的评论（status=2）
   - 不统计被删除的评论（逻辑删除）
   - 包括顶级评论和子评论

2. **性能优化**
   - 使用数据库聚合函数COUNT进行统计
   - 启用Redis缓存提高访问性能
   - 缓存10分钟后自动更新

3. **数据一致性**
   - 评论发表时同步更新文章评论数量
   - 评论删除时同步更新文章评论数量
   - 评论状态变化时同步更新统计

#### 调用示例
```bash
# 获取文章评论数量
curl -X GET "http://localhost:8080/api/comment/article/1/count"
```

#### 前端集成示例
```javascript
// 获取文章评论数量
async function getArticleCommentCount(articleId) {
  try {
    const response = await fetch(`/api/comment/article/${articleId}/count`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    });

    const result = await response.json();

    if (result.code === 200) {
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('获取评论数量失败:', error);
    throw error;
  }
}

// 使用示例
getArticleCommentCount(1)
  .then(count => {
    console.log('文章评论数量:', count);
    // 更新页面显示
    document.getElementById('comment-count').textContent = count;
  })
  .catch(error => {
    console.error('错误:', error);
  });

// 批量获取多篇文章的评论数量
async function getMultipleArticleCommentCounts(articleIds) {
  const counts = {};

  for (const articleId of articleIds) {
    try {
      const count = await getArticleCommentCount(articleId);
      counts[articleId] = count;
    } catch (error) {
      console.error(`获取文章${articleId}评论数量失败:`, error);
      counts[articleId] = 0;
    }
  }

  return counts;
}
```

### 5.7 获取用户评论列表
**GET** `/api/comment/user/{userId}`

#### 功能描述
获取指定用户的评论列表，支持分页查询。返回用户发表的所有评论，包括已通过和待审核的评论。

#### 业务场景
- 用户个人中心显示评论历史
- 用户评论管理页面
- 评论作者文章关联显示

#### 权限要求
- **角色要求**: 所有用户（游客、普通用户、管理员）
- **权限级别**: 读取权限
- **认证要求**: 无需认证（公开访问）
- **隐私控制**: 普通用户可查看任意用户评论，敏感信息根据权限显示

#### 调用限制
- **频率限制**: 50次/分钟/IP
- **数据量限制**: 单页最多返回50条记录
- **缓存策略**: 用户评论列表缓存5分钟
- **分页要求**: 必须使用分页参数防止数据量过大

#### 接口版本控制
- **当前版本**: v1
- **API路径**: `/api/v1/comment/user/{userId}` (未来版本)

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| userId | Long | 是 | - | > 0 | 用户唯一标识ID |

**查询参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| page | Integer | 否 | 1 | ≥ 1 | 页码，从1开始 |
| size | Integer | 否 | 10 | 1-50 | 每页记录数，最大50 |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "articleId": 1,
      "parentId": 0,
      "content": "这是用户发表的评论内容",
      "nickname": "用户名",
      "email": null,
      "website": null,
      "avatar": "https://example.com/avatar.jpg",
      "status": 2,
      "likeCount": 5,
      "replyCount": 2,
      "createTime": "2024-01-01T10:30:00",
      "updateTime": "2024-01-01T10:30:00",
      "article": {
        "id": 1,
        "title": "文章标题",
        "author": "文章作者"
      }
    }
  ],
  "timestamp": 1762852809707
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Array | 用户评论列表数组 |
| data[].id | Long | 评论唯一标识ID |
| data[].articleId | Long | 所属文章ID |
| data[].parentId | Long | 父评论ID，0表示顶级评论 |
| data[].content | String | 评论内容 |
| data[].nickname | String | 评论者昵称 |
| data[].email | String | 评论者邮箱（根据权限显示） |
| data[].website | String | 评论者个人网站（根据权限显示） |
| data[].avatar | String | 评论者头像URL |
| data[].status | Integer | 评论状态：1-待审核，2-已通过，3-已拒绝 |
| data[].likeCount | Integer | 点赞数量 |
| data[].replyCount | Integer | 回复数量 |
| data[].createTime | String | 创建时间（ISO 8601格式） |
| data[].updateTime | String | 更新时间（ISO 8601格式） |
| data[].article | Object | 评论所属文章信息 |
| data[].article.id | Long | 文章ID |
| data[].article.title | String | 文章标题 |
| data[].article.author | String | 文章作者 |
| timestamp | Long | 响应时间戳 |

**错误响应 (HTTP 400)**
```json
{
  "code": 400,
  "message": "用户ID格式错误",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "获取用户评论列表失败",
  "data": null,
  "timestamp": 1762852809707
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 400 | 用户ID格式错误 | 检查用户ID格式是否为数字 |
| 400 | 页码必须大于0 | 使用大于0的页码值 |
| 400 | 每页数量必须在1-50之间 | 将每页数量设置在1-50范围内 |
| 500 | 获取用户评论列表失败 | 检查数据库连接，重试操作 |

#### 业务规则说明
1. **权限控制**
   - 未登录用户只能看到已通过的评论（status=2）
   - 登录用户可以看到自己的所有评论
   - 管理员可以看到所有评论

2. **数据过滤**
   - 根据用户权限过滤评论状态
   - 不返回已删除的评论
   - 可选返回关联文章信息

3. **隐私保护**
   - 游客评论的邮箱和网站信息根据权限显示
   - 敏感信息进行脱敏处理
   - 支持分页加载防止数据量过大

#### 调用示例
```bash
# 获取用户评论列表（默认参数）
curl -X GET "http://localhost:8080/api/comment/user/1"

# 获取指定页码的用户评论列表
curl -X GET "http://localhost:8080/api/comment/user/1?page=2&size=20"

# 获取用户的所有评论
curl -X GET "http://localhost:8080/api/comment/user/1?page=1&size=50"
```

#### 前端集成示例
```javascript
// 获取用户评论列表
async function getUserCommentList(userId, page = 1, size = 10, token = null) {
  const params = new URLSearchParams({
    page,
    size
  });

  const headers = {
    'Content-Type': 'application/json'
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(`/api/comment/user/${userId}?${params}`, {
      method: 'GET',
      headers: headers
    });

    const result = await response.json();

    if (result.code === 200) {
      return result.data;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('获取用户评论列表失败:', error);
    throw error;
  }
}

// 渲染用户评论列表
function renderUserComments(comments) {
  return comments.map(comment => {
    return `
      <div class="user-comment-item" data-id="${comment.id}">
        <div class="comment-header">
          <span class="comment-date">${comment.createTime}</span>
          <span class="comment-status status-${comment.status}">
            ${getStatusText(comment.status)}
          </span>
        </div>
        <div class="comment-content">
          <p>评论于: <a href="/article/${comment.articleId}">${comment.article.title}</a></p>
          <p>内容: ${comment.content}</p>
        </div>
        <div class="comment-stats">
          <span>点赞: ${comment.likeCount}</span>
          <span>回复: ${comment.replyCount}</span>
        </div>
      </div>
    `;
  }).join('');
}

// 状态文本转换
function getStatusText(status) {
  const statusMap = {
    1: '待审核',
    2: '已通过',
    3: '已拒绝'
  };
  return statusMap[status] || '未知';
}

// 使用示例
getUserCommentList(1, 1, 10, userToken)
  .then(comments => {
    const commentsHTML = renderUserComments(comments);
    document.getElementById('user-comments-list').innerHTML = commentsHTML;
  })
  .catch(error => {
    console.error('错误:', error);
  });
}
```

### 5.8 评论点赞
**POST** `/api/comment/{commentId}/like` 🔒

#### 功能描述
为指定评论添加点赞，每个用户对同一条评论只能点赞一次。系统会自动更新评论的点赞数量统计。

#### 业务场景
- 用户对喜欢的评论进行点赞
- 评论热度排名统计
- 优质评论推荐
- 评论互动功能

#### 权限要求
- **角色要求**: 登录用户
- **权限级别**: 写入权限
- **认证要求**: 需要JWT Token认证
- **权限验证**: 需要用户登录才能点赞

#### 调用限制
- **频率限制**: 10次/分钟/用户
- **数据量限制**: 单次点赞一条评论
- **重复限制**: 每个用户对同一条评论只能点赞一次
- **时间间隔**: 点赞操作需间隔至少5秒

#### 接口版本控制
- **当前版本**: v1
- **API路径**: `/api/v1/comment/{commentId}/like` (未来版本)

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| commentId | Long | 是 | - | > 0 | 评论唯一标识ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1762852809707
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | null | 无返回数据 |
| timestamp | Long | 响应时间戳 |

**错误响应 (HTTP 404)**
```json
{
  "code": 2004,
  "message": "评论不存在",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 400)**
```json
{
  "code": 400,
  "message": "已点赞，请勿重复点赞",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "评论点赞失败",
  "data": null,
  "timestamp": 1762852809707
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 2004 | 评论不存在 | 检查评论ID是否正确 |
| 400 | 评论ID格式错误 | 检查评论ID格式是否为数字 |
| 400 | 已点赞，请勿重复点赞 | 该评论已点赞，无法重复点赞 |
| 401 | 未认证 | 检查JWT令牌是否有效 |
| 500 | 评论点赞失败 | 检查数据库连接，重试操作 |

#### 业务规则说明
1. **防重复机制**
   - 每个用户对同一条评论只能点赞一次
   - 点赞后状态持久化，防止重复操作
   - 点赞记录与用户ID关联

2. **数据一致性**
   - 点赞成功后立即更新评论的点赞数量
   - 评论点赞数不会出现负数
   - 数据库操作使用事务保证一致性

3. **权限控制**
   - 需要用户登录认证
   - 不允许点赞自己的评论（可选配置）
   - 验证评论状态（只能点赞已通过审核的评论）

#### 调用示例
```bash
# 评论点赞
curl -X POST "http://localhost:8080/api/comment/1/like" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json"
```

#### 前端集成示例
```javascript
// 评论点赞
async function likeComment(commentId, token) {
  if (!token) {
    throw new Error('未登录，无法点赞评论');
  }

  try {
    const response = await fetch(`/api/comment/${commentId}/like`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      }
    });

    const result = await response.json();

    if (result.code === 200) {
      console.log('评论点赞成功');
      // 更新页面上的点赞数
      updateCommentLikeCount(commentId, true);
      return true;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('评论点赞失败:', error);
    throw error;
  }
}

// 批量点赞处理
async function batchLikeComments(commentIds, token) {
  const results = {};

  for (const commentId of commentIds) {
    try {
      const success = await likeComment(commentId, token);
      results[commentId] = success;
    } catch (error) {
      console.error(`点赞评论${commentId}失败:`, error);
      results[commentId] = false;
    }
  }

  return results;
}

// 使用示例
likeComment(1, userToken)
  .then(success => {
    if (success) {
      console.log('评论点赞成功');
    }
  })
  .catch(error => {
    console.error('错误:', error.message);
    if (error.message.includes('已点赞')) {
      alert('您已点赞过此评论');
    } else {
      alert('点赞失败: ' + error.message);
    }
  });
}
```

### 5.9 取消评论点赞
**DELETE** `/api/comment/{commentId}/like` 🔒

#### 功能描述
取消对指定评论的点赞，系统会自动更新评论的点赞数量统计。只有已点赞的用户才能取消点赞。

#### 业务场景
- 用户取消之前点赞的评论
- 点赞状态错误修正
- 用户偏好变更
- 评论管理功能

#### 权限要求
- **角色要求**: 登录用户
- **权限级别**: 写入权限
- **认证要求**: 需要JWT Token认证
- **权限验证**: 需要用户登录且已对该评论点赞

#### 调用限制
- **频率限制**: 10次/分钟/用户
- **数据量限制**: 单次取消一条评论的点赞
- **状态验证**: 只有已点赞的评论才能取消点赞
- **时间间隔**: 取消点赞操作需间隔至少5秒

#### 接口版本控制
- **当前版本**: v1
- **API路径**: `/api/v1/comment/{commentId}/like` (未来版本)

#### 请求参数
**路径参数**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| commentId | Long | 是 | - | > 0 | 评论唯一标识ID |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1762852809707
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | null | 无返回数据 |
| timestamp | Long | 响应时间戳 |

**错误响应 (HTTP 404)**
```json
{
  "code": 2004,
  "message": "评论不存在",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 400)**
```json
{
  "code": 400,
  "message": "未点赞，无法取消点赞",
  "data": null,
  "timestamp": 1762852809707
}
```

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "取消评论点赞失败",
  "data": null,
  "timestamp": 1762852809707
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 2004 | 评论不存在 | 检查评论ID是否正确 |
| 400 | 评论ID格式错误 | 检查评论ID格式是否为数字 |
| 400 | 未点赞，无法取消点赞 | 该评论未点赞，无法取消 |
| 401 | 未认证 | 检查JWT令牌是否有效 |
| 500 | 取消评论点赞失败 | 检查数据库连接，重试操作 |

#### 业务规则说明
1. **状态验证**
   - 只有已点赞的评论才能取消点赞
   - 验证用户是否对评论进行了点赞
   - 防止未点赞用户取消点赞

2. **数据一致性**
   - 取消点赞后立即更新评论的点赞数量
   - 点赞数不会出现负数（最小为0）
   - 数据库操作使用事务保证一致性

3. **权限控制**
   - 需要用户登录认证
   - 只能取消自己点赞的评论
   - 验证评论状态（只能对已通过审核的评论取消点赞）

#### 调用示例
```bash
# 取消评论点赞
curl -X DELETE "http://localhost:8080/api/comment/1/like" \
  -H "Authorization: Bearer {token}"
```

#### 前端集成示例
```javascript
// 取消评论点赞
async function unlikeComment(commentId, token) {
  if (!token) {
    throw new Error('未登录，无法取消点赞评论');
  }

  try {
    const response = await fetch(`/api/comment/${commentId}/like`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    const result = await response.json();

    if (result.code === 200) {
      console.log('取消评论点赞成功');
      // 更新页面上的点赞数
      updateCommentLikeCount(commentId, false);
      return true;
    } else {
      throw new Error(result.message);
    }
  } catch (error) {
    console.error('取消评论点赞失败:', error);
    throw error;
  }
}

// 切换评论点赞状态
async function toggleCommentLike(commentId, isLiked, token) {
  if (isLiked) {
    return await unlikeComment(commentId, token);
  } else {
    return await likeComment(commentId, token);
  }
}

// 使用示例
unlikeComment(1, userToken)
  .then(success => {
    if (success) {
      console.log('取消点赞成功');
    }
  })
  .catch(error => {
    console.error('错误:', error.message);
    if (error.message.includes('未点赞')) {
      alert('您未点赞过此评论，无需取消');
    } else {
      alert('取消点赞失败: ' + error.message);
    }
  });
}

// 在5.8节的前端集成示例中也需要补充updateCommentLikeCount函数
// 更新评论点赞数的函数示例
function updateCommentLikeCount(commentId, isLike) {
  const likeButton = document.querySelector(`[data-comment-id="${commentId}"] .like-button`);
  if (likeButton) {
    const countSpan = likeButton.querySelector('.like-count');
    if (countSpan) {
      let currentCount = parseInt(countSpan.textContent) || 0;
      countSpan.textContent = isLike ? currentCount + 1 : Math.max(0, currentCount - 1);

      // 更新按钮状态
      likeButton.classList.toggle('liked', isLike);
    }
  }
}
```

> 注意：当前后端不支持“编辑评论内容”接口，相关文档示例已移除以与代码实现保持一致。


## 6. 搜索模块

### 6.1 搜索文章
**POST** `/api/search/article`

#### 功能描述
根据多种条件搜索文章，支持关键词、分类、标签、作者等多维度搜索。

#### 业务场景
- 站内搜索功能
- 文章高级筛选
- 按分类、标签、作者搜索文章

#### 权限要求
- **角色要求**: 所有用户（游客、普通用户、管理员）
- **权限级别**: 读取权限
- **认证要求**: 无需认证（游客可访问）

#### 调用限制
- **频率限制**: 100次/分钟/IP
- **数据量限制**: 单次查询最多返回100条记录
- **缓存策略**: 搜索结果缓存5分钟

#### 请求参数
**请求体 (application/json)**
```json
{
  "keyword": "搜索关键词",
  "categoryId": 1,
  "tagIds": [1, 2, 3],
  "authorId": 1,
  "searchScope": "all",
  "sortBy": "time",
  "pageNum": 1,
  "pageSize": 10,
  "startDate": "2024-01-01",
  "endDate": "2024-12-31"
}
```

**参数说明**
| 参数名 | 类型 | 必填 | 默认值 | 约束 | 说明 |
|--------|------|------|--------|------|------|
| keyword | String | 否 | null | 最大50字符 | 搜索关键词，支持标题和内容搜索 |
| categoryId | Long | 否 | null | >0 | 分类ID，用于筛选指定分类的文章 |
| tagIds | Array[Long] | 否 | null | 每个元素>0 | 标签ID列表，用于筛选指定标签的文章 |
| authorId | Long | 否 | null | >0 | 作者ID，用于筛选指定作者的文章 |
| searchScope | String | 否 | "all" | all,title,content | 搜索范围：all-全部，title-标题，content-内容 |
| sortBy | String | 否 | "time" | time,relevance,view | 排序方式：time-时间，relevance-相关性，view-浏览量 |
| pageNum | Integer | 否 | 1 | >=1 | 页码，从1开始 |
| pageSize | Integer | 否 | 10 | 1-100 | 每页数量，最大100 |
| startDate | String | 否 | null | YYYY-MM-DD | 开始日期，格式为YYYY-MM-DD |
| endDate | String | 否 | null | YYYY-MM-DD | 结束日期，格式为YYYY-MM-DD |

#### 响应格式
**成功响应 (HTTP 200)**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "articleId": 1,
      "title": "文章标题",
      "summary": "文章摘要",
      "content": "文章内容片段",
      "coverImage": "https://example.com/image.jpg",
      "authorId": 1,
      "categoryId": 1,
      "viewCount": 100,
      "likeCount": 20,
      "commentCount": 5,
      "favoriteCount": 10,
      "publishTime": "2024-01-01T00:00:00",
      "relevanceScore": 1.0,
      "matchedField": "title"
    }
  ],
  "timestamp": 1762852809707
}
```

**字段说明**
| 字段名 | 类型 | 说明 |
|--------|------|------|
| code | Integer | 响应状态码 |
| message | String | 响应消息 |
| data | Array | 搜索结果列表 |
| data[].articleId | Long | 文章唯一标识ID |
| data[].title | String | 文章标题 |
| data[].summary | String | 文章摘要 |
| data[].content | String | 文章内容（片段） |
| data[].coverImage | String | 封面图片URL |
| data[].authorId | Long | 作者ID |
| data[].categoryId | Long | 分类ID |
| data[].viewCount | Integer | 浏览次数 |
| data[].likeCount | Integer | 点赞次数 |
| data[].commentCount | Integer | 评论次数 |
| data[].favoriteCount | Integer | 收藏次数 |
| data[].publishTime | String | 发布时间（ISO 8601格式） |
| data[].relevanceScore | Double | 相关性评分 |
| data[].matchedField | String | 匹配字段（title/content） |
| timestamp | Long | 响应时间戳 |

**错误响应 (HTTP 500)**
```json
{
  "code": 500,
  "message": "搜索文章失败",
  "data": null,
  "timestamp": 1762852809707
}
```

#### 错误码列表
| 错误码 | 错误信息 | 解决方案 |
|--------|----------|----------|
| 200 | 操作成功 | - |
| 400 | 请求参数错误 | 检查请求参数格式和类型 |
| 500 | 搜索文章失败 | 检查数据库连接，重试操作 |

#### 调用示例
```bash
# 搜索包含关键词的文章
curl -X POST "http://localhost:8080/api/search/article" \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "Java",
    "pageNum": 1,
    "pageSize": 10
  }'

# 高级搜索：按分类和标签搜索
curl -X POST "http://localhost:8080/api/search/article" \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "Spring Boot",
    "categoryId": 6,
    "tagIds": [1, 2],
    "pageNum": 1,
    "pageSize": 20
  }'
```

---

## 7. 文件上传模块

### 基础信息
- **认证要求**: 所有接口均需要认证（🔒），需在请求头中携带有效的JWT令牌
- **文件大小限制**: 单文件最大10MB
- **支持的图片类型**: jpg, jpeg, png, gif, bmp, webp
- **支持的文件类型**: 任意类型，图片文件会自动识别

### 7.1 上传图片
**POST** `/api/file/upload/image` 🔒

**请求参数:**
- `file`: 图片文件（multipart/form-data）

**响应示例:**
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": "1",
    "url": "http://example.com/uploads/2024/01/01/image.jpg",
    "filename": "image.jpg",
    "size": 102400,
    "fileType": "image/jpeg",
    "uploadTime": "2024-01-01T12:00:00"
  }
}
```

### 7.2 上传文件
**POST** `/api/file/upload/file` 🔒

**请求参数:**
- `file`: 任意文件（multipart/form-data）

**响应示例:**
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": "2",
    "url": "http://example.com/uploads/2024/01/01/document.pdf",
    "filename": "document.pdf",
    "size": 512000,
    "fileType": "application/pdf",
    "uploadTime": "2024-01-01T12:00:00"
  }
}
```

### 7.3 批量上传文件
**POST** `/api/file/upload/batch` 🔒

**请求参数:**
- `files`: 多个文件（multipart/form-data，数组形式）

**响应示例:**
```json
{
  "code": 200,
  "message": "上传成功",
  "data": [
    {
      "id": "3",
      "url": "http://example.com/uploads/2024/01/01/image1.jpg",
      "filename": "image1.jpg",
      "size": 102400,
      "fileType": "image/jpeg",
      "uploadTime": "2024-01-01T12:00:00"
    },
    {
      "id": "4",
      "url": "http://example.com/uploads/2024/01/01/image2.jpg",
      "filename": "image2.jpg",
      "size": 204800,
      "fileType": "image/jpeg",
      "uploadTime": "2024-01-01T12:00:00"
    }
  ]
}
```

### 7.4 获取文件列表
**GET** `/api/file/list` 🔒

**请求参数:**
- `pageNum`: 页码，默认1
- `pageSize`: 每页条数，默认10
- `fileType`: 文件类型（可选，如：image/jpeg, application/pdf）
- `keyword`: 文件名关键字（可选）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": "1",
        "url": "http://example.com/uploads/2024/01/01/image.jpg",
        "filename": "image.jpg",
        "size": 102400,
        "fileType": "image/jpeg",
        "uploadTime": "2024-01-01T12:00:00",
        "uploadUserName": "admin"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "pages": 1
  }
}
```

### 7.5 获取文件详情
**GET** `/api/file/{fileId}` 🔒

**请求参数:**
- `fileId`: 文件ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": "1",
    "url": "http://example.com/uploads/2024/01/01/image.jpg",
    "filename": "image.jpg",
    "originalName": "original.jpg",
    "size": 102400,
    "fileType": "image/jpeg",
    "mimeType": "image/jpeg",
    "fileExtension": "jpg",
    "fileCategory": "image",
    "uploadUserId": "1",
    "uploadUserName": "admin",
    "uploadTime": "2024-01-01T12:00:00",
    "status": "active"
  }
}
```

### 7.6 上传文件
**POST** `/api/file/upload/file` 🔒

**请求参数:**
- `file`: 文件（表单文件）

**响应示例:**
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": {
    "id": "1",
    "url": "http://example.com/uploads/2024/01/01/document.pdf",
    "filename": "document.pdf",
    "originalName": "original.pdf",
    "size": 204800,
    "fileType": "application/pdf",
    "mimeType": "application/pdf",
    "fileExtension": "pdf",
    "fileCategory": "document",
    "uploadUserId": "1",
    "uploadUserName": "admin",
    "uploadTime": "2024-01-01T12:00:00",
    "status": "active"
  }
}
```

### 7.7 批量上传文件
**POST** `/api/file/upload/batch` 🔒

**请求参数:**
- `files`: 文件列表（表单文件数组）

**响应示例:**
```json
{
  "code": 200,
  "message": "文件批量上传成功",
  "data": [
    {
      "id": "1",
      "url": "http://example.com/uploads/2024/01/01/file1.jpg",
      "filename": "file1.jpg",
      "originalName": "original1.jpg",
      "size": 102400,
      "fileType": "image/jpeg",
      "mimeType": "image/jpeg",
      "fileExtension": "jpg",
      "fileCategory": "image",
      "uploadUserId": "1",
      "uploadUserName": "admin",
      "uploadTime": "2024-01-01T12:00:00",
      "status": "active"
    },
    {
      "id": "2",
      "url": "http://example.com/uploads/2024/01/01/file2.png",
      "filename": "file2.png",
      "originalName": "original2.png",
      "size": 153600,
      "fileType": "image/png",
      "mimeType": "image/png",
      "fileExtension": "png",
      "fileCategory": "image",
      "uploadUserId": "1",
      "uploadUserName": "admin",
      "uploadTime": "2024-01-01T12:00:00",
      "status": "active"
    }
  ]
}
```

### 7.8 获取文件列表
**GET** `/api/file/list` 🔒

**请求参数:**
- `page`: 页码，默认1
- `size`: 每页数量，默认10
- `fileType`: 文件类型，可选

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": "1",
      "url": "http://example.com/uploads/2024/01/01/image.jpg",
      "filename": "image.jpg",
      "originalName": "original.jpg",
      "size": 102400,
      "fileType": "image/jpeg",
      "mimeType": "image/jpeg",
      "fileExtension": "jpg",
      "fileCategory": "image",
      "uploadUserId": "1",
      "uploadUserName": "admin",
      "uploadTime": "2024-01-01T12:00:00",
      "status": "active"
    }
  ]
}
```

### 7.9 获取文件详情
**GET** `/api/file/{fileId}` 🔒

**请求参数:**
- `fileId`: 文件ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": "1",
    "url": "http://example.com/uploads/2024/01/01/image.jpg",
    "filename": "image.jpg",
    "originalName": "original.jpg",
    "size": 102400,
    "fileType": "image/jpeg",
    "mimeType": "image/jpeg",
    "fileExtension": "jpg",
    "fileCategory": "image",
    "uploadUserId": "1",
    "uploadUserName": "admin",
    "uploadTime": "2024-01-01T12:00:00",
    "status": "active"
  }
}
```

### 7.10 删除文件
**DELETE** `/api/file/{fileId}` 🔒

**请求参数:**
- `fileId`: 文件ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "文件删除成功",
  "data": null
}
```

### 7.11 检查文件是否存在
**GET** `/api/file/check/md5/{md5}` 🔒

**请求参数:**
- `md5`: 文件MD5值（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": "1",
    "url": "http://example.com/uploads/2024/01/01/image.jpg",
    "filename": "image.jpg",
    "originalName": "original.jpg",
    "size": 102400,
    "fileType": "image/jpeg",
    "mimeType": "image/jpeg",
    "fileExtension": "jpg",
    "fileCategory": "image",
    "uploadUserId": "1",
    "uploadUserName": "admin",
    "uploadTime": "2024-01-01T12:00:00",
    "status": "active"
  }
}
```

### 错误响应示例
```json
{
  "code": 400,
  "message": "文件大小超过限制",
  "data": null
}

{
  "code": 400,
  "message": "不支持的文件类型",
  "data": null
}

{
  "code": 401,
  "message": "未授权访问",
  "data": null
}

{
  "code": 500,
  "message": "上传失败，请重试",
  "data": null
}
```

---

## 8. 统计模块

### 8.1 获取热门文章统计
**GET** `/api/statistics/article/hot`

**请求参数:**
- `limit`: 数量限制，默认10

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "热门文章标题",
      "viewCount": 1000,
      "likeCount": 50,
      "commentCount": 20,
      "favoriteCount": 10,
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

### 8.2 获取置顶文章统计
**GET** `/api/statistics/article/top`

**请求参数:**
- `limit`: 数量限制，默认10

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "置顶文章标题",
      "viewCount": 800,
      "likeCount": 40,
      "commentCount": 15,
      "favoriteCount": 8,
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

### 8.3 获取推荐文章统计
**GET** `/api/statistics/article/recommended`

**请求参数:**
- `limit`: 数量限制，默认10

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "推荐文章标题",
      "viewCount": 600,
      "likeCount": 30,
      "commentCount": 10,
      "favoriteCount": 5,
      "createTime": "2024-01-01T00:00:00"
    }
  ]
}
```

### 8.4 获取文章统计信息
**GET** `/api/statistics/article/{articleId}`

**请求参数:**
- `articleId`: 文章ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "title": "文章标题",
    "viewCount": 500,
    "likeCount": 25,
    "commentCount": 8,
    "favoriteCount": 3,
    "createTime": "2024-01-01T00:00:00"
  }
}
```

### 8.5 增加文章浏览量
**POST** `/api/statistics/article/view/{articleId}`

**请求参数:**
- `articleId`: 文章ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 8.6 增加文章点赞数
**POST** `/api/statistics/article/like/{articleId}/increment`

**请求参数:**
- `articleId`: 文章ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 8.7 减少文章点赞数
**POST** `/api/statistics/article/like/{articleId}/decrement`

**请求参数:**
- `articleId`: 文章ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 8.8 增加文章评论数
**POST** `/api/statistics/article/comment/{articleId}/increment`

**请求参数:**
- `articleId`: 文章ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 8.9 减少文章评论数
**POST** `/api/statistics/article/comment/{articleId}/decrement`

**请求参数:**
- `articleId`: 文章ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 8.10 增加文章收藏数
**POST** `/api/statistics/article/favorite/{articleId}/increment`

**请求参数:**
- `articleId`: 文章ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 8.11 减少文章收藏数
**POST** `/api/statistics/article/favorite/{articleId}/decrement`

**请求参数:**
- `articleId`: 文章ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

---

## 9. 管理后台模块

### 9.1 获取用户列表（管理员）
**GET** `/api/admin/users` 🔒

**查询参数:**
- `page`: 页码，默认1
- `size`: 每页数量，默认10
- `keyword`: 搜索关键词，可选
- `status`: 用户状态，可选（0-禁用，1-正常）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 100,
    "pages": 10,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "username": "testuser",
        "email": "test@example.com",
        "nickname": "测试用户",
        "status": 1,
        "articleCount": 5,
        "commentCount": 10,
        "createTime": "2024-01-01T00:00:00",
        "lastLoginTime": "2024-01-15T00:00:00"
      }
    ]
  }
}
```

**错误响应示例:**
```json
{
  "code": 500,
  "message": "服务器内部错误",
  "timestamp": 1761035822151
}
```

### 9.2 获取文章管理列表（管理员）
**GET** `/api/admin/articles` 🔒

**查询参数:**
- `page`: 页码，默认1
- `size`: 每页数量，默认10
- `keyword`: 搜索关键词，可选
- `status`: 文章状态，可选（0-草稿，1-已发布，2-已下线）
- `authorId`: 作者ID，可选

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 200,
    "pages": 20,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "title": "文章标题",
        "author": {
          "id": 1,
          "username": "author",
          "nickname": "作者昵称"
        },
        "status": 1,
        "viewCount": 100,
        "likeCount": 20,
        "commentCount": 5,
        "createTime": "2024-01-01T00:00:00"
      }
    ]
  }
}
```

**错误响应示例:**
```json
{
  "code": 500,
  "message": "服务器内部错误",
  "timestamp": 1761035822151
}
```

### 9.3 获取评论管理列表（管理员）
**GET** `/api/admin/comments` 🔒

**查询参数:**
- `page`: 页码，默认1
- `size`: 每页数量，默认10
- `keyword`: 搜索关键词，可选
- `status`: 评论状态，可选（1-待审核，2-已通过，3-已拒绝，4-已删除）
- `articleId`: 文章ID，可选

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 500,
    "pages": 50,
    "current": 1,
    "size": 10,
    "records": [
      {
        "id": 1,
        "content": "评论内容",
        "user": {
          "id": 1,
          "username": "commenter",
          "nickname": "评论者"
        },
        "article": {
          "id": 1,
          "title": "文章标题"
        },
        "status": 1,
        "createTime": "2024-01-01T00:00:00"
      }
    ]
  }
}
```

**错误响应示例:**
```json
{
  "code": 500,
  "message": "服务器内部错误",
  "timestamp": 1761035822151
}
```

### 9.4 审核评论（管理员）
**PUT** `/api/comment/{commentId}/review` 🔒

**请求参数:**
- `commentId`: 评论ID（路径参数）
- `status`: 评论状态：1-待审核，2-已通过，3-已拒绝（查询参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1761035850168
}
```

**错误响应示例:**
```json
{
  "code": 404,
  "message": "评论不存在",
  "timestamp": 1761035850168
}
```

### 9.5 用户状态管理（管理员）
**PUT** `/api/admin/users/{userId}/status` 🔒

**请求参数:**
- `userId`: 用户ID（路径参数）
- `status`: 用户状态：0-禁用，1-正常（查询参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1761035850168
}
```

**错误响应示例:**
```json
{
  "code": 404,
  "message": "用户不存在",
  "timestamp": 1761035850168
}
```

### 9.6 删除用户（管理员）
**DELETE** `/api/admin/users/{userId}` 🔒

**请求参数:**
- `userId`: 用户ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "用户删除成功",
  "timestamp": 1761035822151
}
```

**错误响应示例:**
```json
{
  "code": 404,
  "message": "用户不存在",
  "timestamp": 1761035822151
}
```

### 9.7 删除文章（管理员）
**DELETE** `/api/admin/articles/{articleId}` 🔒

**请求参数:**
- `articleId`: 文章ID（路径参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "文章删除成功",
  "timestamp": 1761035822151
}
```

**错误响应示例:**
```json
{
  "code": 404,
  "message": "文章不存在",
  "timestamp": 1761035822151
}
```

### 9.8 获取系统配置（管理员）
**GET** `/api/admin/config` 🔒

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "siteName": "我的博客",
    "siteDescription": "一个优秀的博客网站",
    "siteKeywords": "博客,技术,分享",
    "allowRegister": true,
    "commentAudit": false,
    "maxFileSize": 10485760
  }
}
```

### 9.9 更新系统配置（管理员）
**PUT** `/api/admin/config` 🔒

**请求参数:**
```json
{
  "siteName": "我的博客",
  "siteDescription": "一个优秀的博客网站",
  "siteKeywords": "博客,技术,分享",
  "allowRegister": true,
  "commentAudit": false,
  "maxFileSize": 10485760
}
```

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1761035850168
}
```

### 9.10 修改文章状态（管理员）
**PUT** `/api/admin/articles/{articleId}/status` 🔒

**请求参数:**
- `articleId`: 文章ID（路径参数）
- `status`: 文章状态：0-草稿，1-已发布，2-已下线（查询参数）

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "timestamp": 1761035850168
}
```

### 9.11 获取网站统计信息
**GET** `/api/admin/statistics` 🔒

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalArticles": 100,
    "totalUsers": 50,
    "totalComments": 200,
    "totalViews": 10000,
    "todayViews": 100,
    "recentArticles": [
      {
        "id": 1,
        "title": "最新文章",
        "viewCount": 50,
        "createTime": "2024-01-01T00:00:00Z"
      }
    ]
  }
}
```

### 9.12 获取访问统计信息
**GET** `/api/admin/visit-statistics` 🔒

**查询参数:**
- `type`: 统计类型：day-日统计，week-周统计，month-月统计

**响应示例:**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "totalVisits": 1000,
    "dailyVisits": [
      {
        "date": "2024-01-01",
        "visits": 50
      }
    ],
    "trend": "up"
  }
}
```

### 9.13 清理缓存
**POST** `/api/admin/cache/clear` 🔒

**响应示例:**
```json
{
  "code": 200,
  "message": "缓存清理成功",
  "timestamp": 1761035850168
}
```

---

## 错误处理

### 常见错误响应

#### 参数验证错误
```json
{
  "code": 400,
  "message": "参数验证失败",
  "data": {
    "errors": [
      {
        "field": "username",
        "message": "用户名长度必须在3-20字符之间"
      }
    ]
  }
}
```

#### 认证失败
```json
{
  "code": 401,
  "message": "未认证或token已过期",
  "data": null
}
```

#### 权限不足
```json
{
  "code": 403,
  "message": "权限不足",
  "data": null
}
```

#### 资源不存在
```json
{
  "code": 404,
  "message": "文章不存在",
  "data": null
}
```

---

## 接口调用示例

### JavaScript (Axios)
```javascript
// 设置基础URL和拦截器
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
});

// 请求拦截器 - 添加token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器 - 处理错误
api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      // 清除token，跳转到登录页
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 使用示例
// 登录
const login = async (username, password) => {
  const response = await api.post('/user/login', { username, password });
  if (response.code === 200) {
    localStorage.setItem('token', response.data.token);
    return response.data.user;
  }
};

// 获取文章列表
const getArticles = async (params = {}) => {
  const response = await api.get('/article/list', { params });
  return response.data;
};

// 创建文章
const createArticle = async (articleData) => {
  const response = await api.post('/article', articleData);
  return response.data;
};
```

### 管理员用户管理功能测试用例

以下是我们刚刚测试过的管理员用户管理功能的实际调用示例：

```bash
# 1. 管理员登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# 响应示例
# {
#   "code": 200,
#   "success": true,
#   "message": "登录成功",
#   "data": {
#     "id": 1,
#     "username": "admin",
#     "nickname": "管理员",
#     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
#   },
#   "timestamp": 1761035724179
# }

# 2. 获取用户列表
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer [管理员令牌]"

# 响应示例
# {
#   "code": 200,
#   "success": true,
#   "data": [
#     {
#       "id": 1,
#       "username": "admin",
#       "nickname": "管理员",
#       "email": "admin@example.com",
#       "status": 1,
#       "createTime": "2025-01-01T00:00:00",
#       "lastLoginTime": "2025-10-21T08:30:00"
#     },
#     {
#       "id": 2,
#       "username": "demo_user",
#       "nickname": "演示用户",
#       "email": "demo@example.com",
#       "status": 0,
#       "createTime": "2025-01-02T00:00:00",
#       "lastLoginTime": null
#     }
#   ],
#   "timestamp": 1761035837904
# }

# 3. 更新用户状态
curl -X PUT "http://localhost:8080/api/admin/users/2/status?status=1" \
  -H "Authorization: Bearer [管理员令牌]"

# 响应示例
# {
#   "code": 200,
#   "message": "操作成功",
#   "success": true,
#   "timestamp": 1761035850168
# }

# 4. 删除用户
curl -X DELETE http://localhost:8080/api/admin/users/3 \
  -H "Authorization: Bearer [管理员令牌]"

# 响应示例
# {
#   "code": 200,
#   "message": "用户删除成功",
#   "success": true,
#   "timestamp": 1761035822151
# }

# 5. 用户登录验证
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username": "demo_user", "password": "demo123"}'

# 响应示例
# {
#   "code": 200,
#   "success": true,
#   "message": "登录成功",
#   "data": {
#     "id": 2,
#     "username": "demo_user",
#     "nickname": "演示用户",
#     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
#   },
#   "timestamp": 1761035857913
# }
```

**测试结果总结:**
1. 管理员登录功能正常，能够获取有效的JWT令牌
2. 获取用户列表功能正常，能够返回所有用户的详细信息
3. 更新用户状态功能正常，能够成功将用户状态从0(禁用)更新为1(启用)
4. 删除用户功能正常，能够成功删除指定用户
5. 状态更新后的用户能够正常登录系统

**注意事项:**
- 删除用户接口路径为 `/api/admin/users/{id}`，注意使用复数形式 `users` 而不是单数形式 `user`
- 所有管理员接口都需要在请求头中携带有效的JWT令牌
- 用户状态值：1表示启用，0表示禁用
- 删除用户操作是不可逆的，请谨慎操作

---

## 10. 接口版本控制

### 10.1 版本说明
- 当前API版本：v1
- 版本控制方式：URL路径版本控制
- 示例：`/api/v1/users/profile`

### 10.2 版本兼容性
- 向后兼容：新版本保持对旧版本的兼容
- 废弃通知：废弃的接口会提前3个月通知
- 迁移指南：提供详细的版本迁移文档

## 11. 安全说明

### 11.1 认证机制
- JWT Token认证，有效期24小时
- Refresh Token有效期7天
- Token需要在请求头中携带：`Authorization: Bearer <token>`

### 11.2 权限控制
- 普通用户：只能操作自己的数据
- 管理员：可以管理所有用户和内容
- 超级管理员：拥有系统配置权限

### 11.3 安全措施
- 所有密码使用BCrypt加密存储
- 敏感操作需要二次验证
- API限流：每分钟最多100次请求
- SQL注入防护：使用参数化查询
- XSS防护：输入内容过滤和转义

### 11.4 数据验证
- 所有输入参数进行严格验证
- 文件上传类型和大小限制
- 邮箱格式验证
- 密码强度要求：至少8位，包含字母和数字

## 12. 错误处理补充

### 12.1 业务错误码
- `40001`: 用户名已存在
- `40002`: 邮箱已被注册
- `40003`: 用户名或密码错误
- `40004`: 账户已被禁用
- `40005`: 文章不存在
- `40006`: 无权限操作
- `40007`: 评论已被删除
- `40008`: 文件格式不支持
- `40009`: 文件大小超限
- `40010`: 验证码错误或已过期

### 12.2 系统错误码
- `50001`: 数据库连接失败
- `50002`: 文件上传失败
- `50003`: 邮件发送失败
- `50004`: 缓存服务异常
- `50005`: 第三方服务异常

---

## 注意事项

1. **安全性**
   - 所有用户输入都需要进行验证和过滤
   - 敏感操作需要二次验证
   - 文件上传需要类型和大小限制

2. **性能优化**
   - 文章列表接口支持分页
   - 图片上传支持压缩
   - 热门内容可以考虑缓存

3. **数据一致性**
   - 删除文章时需要同时删除相关评论
   - 删除分类时需要处理该分类下的文章

4. **扩展性**
   - 接口设计支持后续功能扩展
   - 响应格式统一，便于前端处理

**重要提示:**
1. 🔒 表示需要登录认证的接口
2. 所有时间格式均为 ISO 8601 格式
3. 分页参数 page 从 1 开始
4. 文件上传大小限制为 10MB
5. 请求头需要包含 `Content-Type: application/json`（除文件上传外）
6. 生产环境建议使用HTTPS协议
7. 建议实现接口幂等性，避免重复操作
8. 重要操作建议添加操作日志记录

## 代码与文档一致性检查

经过对源代码和API文档的详细比对，发现以下一致性问题和建议：

### 1. 接口实现缺失问题
- **问题**: `CommentService` 接口中定义了 `updateComment(Long commentId, String content)` 方法，但在 `CommentController` 和 `CommentServiceImpl` 中没有对应的实现
- **影响**: API文档中缺少编辑评论接口，导致功能不完整
- **建议**: 实现编辑评论功能或从服务接口中移除该方法

### 2. 状态码不一致问题
- **问题**: 代码中使用标准HTTP状态码（如200、400、401、403、404、500），但文档中使用了非标准状态码如2004表示"评论不存在"
- **建议**: 统一使用标准HTTP状态码，文档中错误码应与实际实现保持一致

### 3. 评论状态值不一致
- **问题**: 文档中描述评论状态为"0-待审核，1-已通过，2-已拒绝"，但代码中评论状态值为1-待审核，2-已通过，3-已拒绝，4-已删除
- **建议**: 更新文档以准确反映代码实现的状态值定义

### 4. 缺失的管理员评论功能
- **问题**: 代码中没有提供专门的管理员评论管理接口（如批量审核、批量删除等）
- **建议**: 添加专门的管理员评论管理接口以完善后台功能

### 5. 性能优化建议
- **问题**: 没有实现评论列表的缓存机制
- **建议**: 为评论列表接口添加Redis缓存以提高性能

### 6. 安全机制建议
- **问题**: 缺少防刷评论的频率限制机制
- **建议**: 实现基于IP或用户ID的评论发布频率限制

### 7. 数据校验增强
- **问题**: 代码中的参数校验可以进一步增强
- **建议**: 增加对评论内容敏感词过滤和格式校验

### 8. 文档与实现认证要求不一致
- **问题**: 文档中描述某些接口如"获取评论详情"和"获取文章评论数量"无需认证，但实际代码实现中这些接口需要JWT Token认证
- **建议**: 更新文档中的权限要求部分，以准确反映实际的认证要求

### 9. 评论状态值验证缺失
- **问题**: 文档中提到审核评论接口会对状态值进行验证（仅允许1、2、3），但实际代码实现未对状态值进行严格验证，允许设置任意数值
- **建议**: 更新文档中的参数验证说明，准确描述实际的验证行为

### 10. 评论数量统计规则不一致
- **问题**: 文档中说明"获取文章评论数量"接口仅统计已通过审核的评论，但实际实现统计所有未删除的评论（包括待审核、已通过、已拒绝状态）
- **建议**: 更新文档中的业务规则说明，准确反映实际的统计逻辑

这份API文档涵盖了博客网站的所有核心功能，为前后端开发提供了详细的接口规范。
