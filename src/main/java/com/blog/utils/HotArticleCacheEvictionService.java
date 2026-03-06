package com.blog.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * 热门文章结果缓存统一失效入口。
 *
 * 统一清理 Spring Cache 管理的热门文章结果缓存，
 * 避免业务代码误删到排行榜 ZSet 或使用错误的 Redis key 前缀。
 */
@Component
@Slf4j
public class HotArticleCacheEvictionService {

    public static final String HOT_ARTICLES_CACHE = "hotArticles";
    public static final String HOT_ARTICLES_PAGE_CACHE = "hotArticlesPage";

    @Autowired
    private CacheManager cacheManager;

    /**
     * 清理所有热门文章结果缓存。
     */
    public void evictAll() {
        clearCache(HOT_ARTICLES_CACHE);
        clearCache(HOT_ARTICLES_PAGE_CACHE);
    }

    private void clearCache(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Spring Cache [{}] 不存在，跳过清理", cacheName);
                return;
            }
            cache.clear();
            log.debug("Spring Cache [{}] 已清理", cacheName);
        } catch (Exception e) {
            log.warn("清理 Spring Cache [{}] 失败", cacheName, e);
        }
    }
}
