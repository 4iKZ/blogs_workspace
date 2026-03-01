package com.blog.utils;

import com.blog.config.CacheConsistencyConfig;
import com.blog.event.CacheInvalidationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 缓存工具增强类
 * 
 * 提供缓存一致性相关的增强功能：
 * - 延迟双删
 * - 异步删除
 * - 事务后置删除
 */
@Slf4j
@Component
public class CacheUtils {

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private CacheConsistencyConfig cacheConfig;

    /**
     * 延迟双删
     * 
     * 流程：
     * 1. 立即删除缓存
     * 2. 发布延迟删除事件（默认延迟500ms）
     * 
     * 适用场景：
     * - 写操作前的缓存失效
     * - 防止并发读请求将旧数据写入缓存
     */
    public void deleteCacheWithDoubleDelete(String cacheKey) {
        if (!cacheConfig.isEnabled()) {
            redisCacheUtils.deleteCache(cacheKey);
            return;
        }

        redisCacheUtils.deleteCache(cacheKey);
        log.debug("第一次删除缓存: key={}", cacheKey);

        CacheInvalidationEvent event = CacheInvalidationEvent.doubleDelete(
            this, cacheKey, cacheConfig.getDelayedDeleteMs()
        );
        eventPublisher.publishEvent(event);
    }

    /**
     * 延迟双删（自定义延迟时间）
     */
    public void deleteCacheWithDoubleDelete(String cacheKey, long delayMs) {
        if (!cacheConfig.isEnabled()) {
            redisCacheUtils.deleteCache(cacheKey);
            return;
        }

        redisCacheUtils.deleteCache(cacheKey);
        log.debug("第一次删除缓存: key={}", cacheKey);

        CacheInvalidationEvent event = CacheInvalidationEvent.doubleDelete(
            this, cacheKey, delayMs
        );
        eventPublisher.publishEvent(event);
    }

    /**
     * 异步删除缓存
     * 
     * 通过事件机制异步删除，不阻塞当前线程
     */
    public void deleteCacheAsync(String cacheKey) {
        if (!cacheConfig.isEnabled()) {
            redisCacheUtils.deleteCache(cacheKey);
            return;
        }

        CacheInvalidationEvent event = CacheInvalidationEvent.delete(this, cacheKey);
        eventPublisher.publishEvent(event);
    }

    /**
     * 延迟删除缓存
     * 
     * 延迟指定时间后删除缓存
     */
    public void deleteCacheWithDelay(String cacheKey, long delayMs) {
        if (!cacheConfig.isEnabled()) {
            redisCacheUtils.deleteCache(cacheKey);
            return;
        }

        CacheInvalidationEvent event = CacheInvalidationEvent.doubleDelete(
            this, cacheKey, delayMs
        );
        eventPublisher.publishEvent(event);
    }

    /**
     * 批量延迟双删
     */
    public void deleteCacheWithDoubleDelete(String... cacheKeys) {
        for (String cacheKey : cacheKeys) {
            deleteCacheWithDoubleDelete(cacheKey);
        }
    }

    /**
     * 批量异步删除
     */
    public void deleteCacheAsync(String... cacheKeys) {
        for (String cacheKey : cacheKeys) {
            deleteCacheAsync(cacheKey);
        }
    }

    /**
     * 更新缓存值（异步）
     */
    public void updateCacheAsync(String cacheKey, Object value) {
        if (!cacheConfig.isEnabled()) {
            redisCacheUtils.setCache(cacheKey, value, 7, java.util.concurrent.TimeUnit.DAYS);
            return;
        }

        CacheInvalidationEvent event = CacheInvalidationEvent.update(this, cacheKey, value);
        eventPublisher.publishEvent(event);
    }
}
