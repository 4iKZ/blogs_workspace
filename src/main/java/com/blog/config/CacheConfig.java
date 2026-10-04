package com.blog.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置类
 *
 * 使用 Caffeine 本地内存缓存，默认 30s TTL，适用于双实例部署场景，
 * 通过短 TTL 快速过期保证最终一致性。
 *
 * 说明：原设计的 Redis L2 分布式缓存因 Spring {@code CompositeCacheManager}
 * 只会返回第一个非 null 的 Cache（并非 L1 → L2 级联查找），
 * hotArticles/hotArticlesPage 始终命中 Caffeine，Redis 分支从未被使用，
 * ponytail: 已作为死配置移除；如需真正的两级缓存需另行实现。
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
@Slf4j
public class CacheConfig {

    private final CaffeineCacheConfig caffeineCacheConfig;

    /**
     * 本地缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        return caffeineCacheManager();
    }

    /**
     * Caffeine 本地缓存管理器
     */
    private CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置 Caffeine 缓存规格
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .maximumSize(caffeineCacheConfig.getMaxSize())
                .expireAfterWrite(caffeineCacheConfig.getDefaultTtl().toSeconds(), TimeUnit.SECONDS)
                .recordStats(); // 开启统计，便于监控

        cacheManager.setCaffeine(caffeine);

        // 注册需要预热的缓存名称
        cacheManager.setCacheNames(java.util.List.of("hotArticles", "hotArticlesPage"));

        log.info("Caffeine 本地缓存初始化: maxSize={}, ttl={}",
                caffeineCacheConfig.getMaxSize(),
                caffeineCacheConfig.getDefaultTtl());

        return cacheManager;
    }
}