# 认证密钥轮换运行手册

1. 在密钥管理系统生成至少 32 字节的新 `JWT_SECRET`、`JWT_REFRESH_SECRET` 和
   `PASSWORD_RESET_HMAC_KEY`，禁止写入仓库。
2. 本版本不支持新旧 JWT 声明格式或签名密钥混合运行，禁止滚动混部。安排维护窗口，
   先从负载均衡摘除并停止所有旧节点。
3. 停机后执行 `database/migrations/20260727_p1_auth_token_version.sql` 并确认
   `users.token_version` 已存在。旧版本节点在迁移后不得重新接入。
4. 使用受控的 `SCAN` 批处理任务清理 Redis 中
   `auth:refresh:jti:*`、`auth:refresh:user-jtis:*`、`auth:refresh:user-families:*`、
   `auth:refresh:family-jtis:*`、`auth:refresh:family-active:*` 与旧
   `auth:refresh:user:*` 会话键，禁止在线请求使用 `KEYS`。
5. 一次性切换全部节点的 `JWT_SECRET`、`JWT_REFRESH_SECRET` 与
   `PASSWORD_RESET_HMAC_KEY`，启动同一版本的新节点，通过健康检查后再恢复流量。
6. JWT 或 Refresh 密钥轮换会使所有旧令牌失效。通知用户重新登录，并监控 401、
   刷新令牌重放拒绝、登录失败率和 Redis 清理重试日志。
7. 若仅需撤销单个用户会话，必须使用原子 SQL 递增 `users.token_version`，并通过统一
   会话撤销服务清理该用户的令牌族、JTI 索引和旧格式会话键；不得只删除单个 Redis 键。
