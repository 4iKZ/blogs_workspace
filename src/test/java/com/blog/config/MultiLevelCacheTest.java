package com.blog.config;

import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多级缓存单元测试
 *
 * 验证 Caffeine 本地缓存的基本功能
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {MultiLevelCacheTest.TestConfig.class})
@Slf4j
class MultiLevelCacheTest {

    @org.springframework.beans.factory.annotation.Autowired
    private CacheManager cacheManager;

    private List<ArticleDTO> mockArticles;

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        public CaffeineCacheConfig caffeineCacheConfig() {
            CaffeineCacheConfig config = new CaffeineCacheConfig();
            config.setEnabled(true);
            config.setMaxSize(100);
            config.setDefaultTtl(java.time.Duration.ofSeconds(30));
            return config;
        }

        @Bean
        public CacheManager cacheManager(CaffeineCacheConfig config) {
            CaffeineCacheManager cacheManager = new CaffeineCacheManager();
            Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                    .maximumSize(config.getMaxSize())
                    .expireAfterWrite(config.getDefaultTtl().toSeconds(), TimeUnit.SECONDS)
                    .recordStats();
            cacheManager.setCaffeine(caffeine);
            return cacheManager;
        }
    }

    @BeforeEach
    void setUp() {
        mockArticles = new ArrayList<>();
        ArticleDTO article = new ArticleDTO();
        article.setId(1L);
        article.setTitle("Test Article");
        mockArticles.add(article);
    }

    @Test
    void testCaffeineCacheBasicOperations() {
        log.info("测试 Caffeine 缓存基本操作");

        // 获取缓存
        org.springframework.cache.Cache cache = cacheManager.getCache("hotArticles");
        assertNotNull(cache, "缓存不应该为空");

        // 测试 put 和 get
        String key = "day:10";
        Result<List<ArticleDTO>> value = Result.success(mockArticles);

        cache.put(key, value);
        org.springframework.cache.Cache.ValueWrapper wrapper = cache.get(key);
        assertNotNull(wrapper, "缓存值不应该为空");
        assertNotNull(wrapper.get(), "缓存内容不应该为空");

        log.info("缓存写入和读取成功");

        // 测试 evict
        cache.evict(key);
        wrapper = cache.get(key);
        assertNull(wrapper, "缓存清除后应该为空");

        log.info("缓存清除成功");
    }

    @Test
    void testCaffeineCacheStats() {
        log.info("测试 Caffeine 缓存统计");

        org.springframework.cache.Cache cache = cacheManager.getCache("hotArticles");

        // 模拟多次缓存操作
        for (int i = 0; i < 10; i++) {
            String key = "key" + i;
            cache.put(key, "value" + i);
            cache.get(key);
        }

        log.info("缓存统计测试完成");
    }

    @Test
    void testCacheExpiration() throws InterruptedException {
        log.info("测试缓存过期");

        // 创建短 TTL 缓存
        Cache<String, String> shortTtlCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();

        shortTtlCache.put("testKey", "testValue");
        assertNotNull(shortTtlCache.getIfPresent("testKey"), "缓存应该存在");

        // 等待过期
        Thread.sleep(1500);

        assertNull(shortTtlCache.getIfPresent("testKey"), "缓存应该已过期");
        log.info("缓存过期测试通过");
    }

    @Test
    void testCacheMaxSize() {
        log.info("测试缓存最大容量");

        Cache<String, String> limitedCache = Caffeine.newBuilder()
                .maximumSize(5)
                .build();

        // 添加超过最大容量的条目
        for (int i = 0; i < 10; i++) {
            limitedCache.put("key" + i, "value" + i);
        }

        // Caffeine 的清理是异步的，estimatedSize 可能暂时超过最大值
        // 但最终会收敛到限制范围内。这里验证缓存仍然正常工作。
        // 通过读取来触发清理
        for (int i = 0; i < 10; i++) {
            limitedCache.getIfPresent("key" + i);
        }

        // 验证缓存功能正常：部分条目存在，部分被驱逐
        int presentCount = 0;
        for (int i = 0; i < 10; i++) {
            if (limitedCache.getIfPresent("key" + i) != null) {
                presentCount++;
            }
        }
        // 由于最大容量是 5，存在的条目数量应该不超过 5
        assertTrue(presentCount <= 5, "存在的缓存条目不应该超过最大值 5，实际: " + presentCount);
        log.info("缓存最大容量测试通过，当前存在条目: {}", presentCount);
    }
}