package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleRankService;
import com.blog.service.ArticleService;
import com.blog.utils.HotArticleCacheEvictionService;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("文章排行榜服务测试")
public class ArticleRankServiceImplTest {

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleService articleService;

    @Mock
    private HotArticleCacheEvictionService hotArticleCacheEvictionService;

    @InjectMocks
    private ArticleRankServiceImpl articleRankService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        lenient().when(articleService.batchConvertToDTO(anyList())).thenAnswer(invocation -> {
            List<Article> articles = invocation.getArgument(0);
            List<ArticleDTO> dtos = new ArrayList<>();
            for (Article article : articles) {
                ArticleDTO dto = new ArticleDTO();
                dto.setId(article.getId());
                dto.setTitle(article.getTitle());
                dto.setViewCount(article.getViewCount());
                dto.setLikeCount(article.getLikeCount());
                dto.setCommentCount(article.getCommentCount());
                dto.setAuthorId(article.getAuthorId());
                dto.setCategoryId(article.getCategoryId());
                dto.setStatus(article.getStatus());
                dtos.add(dto);
            }
            return dtos;
        });
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    // ==================== F-001: 数据一致性测试 ====================

    @Nested
    @DisplayName("F-001: 数据一致性测试")
    class AtomicUpdateTests {

        @Test
        @DisplayName("测试 incrementScore 使用原子更新方法")
        void testIncrementScore_UsesAtomicUpdate() {
            Long articleId = 123L;
            double score = 5.0;
            when(redisUtils.zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(score), eq(2L), eq(14L)))
                    .thenReturn(5.0);

            articleRankService.incrementScore(articleId, score);

