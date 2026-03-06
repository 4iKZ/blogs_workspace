# 缓存策略设计

[← 返回 Wiki 首页](./Home.md)

---

## 缓存总体设计

Lumina 使用 **多层缓存策略**，针对不同业务场景选择合适的缓存机制：

```
┌──────────────────────────────────────────────────────────┐
│             应用层 Spring Cache (@Cacheable)              │
│          热门文章列表查询结果（事件驱动失效）              │
└──────────────────────────────────────────────────────────┘
                          ↕
┌──────────────────────────────────────────────────────────┐
│                    Redis 缓存层                           │
│  ┌─────────────────┐  ┌──────────────┐  ┌─────────────┐ │
│  │   String/Hash   │  │    ZSet      │  │  Set/List   │ │
│  │ 点赞/收藏状态   │  │  文章排行榜  │  │  验证码等   │ │
│  └─────────────────┘  └──────────────┘  └─────────────┘ │
└──────────────────────────────────────────────────────────┘
                          ↕
┌──────────────────────────────────────────────────────────┐
│                    MySQL 持久层                           │
└──────────────────────────────────────────────────────────┘
```

---

## Redis 配置

```yaml
spring:
  redis:
    host: 59.110.22.74
    port: 6379
    password: ...
    timeout: 5000ms
    database: 0
    lettuce:
      pool:
        max-active: 8    # 最大连接数
        max-wait: -1ms   # 无限等待
        max-idle: 8      # 最大空闲连接
        min-idle: 0      # 最小空闲连接
```

---

## Redis Key 规范

| Key 模式 | 类型 | TTL | 说明 |
|---------|------|-----|------|
| `user:comment:like:{userId}:{commentId}` | String | 7天 | 评论点赞状态 |
| `user:article:like:{userId}:{articleId}` | String | 7天 | 文章点赞状态 |
| `user:article:fav:{userId}:{articleId}` | String | 7天 | 文章收藏状态 |
| `article:rank:day` | ZSet | 永久（定时清空） | 文章日榜（score=浏览量+点赞加权） |
| `article:rank:week` | ZSet | 永久（定时清空） | 文章周榜 |
| `captcha:{key}` | String | 5分钟 | 验证码 |
| `lock:{bizType}:{id}` | String | 短暂（锁定期） | 分布式锁 |

---

## 热门文章排行榜（Redis ZSet）

### 设计方案

使用 Redis `ZSet`（有序集合）存储文章排行，`score` 为综合热度分值：

```
ZADD article:rank:day {score} {articleId}
ZREVRANGE article:rank:day 0 9  // 取前10名
```

### 触发时机

```java
// 文章被浏览时：
articleRankService.incrementViewCount(articleId)
    → ZINCRBY article:rank:day 1.0 {articleId}
    → ZINCRBY article:rank:week 1.0 {articleId}

// 文章被点赞时：
articleRankService.incrementLikeCount(articleId)
    → ZINCRBY article:rank:day 2.0 {articleId}  // 点赞权重更高
```

### 榜单重置（定时任务）

```java
// RankResetSchedule.java
@Scheduled(cron = "0 0 0 * * ?")     // 每天零点
void resetDayRank()                    // DEL article:rank:day

@Scheduled(cron = "0 0 0 ? * MON")   // 每周一零点
void resetWeekRank()                   // DEL article:rank:week
```

### 服务器启动初始化

`ArticleRankInitializer` 在 Spring Boot 启动完成后，从 MySQL 加载近期热门文章数据到 Redis ZSet，确保冷启动后榜单不为空。

---

## Spring Cache（@Cacheable）

用于缓存热门文章**查询结果**（List<ArticleDTO>），避免频繁查库：

```java
@Cacheable(value = "hotArticles", key = "#rankType + ':' + #limit")
List<ArticleDTO> getHotArticles(String rankType, Integer limit) { ... }
```

### 缓存失效策略（事件驱动）

当文章浏览量或点赞数变化时，发布事件清除 Spring Cache：

