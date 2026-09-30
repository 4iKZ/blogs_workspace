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

    /**
     * #24: 执行失败时的最大重试次数，耗尽后丢弃（有 CacheConsistencyVerifier 兜底）
     */
    private static final int MAX_RETRIES = 3;
    /**
     * #24: 重试间隔：重新入队延迟 30 秒执行，避开 Redis 抖动窗口
     */
    private static final long RETRY_DELAY_MS = 30_000;

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
                retryOrDiscard(eventDTO);
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

    /**
     * #24: 执行失败不再直接丢弃。有限次重试：延迟重新入队，次数耗尽后丢弃。
     * 先入队新成员再移除旧成员（ZSet 成员是 DTO 序列化字节，改字段后即为新成员）；
     * 入队失败时旧成员保留，下一轮继续重试，不丢队列。
     */
    private void retryOrDiscard(CacheInvalidationEventDTO eventDTO) {
        if (eventDTO.getRetryCount() >= MAX_RETRIES) {
            log.warn("缓存失效重试次数耗尽，丢弃事件: {}", eventDTO);
            queueManager.removeFromQueue(eventDTO);
            return;
        }
        CacheInvalidationEventDTO retried = new CacheInvalidationEventDTO(
                eventDTO.getCacheKey(), eventDTO.getOperation(), eventDTO.getValue(),
                System.currentTimeMillis() + RETRY_DELAY_MS);
        retried.setRetryCount(eventDTO.getRetryCount() + 1);
        try {
            queueManager.addToQueue(retried);
        } catch (Exception requeueEx) {
            log.error("缓存失效事件重新入队失败，保留原事件下轮重试: {}", eventDTO, requeueEx);
            return;
        }
        queueManager.removeFromQueue(eventDTO);
        log.info("缓存失效执行失败，已重新入队 {}ms 后重试（第{}/{}次）: {}",
                RETRY_DELAY_MS, retried.getRetryCount(), MAX_RETRIES, eventDTO.getCacheKey());
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredEvents() {
        if (!cacheConfig.isEnabled()) {
            return;
        }
        queueManager.cleanExpiredEvents();
    }
}
