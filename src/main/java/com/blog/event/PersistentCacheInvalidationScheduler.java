package com.blog.event;

import com.blog.config.CacheConsistencyConfig;
import com.blog.utils.RedisCacheUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PersistentCacheInvalidationScheduler {

    @Autowired
    private CacheInvalidationQueueManager queueManager;

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private CacheConsistencyConfig cacheConfig;

    @Scheduled(fixedDelay = 100)
    public void processPendingInvalidations() {
        if (!cacheConfig.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        Set<Object> readyEvents = queueManager.getReadyEvents(now);

        if (readyEvents == null || readyEvents.isEmpty()) {
            return;
        }

        for (Object eventObj : readyEvents) {
            if (!(eventObj instanceof CacheInvalidationEventDTO)) {
                queueManager.removeFromQueue(eventObj);
                continue;
            }

            CacheInvalidationEventDTO eventDTO = (CacheInvalidationEventDTO) eventObj;

            try {
                executeInvalidation(eventDTO);
                queueManager.removeFromQueue(eventDTO);
            } catch (Exception e) {
                log.error("执行缓存失效失败: {}", eventDTO, e);
                queueManager.removeFromQueue(eventDTO);
            }
        }
    }

    private void executeInvalidation(CacheInvalidationEventDTO eventDTO) {
        String cacheKey = eventDTO.getCacheKey();

        switch (eventDTO.getOperation()) {
            case DELETE:
                redisCacheUtils.deleteCache(cacheKey);
                log.debug("缓存删除成功: key={}", cacheKey);
                break;

            case DOUBLE_DELETE:
                redisCacheUtils.deleteCache(cacheKey);
                log.debug("延迟双删执行成功: key={}", cacheKey);
                break;

            case UPDATE:
                Object value = eventDTO.getValue();
                if (value != null) {
                    redisCacheUtils.setCache(cacheKey, value, 7, TimeUnit.DAYS);
                    log.debug("缓存更新成功: key={}", cacheKey);
                } else {
                    log.warn("缓存更新失败，值为空: key={}", cacheKey);
                }
                break;

            default:
                log.warn("未知的缓存操作类型: {}", eventDTO.getOperation());
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredEvents() {
        if (!cacheConfig.isEnabled()) {
            return;
        }
        queueManager.cleanExpiredEvents();
    }
}
