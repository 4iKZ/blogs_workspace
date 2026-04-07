package com.blog.service;

import com.blog.config.AccessLogBufferProperties;
import com.blog.entity.WebsiteAccessLog;
import com.blog.mapper.WebsiteAccessLogMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 访问日志缓冲服务：将单条写入聚合为批量写入
 */
@Slf4j
@Service
public class AccessLogBufferService implements DisposableBean {

    @Autowired
    private AccessLogBufferProperties properties;

    @Autowired
    private WebsiteAccessLogMapper websiteAccessLogMapper;

    private LinkedBlockingQueue<WebsiteAccessLog> bufferQueue;
    private final AtomicLong droppedCount = new AtomicLong(0);
    private final AtomicLong failedBatchCount = new AtomicLong(0);

    @PostConstruct
    public void init() {
        int queueCapacity = Math.max(1000, properties.getQueueCapacity());
        this.bufferQueue = new LinkedBlockingQueue<>(queueCapacity);
        log.info("初始化访问日志缓冲队列成功，容量: {}, batchSize: {}, flushIntervalMs: {}",
                queueCapacity, Math.max(1, properties.getBatchSize()), Math.max(100, properties.getFlushIntervalMs()));
    }

    /**
     * 非阻塞入队，队列满时丢弃最旧日志，优先保证主流程
     */
    public boolean offer(WebsiteAccessLog accessLog) {
        if (accessLog == null) {
            return false;
        }

        if (bufferQueue.offer(accessLog)) {
            return true;
        }

        WebsiteAccessLog dropped = bufferQueue.poll();
        if (dropped != null) {
            droppedCount.incrementAndGet();
        }

        boolean offered = bufferQueue.offer(accessLog);
        if (!offered) {
            droppedCount.incrementAndGet();
        }

        if (droppedCount.get() % 100 == 0) {
            log.warn("访问日志缓冲队列压力过大，累计丢弃日志条数: {}", droppedCount.get());
        }
        return offered;
    }

    @Scheduled(fixedDelayString = "${access-log.buffer.flush-interval-ms:500}")
    public void flush() {
        int batchSize = Math.max(1, properties.getBatchSize());
        int maxBatches = Math.max(1, properties.getMaxBatchesPerFlush());
        for (int i = 0; i < maxBatches; i++) {
            if (!flushBatch(batchSize)) {
                break;
            }
        }
    }

    private boolean flushBatch(int batchSize) {
        if (bufferQueue == null || bufferQueue.isEmpty()) {
            return false;
        }

        List<WebsiteAccessLog> batch = new ArrayList<>(batchSize);
        bufferQueue.drainTo(batch, batchSize);
        if (batch.isEmpty()) {
            return false;
        }

        try {
            websiteAccessLogMapper.insertBatch(batch);
            log.debug("批量写入访问日志成功，条数: {}, 队列剩余: {}", batch.size(), bufferQueue.size());
        } catch (Exception e) {
            failedBatchCount.incrementAndGet();
            log.error("批量写入访问日志失败，本批条数: {}，将尝试重试/回灌", batch.size(), e);
            if (!retryInsertBatch(batch)) {
                requeueBatch(batch);
            }
        }
        return true;
    }

    private boolean retryInsertBatch(List<WebsiteAccessLog> batch) {
        try {
            websiteAccessLogMapper.insertBatch(batch);
            log.warn("访问日志批量写入重试成功，本批条数: {}", batch.size());
            return true;
        } catch (Exception retryEx) {
            log.error("访问日志批量写入重试失败，本批条数: {}", batch.size(), retryEx);
            return false;
        }
    }

    private void requeueBatch(List<WebsiteAccessLog> batch) {
        int overflow = 0;
        for (WebsiteAccessLog logItem : batch) {
            if (!bufferQueue.offer(logItem)) {
                overflow++;
            }
        }
        if (overflow > 0) {
            long totalDropped = droppedCount.addAndGet(overflow);
            log.error("回灌失败导致日志丢弃，丢弃: {}，累计丢弃: {}", overflow, totalDropped);
        } else {
            log.warn("批量写入失败后已回灌到缓冲队列，本批条数: {}，当前队列: {}", batch.size(), bufferQueue.size());
        }
    }

    @Override
    public void destroy() {
        int batchSize = Math.max(1, properties.getBatchSize());
        int flushed = 0;
        while (bufferQueue != null && !bufferQueue.isEmpty()) {
            List<WebsiteAccessLog> batch = new ArrayList<>(batchSize);
            bufferQueue.drainTo(batch, batchSize);
            if (batch.isEmpty()) {
                break;
            }
            try {
                websiteAccessLogMapper.insertBatch(batch);
                flushed += batch.size();
            } catch (Exception e) {
                log.error("关闭时刷新访问日志失败，本批条数: {}", batch.size(), e);
                break;
            }
        }
        log.info("访问日志缓冲服务关闭完成，已刷新: {}, 丢弃: {}, 失败批次: {}",
                flushed, droppedCount.get(), failedBatchCount.get());
    }
}
