package com.blog.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存配置类
 *
 * 实现 Caffeine (L1 本地缓存) + Redis (L2 分布式缓存) 的多级缓存架构。
 *
 * 缓存查询顺序：Caffeine -> Redis -> MySQL
 *
 * L1 Caffeine：本地内存缓存，30秒 TTL，快速响应热点请求
 * L2 Redis：分布式缓存，2-5分钟 TTL，保证数据一致性
 *
 * 一致性策略：短 TTL 过期，适用于双实例部署场景
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
@Slf4j
public class CacheConfig {

    private final CaffeineCacheConfig caffeineCacheConfig;

    /**
     * 配置多级缓存管理器
     *
     * CompositeCacheManager 按顺序委托给 Caffeine 和 Redis 缓存管理器。
     * 查询时先查 Caffeine，miss 后再查 Redis，都 miss 则执行原方法。
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // L1: Caffeine 本地缓存管理器
        CacheManager caffeineCacheManager = caffeineCacheManager();

        // L2: Redis 分布式缓存管理器
        CacheManager redisCacheManager = redisCacheManager(connectionFactory);

        // 组合缓存管理器：L1 -> L2
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager(
                caffeineCacheManager,
                redisCacheManager
        );
        // 设置为 true，当所有缓存管理器都没有对应缓存时返回 null（而非抛出异常）
        compositeCacheManager.setFallbackToNoOpCache(false);

        log.info("多级缓存管理器初始化完成: L1 Caffeine (TTL: {}) -> L2 Redis",
                caffeineCacheConfig.getDefaultTtl());

        return compositeCacheManager;
    }

    /**
     * Caffeine 本地缓存管理器 (L1)
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

        log.info("Caffeine L1 本地缓存初始化: maxSize={}, ttl={}",
                caffeineCacheConfig.getMaxSize(),
                caffeineCacheConfig.getDefaultTtl());

        return cacheManager;
    }

    /**
     * Redis 分布式缓存管理器 (L2)
     */
    private CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("hotArticles", defaultConfig.entryTtl(Duration.ofMinutes(3)));
        cacheConfigurations.put("hotArticlesPage", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}