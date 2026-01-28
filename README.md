<div align="center">

  <img src="icon.png" width="120" height="120" alt="Lumina Logo">

  # 🌟 Lumina - 代码与思考

  **一个现代化的全栈博客系统 | 记录代码 · 分享思考 · 沉淀知识**

  [![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
  [![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
  [![Vue](https://img.shields.io/badge/Vue-3.4-4fc08d.svg)](https://vuejs.org/)
  [![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg)](https://www.typescriptlang.org/)
  [![Vite](https://img.shields.io/badge/Vite-5.2-646cff.svg)](https://vitejs.dev/)
  [![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

  [功能特性](#-功能特性) · [快速开始](#-快速开始) · [项目结构](#-项目结构) · [技术栈](#-技术栈) · [贡献指南](#-贡献指南)

</div>

---

## 💡 关于 Lumina

Lumina 是一个基于 **Spring Boot 3** 和 **Vue 3** 开发的高性能全栈博客系统。它不仅是一个记录代码的工具，更是一个分享思考、沉淀知识的平台。系统采用前后端分离架构，集成了 **Markdown 深度定制**、**JWT 安全认证**、**实时通知**、**多维度数据统计**等现代化功能，致力于提供极致的阅读与创作体验。

---

## ✨ 功能特性

### 📝 创作与内容管理

| 功能 | 描述 |
| :--- | :--- |
| **📄 全能 Markdown 编辑器** | 支持实时预览、代码高亮（Highlight.js）、数学公式（KaTeX）、流程图（Mermaid） |
| **🗂 灵活的分类与标签** | 多层级分类管理，帮助构建清晰的知识体系 |
| **💬 互动引擎** | 支持点赞、收藏、多级嵌套评论（支持回复、点赞、排序） |
| **🔍 全站搜索** | 基于关键词的高效检索系统，快速定位所需内容 |

### 🎨 极致用户体验

| 功能 | 描述 |
| :--- | :--- |
| **📱 响应式布局** | 完美适配 PC、平板及移动端设备 |
| **🌓 双色主题切换** | 支持暗黑（Dark）与明亮（Light）模式，内置视力保护配色 |
| **🔔 实时通知中心** | 评论提醒、点赞通知即时送达 |
| **👤 个人门户** | 管理个人档案、头像上传、我的点赞与收藏 |

### 📊 管理与运营

| 功能 | 描述 |
| :--- | :--- |
| **📈 大屏数据统计** | 可视化展示全站访问量、用户增长、文章热度趋势 |
| **⚙ 精细化资源管理** | 用户、文章、评论、分类的全生命周期管理（支持逻辑删除） |
| **🔧 动态系统配置** | 无需重启即可动态配置网站属性、文件上传策略、页脚信息等 |

### 🛠 技术基石

| 特性 | 描述 |
| :--- | :--- |
| **🔐 无状态认证** | 基于 Spring Security + JWT 的分布式权限管理 |
| **🛡 内容安全** | 内置敏感词过滤引擎，保障社区环境 |
| **⚡ 高并发保障** | 使用 Redis 分布式锁解决点赞/计数等并发竞争问题 |
| **☁️ 云端存储** | 集成火山引擎 TOS (Volcengine) 实现高性能文件上传与存储 |

---

## 🚀 快速开始

### 环境要求

| 工具 | 版本要求 |
| :--- | :--- |
| **JDK** | 21+ |
| **Node.js** | 18.x+ (推荐 20+) |
| **MySQL** | 8.0+ |
| **Redis** | 6.0+ |
| **Maven** | 3.8+ |

### 安装与配置

#### 1️⃣ 获取源码

```bash
git clone https://github.com/4iKZ/blogs_workspace.git
cd blogs_workspace
```

#### 2️⃣ 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE blog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 执行初始化脚本
mysql -u root -p blog_db < database/init_database.sql
```

#### 3️⃣ 后端配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog_db
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

# 火山引擎 TOS 配置（可选，用于文件上传）
tos:
  access-key: your_access_key
  secret-key: your_secret_key
  endpoint: your_endpoint
  bucket-name: your_bucket
```

#### 4️⃣ 前端配置

```bash
cd frontend
npm install
```

### 运行项目

<div align="center">

```bash
# 启动后端（Windows）
mvnw.cmd spring-boot:run

# 启动后端（Linux/macOS）
./mvnw spring-boot:run

# 启动前端
cd frontend
npm run dev
```

</div>

访问地址：`http://localhost:3000` (前端自动代理 8080 端口 API)

---

## 📖 使用指南

| 项目 | 说明 |
| :--- | :--- |
| **初始管理员账号** | `admin` / `123456` |
| **API 文档** | 运行后访问 `http://localhost:8080/swagger-ui.html` |
| **生产部署** | 建议使用 Nginx 反向代理，并开启 HTTPS |

---

## 📂 项目结构

```text
blogs_workspace
├── 📁 database/              # 数据库初始化与迁移脚本
├── 📁 docs/                  # 项目设计文档、API 文档、需求规格
├── 📁 frontend/              # Vue 3 前端源码
│   ├── 📁 src/
│   │   ├── 📁 components/    # 业务组件库 (文章、评论、侧边栏等)
│   │   ├── 📁 services/      # 基于 Axios 的 API 服务封装
│   │   ├── 📁 store/         # Pinia 状态管理 (用户、通知、文章)
│   │   ├── 📁 views/         # 路由页面组件
│   │   ├── 📁 router/        # Vue Router 路由配置
│   │   └── 📁 utils/         # 工具函数集合
│   ├── 📁 public/            # 静态资源
│   └── 📄 package.json
├── 📁 src/                   # Spring Boot 后端源码
│   ├── 📁 main/java/com/blog/
│   │   ├── 📁 config/        # 安全配置、MyBatis Plus、TOS、Redis
│   │   ├── 📁 controller/    # RESTful 接口层
│   │   ├── 📁 entity/        # 数据库实体类 (MyBatis Plus)
│   │   ├── 📁 mapper/        # 数据访问层
│   │   ├── 📁 service/       # 核心业务逻辑层
│   │   ├── 📁 dto/           # 数据传输对象
│   │   ├── 📁 security/      # JWT 安全认证
│   │   └── 📁 util/          # 工具类集合
│   └── 📁 main/resources/
│       ├── 📁 mapper/        # MyBatis XML 映射文件
│       └── 📄 application.yml
├── 📄 icon.png               # 项目图标
├── 📄 pom.xml                # 后端依赖管理
└── 📄 README.md              # 项目说明文档
```

---

## 🛠 技术栈

### 后端技术

| 技术 | 版本 | 用途 |
| :--- | :--- | :--- |
| **Java** | 21 | 编程语言 |
| **Spring Boot** | 3.5.6 | 应用框架 |
| **Spring Security** | 3.x | 安全认证 |
| **MyBatis Plus** | 3.5.5 | ORM 框架 |
| **JWT** | 0.11.5 | Token 认证 |
| **Redis** | - | 缓存/分布式锁 |
| **MySQL** | 8.0+ | 关系型数据库 |
| **HikariCP** | - | 数据库连接池 |
| **Hutool** | 5.8.16 | Java 工具库 |

### 前端技术

| 技术 | 版本 | 用途 |
| :--- | :--- | :--- |
| **Vue** | 3.4 | 前端框架 |
| **TypeScript** | 5.2 | 编程语言 |
| **Vite** | 5.2 | 构建工具 |
| **Pinia** | 2.1.7 | 状态管理 |
| **Vue Router** | 4.3.0 | 路由管理 |
| **Element Plus** | 2.7.6 | UI 组件库 |
| **MD Editor v3** | - | Markdown 编辑器 |
| **Axios** | - | HTTP 客户端 |

---

## 🤝 贡献指南

我们欢迎任何形式的贡献（代码提交、Bug 报告、新功能建议）！

<div align="center">

```bash
# 1. Fork 本项目
# 2. 创建您的特性分支
git checkout -b feature/AmazingFeature

# 3. 提交您的更改
git commit -m 'Add some AmazingFeature'

# 4. 推送到分支
git push origin feature/AmazingFeature

# 5. 开启一个 Pull Request
```

</div>

---

## 📄 开源协议

本项目基于 **[MIT License](LICENSE)** 协议开源。

---

## 📮 联系方式

<div align="center">

| 项目 | 信息 |
| :--- | :--- |
| **👤 作者** | 4iKZ |
| **📧 邮箱** | [syhaox@outlook.com](mailto:syhaox@outlook.com) |
| **🔗 GitHub** | [https://github.com/4iKZ](https://github.com/4iKZ) |
| **📦 仓库** | [https://github.com/4iKZ/blogs_workspace](https://github.com/4iKZ/blogs_workspace) |

</div>

---

<div align="center">

  **⭐ 如果这个项目对你有帮助，请给一个 Star 支持一下！**

  *Lumina - 让代码发光，让思考留痕。*

  [↑ 返回顶部](#-lumina---代码与思考)

</div>
