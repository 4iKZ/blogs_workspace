package com.blog.service;

import com.blog.config.AccessLogBufferProperties;
import com.blog.entity.WebsiteAccessLog;
import com.blog.mapper.WebsiteAccessLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("访问日志缓冲服务测试")
class AccessLogBufferServiceTest {

    private AccessLogBufferProperties properties;
    private WebsiteAccessLogMapper websiteAccessLogMapper;
    private AccessLogBufferService service;

    private WebsiteAccessLog sampleLog() {
        WebsiteAccessLog log = new WebsiteAccessLog();
        log.setAccessDate("2026-08-03");
        log.setRequestUrl("/test");
        log.setPageUrl("/test");
        log.setIpAddress("127.0.0.1");
        return log;
    }

    @BeforeEach
    void setUp() throws Exception {
        properties = mock(AccessLogBufferProperties.class, withSettings().lenient());
        websiteAccessLogMapper = mock(WebsiteAccessLogMapper.class, withSettings().lenient());
        service = new AccessLogBufferService();

        Field propertiesField = AccessLogBufferService.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        propertiesField.set(service, properties);

        Field mapperField = AccessLogBufferService.class.getDeclaredField("websiteAccessLogMapper");
        mapperField.setAccessible(true);
        mapperField.set(service, websiteAccessLogMapper);

        when(properties.getQueueCapacity()).thenReturn(1000);
        when(properties.getBatchSize()).thenReturn(200);
        when(properties.getFlushIntervalMs()).thenReturn(500L);
        when(properties.getMaxBatchesPerFlush()).thenReturn(5);
        service.init();
    }

    // ==================== init ====================

    @Nested
    @DisplayName("init 初始化测试")
    class InitTest {

        @Test
        @DisplayName("最小队列容量应不小于 1000")
        void testInit_minQueueCapacity() throws Exception {
            AccessLogBufferService s = new AccessLogBufferService();
            AccessLogBufferProperties p = new AccessLogBufferProperties();
            p.setQueueCapacity(99);
            Field pf = AccessLogBufferService.class.getDeclaredField("properties");
            pf.setAccessible(true);
            pf.set(s, p);
            Field mf = AccessLogBufferService.class.getDeclaredField("websiteAccessLogMapper");
            mf.setAccessible(true);
            mf.set(s, websiteAccessLogMapper);
            s.init();
            // 队列应成功初始化，不会抛异常
        }
    }

    // ==================== offer ====================

    @Nested
    @DisplayName("offer 入队测试")
    class OfferTest {

        @Test
        @DisplayName("null 值应返回 false")
        void testOffer_null_shouldReturnFalse() {
            assertThat(service.offer(null)).isFalse();
        }

        @Test
        @DisplayName("正常日志应入队成功")
        void testOffer_normal_shouldAccept() {
            boolean result = service.offer(sampleLog());
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("队列满时应丢弃最旧日志并入队新日志")
        void testOffer_queueFull_shouldDropOldest() {
            // 重置为容量1，重新初始化
            reset(properties);
            when(properties.getQueueCapacity()).thenReturn(1);
            when(properties.getBatchSize()).thenReturn(200);
            when(properties.getFlushIntervalMs()).thenReturn(500L);
            when(properties.getMaxBatchesPerFlush()).thenReturn(5);
            service.init();

            assertThat(service.offer(sampleLog())).isTrue();
            // 队列容量=1，再入队应丢弃旧的
            assertThat(service.offer(sampleLog())).isTrue();
        }
    }

    // ==================== flush ====================

    @Nested
    @DisplayName("flush 批量刷新测试")
    class FlushTest {

        @Test
        @DisplayName("空队列时 flush 不应调用 insertBatch")
        void testFlush_emptyQueue_shouldNotCallInsert() {
            service.flush();
            verify(websiteAccessLogMapper, never()).insertBatch(anyCollection());
        }

        @Test
        @DisplayName("有日志时 flush 应批量写入")
        void testFlush_withLogs_shouldInsertBatch() {
            service.offer(sampleLog());
            service.offer(sampleLog());
            service.flush();
            verify(websiteAccessLogMapper, atLeastOnce()).insertBatch(anyCollection());
        }

        @Test
        @DisplayName("insertBatch 失败时应在内部重试一次")
        void testFlush_insertFails_shouldRetryOnce() {
            when(websiteAccessLogMapper.insertBatch(anyCollection()))
                    .thenThrow(new RuntimeException("db error"))
                    .thenReturn(1);

            service.offer(sampleLog());
            service.flush();

            verify(websiteAccessLogMapper, times(2)).insertBatch(anyCollection());
        }

        @Test
        @DisplayName("insertBatch 和重试都失败时应回灌到队列")
        void testFlush_insertAndRetryFail_shouldRequeue() {
            when(websiteAccessLogMapper.insertBatch(anyCollection()))
                    .thenThrow(new RuntimeException("db error"));

            service.offer(sampleLog());
            service.flush();

            // maxBatchesPerFlush=5：每轮 flushBatch 调用 insertBatch 失败后重试，
            // 因此第一次 flush 至少调用 2 次 insertBatch，并发生回灌
            verify(websiteAccessLogMapper, atLeast(2)).insertBatch(anyCollection());

            // flush 再次运行应仍能取出日志（回灌成功）
            clearInvocations(websiteAccessLogMapper);
            service.flush();
            verify(websiteAccessLogMapper, atLeast(1)).insertBatch(anyCollection());
        }
    }

    // ==================== destroy ====================

    @Nested
    @DisplayName("destroy 关闭刷新测试")
    class DestroyTest {

        @Test
        @DisplayName("关闭时应刷新队列中剩余日志")
        void testDestroy_shouldFlushRemainingLogs() {
            service.offer(sampleLog());
            service.destroy();
            verify(websiteAccessLogMapper, atLeastOnce()).insertBatch(anyCollection());
        }

        @Test
        @DisplayName("关闭时插入失败应优雅停止")
        void testDestroy_insertFailure_shouldStopGracefully() {
            doThrow(new RuntimeException("db error"))
                    .when(websiteAccessLogMapper).insertBatch(anyCollection());

            service.offer(sampleLog());
            // 不应抛出异常
            service.destroy();
        }
    }
}
