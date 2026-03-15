package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine 本地缓存配置
 *
 * 用于配置 L1 本地缓存参数，配合 Redis 形成 L2 多级缓存架构。
 * 本地缓存设置短 TTL，适用于双实例部署场景，通过快速过期保证最终一致性。
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

    /**
     * 热门文章缓存过期时间（秒）
     */
    private Duration hotArticlesTtl = Duration.ofSeconds(30);
}