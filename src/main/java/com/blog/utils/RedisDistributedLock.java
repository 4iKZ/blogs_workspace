package com.blog.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 * 用于解决并发场景下的数据一致性问题
 */
@Component
@Slf4j
public class RedisDistributedLock implements DisposableBean {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_EXPIRE_TIME = 10; // 默认锁过期时间：10 秒
    private static final long DEFAULT_WAIT_TIME = 3; // 默认等待时间：3 秒

    // 看门狗线程池，用于自动续期锁
    private final ScheduledExecutorService watchdogExecutor = 
        Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "lock-watchdog");
            t.setDaemon(true);
            return t;
        });

    // 存储看门狗任务，用于取消
    private final ConcurrentHashMap<String, ScheduledFuture<?>> watchdogFutures = new ConcurrentHashMap<>();

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
                // 启动看门狗，自动续期锁
                startWatchdog(fullKey, lockValue, expireTime, expireUnit);
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
            // 取消看门狗
            cancelWatchdog(fullKey);
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
     * 生成评论点赞锁的 key
     * @param commentId 评论 ID
     * @param userId 用户 ID
     * @return 锁 key
     */
    public static String generateCommentLikeLockKey(Long commentId, Long userId) {
        return "comment:like:" + commentId + ":" + userId;
    }

    /**
     * 启动看门狗，定期续期锁
     * @param lockKey 锁的完整 key
     * @param lockValue 锁的 value（用于验证）
     * @param expireTime 过期时间
     * @param expireUnit 时间单位
     */
    private void startWatchdog(String lockKey, String lockValue, long expireTime, TimeUnit expireUnit) {
        // 每 expireTime/3 时间续期一次，确保锁不会过期
        long delay = expireUnit.toMillis(expireTime) / 3;
        if (delay < 100) {
            delay = 100; // 最小续期间隔 100ms
        }

        ScheduledFuture<?> future = watchdogExecutor.scheduleAtFixedRate(() -> {
            try {
                // 检查锁是否还存在
                Boolean exists = redisTemplate.hasKey(lockKey);
                if (Boolean.FALSE.equals(exists)) {
                    // 锁已不存在，取消看门狗
                    cancelWatchdog(lockKey);
                    log.debug("看门狗取消：锁已不存在，key: {}", lockKey);
                    return;
                }

                // 检查锁的 value 是否匹配（防止误续其他线程的锁）
                Object currentValue = redisTemplate.opsForValue().get(lockKey);
                if (!lockValue.equals(currentValue)) {
                    // 锁的 value 不匹配，说明锁已被其他线程获取，取消看门狗
                    cancelWatchdog(lockKey);
                    log.warn("看门狗取消：锁 value 不匹配，key: {}", lockKey);
                    return;
                }

                // 续期锁
                redisTemplate.expire(lockKey, expireTime, expireUnit);
                log.trace("看门狗续期：key: {}, 续期至：{} {}", lockKey, expireTime, expireUnit);
            } catch (Exception e) {
                log.error("看门狗续期失败，key: {}", lockKey, e);
            }
        }, delay, delay, TimeUnit.MILLISECONDS);

        watchdogFutures.put(lockKey, future);
        log.debug("看门狗启动：key: {}, 续期间隔：{}ms", lockKey, delay);
    }

    /**
     * 取消看门狗任务
     * @param lockKey 锁的完整 key
     */
    private void cancelWatchdog(String lockKey) {
        ScheduledFuture<?> future = watchdogFutures.remove(lockKey);
        if (future != null) {
            future.cancel(false);
            log.debug("看门狗已取消：key: {}", lockKey);
        }
    }

    /**
     * 销毁方法，关闭线程池
     */
    @Override
    public void destroy() {
        log.info("关闭分布式锁看门狗线程池");
        // 取消所有看门狗任务
        for (ScheduledFuture<?> future : watchdogFutures.values()) {
            if (future != null) {
                future.cancel(false);
            }
        }
        watchdogFutures.clear();
        
        // 关闭线程池
        watchdogExecutor.shutdown();
        try {
            if (!watchdogExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("看门狗线程池未能在 5 秒内关闭，强制关闭");
                watchdogExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("关闭看门狗线程池被中断", e);
            watchdogExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
