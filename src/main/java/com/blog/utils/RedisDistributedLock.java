package com.blog.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 * 用于解决并发场景下的数据一致性问题
 */
@Component
@Slf4j
public class RedisDistributedLock {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_EXPIRE_TIME = 10; // 默认锁过期时间：10秒
    private static final long DEFAULT_WAIT_TIME = 3; // 默认等待时间：3秒

    /**
     * 获取分布式锁
     * @param lockKey 锁的key
     * @return 锁的value（用于释放锁时验证），获取失败返回null
     */
    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS, DEFAULT_WAIT_TIME, TimeUnit.SECONDS);
    }

    /**
     * 获取分布式锁（带自定义过期时间）
     * @param lockKey 锁的key
     * @param expireTime 锁的过期时间
     * @param expireUnit 时间单位
     * @return 锁的value（用于释放锁时验证），获取失败返回null
     */
    public String tryLock(String lockKey, long expireTime, TimeUnit expireUnit) {
        return tryLock(lockKey, expireTime, expireUnit, DEFAULT_WAIT_TIME, TimeUnit.SECONDS);
    }

    /**
     * 获取分布式锁（完整参数）
     * @param lockKey 锁的key
     * @param expireTime 锁的过期时间
     * @param expireUnit 过期时间单位
     * @param waitTime 最大等待时间
     * @param waitUnit 等待时间单位
     * @return 锁的value（用于释放锁时验证），获取失败返回null
     */
    public String tryLock(String lockKey, long expireTime, TimeUnit expireUnit,
                          long waitTime, TimeUnit waitUnit) {
        String lockValue = UUID.randomUUID().toString();
        String fullKey = LOCK_PREFIX + lockKey;
        long startTime = System.currentTimeMillis();
        long waitMillis = waitUnit.toMillis(waitTime);

        // 循环尝试获取锁，直到超时
        while (true) {
            // 使用 SET NX EX 命令原子性地设置锁
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(fullKey, lockValue, expireTime, expireUnit);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("获取分布式锁成功，key: {}", lockKey);
                return lockValue;
            }

            // 检查是否超时
            if (System.currentTimeMillis() - startTime > waitMillis) {
                log.warn("获取分布式锁超时，key: {}", lockKey);
                return null;
            }

            // 短暂休眠后重试
            try {
                TimeUnit.MILLISECONDS.sleep(50); // 50ms重试间隔
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("获取分布式锁被中断，key: {}", lockKey);
                return null;
            }
        }
    }

    /**
     * 释放分布式锁
     * 使用 Lua 脚本确保只释放自己持有的锁
     * @param lockKey 锁的key
     * @param lockValue 锁的value（获取锁时返回的值）
     * @return true=释放成功，false=释放失败
     */
    public boolean unlock(String lockKey, String lockValue) {
        if (lockValue == null) {
            return false;
        }

        String fullKey = LOCK_PREFIX + lockKey;

        // Lua脚本：只有当锁的value匹配时才删除（避免误删其他线程的锁）
        String luaScript =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else " +
                        "return 0 " +
                        "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(fullKey), lockValue);

        boolean unlocked = result != null && result == 1;
        if (unlocked) {
            log.debug("释放分布式锁成功，key: {}", lockKey);
        } else {
            log.warn("释放分布式锁失败（锁可能已过期或被其他线程持有），key: {}", lockKey);
        }
        return unlocked;
    }

    /**
     * 强制释放锁（不验证value，仅用于异常情况）
     * @param lockKey 锁的key
     */
    public void forceUnlock(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        redisTemplate.delete(fullKey);
        log.warn("强制释放分布式锁，key: {}", lockKey);
    }

    /**
     * 检查锁是否存在
     * @param lockKey 锁的key
     * @return true=存在，false=不存在
     */
    public boolean isLocked(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
    }

    /**
     * 生成评论点赞锁的key
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 锁key
     */
    public static String generateCommentLikeLockKey(Long commentId, Long userId) {
        return "comment:like:" + commentId + ":" + userId;
    }
}
