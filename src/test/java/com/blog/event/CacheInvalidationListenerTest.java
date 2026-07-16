package com.blog.event;

import com.blog.config.CacheConsistencyConfig;
import com.blog.utils.RedisCacheUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CacheInvalidationListenerTest {

    @Test
    void handleCacheInvalidation_disabled_shouldSkip() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(false);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);

        CacheInvalidationListener listener = new CacheInvalidationListener();
        setField(listener, "redisCacheUtils", redisCacheUtils);
        setField(listener, "cacheConfig", cacheConfig);
        setField(listener, "queueManager", mock(CacheInvalidationQueueManager.class));

        CacheInvalidationEvent event = CacheInvalidationEvent.delete(this, "key:1");
        listener.handleCacheInvalidation(event);

        verifyNoInteractions(redisCacheUtils);
    }

    @Test
    void handleCacheInvalidation_delete_shouldRemoveCache() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(true);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);

        CacheInvalidationListener listener = new CacheInvalidationListener();
        setField(listener, "redisCacheUtils", redisCacheUtils);
        setField(listener, "cacheConfig", cacheConfig);
        setField(listener, "queueManager", mock(CacheInvalidationQueueManager.class));

        CacheInvalidationEvent event = CacheInvalidationEvent.delete(this, "article:1");
        listener.handleCacheInvalidation(event);

        verify(redisCacheUtils, times(1)).deleteCache("article:1");
    }

    @Test
    void handleCacheInvalidation_doubleDelete_shouldQueueWhenDelayed() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(true);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);
        CacheInvalidationQueueManager queueManager = mock(CacheInvalidationQueueManager.class);

        CacheInvalidationListener listener = new CacheInvalidationListener();
        setField(listener, "redisCacheUtils", redisCacheUtils);
        setField(listener, "cacheConfig", cacheConfig);
        setField(listener, "queueManager", queueManager);

        CacheInvalidationEvent event = CacheInvalidationEvent.doubleDelete(this, "article:1", 1500);
        listener.handleCacheInvalidation(event);

        verify(redisCacheUtils, never()).deleteCache(anyString());
        verify(queueManager, times(1)).addToQueue(any(CacheInvalidationEventDTO.class));
    }

    @Test
    void handleCacheInvalidation_update_shouldSetCache() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(true);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);

        CacheInvalidationListener listener = new CacheInvalidationListener();
        setField(listener, "redisCacheUtils", redisCacheUtils);
        setField(listener, "cacheConfig", cacheConfig);
        setField(listener, "queueManager", mock(CacheInvalidationQueueManager.class));

        CacheInvalidationEvent event = CacheInvalidationEvent.update(this, "article:1", "value");
        listener.handleCacheInvalidation(event);

        verify(redisCacheUtils, times(1)).setCache(eq("article:1"), eq("value"), anyLong(), eq(TimeUnit.DAYS));
    }

    @Test
    void handleCacheInvalidation_update_nullValue_shouldSkip() {
        CacheConsistencyConfig cacheConfig = mock(CacheConsistencyConfig.class);
        when(cacheConfig.isEnabled()).thenReturn(true);
        RedisCacheUtils redisCacheUtils = mock(RedisCacheUtils.class);

        CacheInvalidationListener listener = new CacheInvalidationListener();
        setField(listener, "redisCacheUtils", redisCacheUtils);
        setField(listener, "cacheConfig", cacheConfig);
        setField(listener, "queueManager", mock(CacheInvalidationQueueManager.class));

        CacheInvalidationEvent event = CacheInvalidationEvent.update(this, "article:1", null);
        listener.handleCacheInvalidation(event);

        verify(redisCacheUtils, never()).setCache(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
