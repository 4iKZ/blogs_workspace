package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ArticleStatisticsDTO;
import com.blog.entity.Article;
import com.blog.event.ArticleViewCountChangeEvent;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.service.ArticleRankService;
import com.blog.utils.AuthUtils;
import com.blog.utils.RedisCacheUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleStatisticsServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private UserLikeMapper userLikeMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedisCacheUtils redisCacheUtils;

    @Mock
    private ArticleRankService articleRankService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ArticleStatisticsServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    // ==================== getArticleStatistics ====================

    @Test
    void getArticleStatistics_whenArticleExists_shouldReturnStatistics() {
        Article article = new Article();
        article.setId(1L);
        article.setViewCount(100);
        article.setLikeCount(10);
        article.setCommentCount(5);
        article.setFavoriteCount(3);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(redisCacheUtils.getArticleRedisViewCount(1L)).thenReturn(20);

        Result<ArticleStatisticsDTO> result = service.getArticleStatistics(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getViewCount()).isEqualTo(120);
        assertThat(result.getData().getLikeCount()).isEqualTo(10);
        assertThat(result.getData().getCommentCount()).isEqualTo(5);
        assertThat(result.getData().getFavoriteCount()).isEqualTo(3);
    }

    @Test
    void getArticleStatistics_whenArticleNotFound_shouldReturnError() {
        when(articleMapper.selectById(99L)).thenReturn(null);

        Result<ArticleStatisticsDTO> result = service.getArticleStatistics(99L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文章不存在");
    }

    @Test
    void getArticleStatistics_whenRedisViewCountNull_shouldTreatAsZero() {
        Article article = new Article();
        article.setId(1L);
        article.setViewCount(100);
        when(articleMapper.selectById(1L)).thenReturn(article);
        when(redisCacheUtils.getArticleRedisViewCount(1L)).thenReturn(0);

        Result<ArticleStatisticsDTO> result = service.getArticleStatistics(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getViewCount()).isEqualTo(100);
    }

    // ==================== incrementViewCount ====================

    @Test
    void incrementViewCount_whenArticleExistsAndPublished_shouldIncrement() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(1L)).thenReturn(article);
        doReturn(true).when(valueOperations).setIfAbsent(anyString(), any(), anyLong(), any());
        doReturn(1L).when(valueOperations).increment(anyString(), anyLong());
        doNothing().when(articleRankService).incrementViewScore(anyLong(), anyLong(), anyLong());
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Result<Void> result = service.incrementViewCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(valueOperations).increment(anyString(), eq(1L));
        verify(eventPublisher).publishEvent(any(ArticleViewCountChangeEvent.class));
    }

    @Test
    void incrementViewCount_whenArticleNotFound_shouldReturnErrorAndCleanup() {
        when(articleMapper.selectById(99L)).thenReturn(null);
        doReturn(true).when(valueOperations).setIfAbsent(anyString(), any(), anyLong(), any());

        Result<Void> result = service.incrementViewCount(99L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文章不存在");
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void incrementViewCount_whenArticleNotPublished_shouldReturnErrorAndCleanup() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(1); // draft
        when(articleMapper.selectById(1L)).thenReturn(article);
        doReturn(true).when(valueOperations).setIfAbsent(anyString(), any(), anyLong(), any());

        Result<Void> result = service.incrementViewCount(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文章未发布");
        verify(redisTemplate).delete(anyString());
    }

    @Test
    void incrementViewCount_whenDuplicateView_shouldIgnore() {
        doReturn(false).when(valueOperations).setIfAbsent(anyString(), any(), anyLong(), any());

        Result<Void> result = service.incrementViewCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper, never()).selectById(anyLong());
    }

    // ==================== incrementLikeCount / decrementLikeCount
    // ====================

    @Test
    void incrementLikeCount_whenArticleExists_shouldSucceed() {
        when(articleMapper.updateLikeCount(1L, 1)).thenReturn(1);

        Result<Void> result = service.incrementLikeCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).updateLikeCount(1L, 1);
    }

    @Test
    void incrementLikeCount_whenArticleNotFound_shouldReturnError() {
        when(articleMapper.updateLikeCount(99L, 1)).thenReturn(0);

        Result<Void> result = service.incrementLikeCount(99L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("文章不存在");
    }

    @Test
    void decrementLikeCount_whenSafelyDecrement_shouldSucceed() {
        when(articleMapper.decrementLikeCountSafely(1L)).thenReturn(1);

        Result<Void> result = service.decrementLikeCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).decrementLikeCountSafely(1L);
    }

    // ==================== incrementCommentCount / decrementCommentCount
    // ====================

    @Test
    void incrementCommentCount_whenArticleExists_shouldSucceed() {
        when(articleMapper.updateCommentCount(1L, 1)).thenReturn(1);

        Result<Void> result = service.incrementCommentCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).updateCommentCount(1L, 1);
    }

    @Test
    void decrementCommentCount_nonPositiveCount_shouldReturnSuccessDirectly() {
        Result<Void> result = service.decrementCommentCount(1L, 0);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper, never()).updateCommentCount(anyLong(), anyInt());
    }

    // ==================== incrementFavoriteCount / decrementFavoriteCount
    // ====================

    @Test
    void incrementFavoriteCount_whenArticleExists_shouldSucceed() {
        when(articleMapper.updateFavoriteCount(1L, 1)).thenReturn(1);

        Result<Void> result = service.incrementFavoriteCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).updateFavoriteCount(1L, 1);
    }

    @Test
    void decrementFavoriteCount_whenSafelyDecrement_shouldSucceed() {
        when(articleMapper.decrementFavoriteCountSafely(1L)).thenReturn(1);

        Result<Void> result = service.decrementFavoriteCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).decrementFavoriteCountSafely(1L);
    }

    // ==================== getHotArticleStatistics ====================

    @Test
    void getHotArticleStatistics_whenHotArticlesExist_shouldReturnStatistics() {
        Article article = new Article();
        article.setId(1L);
        article.setViewCount(100);
        article.setLikeCount(10);
        article.setCommentCount(5);
        article.setFavoriteCount(3);
        when(articleMapper.selectHotArticles(10)).thenReturn(List.of(article));
        when(redisCacheUtils.batchGetArticleRedisViewCount(List.of(1L))).thenReturn(Collections.singletonMap(1L, 20));

        Result<List<ArticleStatisticsDTO>> result = service.getHotArticleStatistics(10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getViewCount()).isEqualTo(120);
    }

    @Test
    void getHotArticleStatistics_whenNoHotArticles_shouldReturnEmptyList() {
        when(articleMapper.selectHotArticles(10)).thenReturn(Collections.emptyList());

        Result<List<ArticleStatisticsDTO>> result = service.getHotArticleStatistics(10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    // ==================== getTopArticleStatistics ====================

    @Test
    void getTopArticleStatistics_whenTopArticlesExist_shouldReturnStatistics() {
        Article article = new Article();
        article.setId(1L);
        article.setViewCount(200);
        article.setLikeCount(20);
        article.setCommentCount(10);
        article.setFavoriteCount(5);
        when(articleMapper.selectTopArticles(5)).thenReturn(List.of(article));
        when(redisCacheUtils.batchGetArticleRedisViewCount(List.of(1L))).thenReturn(Collections.singletonMap(1L, 30));

        Result<List<ArticleStatisticsDTO>> result = service.getTopArticleStatistics(5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getViewCount()).isEqualTo(230);
    }

    // ==================== getRecommendedArticleStatistics ====================

    @Test
    void getRecommendedArticleStatistics_whenRecommendedExist_shouldReturnStatistics() {
        Article article = new Article();
        article.setId(1L);
        article.setViewCount(50);
        article.setLikeCount(5);
        article.setCommentCount(2);
        article.setFavoriteCount(1);
        when(articleMapper.selectRecommendedArticles(3)).thenReturn(List.of(article));
        when(redisCacheUtils.batchGetArticleRedisViewCount(List.of(1L))).thenReturn(Collections.singletonMap(1L, 10));

        Result<List<ArticleStatisticsDTO>> result = service.getRecommendedArticleStatistics(3);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getViewCount()).isEqualTo(60);
    }

    // ==================== syncViewCountToDatabase / onApplicationEvent
    // ====================

    @Test
    void syncViewCountToDatabase_whenQueueEmpty_shouldReturnEarly() {
        when(stringRedisTemplate.execute(any(org.springframework.data.redis.core.script.DefaultRedisScript.class),
                anyList(), any())).thenReturn(Collections.emptyList());

        service.syncViewCountToDatabase();

        verify(articleMapper, never()).incrementViewCountBatch(anyLong(), anyInt());
    }

    @Test
    void onApplicationEvent_shouldSyncViewCount() {
        when(stringRedisTemplate.execute(any(org.springframework.data.redis.core.script.DefaultRedisScript.class),
                anyList(), any(), any())).thenReturn(Collections.emptyList());

        service.onApplicationEvent(mock(org.springframework.context.event.ContextClosedEvent.class));

        verify(stringRedisTemplate).execute(any(org.springframework.data.redis.core.script.DefaultRedisScript.class),
                anyList(), any(), any());
    }

    // ==================== incrementViewCount 异常分支 ====================

    @Test
    void incrementViewCount_whenIncrementViewScoreThrows_shouldStillReturnSuccess() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(1L)).thenReturn(article);
        doReturn(true).when(valueOperations).setIfAbsent(anyString(), any(), anyLong(), any());
        doReturn(1L).when(valueOperations).increment(anyString(), anyLong());
        doThrow(new RuntimeException("rank error")).when(articleRankService).incrementViewScore(anyLong(), anyLong(),
                anyLong());
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Result<Void> result = service.incrementViewCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(eventPublisher).publishEvent(any(ArticleViewCountChangeEvent.class));
    }

    @Test
    void incrementViewCount_whenSelectByIdThrows_shouldReturnError() {
        doReturn(true).when(valueOperations).setIfAbsent(anyString(), any(), anyLong(), any());
        doThrow(new RuntimeException("db error")).when(articleMapper).selectById(1L);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Result<Void> result = service.incrementViewCount(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("增加文章浏览量失败");
    }

    @Test
    void incrementViewCount_whenIncrementThrows_shouldReturnError() {
        Article article = new Article();
        article.setId(1L);
        article.setStatus(2);
        article.setAuthorId(2L);
        when(articleMapper.selectById(1L)).thenReturn(article);
        doReturn(true).when(valueOperations).setIfAbsent(anyString(), any(), anyLong(), any());
        doThrow(new RuntimeException("redis error")).when(valueOperations).increment(anyString(), anyLong());
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Result<Void> result = service.incrementViewCount(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("增加文章浏览量失败");
    }

    // ==================== getClientIp ====================

    @Test
    void getClientIp_whenXForwardedForContainsMultipleIps_shouldReturnFirst() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Method method = ArticleStatisticsServiceImpl.class.getDeclaredMethod("getClientIp");
        method.setAccessible(true);
        String ip = (String) method.invoke(service);

        assertThat(ip).isEqualTo("192.168.1.1");
    }

    @Test
    void getClientIp_whenXForwardedForIsUnknown_shouldSkip() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Method method = ArticleStatisticsServiceImpl.class.getDeclaredMethod("getClientIp");
        method.setAccessible(true);
        String ip = (String) method.invoke(service);

        assertThat(ip).isEqualTo("127.0.0.1");
    }

    @Test
    void getClientIp_whenXRealIPIsUnknown_shouldFallbackToRemoteAddr() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("unknown");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Method method = ArticleStatisticsServiceImpl.class.getDeclaredMethod("getClientIp");
        method.setAccessible(true);
        String ip = (String) method.invoke(service);

        assertThat(ip).isEqualTo("127.0.0.1");
    }

    @Test
    void getClientIp_whenXForwardedForIsEmpty_shouldSkip() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Method method = ArticleStatisticsServiceImpl.class.getDeclaredMethod("getClientIp");
        method.setAccessible(true);
        String ip = (String) method.invoke(service);

        assertThat(ip).isEqualTo("127.0.0.1");
    }

    @Test
    void getClientIp_whenAllHeadersMissing_shouldReturnRemoteAddr() throws Exception {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        Method method = ArticleStatisticsServiceImpl.class.getDeclaredMethod("getClientIp");
        method.setAccessible(true);
        String ip = (String) method.invoke(service);

        assertThat(ip).isEqualTo("127.0.0.1");
    }

    // ==================== decrementLikeCount / decrementCommentCount /
    // decrementFavoriteCount ====================

    @Test
    void decrementLikeCount_whenSafelyDecrementReturnsZero_shouldReturnSuccess() {
        when(articleMapper.decrementLikeCountSafely(1L)).thenReturn(0);

        Result<Void> result = service.decrementLikeCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).decrementLikeCountSafely(1L);
    }

    @Test
    void decrementCommentCount_whenSingleArgAndMapperReturnsZero_shouldReturnSuccess() {
        when(articleMapper.updateCommentCount(1L, -1)).thenReturn(0);

        Result<Void> result = service.decrementCommentCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).updateCommentCount(1L, -1);
    }

    @Test
    void decrementFavoriteCount_whenSafelyDecrementReturnsZero_shouldReturnSuccess() {
        when(articleMapper.decrementFavoriteCountSafely(1L)).thenReturn(0);

        Result<Void> result = service.decrementFavoriteCount(1L);

        assertThat(result.isSuccess()).isTrue();
        verify(articleMapper).decrementFavoriteCountSafely(1L);
    }

    // ==================== syncViewCountToDatabase ====================

    @Test
    void syncViewCountToDatabase_whenLuaReturnsValidPairs_shouldSyncSuccessfully() {
        doReturn(List.of(List.of(1L, 5L), List.of(2L, 3L)))
                .when(stringRedisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString());
        when(articleMapper.incrementViewCountBatch(1L, 5)).thenReturn(1);
        when(articleMapper.incrementViewCountBatch(2L, 3)).thenReturn(1);

        service.syncViewCountToDatabase();

        verify(articleMapper).incrementViewCountBatch(1L, 5);
        verify(articleMapper).incrementViewCountBatch(2L, 3);
    }

    @Test
    void syncViewCountToDatabase_whenItemIsNotList_shouldSkip() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(List.of("invalid"));

        service.syncViewCountToDatabase();

        verify(articleMapper, never()).incrementViewCountBatch(anyLong(), anyInt());
    }

    @Test
    void syncViewCountToDatabase_whenPairSizeLessThan2_shouldSkip() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(List.of(List.of(1L)));

        service.syncViewCountToDatabase();

        verify(articleMapper, never()).incrementViewCountBatch(anyLong(), anyInt());
    }

    @Test
    void syncViewCountToDatabase_whenDuplicateArticleIds_shouldDeduplicate() {
        doReturn(List.of(List.of(1L, 5L), List.of(1L, 3L)))
                .when(stringRedisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString());
        when(articleMapper.incrementViewCountBatch(1L, 5)).thenReturn(1);

        service.syncViewCountToDatabase();

        verify(articleMapper).incrementViewCountBatch(1L, 5);
        verify(articleMapper, never()).incrementViewCountBatch(eq(1L), eq(3));
    }

    @Test
    void syncViewCountToDatabase_whenIncrementIsZero_shouldSkip() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(List.of(List.of(1L, 0L)));

        service.syncViewCountToDatabase();

        verify(articleMapper, never()).incrementViewCountBatch(anyLong(), anyInt());
    }

    @Test
    void syncViewCountToDatabase_whenIncrementViewCountBatchReturnsZero_shouldNotIncrementSuccessCount() {
        doReturn(List.of(List.of(1L, 5L)))
                .when(stringRedisTemplate)
                .execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString());
        when(articleMapper.incrementViewCountBatch(1L, 5)).thenReturn(0);

        service.syncViewCountToDatabase();

        verify(articleMapper).incrementViewCountBatch(1L, 5);
    }

    @Test
    void syncViewCountToDatabase_whenItemThrowsNumberFormatException_shouldCatchAndContinue() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(List.of(List.of("abc", 5L)));

        service.syncViewCountToDatabase();

        verify(articleMapper, never()).incrementViewCountBatch(anyLong(), anyInt());
    }

    @Test
    void syncViewCountToDatabase_whenUnexpectedExceptionInLoop_shouldBeCaughtByOuterTry() {
        Iterator<Object> iterator = mock(Iterator.class);
        when(iterator.hasNext()).thenThrow(new RuntimeException("iterator error"));
        List<Object> syncData = mock(List.class);
        when(syncData.iterator()).thenReturn(iterator);
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenReturn(syncData);

        assertThatCode(() -> service.syncViewCountToDatabase()).doesNotThrowAnyException();
    }

    // ==================== atomicPopViewCounts ====================

    @Test
    void atomicPopViewCounts_whenExecuteThrows_shouldReturnEmptyList() throws Exception {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
                .thenThrow(new RuntimeException("script error"));

        Method method = ArticleStatisticsServiceImpl.class.getDeclaredMethod("atomicPopViewCounts", int.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) method.invoke(service, 10);

        assertThat(result).isEmpty();
    }

    // ==================== getHotArticleStatistics ====================

    @Test
    void getHotArticleStatistics_whenViewCountIsNull_shouldUseZero() {
        Article article = new Article();
        article.setId(1L);
        // viewCount 默认为 null
        article.setLikeCount(10);
        article.setCommentCount(5);
        article.setFavoriteCount(3);
        when(articleMapper.selectHotArticles(10)).thenReturn(List.of(article));
        when(redisCacheUtils.batchGetArticleRedisViewCount(List.of(1L))).thenReturn(Collections.singletonMap(1L, 20));

        Result<List<ArticleStatisticsDTO>> result = service.getHotArticleStatistics(10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getViewCount()).isEqualTo(20);
    }

    @Test
    void getHotArticleStatistics_whenRedisViewCountMapMissingArticle_shouldUseZero() {
        Article article = new Article();
        article.setId(1L);
        article.setViewCount(100);
        article.setLikeCount(10);
        article.setCommentCount(5);
        article.setFavoriteCount(3);
        when(articleMapper.selectHotArticles(10)).thenReturn(List.of(article));
        when(redisCacheUtils.batchGetArticleRedisViewCount(List.of(1L))).thenReturn(Collections.emptyMap());

        Result<List<ArticleStatisticsDTO>> result = service.getHotArticleStatistics(10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getViewCount()).isEqualTo(100);
    }

    // ==================== getTopArticleStatistics ====================

    @Test
    void getTopArticleStatistics_whenNoTopArticles_shouldReturnEmptyList() {
        when(articleMapper.selectTopArticles(5)).thenReturn(Collections.emptyList());

        Result<List<ArticleStatisticsDTO>> result = service.getTopArticleStatistics(5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void getTopArticleStatistics_whenViewCountIsNull_shouldUseZero() {
        Article article = new Article();
        article.setId(1L);
        // viewCount 默认为 null
        article.setLikeCount(10);
        article.setCommentCount(5);
        article.setFavoriteCount(3);
        when(articleMapper.selectTopArticles(5)).thenReturn(List.of(article));
        when(redisCacheUtils.batchGetArticleRedisViewCount(List.of(1L))).thenReturn(Collections.singletonMap(1L, 20));

        Result<List<ArticleStatisticsDTO>> result = service.getTopArticleStatistics(5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getViewCount()).isEqualTo(20);
    }

    @Test
    void getTopArticleStatistics_whenRedisViewCountMapMissingArticle_shouldUseZero() {
        Article article = new Article();
        article.setId(1L);
        article.setViewCount(100);
        article.setLikeCount(10);
        article.setCommentCount(5);
        article.setFavoriteCount(3);
        when(articleMapper.selectTopArticles(5)).thenReturn(List.of(article));
        when(redisCacheUtils.batchGetArticleRedisViewCount(List.of(1L))).thenReturn(Collections.emptyMap());

        Result<List<ArticleStatisticsDTO>> result = service.getTopArticleStatistics(5);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getViewCount()).isEqualTo(100);
    }

    // ==================== getRecommendedArticleStatistics ====================

    @Test
    void getRecommendedArticleStatistics_whenNoRecommendedArticles_shouldReturnEmptyList() {
        when(articleMapper.selectRecommendedArticles(3)).thenReturn(Collections.emptyList());

        Result<List<ArticleStatisticsDTO>> result = service.getRecommendedArticleStatistics(3);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    void getRecommendedArticleStatistics_whenViewCountIsNull_shouldUseZero() {
        Article article = new Article();
        article.setId(1L);
        // viewCount 默认为 null
        article.setLikeCount(10);
        article.setCommentCount(5);
        article.setFavoriteCount(3);
        when(articleMapper.selectRecommendedArticles(3)).thenReturn(List.of(article));
        when(redisCacheUtils.batchGetArticleRedisViewCount(List.of(1L))).thenReturn(Collections.singletonMap(1L, 20));

        Result<List<ArticleStatisticsDTO>> result = service.getRecommendedArticleStatistics(3);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getViewCount()).isEqualTo(20);
    }

    @Test
    void getRecommendedArticleStatistics_whenRedisViewCountMapMissingArticle_shouldUseZero() {
        Article article = new Article();
        article.setId(1L);
        article.setViewCount(100);
        article.setLikeCount(10);
        article.setCommentCount(5);
        article.setFavoriteCount(3);
        when(articleMapper.selectRecommendedArticles(3)).thenReturn(List.of(article));
        when(redisCacheUtils.batchGetArticleRedisViewCount(List.of(1L))).thenReturn(Collections.emptyMap());

        Result<List<ArticleStatisticsDTO>> result = service.getRecommendedArticleStatistics(3);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getViewCount()).isEqualTo(100);
    }
}
