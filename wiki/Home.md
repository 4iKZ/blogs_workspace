# Lumina 博客系统 Wiki

> **在线体验**: [https://luminablog.cn](https://luminablog.cn)  
> **开源地址**: [https://github.com/4iKZ/blogs_workspace](https://github.com/4iKZ/blogs_workspace)

**Lumina** 是一个基于 **Spring Boot 3 + Vue 3** 构建的开源全栈博客平台，采用前后端完全分离架构，具备丰富的创作工具、精细的权限管理、高并发缓存策略与完善的社区互动体验。

---

## 📖 Wiki 导航

| 页面 | 说明 |
|------|------|
| [🏗️ 系统架构](./Architecture.md) | 整体架构设计、技术选型与模块划分 |
| [⚙️ 后端实现](./Backend.md) | Spring Boot 后端各模块详细说明 |
| [🎨 前端实现](./Frontend.md) | Vue 3 前端结构、组件与状态管理 |
| [🗄️ 数据库设计](./Database.md) | 所有表结构、字段说明与关系图 |
| [📡 API 参考](./API.md) | 完整 RESTful API 接口文档 |
| [🔐 安全与认证](./Security-Auth.md) | JWT 认证、Spring Security 配置 |
| [⚡ 缓存策略](./Cache-Design.md) | Redis 缓存设计与并发控制 |
| [🚀 部署指南](./Deployment.md) | 生产环境部署配置 |
| [🛠️ 开发环境](./Dev-Setup.md) | 本地开发环境搭建 |

---

## 🌟 核心功能一览

### 创作与内容管理
| 功能 | 说明 |
|------|------|
| **全能 Markdown 编辑器** | MD Editor v3，实时预览、代码高亮、数学公式（KaTeX）、流程图（Mermaid） |
| **文章全生命周期** | 草稿保存 → 发布 → 编辑 → 逻辑删除，支持封面图片上传 |
| **分类 & 标签体系** | 多层级分类管理，标签云展示 |
| **全文搜索** | MySQL 全文索引，支持标题与内容模糊匹配 |
| **大文件分片上传** | Web Worker 图片压缩 + 分片上传，支持断点续传 |

### 社区互动
| 功能 | 说明 |
|------|------|
| **多级嵌套评论** | 楼中楼回复、评论点赞、多维度排序（最新/最热） |
| **文章点赞 & 收藏** | Redis 分布式锁保障高并发数据一致性 |
| **实时通知中心** | 评论、点赞通知，已读/未读状态管理 |
| **用户关注系统** | 关注/取消关注，关注流文章聚合 |
| **热门排行榜** | 日榜/周榜，Redis ZSet 实现，定时任务自动重置 |

### 管理后台
| 功能 | 说明 |
|------|------|
| **数据统计大屏** | PV/UV 趋势图、用户增长、文章热度 |
| **用户管理** | 查看、封禁、删除、角色权限管理 |
| **内容管理** | 文章与评论审核、批量操作 |
| **系统动态配置** | 无需重启修改网站信息 |
| **数据备份** | 数据库一键备份与导出 |

---

## 🛠️ 技术栈总览

```
后端 (Backend)
├── Spring Boot 3.5.6
├── Spring Security + JWT (io.jsonwebtoken 0.11.5)
├── MyBatis Plus 3.5.5
├── MySQL 8.0+ (HikariCP 连接池)
├── Redis 6.0+ (Lettuce 客户端)
├── 火山引擎 TOS (文件存储)
├── SpringDoc OpenAPI 2.5.0 (Swagger UI)
├── Hutool 5.8.16
├── Lombok 1.18.32
└── Java 21

前端 (Frontend)
├── Vue 3.4 (Composition API)
├── TypeScript 5.2
├── Vite 5.2
├── Element Plus 2.7.6
├── Pinia 2.1.7
├── Vue Router 4.3.0
├── MD Editor v3 6.2.0
└── Axios 1.6.8
```

---

## 📁 项目根目录结构

```
blogs_workspace/
├── src/                          # 后端源码 (Spring Boot)
│   ├── main/java/com/blog/       # Java 主程序
│   └── main/resources/           # 配置文件与 Mapper XML
├── frontend/                     # 前端源码 (Vue 3)
│   ├── src/                      # 前端主程序
│   └── public/                   # 静态资源
├── database/
│   ├── schema.sql                # 建库建表脚本
│   └── data.sql                  # 初始化数据（含默认管理员）
├── docs/                         # 项目文档
├── wiki/                         # 本 Wiki
├── pom.xml                       # Maven 配置
├── nginx.conf                    # Nginx 生产配置
├── README.md                     # 项目说明
└── CLAUDE.md                     # 开发规范说明
```

---

## 👤 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `123456` |

> ⚠️ 生产环境请及时修改默认密码

---

## 📄 开源许可

本项目基于 [MIT License](../LICENSE) 开源，可自由使用、修改和分发。

**作者**: 4iKZ | **邮箱**: syhaox@outlook.com | **博客**: [luminablog.cn](https://luminablog.cn)