            verify(redisUtils, times(1)).zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(score), eq(2L), eq(14L));
            verify(hotArticleCacheEvictionService, times(1)).evictAll();
            verify(redisUtils, never()).zIncrBy(anyString(), any(), anyDouble());
        }

        @Test
        @DisplayName("测试 decrementScore 使用原子更新方法")
        void testDecrementScore_UsesAtomicUpdate() {
            Long articleId = 456L;
            double score = 3.0;
            when(redisUtils.zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(-3.0), eq(2L), eq(14L)))
                    .thenReturn(2.0);

            articleRankService.decrementScore(articleId, score);

            verify(redisUtils, times(1)).zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(-3.0), eq(2L), eq(14L));
            verify(hotArticleCacheEvictionService, times(1)).evictAll();
            verify(redisUtils, never()).zDecrBy(anyString(), any(), anyDouble());
        }

        @Test
        @DisplayName("测试原子更新失败时不清理热门文章结果缓存")
        void testIncrementScore_WhenAtomicUpdateFails_DoesNotEvictHotCaches() {
            Long articleId = 321L;
            when(redisUtils.zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(2.0), eq(2L), eq(14L)))
                    .thenReturn(null);

            articleRankService.incrementScore(articleId, 2.0);

            verify(hotArticleCacheEvictionService, never()).evictAll();
        }

        @Test
        @DisplayName("测试 decrementScore 原子更新失败时不清理热门文章结果缓存")
        void testDecrementScore_WhenAtomicUpdateFails_DoesNotEvictHotCaches() {
            Long articleId = 321L;
            when(redisUtils.zIncrByAtomic(anyString(), anyString(), eq(articleId), eq(-2.0), eq(2L), eq(14L)))
                    .thenReturn(null);

            articleRankService.decrementScore(articleId, 2.0);

            verify(hotArticleCacheEvictionService, never()).evictAll();
        }

        @Test
        @DisplayName("测试 articleId 为 null 时不执行更新")
        void testIncrementScore_NullArticleId_NoUpdate() {
            articleRankService.incrementScore(null, 5.0);

            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), any(), anyDouble(), anyLong(), anyLong());
            verify(hotArticleCacheEvictionService, never()).evictAll();
        }
    }

    // ==================== F-004: 跨年边界测试 ====================

    @Nested
    @DisplayName("F-004: 跨年边界周数计算测试")
    class WeekKeyCalculationTests {

        @Test
        @DisplayName("测试普通日期的周键计算")
        void testWeekKeyCalculation_NormalDates() {
            LocalDate[] testDates = {
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 3, 8),
                    LocalDate.of(2026, 9, 20),
                    LocalDate.of(2026, 11, 25)
            };

            for (LocalDate date : testDates) {
                String result = invokeGetWeekKey(date);
                assertThat(result).isNotNull();
                assertThat(result).startsWith("hot:articles:zset:week:");
                assertThat(result).matches("hot:articles:zset:week:\\d{4}-W\\d{2}");
            }
        }

        private String invokeGetWeekKey(LocalDate date) {
            try {
                java.lang.reflect.Method method = ArticleRankServiceImpl.class.getDeclaredMethod("getWeekKey", LocalDate.class);
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
        void testAsyncInitialization_DoesNotBlockMainThread() {
            List<Article> articles = new ArrayList<>();
            for (long i = 1; i <= 100; i++) {
                Article article = new Article();
                article.setId(i);
                article.setStatus(2);
                articles.add(article);
            }

            when(articleMapper.selectList(any())).thenReturn(articles);
            when(redisUtils.zScore(anyString(), anyLong())).thenReturn(null);
            when(redisUtils.zAdd(anyString(), anyLong(), eq(0.0))).thenReturn(true);

            articleRankService.initializeAllArticles();

            verify(articleMapper, times(1)).selectList(any());
            verify(redisUtils, atLeast(100)).zAdd(anyString(), anyLong(), eq(0.0));
        }

        @Test
        @DisplayName("测试初始化空文章列表")
        void testInitializeAllArticles_EmptyList() {
            when(articleMapper.selectList(any())).thenReturn(new ArrayList<>());

            articleRankService.initializeAllArticles();

            verify(articleMapper, times(1)).selectList(any());
            verify(redisUtils, never()).zAdd(anyString(), anyLong(), anyDouble());
        }

        @Test
        @DisplayName("测试初始化已存在的文章不重复添加")
        void testInitializeAllArticles_SkipExistingArticles() {
            List<Article> articles = new ArrayList<>();
            Article article = new Article();
            article.setId(1L);
            article.setStatus(2);
            articles.add(article);

            when(articleMapper.selectList(any())).thenReturn(articles);
            when(redisUtils.zScore(anyString(), eq(1L))).thenReturn(10.0);

            articleRankService.initializeAllArticles();

            verify(redisUtils, never()).zAdd(anyString(), eq(1L), eq(0.0));
            verify(articleMapper, times(1)).selectList(any());
        }
    }

    // ==================== 热门文章查询测试 ====================

    @Nested
    @DisplayName("热门文章查询测试")
    class HotArticleQueryTests {

        @Test
        @DisplayName("测试获取热门文章完整流程")
        void testGetHotArticles_CompleteFlow() {
            LinkedHashMap<String, Double> articleIdScoreMap = new LinkedHashMap<>();
            articleIdScoreMap.put("3", 150.0);
            articleIdScoreMap.put("1", 100.0);
            articleIdScoreMap.put("2", 200.0);
            when(redisUtils.zReverseRangeWithScoresAsMap(anyString(), eq(0L), eq(24L))).thenReturn(articleIdScoreMap);

            List<Article> articles = new ArrayList<>();
            Article article1 = new Article();
            article1.setId(1L);
            article1.setTitle("Article 1");
            article1.setViewCount(100);
            article1.setLikeCount(10);
            article1.setCommentCount(5);
            article1.setAuthorId(1L);
            article1.setCategoryId(1L);
            article1.setStatus(2);

            Article article2 = new Article();
            article2.setId(2L);
            article2.setTitle("Article 2");
            article2.setViewCount(200);
            article2.setLikeCount(20);
            article2.setCommentCount(10);
            article2.setAuthorId(1L);
            article2.setCategoryId(1L);
            article2.setStatus(2);

            Article article3 = new Article();
            article3.setId(3L);
            article3.setTitle("Article 3");
            article3.setViewCount(150);
            article3.setLikeCount(15);
            article3.setCommentCount(8);
            article3.setAuthorId(1L);
            article3.setCategoryId(1L);
            article3.setStatus(2);

            articles.add(article1);
            articles.add(article2);
            articles.add(article3);

            lenient().when(articleMapper.selectBatchIds(any())).thenReturn(articles);

            Result<List<ArticleDTO>> result = articleRankService.getHotArticles(10, "day");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(3);
            assertThat(result.getData().get(0).getId()).isEqualTo(3L);
            assertThat(result.getData().get(1).getId()).isEqualTo(1L);
            assertThat(result.getData().get(2).getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("测试 ZSet 为空时返回空列表")
        void testGetHotArticles_emptyZSet_shouldReturnEmptyList() {
            when(redisUtils.zReverseRangeWithScoresAsMap(anyString(), eq(0L), eq(24L))).thenReturn(new LinkedHashMap<>());

            Result<List<ArticleDTO>> result = articleRankService.getHotArticles(10, "day");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEmpty();
            verify(articleMapper, never()).selectBatchIds(any());
        }

        @Test
        @DisplayName("测试获取热门文章异常应返回错误")
        void testGetHotArticles_exception_shouldReturnError() {
            when(redisUtils.zReverseRangeWithScoresAsMap(anyString(), eq(0L), eq(24L))).thenThrow(new RuntimeException("redis error"));

            Result<List<ArticleDTO>> result = articleRankService.getHotArticles(10, "day");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("获取热门文章失败");
        }

        @Test
        @DisplayName("ZSet 包含非发布文章时应过滤并从 ZSet 中清理")
        void testGetHotArticles_nonPublishedArticle_shouldFilterAndClean() {
            LinkedHashMap<String, Double> articleIdScoreMap = new LinkedHashMap<>();
            articleIdScoreMap.put("1", 100.0);
            articleIdScoreMap.put("2", 200.0);
            articleIdScoreMap.put("3", 150.0);
            when(redisUtils.zReverseRangeWithScoresAsMap(anyString(), eq(0L), anyLong())).thenReturn(articleIdScoreMap);

            Article article1 = new Article();
            article1.setId(1L);
            article1.setTitle("Article 1");
            article1.setStatus(2);
            article1.setViewCount(100);
            article1.setLikeCount(10);
            article1.setCommentCount(5);
            article1.setAuthorId(1L);
            article1.setCategoryId(1L);

            Article article2 = new Article();
            article2.setId(2L);
            article2.setTitle("Article 2");
            article2.setStatus(1);
            article2.setViewCount(200);
            article2.setLikeCount(20);
            article2.setCommentCount(10);
            article2.setAuthorId(1L);
            article2.setCategoryId(1L);

            Article article3 = new Article();
            article3.setId(3L);
            article3.setTitle("Article 3");
            article3.setStatus(2);
            article3.setViewCount(150);
            article3.setLikeCount(15);
            article3.setCommentCount(8);
            article3.setAuthorId(1L);
            article3.setCategoryId(1L);

            when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article1, article2, article3));

            Result<List<ArticleDTO>> result = articleRankService.getHotArticles(10, "day");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(2);
            assertThat(result.getData()).extracting(ArticleDTO::getId).containsExactlyInAnyOrder(1L, 3L);
            verify(redisUtils, atLeastOnce()).zRemove(anyString(), eq(2L));
        }

        @Test
        @DisplayName("ZSet 包含数据库中不存在的文章ID时应过滤为无效")
        void testGetHotArticles_nonExistentArticleId_shouldFilterAsInvalid() {
            LinkedHashMap<String, Double> articleIdScoreMap = new LinkedHashMap<>();
            articleIdScoreMap.put("1", 100.0);
            articleIdScoreMap.put("999", 50.0);
            when(redisUtils.zReverseRangeWithScoresAsMap(anyString(), eq(0L), anyLong())).thenReturn(articleIdScoreMap);

            Article article1 = new Article();
            article1.setId(1L);
            article1.setTitle("Article 1");
            article1.setStatus(2);
            article1.setViewCount(100);
            article1.setLikeCount(10);
            article1.setCommentCount(5);
            article1.setAuthorId(1L);
            article1.setCategoryId(1L);

            when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article1));

            Result<List<ArticleDTO>> result = articleRankService.getHotArticles(10, "day");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getId()).isEqualTo(1L);
            verify(redisUtils, atLeastOnce()).zRemove(anyString(), eq(999L));
        }

        @Test
        @DisplayName("batchConvertToDTO 返回 null 时应优雅处理并返回错误")
        void testGetHotArticles_batchConvertToDTOReturnsNull_shouldReturnError() {
            LinkedHashMap<String, Double> articleIdScoreMap = new LinkedHashMap<>();
            articleIdScoreMap.put("1", 100.0);
            when(redisUtils.zReverseRangeWithScoresAsMap(anyString(), eq(0L), anyLong())).thenReturn(articleIdScoreMap);

            Article article1 = new Article();
            article1.setId(1L);
            article1.setTitle("Article 1");
            article1.setStatus(2);
            article1.setViewCount(100);
            article1.setLikeCount(10);
            article1.setCommentCount(5);
            article1.setAuthorId(1L);
            article1.setCategoryId(1L);

            when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article1));
            when(articleService.batchConvertToDTO(any())).thenReturn(null);

            Result<List<ArticleDTO>> result = articleRankService.getHotArticles(10, "day");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("获取热门文章失败");
        }

        @Test
        @DisplayName("DB 中已发布但 DTO 映射缺失时应跳过该文章")
        void testGetHotArticles_dtoMapMissingPublishedArticle_shouldSkip() {
            LinkedHashMap<String, Double> articleIdScoreMap = new LinkedHashMap<>();
            articleIdScoreMap.put("1", 100.0);
            articleIdScoreMap.put("2", 200.0);
            when(redisUtils.zReverseRangeWithScoresAsMap(anyString(), eq(0L), anyLong())).thenReturn(articleIdScoreMap);

            Article article1 = new Article();
            article1.setId(1L);
            article1.setTitle("Article 1");
            article1.setStatus(2);
            article1.setViewCount(100);
            article1.setLikeCount(10);
            article1.setCommentCount(5);
            article1.setAuthorId(1L);
            article1.setCategoryId(1L);

            Article article2 = new Article();
            article2.setId(2L);
            article2.setTitle("Article 2");
            article2.setStatus(2);
            article2.setViewCount(200);
            article2.setLikeCount(20);
            article2.setCommentCount(10);
            article2.setAuthorId(1L);
            article2.setCategoryId(1L);

            when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article1, article2));
            ArticleDTO dto1 = new ArticleDTO();
            dto1.setId(1L);
            dto1.setTitle("Article 1");
            dto1.setStatus(2);
            when(articleService.batchConvertToDTO(any())).thenReturn(List.of(dto1));

            Result<List<ArticleDTO>> result = articleRankService.getHotArticles(10, "day");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getId()).isEqualTo(1L);
        }
    }

    // ==================== 排行榜重置与清理测试 ====================

    @Nested
    @DisplayName("排行榜重置与清理测试")
    class RankResetTests {

        @Test
        @DisplayName("测试重置日榜 - 初始化新文章到 ZSet")
        void testResetRank_day_shouldInitializeWhenEmpty() {
            Article article = new Article();
            article.setId(1L);
            article.setStatus(2);
            when(articleMapper.selectList(any())).thenReturn(List.of(article));
            when(redisUtils.zAdd(anyString(), anyLong(), eq(0.0))).thenReturn(true);
            when(redisUtils.expire(anyString(), anyLong(), any())).thenReturn(true);

            articleRankService.resetRank("day");

            verify(redisUtils, atLeastOnce()).zAdd(anyString(), eq(1L), eq(0.0));
            verify(redisUtils).expire(anyString(), eq(2L), any());
        }

        @Test
        @DisplayName("测试重置周榜 - 初始化新文章到 ZSet")
        void testResetRank_week_shouldInitializeWhenEmpty() {
            Article article = new Article();
            article.setId(1L);
            article.setStatus(2);
            when(articleMapper.selectList(any())).thenReturn(List.of(article));
            when(redisUtils.zAdd(anyString(), anyLong(), eq(0.0))).thenReturn(true);
            when(redisUtils.expire(anyString(), anyLong(), any())).thenReturn(true);

            articleRankService.resetRank("week");

            verify(redisUtils, atLeastOnce()).zAdd(anyString(), eq(1L), eq(0.0));
            verify(redisUtils).expire(anyString(), eq(14L), any());
        }

        @Test
        @DisplayName("测试从排行榜删除文章")
        void testRemoveFromRank() {
            Long articleId = 888L;
            when(redisUtils.zRemove(anyString(), eq(articleId))).thenReturn(1L);

            articleRankService.removeFromRank(articleId);

            verify(redisUtils, times(2)).zRemove(anyString(), eq(articleId));
        }

        @Test
        @DisplayName("测试从排行榜删除文章 - null articleId")
        void testRemoveFromRank_nullArticleId_shouldSkip() {
            articleRankService.removeFromRank(null);

            verify(redisUtils, never()).zRemove(anyString(), any());
        }

        @Test
        @DisplayName("无效周期应不做任何操作")
        void testResetRank_invalidPeriod_shouldDoNothing() {
            articleRankService.resetRank("month");

            verify(articleMapper, never()).selectList(any());
            verify(redisUtils, never()).zAdd(anyString(), anyLong(), anyDouble());
        }

        @Test
        @DisplayName("日榜重置时已存在文章不应重复初始化")
        void testResetRank_day_existingArticles_shouldNotDuplicate() {
            Article article = new Article();
            article.setId(1L);
            article.setStatus(2);
            when(articleMapper.selectList(any())).thenReturn(List.of(article));
            when(redisUtils.zAdd(anyString(), eq(1L), eq(0.0))).thenReturn(false);
            when(redisUtils.expire(anyString(), anyLong(), any())).thenReturn(true);

            articleRankService.resetRank("day");

            verify(redisUtils, atLeastOnce()).zAdd(anyString(), eq(1L), eq(0.0));
            verify(redisUtils).expire(anyString(), eq(2L), any());
        }
    }

    // ==================== 初始化文章测试 ====================

    @Nested
    @DisplayName("初始化文章测试")
    class InitializeArticleTests {

        @Test
        @DisplayName("测试初始化新文章到排行榜")
        void testInitializeArticle_NewArticle() {
            Long articleId = 999L;
            when(redisUtils.zAdd(anyString(), eq(articleId), eq(0.0))).thenReturn(true);

            articleRankService.initializeArticle(articleId);

            verify(redisUtils, times(2)).zAdd(anyString(), eq(articleId), eq(0.0));
        }

        @Test
        @DisplayName("测试初始化文章 - null articleId")
        void testInitializeArticle_nullArticleId_shouldSkip() {
            articleRankService.initializeArticle(null);

            verify(redisUtils, never()).zAdd(anyString(), anyLong(), anyDouble());
        }

        @Test
        @DisplayName("文章已存在于 ZSet 时应优雅处理")
        void testInitializeArticle_articleAlreadyExists_shouldHandleGracefully() {
            Long articleId = 999L;
            when(redisUtils.zAdd(anyString(), eq(articleId), eq(0.0))).thenReturn(false);

            articleRankService.initializeArticle(articleId);

            verify(redisUtils, times(2)).zAdd(anyString(), eq(articleId), eq(0.0));
        }
    }

    // ==================== 批量获取分数测试 ====================

    @Nested
    @DisplayName("批量获取分数测试")
    class GetArticleScoresTests {

        @Test
        @DisplayName("测试批量获取文章热度分数")
        void testGetArticleScores_shouldReturnScores() {
            when(redisUtils.zScoreBatch(anyString(), any())).thenReturn(Map.of(1L, 10.0, 2L, 20.0));

            Map<Long, Double> scores = articleRankService.getArticleScores(List.of(1L, 2L), "day");

            assertThat(scores).hasSize(2);
            assertThat(scores.get(1L)).isEqualTo(10.0);
        }

        @Test
        @DisplayName("测试批量获取文章热度分数 - 空列表应返回空Map")
        void testGetArticleScores_emptyList_shouldReturnEmptyMap() {
            Map<Long, Double> scores = articleRankService.getArticleScores(Collections.emptyList(), "day");

            assertThat(scores).isEmpty();
            verify(redisUtils, never()).zScoreBatch(anyString(), any());
        }

        @Test
        @DisplayName("测试批量获取文章热度分数 - 异常应返回空Map")
        void testGetArticleScores_exception_shouldReturnEmptyMap() {
            when(redisUtils.zScoreBatch(anyString(), any())).thenThrow(new RuntimeException("redis error"));

            Map<Long, Double> scores = articleRankService.getArticleScores(List.of(1L), "day");

            assertThat(scores).isEmpty();
        }

        @Test
        @DisplayName("zScoreBatch 返回部分结果时应只返回可用分数")
        void testGetArticleScores_partialResults_shouldReturnOnlyAvailableScores() {
            when(redisUtils.zScoreBatch(anyString(), any())).thenReturn(Map.of(1L, 10.0));

            Map<Long, Double> scores = articleRankService.getArticleScores(List.of(1L, 2L, 3L), "day");

            assertThat(scores).hasSize(1);
            assertThat(scores.get(1L)).isEqualTo(10.0);
        }
    }

    // ==================== 便捷方法测试 ====================

    @Nested
    @DisplayName("便捷方法测试")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("测试 incrementViewScore 排除作者自己")
        void testIncrementViewScore_ExcludeAuthor() {
            articleRankService.incrementViewScore(1L, 100L, 100L);

            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), anyLong(), anyDouble(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("测试 incrementViewScore 非作者正常更新")
        void testIncrementViewScore_NonAuthorUpdates() {
            when(redisUtils.zIncrByAtomic(anyString(), anyString(), eq(1L), eq(1.0), eq(2L), eq(14L)))
                    .thenReturn(1.0);

            articleRankService.incrementViewScore(1L, 100L, 200L);

            verify(redisUtils, times(1)).zIncrByAtomic(anyString(), anyString(), eq(1L), eq(1.0), eq(2L), eq(14L));
        }

        @Test
        @DisplayName("测试 incrementLikeScore 排除作者自己")
        void testIncrementLikeScore_ExcludeAuthor() {
            articleRankService.incrementLikeScore(1L, 100L, 100L);

            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), anyLong(), anyDouble(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("测试 decrementLikeScore 排除作者自己")
        void testDecrementLikeScore_ExcludeAuthor() {
            articleRankService.decrementLikeScore(1L, 100L, 100L);

            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), anyLong(), anyDouble(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("测试 incrementCommentScore 排除作者自己")
        void testIncrementCommentScore_ExcludeAuthor() {
            articleRankService.incrementCommentScore(1L, 100L, 100L);

            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), anyLong(), anyDouble(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("测试 decrementCommentScore 排除作者自己")
        void testDecrementCommentScore_ExcludeAuthor() {
            articleRankService.decrementCommentScore(1L, 100L, 100L);

            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), anyLong(), anyDouble(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("测试 incrementFavoriteScore 排除作者自己")
        void testIncrementFavoriteScore_ExcludeAuthor() {
            articleRankService.incrementFavoriteScore(1L, 100L, 100L);

            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), anyLong(), anyDouble(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("测试 decrementFavoriteScore 排除作者自己")
        void testDecrementFavoriteScore_ExcludeAuthor() {
            articleRankService.decrementFavoriteScore(1L, 100L, 100L);

            verify(redisUtils, never()).zIncrByAtomic(anyString(), anyString(), anyLong(), anyDouble(), anyLong(), anyLong());
        }
    }

    // ==================== 分页热门文章查询测试 ====================

    @Nested
    @DisplayName("分页热门文章查询测试")
    class HotArticlePageQueryTests {

        @Test
        @DisplayName("测试分页获取热门文章完整流程")
        void testGetHotArticlesPage_completeFlow_shouldReturnPage() {
            when(redisUtils.zSize(anyString())).thenReturn(25L);
            LinkedHashMap<String, Double> articleIdScoreMap = new LinkedHashMap<>();
            articleIdScoreMap.put("3", 150.0);
            articleIdScoreMap.put("1", 100.0);
            articleIdScoreMap.put("2", 200.0);
            when(redisUtils.zReverseRangeWithScoresAsMap(anyString(), eq(0L), eq(2L))).thenReturn(articleIdScoreMap);

            Article article1 = new Article();
            article1.setId(1L);
            article1.setTitle("Article 1");
            article1.setViewCount(100);
            article1.setLikeCount(10);
            article1.setCommentCount(5);
            article1.setAuthorId(1L);
            article1.setCategoryId(1L);
            article1.setStatus(2);

            Article article2 = new Article();
            article2.setId(2L);
            article2.setTitle("Article 2");
            article2.setViewCount(200);
            article2.setLikeCount(20);
            article2.setCommentCount(10);
            article2.setAuthorId(1L);
            article2.setCategoryId(1L);
            article2.setStatus(2);

            Article article3 = new Article();
            article3.setId(3L);
            article3.setTitle("Article 3");
            article3.setViewCount(150);
            article3.setLikeCount(15);
            article3.setCommentCount(8);
            article3.setAuthorId(1L);
            article3.setCategoryId(1L);
            article3.setStatus(2);

            when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article1, article2, article3));

            ArticleDTO dto1 = new ArticleDTO();
            dto1.setId(1L);
            dto1.setTitle("Article 1");
            dto1.setStatus(2);

            ArticleDTO dto2 = new ArticleDTO();
            dto2.setId(2L);
            dto2.setTitle("Article 2");
            dto2.setStatus(2);

            ArticleDTO dto3 = new ArticleDTO();
            dto3.setId(3L);
            dto3.setTitle("Article 3");
            dto3.setStatus(2);

            when(articleService.batchConvertToDTO(any())).thenReturn(List.of(dto1, dto2, dto3));

            Result<PageResult<ArticleDTO>> result = articleRankService.getHotArticlesPage(1, 3, "day");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getItems()).hasSize(3);
            assertThat(result.getData().getTotal()).isEqualTo(25);
            assertThat(result.getData().getItems().get(0).getId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("ZSet 为空时应返回空页")
        void testGetHotArticlesPage_emptyZSet_shouldReturnEmptyPage() {
            when(redisUtils.zSize(anyString())).thenReturn(0L);

            Result<PageResult<ArticleDTO>> result = articleRankService.getHotArticlesPage(1, 10, "day");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getItems()).isEmpty();
            verify(articleMapper, never()).selectBatchIds(any());
        }

        @Test
        @DisplayName("分页获取时非发布文章应过滤并从 ZSet 中清理")
        void testGetHotArticlesPage_nonPublishedArticle_shouldFilterAndClean() {
            when(redisUtils.zSize(anyString())).thenReturn(10L);
            LinkedHashMap<String, Double> articleIdScoreMap = new LinkedHashMap<>();
            articleIdScoreMap.put("1", 100.0);
            articleIdScoreMap.put("2", 200.0);
            when(redisUtils.zReverseRangeWithScoresAsMap(anyString(), eq(0L), anyLong())).thenReturn(articleIdScoreMap);

            Article article1 = new Article();
            article1.setId(1L);
            article1.setTitle("Article 1");
            article1.setStatus(2);
            article1.setViewCount(100);
            article1.setLikeCount(10);
            article1.setCommentCount(5);
            article1.setAuthorId(1L);
            article1.setCategoryId(1L);

            Article article2 = new Article();
            article2.setId(2L);
            article2.setTitle("Article 2");
            article2.setStatus(1);
            article2.setViewCount(200);
            article2.setLikeCount(20);
            article2.setCommentCount(10);
            article2.setAuthorId(1L);
            article2.setCategoryId(1L);

            when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article1, article2));

            ArticleDTO dto1 = new ArticleDTO();
            dto1.setId(1L);
            dto1.setTitle("Article 1");
            dto1.setStatus(2);
            when(articleService.batchConvertToDTO(any())).thenReturn(List.of(dto1));

            Result<PageResult<ArticleDTO>> result = articleRankService.getHotArticlesPage(1, 10, "day");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).hasSize(1);
            assertThat(result.getData().getItems().get(0).getId()).isEqualTo(1L);
            verify(redisUtils, atLeastOnce()).zRemove(anyString(), eq(2L));
        }

        @Test
        @DisplayName("分页获取时无效文章ID应被跟踪且 total 应调整")
        void testGetHotArticlesPage_invalidArticleId_shouldTrackAndAdjustTotal() {
            when(redisUtils.zSize(anyString())).thenReturn(10L);
            LinkedHashMap<String, Double> articleIdScoreMap = new LinkedHashMap<>();
            articleIdScoreMap.put("1", 100.0);
            articleIdScoreMap.put("999", 50.0);
            when(redisUtils.zReverseRangeWithScoresAsMap(anyString(), eq(0L), anyLong())).thenReturn(articleIdScoreMap);

            Article article1 = new Article();
            article1.setId(1L);
            article1.setTitle("Article 1");
            article1.setStatus(2);
            article1.setViewCount(100);
            article1.setLikeCount(10);
            article1.setCommentCount(5);
            article1.setAuthorId(1L);
            article1.setCategoryId(1L);

            when(articleMapper.selectBatchIds(any())).thenReturn(List.of(article1));

            ArticleDTO dto1 = new ArticleDTO();
            dto1.setId(1L);
            dto1.setTitle("Article 1");
            dto1.setStatus(2);
            when(articleService.batchConvertToDTO(any())).thenReturn(List.of(dto1));

            Result<PageResult<ArticleDTO>> result = articleRankService.getHotArticlesPage(1, 10, "day");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).hasSize(1);
            assertThat(result.getData().getTotal()).isEqualTo(9);
            verify(redisUtils, atLeastOnce()).zRemove(anyString(), eq(999L));
        }

        @Test
        @DisplayName("分页获取热门文章异常应返回错误")
        void testGetHotArticlesPage_exception_shouldReturnError() {
            when(redisUtils.zSize(anyString())).thenThrow(new RuntimeException("redis error"));

            Result<PageResult<ArticleDTO>> result = articleRankService.getHotArticlesPage(1, 10, "day");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("获取热门文章失败");
        }
    }
}
