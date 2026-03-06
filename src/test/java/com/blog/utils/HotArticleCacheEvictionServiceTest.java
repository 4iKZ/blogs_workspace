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
}
