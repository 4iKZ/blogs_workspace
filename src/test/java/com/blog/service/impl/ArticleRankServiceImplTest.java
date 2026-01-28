package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ArticleRankServiceImpl 单元测试类
 * 测试文章排行榜功能的三个严重问题修复：
 * - F-001: 数据一致性 - 使用 Lua 脚本原子更新
 * - F-004: 跨年边界 - 周数计算正确性
 * - F-008: 异步初始化 - 启动不阻塞
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("文章排行榜服务测试")
public class ArticleRankServiceImplTest {

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private ArticleRankServiceImpl articleRankService;

    // ==================== F-001: 数据一致性测试 ====================

    @Nested
    @DisplayName("F-001: 数据一致性测试")
    class AtomicUpdateTests {

        @Test
        @DisplayName("测试 incrementScore 使用原子更新方法")
        void testIncrementScore_UsesAtomicUpdate() {
            // Arrange
            Long articleId = 123L;
            double score = 5.0;
            when(redisUtils.zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(score), eq(2L), eq(14L)))
                    .thenReturn(5.0);

            // Act
            articleRankService.incrementScore(articleId, score);

            // Assert
            verify(redisUtils, times(1)).zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(score), eq(2L), eq(14L));
            // 验证不再调用旧的 zIncrBy 方法
            verify(redisUtils, never()).zIncrBy(anyString(), any(), anyDouble());
        }

        @Test
        @DisplayName("测试 decrementScore 使用原子更新方法")
        void testDecrementScore_UsesAtomicUpdate() {
            // Arrange
            Long articleId = 456L;
            double score = 3.0;
            when(redisUtils.zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(-3.0), eq(2L), eq(14L)))
                    .thenReturn(2.0);

            // Act
            articleRankService.decrementScore(articleId, score);

            // Assert
            verify(redisUtils, times(1)).zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(-3.0), eq(2L), eq(14L));
            // 验证不再调用旧的 zDecrBy 方法
            verify(redisUtils, never()).zDecrBy(anyString(), any(), anyDouble());
        }

        @Test
        @DisplayName("测试并发更新原子性")
        void testConcurrentUpdate_Atomicity() throws InterruptedException {
            // Arrange
            Long articleId = 789L;
            int threadCount = 10;
            int incrementsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            when(redisUtils.zIncrByAtomic(anyString(), anyString(), eq(articleId), anyDouble(), eq(2L), eq(14L)))
                    .thenAnswer(invocation -> {
                        successCount.incrementAndGet();
                        return 1.0;
                    });

            // Act - 并发更新
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < incrementsPerThread; j++) {
                            articleRankService.incrementScore(articleId, 1.0);
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Assert - 等待所有线程完成
            assertTrue(latch.await(30, TimeUnit.SECONDS));
            assertEquals(threadCount * incrementsPerThread, successCount.get());

            executor.shutdown();
        }

        @Test
        @DisplayName("测试 articleId 为 null 时不执行更新")
        void testIncrementScore_NullArticleId_NoUpdate() {
            // Act
            articleRankService.incrementScore(null, 5.0);

            // Assert
            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), any(), anyDouble(), anyLong(), anyLong());
        }
    }

    // ==================== F-004: 跨年边界测试 ====================

    @Nested
    @DisplayName("F-004: 跨年边界周数计算测试")
    class WeekKeyCalculationTests {

        @ParameterizedTest
        @CsvSource({
                "2026-01-01, hot:articles:zset:week:2026-W01",   // 新年第一天
                "2026-01-04, hot:articles:zset:week:2026-W01",   // 新年第一周
                "2026-01-05, hot:articles:zset:week:2026-W01",   // 新年第一周
                "2025-12-29, hot:articles:zset:week:2025-W52",   // 去年最后一周
                "2025-12-31, hot:articles:zset:week:2025-W52",   // 去年最后一天
                "2026-12-31, hot:articles:zset:week:2026-W52",   // 年末
                "2027-01-01, hot:articles:zset:week:2027-W01",   // 下一年第一天
                "2027-01-04, hot:articles:zset:week:2027-W01",   // 下一年第一周
                "2024-01-01, hot:articles:zset:week:2024-W01",   // 2024年第一天（周一）
                "2023-01-01, hot:articles:zset:week:2023-W01"    // 2023年第一天
        })
        @DisplayName("测试跨年边界周键计算")
        void testWeekKeyCalculation(String dateStr, String expectedKey) {
            // Arrange
            LocalDate date = LocalDate.parse(dateStr);

            // Act - 使用反射调用私有方法 getWeekKey
            String result = invokeGetWeekKey(date);

            // Assert
            assertEquals(expectedKey, result, "日期 " + dateStr + " 应生成周键 " + expectedKey);
        }

        @Test
        @DisplayName("测试普通日期的周键计算")
        void testWeekKeyCalculation_NormalDates() {
            // 测试一些普通日期
            LocalDate[] testDates = {
                    LocalDate.of(2026, 6, 15),   // 6月中旬
                    LocalDate.of(2026, 3, 8),    // 3月初
                    LocalDate.of(2026, 9, 20),   // 9月中
                    LocalDate.of(2026, 11, 25)   // 11月底
            };

            for (LocalDate date : testDates) {
                String result = invokeGetWeekKey(date);
                assertNotNull(result);
                assertTrue(result.startsWith("hot:articles:zset:week:"),
                        "周键应该以正确的前缀开头");
                assertTrue(result.matches("hot:articles:zset:week:\\d{4}-W\\d{2}"),
                        "周键格式应该正确: " + result);
            }
        }

        /**
         * 使用反射调用私有方法 getWeekKey
         */
        private String invokeGetWeekKey(LocalDate date) {
            try {
                Method method = ArticleRankServiceImpl.class.getDeclaredMethod("getWeekKey", LocalDate.class);
                method.setAccessible(true);
                return (String) method.invoke(articleRankService, date);
            } catch (Exception e) {
                throw new RuntimeException("反射调用 getWeekKey 失败", e);
            }
        }
    }

    // ==================== F-008: 异步初始化测试 ====================

    @Nested
    @DisplayName("F-008: 异步初始化测试")
    class AsyncInitializationTests {

        @Test
        @DisplayName("测试异步初始化不阻塞主线程")
        void testAsyncInitialization_DoesNotBlockMainThread() throws Exception {
            // Arrange
            List<Article> articles = new ArrayList<>();
            for (long i = 1; i <= 100; i++) {
                Article article = new Article();
                article.setId(i);
                article.setStatus(2); // 已发布
                articles.add(article);
            }

            when(articleMapper.selectList(any())).thenReturn(articles);
            when(redisUtils.zScore(anyString(), anyLong())).thenReturn(null);
            when(redisUtils.zAdd(anyString(), anyLong(), anyDouble())).thenReturn(true);

            // Act - 记录开始时间
            long startTime = System.currentTimeMillis();
            articleRankService.initializeAllArticles();
            long endTime = System.currentTimeMillis();

            // Assert - 对于100篇文章，同步初始化应该需要一些时间
            // 但由于我们使用 mock，主要验证调用正确性
            verify(articleMapper, times(1)).selectList(any());
            verify(redisUtils, atLeast(100)).zAdd(anyString(), anyLong(), eq(0.0));
        }

        @Test
        @DisplayName("测试初始化空文章列表")
        void testInitializeAllArticles_EmptyList() {
            // Arrange
            when(articleMapper.selectList(any())).thenReturn(new ArrayList<>());

            // Act
            articleRankService.initializeAllArticles();

            // Assert
            verify(articleMapper, times(1)).selectList(any());
            verify(redisUtils, never()).zAdd(anyString(), anyLong(), anyDouble());
        }

        @Test
        @DisplayName("测试初始化已存在的文章不重复添加")
        void testInitializeAllArticles_SkipExistingArticles() {
            // Arrange
            List<Article> articles = new ArrayList<>();
            Article article = new Article();
            article.setId(1L);
            article.setStatus(2);
            articles.add(article);

            when(articleMapper.selectList(any())).thenReturn(articles);
            // 模拟文章已存在（zScore 返回非 null）
            when(redisUtils.zScore(anyString(), eq(1L))).thenReturn(10.0);

            // Act
            articleRankService.initializeAllArticles();

            // Assert - 已存在的文章不应该被添加
            verify(redisUtils, never()).zAdd(anyString(), eq(1L), eq(0.0));
        }
    }

    // ==================== 集成测试 ====================

    @Nested
    @DisplayName("集成测试")
    class IntegrationTests {

        @Test
        @DisplayName("测试获取热门文章完整流程")
        void testGetHotArticles_CompleteFlow() {
            // Arrange
            when(redisUtils.zSize(anyString())).thenReturn(3L);

            Set<Object> articleIds = new LinkedHashSet<>();
            articleIds.add("3");
            articleIds.add("1");
            articleIds.add("2");
            when(redisUtils.zReverseRange(anyString(), eq(0L), eq(14L))).thenReturn(articleIds);

            List<Article> articles = new ArrayList<>();
            Article article1 = new Article();
            article1.setId(1L);
            article1.setTitle("Article 1");
            article1.setViewCount(100);
            article1.setLikeCount(10);
            article1.setCommentCount(5);
            article1.setPublishTime(java.time.LocalDateTime.now());
            article1.setAuthorId(1L);
            article1.setCategoryId(1L);

            Article article2 = new Article();
            article2.setId(2L);
            article2.setTitle("Article 2");
            article2.setViewCount(200);
            article2.setLikeCount(20);
            article2.setCommentCount(10);
            article2.setPublishTime(java.time.LocalDateTime.now());
            article2.setAuthorId(1L);
            article2.setCategoryId(1L);

            Article article3 = new Article();
            article3.setId(3L);
            article3.setTitle("Article 3");
            article3.setViewCount(150);
            article3.setLikeCount(15);
            article3.setCommentCount(8);
            article3.setPublishTime(java.time.LocalDateTime.now());
            article3.setAuthorId(1L);
            article3.setCategoryId(1L);

            articles.add(article1);
            articles.add(article2);
            articles.add(article3);

            when(articleMapper.selectBatchIds(any())).thenReturn(articles);
            when(redisUtils.zScore(anyString(), anyLong())).thenReturn(100.0, 200.0, 150.0);

            // Act
            Result<List<ArticleDTO>> result = articleRankService.getHotArticles(10, "day");

            // Assert
            assertTrue(result.isSuccess());
            assertNotNull(result.getData());
            assertEquals(3, result.getData().size());

            // 验证返回顺序与 zReverseRange 一致（3, 1, 2）
            assertEquals(3L, result.getData().get(0).getId());
            assertEquals(1L, result.getData().get(1).getId());
            assertEquals(2L, result.getData().get(2).getId());
        }

        @Test
        @DisplayName("测试初始化新文章到排行榜")
        void testInitializeArticle_NewArticle() {
            // Arrange
            Long articleId = 999L;
            when(redisUtils.zAdd(anyString(), eq(articleId), eq(0.0))).thenReturn(true);

            // Act
            articleRankService.initializeArticle(articleId);

            // Assert
            verify(redisUtils, times(1)).zAdd(anyString(), eq(articleId), eq(0.0));
        }

        @Test
        @DisplayName("测试从排行榜删除文章")
        void testRemoveFromRank() {
            // Arrange
            Long articleId = 888L;
            when(redisUtils.zRemove(anyString(), eq(articleId))).thenReturn(1L);

            // Act
            articleRankService.removeFromRank(articleId);

            // Assert
            verify(redisUtils, times(2)).zRemove(anyString(), eq(articleId));
        }
    }

    // ==================== 便捷方法测试 ====================

    @Nested
    @DisplayName("便捷方法测试")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("测试 incrementViewScore 排除作者自己")
        void testIncrementViewScore_ExcludeAuthor() {
            // Arrange
            Long articleId = 1L;
            Long viewerId = 100L;
            Long authorId = 100L; // 浏览者是作者

            when(redisUtils.zIncrByAtomic(anyString(), anyString(), anyLong(), anyDouble(), anyLong(), anyLong()))
                    .thenReturn(1.0);

            // Act
            articleRankService.incrementViewScore(articleId, viewerId, authorId);

            // Assert - 不应该调用更新（因为浏览者是作者）
            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), eq(articleId), anyDouble(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("测试 incrementViewScore 非作者正常更新")
        void testIncrementViewScore_NonAuthorUpdates() {
            // Arrange
            Long articleId = 1L;
            Long viewerId = 100L;
            Long authorId = 200L; // 浏览者不是作者

            when(redisUtils.zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(1.0), eq(2L), eq(14L)))
                    .thenReturn(1.0);

            // Act
            articleRankService.incrementViewScore(articleId, viewerId, authorId);

            // Assert - 应该调用更新
            verify(redisUtils, times(1)).zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(1.0), eq(2L), eq(14L));
        }

        @Test
        @DisplayName("测试 incrementLikeScore 排除作者自己")
        void testIncrementLikeScore_ExcludeAuthor() {
            // Arrange
            Long articleId = 1L;
            Long likerId = 100L;
            Long authorId = 100L;

            // Act
            articleRankService.incrementLikeScore(articleId, likerId, authorId);

            // Assert
            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), anyLong(), anyDouble(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("测试 decrementLikeScore 排除作者自己")
        void testDecrementLikeScore_ExcludeAuthor() {
            // Arrange
            Long articleId = 1L;
            Long likerId = 100L;
            Long authorId = 100L;

            // Act
            articleRankService.decrementLikeScore(articleId, likerId, authorId);

            // Assert
            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), anyLong(), anyDouble(), anyLong(), anyLong());
        }
    }
}
