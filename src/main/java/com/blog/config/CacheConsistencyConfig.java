package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存一致性功能配置
 * 
 * 提供功能开关和相关参数配置，支持动态调整和回滚
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cache.consistency")
public class CacheConsistencyConfig {

    /**
     * 是否启用缓存一致性增强功能
     * 设为false可快速回滚到原有逻辑
     */
    private boolean enabled = true;

    /**
     * 是否启用延迟双删
     */
    private boolean enableDoubleDelete = true;

    /**
     * 是否启用异步缓存失效
     */
    private boolean enableAsyncInvalidation = true;

    /**
     * 延迟删除时间（毫秒）
     * 用于延迟双删策略
     */
    private long delayedDeleteMs = 500;

    /**
     * 是否启用缓存一致性验证
     */
    private boolean enableVerification = true;

    /**
     * 缓存一致性验证间隔（分钟）
     */
    private int verificationIntervalMinutes = 5;

    /**
     * 验证抽样数量
     */
    private int verificationSampleSize = 100;
}
