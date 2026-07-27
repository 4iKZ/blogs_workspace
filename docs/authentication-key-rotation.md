# 认证密钥轮换运行手册

1. 在密钥管理系统生成至少 32 字节的新 `JWT_SECRET`、`JWT_REFRESH_SECRET` 和
   `PASSWORD_RESET_HMAC_KEY`，禁止写入仓库。
2. 先部署能读取新密钥的应用实例，再一次性切换全部实例的环境变量并滚动重启。
3. JWT 或 Refresh 密钥轮换会使对应旧令牌失效。清理 Redis 中
   `auth:refresh:jti:*` 与 `auth:refresh:user-jtis:*` 会话键时使用受控的
   `SCAN` 批处理运维任务，禁止在线请求使用 `KEYS`。
4. 密钥轮换后通知用户重新登录，并监控 401、刷新重放拒绝和登录失败率。
5. 若仅需撤销单个用户会话，递增 `users.token_version`，并按该用户的
   `auth:refresh:user-jtis:<userId>` 索引删除对应 jti 键。
