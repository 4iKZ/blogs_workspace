# Lumina - 代码与思考

<p align="center">
  <img src="frontend/public/favicon.svg" width="100" height="100" alt="Lumina Logo">
</p>

<p align="center">
  <a href="https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html">
    <img src="https://img.shields.io/badge/Java-21-blue.svg" alt="Java Version">
  </a>
  <a href="https://spring.io/projects/spring-boot">
    <img src="https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg" alt="Spring Boot">
  </a>
  <a href="https://vuejs.org/">
    <img src="https://img.shields.io/badge/Vue-3.4-4fc08d.svg" alt="Vue Version">
  </a>
  <a href="https://www.typescriptlang.org/">
    <img src="https://img.shields.io/badge/TypeScript-5.2-blue.svg" alt="TypeScript">
  </a>
  <a href="https://vitejs.dev/">
    <img src="https://img.shields.io/badge/Vite-5.2-646cff.svg" alt="Vite">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
  </a>
</p>

Lumina 是一个基于 Spring Boot 3 和 Vue 3 开发的高性能全栈博客系统。它不仅是一个记录代码的工具，更是一个分享思考、沉淀知识的平台。系统采用前后端分离架构，集成了 Markdown 深度定制、JWT 安全认证、实时通知、多维度数据统计等现代化功能，致力于提供极致的阅读与创作体验。

## ✨ 功能特性

### 📝 创作与内容管理
- **全能 Markdown 编辑器**：支持实时预览、代码高亮（Highlight.js）、数学公式（KaTeX）、流程图（Mermaid）等。
- **灵活的分类与标签**：多层级分类管理，帮助构建清晰的知识体系。
- **互动引擎**：支持点赞、收藏、多级嵌套评论（支持回复、点赞、排序）。
- **全站搜索**：基于关键词的高效检索系统，快速定位所需内容。

### 🎨 极致用户体验
- **响应式布局**：完美适配 PC、平板及移动端设备。
- **双色主题切换**：支持暗黑（Dark）与明亮（Light）模式，内置视力保护配色。
- **实时通知中心**：评论提醒、点赞通知即时送达。
- **个人门户**：管理个人档案、头像上传、我的点赞与收藏。

### 📊 管理与运营
- **大屏数据统计**：可视化展示全站访问量、用户增长、文章热度趋势。
- **精细化资源管理**：用户、文章、评论、分类的全生命周期管理（支持逻辑删除）。
- **动态系统配置**：无需重启即可动态配置网站属性、文件上传策略、页脚信息等。

### 🛠 技术基石
- **无状态认证**：基于 Spring Security + JWT 的分布式权限管理。
- **内容安全**：内置敏感词过滤引擎，保障社区环境。
- **高并发保障**：使用 Redis 分布式锁解决点赞/计数等并发竞争问题。
- **云端存储**：集成火山引擎 TOS (Volcengine) 实现高性能文件上传与存储。

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
1. **获取源码**
   ```bash
   git clone https://github.com/4iKZ/blogs_workspace.git
   cd blogs_workspace
   ```

2. **初始化数据库**
   - 创建数据库 `blog`。
   - 顺序执行 `database/` 目录下的 SQL 脚本：`create_tables.sql` -> `init_data.sql` -> `notifications.sql`。

3. **后端配置**
   - 编辑 `src/main/resources/application.yml`：
     - 配置 `spring.datasource` (MySQL 连接)。
     - 配置 `spring.data.redis` (Redis 连接)。
     - 配置 `tos` (火山引擎凭证，若需上传文件)。

4. **前端配置**
   ```bash
   cd frontend
   npm install
   ```

### 运行指令
**启动后端：**
```bash
# Windows
mvnw.cmd spring-boot:run
# Linux/macOS
./mvnw spring-boot:run
```
**启动前端：**
```bash
cd frontend
npm run dev
```
访问地址：`http://localhost:3000` (默认代理 8080 端口 API)。

## 📖 使用指南
- **初始管理员**：账号 `admin` / 密码 `123456`。
- **API 文档**：项目运行后访问 `http://localhost:8080/swagger-ui.html`。
- **部署建议**：生产环境建议使用 Nginx 反向代理，并开启 HTTPS。

## 📂 项目结构
```text
blogs_workspace
├── database/               # 数据库初始化与迁移脚本
├── docs/                   # 项目设计文档、API 文档、需求规格
├── frontend/               # Vue 3 前端源码
│   ├── src/
│   │   ├── components/     # 业务组件库 (文章、评论、侧边栏等)
│   │   ├── services/       # 基于 Axios 的 API 服务封装
│   │   ├── store/          # Pinia 状态管理 (用户、通知、文章)
│   │   └── views/          # 路由页面组件
├── src/                    # Spring Boot 后端源码
│   ├── main/java/com/blog/
│   │   ├── config/         # 安全配置、MyBatis Plus、TOS、Redis
│   │   ├── controller/     # RESTful 接口层
│   │   ├── entity/         # 数据库实体类 (MyBatis Plus)
│   │   ├── mapper/         # 数据访问层
│   │   └── service/        # 核心业务逻辑层
└── pom.xml                 # 后端依赖管理
```

## 🤝 贡献指南
我们欢迎任何形式的贡献（代码提交、Bug 报告、新功能建议）！
1. Fork 本项目。
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)。
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)。
4. 推送到分支 (`git push origin feature/AmazingFeature`)。
5. 开启一个 Pull Request。

## 📄 许可证信息
本项目基于 [MIT License](LICENSE) 协议开源。

## 📮 联系方式
- **作者**: 4iKZ
- **邮箱**: syhaox@outlook.com
- **GitHub**: [https://github.com/4iKZ](https://github.com/4iKZ)

---
*Lumina - 让代码发光，让思考留痕。*
