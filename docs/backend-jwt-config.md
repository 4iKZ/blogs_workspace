# 后端 JWT 配置与过滤器容错

## 问题
- 终端日志显示 `JWT令牌验证失败: The specified key byte array is 0 bits ...`，说明 `jwt.secret` 未配置或长度不足，导致 HMAC-SHA 验证失败，触发 403。

## 配置建议
- 在 `application.yml` 或环境变量中配置 `jwt.secret`：使用 Base64 编码的随机 32 字节以上的密钥。

```yaml
jwt:
  secret: "pC2H6cV...（Base64 编码的随机字节串）"
  expiration: 86400000 # 24h
  prefix: "Bearer"
  header: "Authorization"
```

## 代码建议（JWTUtils）
- 使用 JJWT 的 `Keys.hmacShaKeyFor` 并在启动时校验长度：

```java
byte[] secretBytes = Base64.getDecoder().decode(secret);
if (secretBytes.length < 32) {
  throw new IllegalStateException("JWT secret length must be >= 256 bits");
}
Key key = Keys.hmacShaKeyFor(secretBytes);
```

## 过滤器容错（JwtAuthenticationFilter）
- 在 `doFilterInternal` 中对解析失败进行 `try/catch` 并记录 `warn/trace`，不要对公开端点直接返回 403；继续 `filterChain.doFilter(request, response)`。
- 仅在受保护端点上要求认证上下文存在。

## SecurityConfig
- 确保公开端点通过 `permitAll()` 并放在 `anyRequest().authenticated()` 前。
- 管理员端点维持 `hasRole('admin')` 要求。

## CORS
- 确保允许 `Authorization`、自定义头；如需 Cookie，设置 `allowCredentials(true)` 并限定来源。

## 日志
- 在令牌生成、刷新、验证失败处加入结构化日志：traceId、耗时、关键参数、异常栈，提升定位效率。
