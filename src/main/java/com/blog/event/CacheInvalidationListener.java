package com.blog.event;

import com.blog.config.CacheConsistencyConfig;
import com.blog.utils.RedisCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CacheInvalidationListener {

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private CacheConsistencyConfig cacheConfig;

    @Autowired
    private CacheInvalidationQueueManager queueManager;

    @Async
    @EventListener
    public void handleCacheInvalidation(CacheInvalidationEvent event) {
        if (!cacheConfig.isEnabled()) {
            log.debug("缓存一致性功能已禁用，跳过事件: {}", event);
            return;
        }

        try {
            long delayMs = event.getRemainingDelayMs();

            if (delayMs > 0) {
                CacheInvalidationEventDTO dto = CacheInvalidationEventDTO.fromEvent(event);
                queueManager.addToQueue(dto);
                log.debug("延迟事件已存入Redis队列，等待 {}ms: {}", delayMs, event);
                return;
            }

            switch (event.getOperation()) {
                case DELETE:
                    executeDelete(event);
                    break;

                case DOUBLE_DELETE:
                    executeDoubleDelete(event);
                    break;

                case UPDATE:
                    executeUpdate(event);
                    break;

                default:
                    log.warn("未知的缓存操作类型: {}", event.getOperation());
            }

        } catch (Exception e) {
            log.error("缓存失效处理失败: {}", event, e);
        }
    }

    private void executeDelete(CacheInvalidationEvent event) {
        String cacheKey = event.getCacheKey();
        redisCacheUtils.deleteCache(cacheKey);
        log.debug("缓存删除成功: key={}", cacheKey);
    }

    private void executeDoubleDelete(CacheInvalidationEvent event) {
        String cacheKey = event.getCacheKey();
        redisCacheUtils.deleteCache(cacheKey);
        log.debug("延迟双删执行成功: key={}", cacheKey);
    }

    private void executeUpdate(CacheInvalidationEvent event) {
        String cacheKey = event.getCacheKey();
        Object value = event.getValue();

        if (value == null) {
            log.warn("缓存更新失败，值为空: key={}", cacheKey);
            return;
        }

        redisCacheUtils.setCache(cacheKey, value, 7, TimeUnit.DAYS);
        log.debug("缓存更新成功: key={}", cacheKey);
    }
}
