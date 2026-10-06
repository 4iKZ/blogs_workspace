package com.blog.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("热门文章缓存失效服务测试")
class HotArticleCacheEvictionServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache hotArticlesCache;

    @Mock
    private Cache hotArticlesPageCache;

    @InjectMocks
    private HotArticleCacheEvictionService hotArticleCacheEvictionService;

    @Test
    @DisplayName("应同时清理热门文章列表和分页缓存")
    void shouldEvictBothHotArticleCaches() {
        when(cacheManager.getCache(HotArticleCacheEvictionService.HOT_ARTICLES_CACHE)).thenReturn(hotArticlesCache);
        when(cacheManager.getCache(HotArticleCacheEvictionService.HOT_ARTICLES_PAGE_CACHE)).thenReturn(hotArticlesPageCache);

        hotArticleCacheEvictionService.evictAll();

        verify(hotArticlesCache, times(1)).clear();
        verify(hotArticlesPageCache, times(1)).clear();
    }

    @Test
    @DisplayName("缓存空间不存在时应安全跳过")
    void shouldSkipMissingCachesSafely() {
        when(cacheManager.getCache(anyString())).thenReturn(null);

        hotArticleCacheEvictionService.evictAll();

        verify(cacheManager, times(1)).getCache(HotArticleCacheEvictionService.HOT_ARTICLES_CACHE);
        verify(cacheManager, times(1)).getCache(HotArticleCacheEvictionService.HOT_ARTICLES_PAGE_CACHE);
        verifyNoInteractions(hotArticlesCache, hotArticlesPageCache);
    }

    @Test
    @DisplayName("节流：窗口内重复调用只清理一次")
    void throttled_secondCallWithinWindow_shouldSkip() throws Exception {
        setMinEvictIntervalMs(60_000L);
        when(cacheManager.getCache(HotArticleCacheEvictionService.HOT_ARTICLES_CACHE)).thenReturn(hotArticlesCache);
        when(cacheManager.getCache(HotArticleCacheEvictionService.HOT_ARTICLES_PAGE_CACHE)).thenReturn(hotArticlesPageCache);

        hotArticleCacheEvictionService.evictAllThrottled();
        hotArticleCacheEvictionService.evictAllThrottled();

        verify(hotArticlesCache, times(1)).clear();
        verify(hotArticlesPageCache, times(1)).clear();
    }

    @Test
    @DisplayName("节流：最小间隔为 0 时每次调用都清理")
    void throttled_zeroInterval_shouldClearEveryCall() throws Exception {
        setMinEvictIntervalMs(0L);
        when(cacheManager.getCache(HotArticleCacheEvictionService.HOT_ARTICLES_CACHE)).thenReturn(hotArticlesCache);
        when(cacheManager.getCache(HotArticleCacheEvictionService.HOT_ARTICLES_PAGE_CACHE)).thenReturn(hotArticlesPageCache);

        hotArticleCacheEvictionService.evictAllThrottled();
        hotArticleCacheEvictionService.evictAllThrottled();

        verify(hotArticlesCache, times(2)).clear();
        verify(hotArticlesPageCache, times(2)).clear();
    }

    @Test
    @DisplayName("evictAll 仍为即时清理，不受节流影响")
    void evictAll_shouldRemainImmediate() throws Exception {
        setMinEvictIntervalMs(60_000L);
        when(cacheManager.getCache(HotArticleCacheEvictionService.HOT_ARTICLES_CACHE)).thenReturn(hotArticlesCache);
        when(cacheManager.getCache(HotArticleCacheEvictionService.HOT_ARTICLES_PAGE_CACHE)).thenReturn(hotArticlesPageCache);

        // 节流调用占用了窗口，但即时 evictAll 仍应再次清理
        hotArticleCacheEvictionService.evictAllThrottled();
        hotArticleCacheEvictionService.evictAll();

        verify(hotArticlesCache, times(2)).clear();
        verify(hotArticlesPageCache, times(2)).clear();
    }

    private void setMinEvictIntervalMs(long value) throws Exception {
        java.lang.reflect.Field field = HotArticleCacheEvictionService.class.getDeclaredField("minEvictIntervalMs");
        field.setAccessible(true);
        field.setLong(hotArticleCacheEvictionService, value);
    }
}
