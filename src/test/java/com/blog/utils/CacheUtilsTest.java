package com.blog.utils;

import com.blog.config.CacheConsistencyConfig;
import com.blog.event.CacheInvalidationEvent;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CacheUtilsTest {

    @Test
    void deleteCacheWithDoubleDelete_disabled_shouldDeleteImmediately() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(false);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        CacheUtils cacheUtils = new CacheUtils();
        setField(cacheUtils, "cacheConfig", cacheConfig);
        setField(cacheUtils, "redisCacheUtils", redisCacheUtils);
        setField(cacheUtils, "eventPublisher", eventPublisher);

        cacheUtils.deleteCacheWithDoubleDelete("key:1");

        verify(redisCacheUtils, times(1)).deleteCache("key:1");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deleteCacheWithDoubleDelete_enabled_shouldDeleteAndPublishEvent() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(true);
        when(cacheConfig.getDelayedDeleteMs()).thenReturn(1500L);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        CacheUtils cacheUtils = new CacheUtils();
        setField(cacheUtils, "cacheConfig", cacheConfig);
        setField(cacheUtils, "redisCacheUtils", redisCacheUtils);
        setField(cacheUtils, "eventPublisher", eventPublisher);

        cacheUtils.deleteCacheWithDoubleDelete("key:1");

        verify(redisCacheUtils, times(1)).deleteCache("key:1");
        verify(eventPublisher, times(1)).publishEvent(any(CacheInvalidationEvent.class));
    }

    @Test
    void deleteCacheAsync_enabled_shouldPublishDeleteEvent() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(true);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        CacheUtils cacheUtils = new CacheUtils();
        setField(cacheUtils, "cacheConfig", cacheConfig);
        setField(cacheUtils, "redisCacheUtils", redisCacheUtils);
        setField(cacheUtils, "eventPublisher", eventPublisher);

        cacheUtils.deleteCacheAsync("key:1");

        verify(redisCacheUtils, never()).deleteCache(anyString());
        verify(eventPublisher, times(1)).publishEvent(any(CacheInvalidationEvent.class));
    }

    @Test
    void deleteCacheAsync_disabled_shouldDeleteImmediately() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(false);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        CacheUtils cacheUtils = new CacheUtils();
        setField(cacheUtils, "cacheConfig", cacheConfig);
        setField(cacheUtils, "redisCacheUtils", redisCacheUtils);
        setField(cacheUtils, "eventPublisher", eventPublisher);

        cacheUtils.deleteCacheAsync("key:1");

        verify(redisCacheUtils, times(1)).deleteCache("key:1");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deleteCacheWithDelay_enabled_shouldPublishDelayEvent() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(true);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        CacheUtils cacheUtils = new CacheUtils();
        setField(cacheUtils, "cacheConfig", cacheConfig);
        setField(cacheUtils, "redisCacheUtils", mock(RedisCacheUtils.class));
        setField(cacheUtils, "eventPublisher", eventPublisher);

        cacheUtils.deleteCacheWithDelay("key:1", 500);

        verify(eventPublisher, times(1)).publishEvent(any(CacheInvalidationEvent.class));
    }

    @Test
    void deleteCacheWithDoubleDelete_batch_shouldProcessAllKeys() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(false);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);

        CacheUtils cacheUtils = new CacheUtils();
        setField(cacheUtils, "cacheConfig", cacheConfig);
        setField(cacheUtils, "redisCacheUtils", redisCacheUtils);
        setField(cacheUtils, "eventPublisher", mock(ApplicationEventPublisher.class));

        cacheUtils.deleteCacheWithDoubleDelete("key:1", "key:2", "key:3");

        verify(redisCacheUtils, times(3)).deleteCache(anyString());
        verify(redisCacheUtils).deleteCache("key:1");
        verify(redisCacheUtils).deleteCache("key:2");
        verify(redisCacheUtils).deleteCache("key:3");
    }

    @Test
    void updateCacheAsync_enabled_shouldPublishUpdateEvent() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(true);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        CacheUtils cacheUtils = new CacheUtils();
        setField(cacheUtils, "cacheConfig", cacheConfig);
        setField(cacheUtils, "redisCacheUtils", mock(RedisCacheUtils.class));
        setField(cacheUtils, "eventPublisher", eventPublisher);

        cacheUtils.updateCacheAsync("key:1", "value");

        verify(eventPublisher, times(1)).publishEvent(any(CacheInvalidationEvent.class));
    }

    @Test
    void updateCacheAsync_disabled_shouldSetCacheDirectly() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(false);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);

        CacheUtils cacheUtils = new CacheUtils();
        setField(cacheUtils, "cacheConfig", cacheConfig);
        setField(cacheUtils, "redisCacheUtils", redisCacheUtils);
        setField(cacheUtils, "eventPublisher", mock(ApplicationEventPublisher.class));

        cacheUtils.updateCacheAsync("key:1", "value");

        verify(redisCacheUtils, times(1)).setCache(eq("key:1"), eq("value"), anyLong(), eq(TimeUnit.DAYS));
        verifyNoInteractions(mock(ApplicationEventPublisher.class));
    }

    private static void setField(CacheUtils target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = CacheUtils.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
