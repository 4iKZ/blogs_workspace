package com.blog.event;

import com.blog.config.CacheConsistencyConfig;
import com.blog.utils.RedisCacheUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * #24: 缓存双删的第二删遇 Redis 抖动不再直接丢队列，
 * 而是有限次延迟重试；次数耗尽后才丢弃。
 */
class PersistentCacheInvalidationSchedulerTest {

    private PersistentCacheInvalidationScheduler scheduler(
            CacheInvalidationQueueManager queueManager,
            RedisCacheUtils redisCacheUtils) {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(true);

        PersistentCacheInvalidationScheduler s = new PersistentCacheInvalidationScheduler();
        setField(s, "queueManager", queueManager);
        setField(s, "redisCacheUtils", redisCacheUtils);
        setField(s, "cacheConfig", cacheConfig);
        return s;
    }

    @Test
    void processPendingInvalidations_whenRedisJitters_shouldRequeueForRetry() {
        CacheInvalidationQueueManager queueManager = mock(CacheInvalidationQueueManager.class);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);
        // 模拟 Redis 抖动：第二删抛异常
        doThrow(new RuntimeException("Redis jitter")).when(redisCacheUtils).deleteCache("article:1");

        CacheInvalidationEventDTO event = new CacheInvalidationEventDTO(
                "article:1", CacheOperation.DOUBLE_DELETE, null, System.currentTimeMillis());
        when(queueManager.getReadyEvents(anyLong())).thenReturn(Set.of(event));

        scheduler(queueManager, redisCacheUtils).processPendingInvalidations();

        // 不是直接丢弃，而是重新入队延迟重试
        ArgumentCaptor<CacheInvalidationEventDTO> captor =
                ArgumentCaptor.forClass(CacheInvalidationEventDTO.class);
        verify(queueManager).addToQueue(captor.capture());
        CacheInvalidationEventDTO retried = captor.getValue();
        assertThat(retried.getCacheKey()).isEqualTo("article:1");
        assertThat(retried.getRetryCount()).isEqualTo(1);
        assertThat(retried.getExecuteTime()).isGreaterThan(System.currentTimeMillis());
        // 旧成员被移除（新成员已入队成功后）
        verify(queueManager).removeFromQueue(event);
    }

    @Test
    void processPendingInvalidations_whenRetriesExhausted_shouldDiscard() {
        CacheInvalidationQueueManager queueManager = mock(CacheInvalidationQueueManager.class);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);
        doThrow(new RuntimeException("Redis jitter")).when(redisCacheUtils).deleteCache("article:1");

        CacheInvalidationEventDTO event = new CacheInvalidationEventDTO(
                "article:1", CacheOperation.DOUBLE_DELETE, null, System.currentTimeMillis());
        event.setRetryCount(3);
        when(queueManager.getReadyEvents(anyLong())).thenReturn(Set.of(event));

        scheduler(queueManager, redisCacheUtils).processPendingInvalidations();

        // 重试耗尽：不再入队，直接丢弃
        verify(queueManager, never()).addToQueue(any());
        verify(queueManager).removeFromQueue(event);
    }

    @Test
    void processPendingInvalidations_whenRequeueFails_shouldKeepOriginalEvent() {
        CacheInvalidationQueueManager queueManager = mock(CacheInvalidationQueueManager.class);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);
        doThrow(new RuntimeException("Redis jitter")).when(redisCacheUtils).deleteCache("article:1");
        // 重新入队也失败（Redis 持续抖动）
        doThrow(new RuntimeException("Redis down")).when(queueManager).addToQueue(any());

        CacheInvalidationEventDTO event = new CacheInvalidationEventDTO(
                "article:1", CacheOperation.DOUBLE_DELETE, null, System.currentTimeMillis());
        when(queueManager.getReadyEvents(anyLong())).thenReturn(Set.of(event));

        scheduler(queueManager, redisCacheUtils).processPendingInvalidations();

        // 入队失败时不移除旧成员，下一轮继续重试 → 队列项未丢
        verify(queueManager, never()).removeFromQueue(any());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
