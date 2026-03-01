package com.blog.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 缓存失效事件
 * 
 * 用于在事务提交后异步处理缓存失效，确保缓存与数据库的一致性。
 * 支持三种操作类型：
 * - DELETE: 直接删除缓存
 * - UPDATE: 更新缓存值
 * - DOUBLE_DELETE: 延迟双删（先删除，延迟后再删除）
 */
@Getter
public class CacheInvalidationEvent extends ApplicationEvent {

    private final String cacheKey;
    private final CacheOperation operation;
    private final Object value;
    private final long executeTime;

    public CacheInvalidationEvent(Object source, String cacheKey,
            CacheOperation operation, Object value, long delayMs) {
        super(source);
        this.cacheKey = cacheKey;
        this.operation = operation;
        this.value = value;
        this.executeTime = System.currentTimeMillis() + delayMs;
    }

    public static CacheInvalidationEvent delete(Object source, String cacheKey) {
        return new CacheInvalidationEvent(source, cacheKey, CacheOperation.DELETE, null, 0);
    }

    public static CacheInvalidationEvent doubleDelete(Object source, String cacheKey, long delayMs) {
        return new CacheInvalidationEvent(source, cacheKey, CacheOperation.DOUBLE_DELETE, null, delayMs);
    }

    public static CacheInvalidationEvent update(Object source, String cacheKey, Object value) {
        return new CacheInvalidationEvent(source, cacheKey, CacheOperation.UPDATE, value, 0);
    }

    public long getRemainingDelayMs() {
        return Math.max(0, executeTime - System.currentTimeMillis());
    }

    public boolean needsDelay() {
        return executeTime > System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "CacheInvalidationEvent{" +
                "cacheKey='" + cacheKey + '\'' +
                ", operation=" + operation +
                ", executeTime=" + executeTime +
                '}';
    }
}
