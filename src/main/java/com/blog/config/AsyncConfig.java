package com.blog.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步配置类
 * 启用 @Async 注解支持，配置异步任务线程池
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {
    private final AtomicLong notificationRejectedCount = new AtomicLong(0);
    private final AtomicLong cacheFallbackCount = new AtomicLong(0);

    /**
     * 配置异步任务线程池
     */
    @Bean(name = "notificationTaskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数
        executor.setCorePoolSize(5);

        // 最大线程数
        executor.setMaxPoolSize(20);

        // 队列容量
        executor.setQueueCapacity(200);

        // 线程名称前缀
        executor.setThreadNamePrefix("notification-async-");

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 拒绝策略：丢弃最旧任务，避免反压主业务线程；同时记录拒绝次数用于观测
        ThreadPoolExecutor.DiscardOldestPolicy discardOldestPolicy = new ThreadPoolExecutor.DiscardOldestPolicy();
        executor.setRejectedExecutionHandler((task, pool) -> {
            long count = notificationRejectedCount.incrementAndGet();
            if (count == 1 || count % 100 == 0) {
                log.warn("通知线程池触发拒绝策略，累计拒绝: {}, active: {}, queueSize: {}",
                        count, pool.getActiveCount(), pool.getQueue().size());
            }
            discardOldestPolicy.rejectedExecution(task, pool);
        });

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        return executor;
    }

    /**
     * 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            // 记录异步任务中的未捕获异常
            Thread.getDefaultUncaughtExceptionHandler()
                    .uncaughtException(Thread.currentThread(), throwable);
        };
    }

    /**
     * 上传清理任务线程池
     * 用于处理分片上传过期会话的清理任务
     */
    @Bean(name = "uploadCleanupExecutor")
    public Executor getUploadCleanupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：设置为2，避免长时间占用资源
        executor.setCorePoolSize(2);

        // 最大线程数：最多4个线程处理并发清理
        executor.setMaxPoolSize(4);

        // 队列容量：最多缓存50个待清理任务
        executor.setQueueCapacity(50);

        // 线程名称前缀
        executor.setThreadNamePrefix("upload-cleanup-");

        // 线程空闲时间（秒）
        executor.setKeepAliveSeconds(60);

        // 拒绝策略：直接丢弃并记录日志
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("上传清理任务队列已满，丢弃任务");
        });

        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间（秒）
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        return executor;
    }

    /**
     * 缓存失效事件线程池
     */
    @Bean(name = "cacheTaskExecutor")
    public Executor getCacheTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("cache-task-");
        executor.setKeepAliveSeconds(60);
        // 缓存失效属于一致性关键路径，拒绝时降级为调用线程执行，避免静默丢弃
        ThreadPoolExecutor.CallerRunsPolicy callerRunsPolicy = new ThreadPoolExecutor.CallerRunsPolicy();
        executor.setRejectedExecutionHandler((task, pool) -> {
            long count = cacheFallbackCount.incrementAndGet();
            if (count == 1 || count % 100 == 0) {
                log.warn("缓存线程池队列已满，降级为调用线程执行，累计降级: {}, active: {}, queueSize: {}",
                        count, pool.getActiveCount(), pool.getQueue().size());
            }
            callerRunsPolicy.rejectedExecution(task, pool);
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }
}
