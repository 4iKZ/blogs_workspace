package com.blog.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

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
     * 节流最小间隔（毫秒）：小于该间隔的重复失效请求将被跳过。
     */
    @Value("${cache.hot-articles.min-evict-interval-ms:5000}")
    private long minEvictIntervalMs = 5000L;

    /**
     * 上次执行节流清理的时间戳（毫秒），用于并发安全地节流。
     */
    private final AtomicLong lastEvictTime = new AtomicLong(0L);

    /**
     * 立即清理所有热门文章结果缓存（供低频路径使用，如管理端重置榜单）。
     */
    public void evictAll() {
        clearCache(HOT_ARTICLES_CACHE);
        clearCache(HOT_ARTICLES_PAGE_CACHE);
    }

    /**
     * 节流清理热门文章结果缓存（供浏览量/点赞等高频路径使用）。
     * 与上次清理间隔小于 {@link #minEvictIntervalMs} 时跳过清理；
     * 使用 AtomicLong + CAS 保证多线程并发下最多只有一个线程完成本次清理。
     */
    public void evictAllThrottled() {
        long now = System.currentTimeMillis();
        long last = lastEvictTime.get();
        if (now - last < minEvictIntervalMs) {
            log.debug("热门文章结果缓存清理被节流，距上次清理 {} ms（最小间隔 {} ms），跳过本次",
                    now - last, minEvictIntervalMs);
            return;
        }
        if (!lastEvictTime.compareAndSet(last, now)) {
            log.debug("热门文章结果缓存清理时间戳已被其他线程更新，跳过本次");
            return;
        }
        evictAll();
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