```java
// ArticleEventListener.java
@EventListener
void handleArticleViewCountChange(ArticleViewCountChangeEvent event) {
    hotArticleCacheEvictionService.evictAll();  // 清除所有 hotArticles Cache
}
```

> **注意**：只清除 Spring Cache 查询结果，不清除 Redis ZSet 排行数据。

---

## 缓存双删策略（Cache Double Delete）

针对写操作的缓存一致性问题，采用「先删缓存 → 写数据库 → 延迟再删缓存」策略：

```yaml
cache:
  consistency:
    enabled: true
    enable-double-delete: true         # 启用双删
    delayed-delete-ms: 1500            # 延迟删除时间（1.5秒）
    enable-async-invalidation: true    # 启用异步缓存失效
```

### 流程

```
写操作（更新文章/用户信息）
    │
    ├─ 1. 删除相关 Redis Key（立即）
    │
    ├─ 2. 更新 MySQL 数据
    │
    └─ 3. 延迟 1500ms 后再次删除 Redis Key（异步）
              → 防止数据库主从延迟导致读到旧数据
```

---

## 事务同步缓存更新

点赞、收藏等操作先写 DB，提交后再更新 Redis，确保数据库提交成功才更新缓存：

```java
// CommentServiceImpl.java（点赞核心逻辑）
@Transactional
void likeComment(Long commentId, Long userId) {
    
    // 1. 写入数据库
    commentLikeMapper.insert(new CommentLike(commentId, userId));
    
    // 2. 注册事务同步回调
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 3. DB 提交成功后，才更新 Redis
                String likeCacheKey = "user:comment:like:" + userId + ":" + commentId;
                redisCacheUtils.setCache(likeCacheKey, true, 7, TimeUnit.DAYS);
            }
        }
    );
}
```

---

## 缓存一致性验证（定时任务）

`CacheConsistencyVerifier` 每5分钟随机抽样100条数据，校验 Redis 缓存与 MySQL 数据库的一致性：

```yaml
cache:
  consistency:
    enable-verification: true
    verification-interval-minutes: 5
    verification-sample-size: 100
```

发现不一致时，以 MySQL 数据为准，自动修复 Redis 缓存。

---

## 分布式锁实现

`RedisDistributedLock` 基于 Redis `SET key value NX PX timeout` 命令：

```java
// 加锁
String lockValue = UUID.randomUUID().toString();
Boolean success = redisTemplate.opsForValue().setIfAbsent(
    lockKey, lockValue, 30, TimeUnit.SECONDS
);

// 释放锁（Lua 脚本保证原子性）
String luaScript =
    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
    "  return redis.call('del', KEYS[1]) " +
    "else return 0 end";
redisTemplate.execute(new DefaultRedisScript<>(luaScript, Long.class),
    Collections.singletonList(lockKey), lockValue);
```

**锁 Key 命名规范：**

```java
// 评论点赞锁
String lockKey = "lock:comment:like:" + userId + ":" + commentId;

// 文章点赞锁
String lockKey = "lock:article:like:" + userId + ":" + articleId;
```

---

## 验证码缓存

```
POST /api/captcha/generate
    │
    └─ 生成随机验证码（4位数字+字母）
       SET captcha:{key} {code} EX 300    // 存入 Redis，5分钟有效
       返回 captchaKey 给前端

注册时：
    提交 captcha + captchaKey
    GET captcha:{captchaKey} → 对比验证
    DEL captcha:{captchaKey}              // 使用后即删除，防重放
```

---

## 缓存穿透防护

目前通过以下措施防止缓存穿透：

1. **参数校验**：Controller 层对 ID 等参数进行合法性校验
2. **数据库查询结果为空时不缓存**（避免空值缓存泛滥）
3. **Spring Cache key 包含业务参数**，不同参数使用不同 key

> 如需增强，可在 `RedisCacheUtils` 中增加空值缓存逻辑（TTL 设为较短时间，如 60 秒）。
