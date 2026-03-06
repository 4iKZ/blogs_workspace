# 开发环境搭建

[← 返回 Wiki 首页](./Home.md)

---

## 环境依赖

| 工具 | 版本要求 | 用途 |
|------|---------|------|
| JDK | 21+ | 后端运行时 |
| Maven | 3.8+ | 后端依赖管理与构建 |
| Node.js | 18.x+ (推荐 20+) | 前端开发 |
| npm | 9+ | 前端包管理 |
| MySQL | 8.0+ | 本地数据库 |
| Redis | 6.0+ | 本地缓存（Windows 可用 Redis Desktop Manager） |

> **Windows Redis 路径**: Redis Desktop Manager 或使用 `D:\software\Redis\redis-cli.exe`

---

## 第一步：获取源码

```bash
git clone https://github.com/4iKZ/blogs_workspace.git
cd blogs_workspace
```

---

## 第二步：初始化数据库

```bash
# 建库、建表、触发器
mysql -u root -p < database/schema.sql

# 插入示例数据（含默认管理员 admin/123456）
mysql -u root -p blog_db < database/data.sql
```

---

## 第三步：配置后端

编辑 `src/main/resources/application.yml`，修改数据库和 Redis 连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_password

  redis:
    host: localhost
    port: 6379
    password: your_redis_password    # 无密码时删除此行
    database: 0
```

> **推荐**：使用环境变量方式配置，避免密码提交到代码库：
> ```bash
> export SPRING_DATASOURCE_PASSWORD=your_password
> ```

---

## 第四步：启动后端

```bash
# Windows
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

**或使用 Maven 直接运行：**
```bash
mvn spring-boot:run
```

后端启动成功后访问：
- API 服务：`http://localhost:8080`
- Swagger 文档：`http://localhost:8080/swagger-ui.html`

---

## 第五步：启动前端

```bash
cd frontend
npm install          # 首次需安装依赖
npm run dev          # 启动开发服务器
```

前端开发服务器运行在 `http://localhost:3000`。

**代理配置**（`vite.config.ts`）：
```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

所有 `/api/*` 请求会自动代理到后端 8080 端口。

---

## 第六步：验证运行

1. 打开 `http://localhost:3000`
2. 使用管理员账号登录：用户名 `admin`，密码 `123456`
3. 访问管理后台：`http://localhost:3000/admin`

---

## 开发命令速查

### 后端

```bash
# 开发运行
mvn spring-boot:run

# 构建 JAR（跳过测试）
mvn clean package -DskipTests

# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=UserServiceTest

# 运行特定测试方法
mvn test -Dtest=UserServiceTest#testLogin
```

### 前端

```bash
# 开发服务器
npm run dev

# 生产构建（含 TypeScript 类型检查）
npm run build

# 仅构建（跳过类型检查，更快）
npx vite build

# 预览生产构建
npm run preview
```

---

## IDE 配置建议

### IntelliJ IDEA（后端）

1. **启用 Lombok 注解处理**：Settings → Build → Annotation Processors → 勾选 Enable annotation processing
2. **Java SDK 设置为 21**
3. **安装插件**：Lombok、MyBatisX
4. **导入项目**：File → Open → 选择项目根目录（Maven 项目会自动识别）

### VS Code（前端）

推荐扩展：
- **Volar**（Vue Language Features）- Vue 3 支持
- **TypeScript Vue Plugin (Volar)** - TypeScript 支持
- **ESLint** - 代码检查
- **Prettier** - 代码格式化
- **Element Plus Snippets** - 组件代码片段

---

## 项目配置文件说明

| 文件 | 说明 |
|------|------|
| `src/main/resources/application.yml` | 主配置（数据库、Redis、JWT、日志） |
| `src/test/resources/application.yml` | 测试环境配置（H2 内存数据库） |
| `frontend/vite.config.ts` | Vite 构建配置（代理、路径别名） |
| `frontend/tsconfig.json` | TypeScript 编译配置 |
| `pom.xml` | Maven 依赖管理 |
| `frontend/package.json` | npm 依赖管理 |

---

## 测试环境说明

后端测试使用 **H2 内存数据库**，无需额外配置 MySQL：

```yaml
# src/test/resources/application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
```

---

## 常见问题

### Q: 启动报错 "Unable to connect to Redis"
Redis 未运行，启动 Redis 服务后重试：
```bash
# Linux
sudo systemctl start redis

# Windows（假设安装在 D:\software\Redis）
D:\software\Redis\redis-server.exe
```

### Q: 前端请求 404 / 502
确保后端已启动在 8080 端口，Vite 代理才能正常工作。

### Q: 端口占用
```bash
# Windows 查找占用 8080 的进程
netstat -ano | findstr :8080
taskkill /PID {pid} /F

# Linux
lsof -i :8080
kill -9 {pid}
```

### Q: Maven 下载依赖慢
配置阿里云 Maven 镜像，在 `~/.m2/settings.xml` 中添加：
```xml
<mirror>
  <id>aliyunmaven</id>
  <mirrorOf>*</mirrorOf>
  <name>阿里云公共仓库</name>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### Q: npm install 报错
```bash
# 清除缓存后重试
npm cache clean --force
npm install

# 使用淘宝镜像
npm install --registry=https://registry.npmmirror.com
```

### Q: TypeScript 编译报错 "Parameter 'xxx' is declared but its value is never read"
使用下划线前缀命名未使用的参数：
```typescript
// 错误
function handle(event: Event) { ... }

// 正确
function handle(_event: Event) { ... }
```
