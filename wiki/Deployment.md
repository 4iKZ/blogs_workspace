# 部署运维指南

[← 返回 Wiki 首页](./Home.md)

---

## 环境要求

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21+ | 必须 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 缓存/分布式锁 |
| Node.js | 18.x+ (推荐 20+) | 前端构建 |
| Nginx | 1.18+ | 反向代理 |
| Maven | 3.8+ | 后端构建 |

---

## 快速部署流程

### 1. 克隆代码

```bash
git clone https://github.com/4iKZ/blogs_workspace.git
cd blogs_workspace
```

### 2. 初始化数据库

```bash
# 建库、建表、触发器
mysql -u root -p < database/schema.sql

# 插入初始数据（含默认管理员 admin/123456）
mysql -u root -p blog_db < database/data.sql
```

### 3. 构建后端

```bash
# 跳过测试打包
mvn clean package -DskipTests

# 产物位于 target/blog-backend-0.0.1-SNAPSHOT.jar
```

### 4. 构建前端

```bash
cd frontend
npm install
npm run build
# 产物位于 frontend/dist/
```

### 5. 配置环境变量

```bash
# 必须设置的环境变量
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/blog_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false"
export SPRING_DATASOURCE_USERNAME="your_db_user"
export SPRING_DATASOURCE_PASSWORD="your_db_password"
export JWT_SECRET="your_random_32_byte_secret_key_here"
```

### 6. 启动后端服务

```bash
java -jar target/blog-backend-0.0.1-SNAPSHOT.jar
```

推荐使用 `nohup` 或 `systemd` 管理进程：

```bash
nohup java -jar target/blog-backend-0.0.1-SNAPSHOT.jar > logs/app.log 2>&1 &
```

### 7. 配置 Nginx

参考仓库中的 `nginx.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 本地上传文件访问
    location /uploads/ {
        alias /data/uploads/blog/;
        expires 30d;
        add_header Cache-Control "public, max-age=2592000";
        add_header Access-Control-Allow-Origin *;
    }

    # 前端静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        root /path/to/frontend/dist;
        expires 30d;
        add_header Cache-Control "public, max-age=2592000";
    }
}
```

---

## 应用配置

### application.yml 关键配置

```yaml
# 服务端口
server:
  port: 8080

# 数据库连接池（HikariCP）
spring:
  datasource:
    hikari:
      minimum-idle: 5          # 最小空闲连接
      maximum-pool-size: 15    # 最大连接数
      idle-timeout: 30000      # 空闲超时（30秒）
      max-lifetime: 1800000    # 连接最大生命周期（30分钟）

# Redis 连接池（Lettuce）
  redis:
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0

# 文件上传限制
  servlet:
    multipart:
      max-file-size: 10MB      # 单文件最大
      max-request-size: 50MB   # 请求体最大

# JWT 有效期（7天）
jwt:
  expiration: 604800000
```

### 通过环境变量覆盖配置

`application.yml` 支持 `${ENV_VAR:defaultValue}` 语法，所有敏感配置均应通过环境变量注入：

| 环境变量 | 说明 |
|---------|------|
| `SPRING_DATASOURCE_URL` | 数据库连接 URL |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥（至少 32 字节） |

---

## systemd 服务配置

创建 `/etc/systemd/system/lumina-blog.service`：

```ini
[Unit]
Description=Lumina Blog Backend
After=network.target mysql.service redis.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/lumina
ExecStart=/usr/bin/java -jar /opt/lumina/blog-backend-0.0.1-SNAPSHOT.jar
Environment="SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/blog_db?..."
Environment="SPRING_DATASOURCE_USERNAME=blog_user"
Environment="SPRING_DATASOURCE_PASSWORD=your_password"
Environment="JWT_SECRET=your_secret"
Restart=on-failure
RestartSec=10s
StandardOutput=append:/opt/lumina/logs/blog-backend.log
StandardError=append:/opt/lumina/logs/blog-backend.log

[Install]
WantedBy=multi-user.target
```

```bash
# 启用并启动
systemctl daemon-reload
systemctl enable lumina-blog
systemctl start lumina-blog
systemctl status lumina-blog
```

---

## 日志管理

日志配置（`application.yml`）：

```yaml
logging:
  file:
    name: logs/blog-backend.log    # 日志文件路径
    max-size: 10MB                 # 单文件最大大小
    max-history: 30                # 保留最近30天
  level:
    com.blog: info                 # 应用日志级别
```

日志文件位置：`logs/blog-backend.log`（相对于工作目录）

---

## 数据备份

### 自动备份（管理后台）

登录管理后台 → 系统设置 → 数据备份，可一键创建备份。

### 手动备份

```bash
# 导出数据库
mysqldump -u root -p blog_db > backup_$(date +%Y%m%d_%H%M%S).sql

# 恢复数据库
mysql -u root -p blog_db < backup_20250101_000000.sql
```

---

## 上传文件存储

### 本地存储路径

```
/data/uploads/blog/
├── images/          # 文章图片
├── avatars/         # 用户头像
└── attachments/     # 其他附件
```

### 火山引擎 TOS（云存储）

TOS 配置（`application.yml`）：

```yaml
tos:
  access-key-id: ...          # 建议通过环境变量注入
  secret-access-key: ...      # 建议通过环境变量注入
  endpoint: https://tos-cn-beijing.volces.com
  region: cn-beijing
  bucket-name: syhaox
  base-folder: old_book_system/
  acl: public-read
  default-image-style: lumina
```

---

## HTTPS 配置（推荐）

在 Nginx 中配置 SSL：

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # ... 其余配置同 HTTP
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

---

## 常见问题排查

### 启动后连不上数据库
- 检查 MySQL 是否运行：`systemctl status mysql`
- 检查连接 URL 和端口是否正确
- 检查防火墙是否放开了 3306 端口

### Redis 连接失败
- 检查 Redis 是否运行：`redis-cli ping`
- 检查 Redis 密码配置
- 检查 `redis.host` 配置是否正确

### 前端页面空白 / 404
- 检查 Nginx 配置中 `root` 路径是否指向正确的 `dist` 目录
- 检查 `try_files $uri $uri/ /index.html` 是否配置（SPA 路由需要）

### API 请求 403
- 检查 Nginx 是否正确代理了 `/api` 路径
- 检查 `proxy_pass http://localhost:8080` 是否包含尾部路径

### 图片上传失败
- 检查上传目录权限：`chmod -R 755 /data/uploads/blog/`
- 检查磁盘空间：`df -h`
- 检查 TOS 配置是否正确（如使用云存储）
