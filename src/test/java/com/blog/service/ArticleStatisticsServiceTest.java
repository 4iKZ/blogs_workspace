package com.blog.service;

import com.blog.dto.ArticleStatisticsDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.service.impl.ArticleStatisticsServiceImpl;
import com.blog.utils.RedisCacheUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 文章统计服务测试类
 */
@ExtendWith(MockitoExtension.class)
class ArticleStatisticsServiceTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private UserLikeMapper userLikeMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedisCacheUtils redisCacheUtils;

    @Mock
    private com.blog.service.ArticleRankService articleRankService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private jakarta.servlet.http.HttpServletRequest request;

    @InjectMocks
    private ArticleStatisticsServiceImpl articleStatisticsService;

    private Article testArticle;
    private static final Long TEST_ARTICLE_ID = 1L;
    private static final Long NON_EXISTENT_ARTICLE_ID = 999L;

    @BeforeEach
    void setUp() {
        testArticle = new Article();
        testArticle.setId(TEST_ARTICLE_ID);
        testArticle.setTitle("测试文章");
        testArticle.setViewCount(100);
        testArticle.setLikeCount(50);
        testArticle.setCommentCount(20);
        testArticle.setFavoriteCount(10);
        testArticle.setStatus(2);

        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        lenient().when(redisTemplate.opsForSet()).thenReturn(mock(org.springframework.data.redis.core.SetOperations.class));
    }

    @Test
    void testGetArticleStatistics_Success() {
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);
        when(redisCacheUtils.getArticleRedisViewCount(TEST_ARTICLE_ID)).thenReturn(0);

        var result = articleStatisticsService.getArticleStatistics(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());

        ArticleStatisticsDTO statistics = result.getData();
        assertEquals(TEST_ARTICLE_ID, statistics.getArticleId());
        assertEquals(100, statistics.getViewCount());
        assertEquals(50, statistics.getLikeCount());
        assertEquals(20, statistics.getCommentCount());
        assertEquals(10, statistics.getFavoriteCount());

        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
    }

    @Test
    void testGetArticleStatistics_ArticleNotFound() {
        when(articleMapper.selectById(NON_EXISTENT_ARTICLE_ID)).thenReturn(null);

        var result = articleStatisticsService.getArticleStatistics(NON_EXISTENT_ARTICLE_ID);

        assertFalse(result.isSuccess());
        assertEquals("文章不存在", result.getMessage());

        verify(articleMapper, times(1)).selectById(NON_EXISTENT_ARTICLE_ID);
    }

    @Test
    void testIncrementViewCount_Success() {
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        var result = articleStatisticsService.incrementViewCount(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());

        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
    }

    @Test
    void testIncrementViewCount_ArticleNotFound() {
        when(articleMapper.selectById(NON_EXISTENT_ARTICLE_ID)).thenReturn(null);
        when(redisTemplate.opsForValue().setIfAbsent(anyString(), anyString(), anyLong(), any())).thenReturn(true);

        var result = articleStatisticsService.incrementViewCount(NON_EXISTENT_ARTICLE_ID);

        assertFalse(result.isSuccess());
        assertEquals("文章不存在", result.getMessage());
    }

    @Test
    void testIncrementLikeCount_Success() {
        when(articleMapper.updateLikeCount(TEST_ARTICLE_ID, 1)).thenReturn(1);

        var result = articleStatisticsService.incrementLikeCount(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());

        verify(articleMapper, times(1)).updateLikeCount(TEST_ARTICLE_ID, 1);
    }

    @Test
    void testIncrementLikeCount_ArticleNotFound() {
        when(articleMapper.updateLikeCount(NON_EXISTENT_ARTICLE_ID, 1)).thenReturn(0);

        var result = articleStatisticsService.incrementLikeCount(NON_EXISTENT_ARTICLE_ID);

        assertFalse(result.isSuccess());
        assertEquals("文章不存在", result.getMessage());
    }

    @Test
    void testDecrementLikeCount_Success() {
        when(articleMapper.decrementLikeCountSafely(TEST_ARTICLE_ID)).thenReturn(1);

        var result = articleStatisticsService.decrementLikeCount(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());

        verify(articleMapper, times(1)).decrementLikeCountSafely(TEST_ARTICLE_ID);
    }

    @Test
    void testDecrementLikeCount_ZeroLikes() {
        when(articleMapper.decrementLikeCountSafely(TEST_ARTICLE_ID)).thenReturn(0);

        var result = articleStatisticsService.decrementLikeCount(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());
    }

    @Test
    void testIncrementCommentCount_Success() {
        when(articleMapper.updateCommentCount(TEST_ARTICLE_ID, 1)).thenReturn(1);

        var result = articleStatisticsService.incrementCommentCount(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());

        verify(articleMapper, times(1)).updateCommentCount(TEST_ARTICLE_ID, 1);
    }

    @Test
    void testIncrementCommentCount_ArticleNotFound() {
        when(articleMapper.updateCommentCount(NON_EXISTENT_ARTICLE_ID, 1)).thenReturn(0);

        var result = articleStatisticsService.incrementCommentCount(NON_EXISTENT_ARTICLE_ID);

        assertFalse(result.isSuccess());
        assertEquals("文章不存在", result.getMessage());
    }

    @Test
    void testDecrementCommentCount_Success() {
        when(articleMapper.updateCommentCount(TEST_ARTICLE_ID, -1)).thenReturn(1);

        var result = articleStatisticsService.decrementCommentCount(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());

        verify(articleMapper, times(1)).updateCommentCount(TEST_ARTICLE_ID, -1);
    }

    @Test
    void testDecrementCommentCount_ZeroComments() {
        when(articleMapper.updateCommentCount(TEST_ARTICLE_ID, -1)).thenReturn(0);

        var result = articleStatisticsService.decrementCommentCount(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());
    }

    @Test
    void testIncrementFavoriteCount_Success() {
        when(articleMapper.updateFavoriteCount(TEST_ARTICLE_ID, 1)).thenReturn(1);

        var result = articleStatisticsService.incrementFavoriteCount(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());

        verify(articleMapper, times(1)).updateFavoriteCount(TEST_ARTICLE_ID, 1);
    }

    @Test
    void testIncrementFavoriteCount_ArticleNotFound() {
        when(articleMapper.updateFavoriteCount(NON_EXISTENT_ARTICLE_ID, 1)).thenReturn(0);

        var result = articleStatisticsService.incrementFavoriteCount(NON_EXISTENT_ARTICLE_ID);

        assertFalse(result.isSuccess());
        assertEquals("文章不存在", result.getMessage());
    }

    @Test
    void testDecrementFavoriteCount_Success() {
        when(articleMapper.decrementFavoriteCountSafely(TEST_ARTICLE_ID)).thenReturn(1);

        var result = articleStatisticsService.decrementFavoriteCount(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());

        verify(articleMapper, times(1)).decrementFavoriteCountSafely(TEST_ARTICLE_ID);
    }

    @Test
    void testDecrementFavoriteCount_ZeroFavorites() {
        when(articleMapper.decrementFavoriteCountSafely(TEST_ARTICLE_ID)).thenReturn(0);

        var result = articleStatisticsService.decrementFavoriteCount(TEST_ARTICLE_ID);

        assertTrue(result.isSuccess());
    }

    @Test
    void testGetHotArticleStatistics_Success() {
        when(articleMapper.selectHotArticles(10)).thenReturn(Arrays.asList(testArticle));
        when(redisCacheUtils.batchGetArticleRedisViewCount(anyList())).thenReturn(java.util.Collections.emptyMap());

        var result = articleStatisticsService.getHotArticleStatistics(10);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());

        verify(articleMapper, times(1)).selectHotArticles(10);
    }
}
