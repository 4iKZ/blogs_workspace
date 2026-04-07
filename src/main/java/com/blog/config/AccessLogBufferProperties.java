package com.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 访问日志缓冲配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "access-log.buffer")
public class AccessLogBufferProperties {

    /**
     * 缓冲队列容量
     */
    private int queueCapacity = 10000;

    /**
     * 单次批量写入数量
     */
    private int batchSize = 200;

    /**
     * 刷盘间隔（毫秒）
     */
    private long flushIntervalMs = 500L;

    /**
     * 单次调度最多处理的批次数
     */
    private int maxBatchesPerFlush = 5;
}
