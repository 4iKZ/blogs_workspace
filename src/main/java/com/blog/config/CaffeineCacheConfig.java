package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine 本地缓存配置
 *
 * 用于配置纯 Caffeine 单层 Spring Cache 参数（maxSize / defaultTtl）。
 * 说明：原设计的 Redis L2 分布式缓存已作为死配置移除（详见 {@link CacheConfig} 类注释），
 * 如需真正的两级缓存需另行实现。
 */
@Configuration
@ConfigurationProperties(prefix = "cache.local")
@Data
public class CaffeineCacheConfig {

    /**
     * 是否启用本地缓存
     */
    private boolean enabled = true;

    /**
     * 最大缓存条目数
     */
    private int maxSize = 1000;

    /**
     * 默认过期时间（秒）
     */
    private Duration defaultTtl = Duration.ofSeconds(30);
}
