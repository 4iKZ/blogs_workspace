package com.blog.service;

import com.blog.dto.ArticleStatisticsDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.service.impl.ArticleStatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private ArticleStatisticsServiceImpl articleStatisticsService;

    private Article testArticle;
    private static final Long TEST_ARTICLE_ID = 1L;
    private static final Long NON_EXISTENT_ARTICLE_ID = 999L;

    @BeforeEach
    void setUp() {
        // 初始化测试文章数据
        testArticle = new Article();
        testArticle.setId(TEST_ARTICLE_ID);
        testArticle.setTitle("测试文章");
        testArticle.setViewCount(100);
        testArticle.setLikeCount(50);
        testArticle.setCommentCount(20);
        testArticle.setFavoriteCount(10);
    }

    @Test
    void testGetArticleStatistics_Success() {
        // 准备测试数据
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);

        // 执行测试
        var result = articleStatisticsService.getArticleStatistics(TEST_ARTICLE_ID);

        // 验证结果
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        
        ArticleStatisticsDTO statistics = result.getData();
        assertEquals(TEST_ARTICLE_ID, statistics.getArticleId());
        assertEquals(100, statistics.getViewCount());
        assertEquals(50, statistics.getLikeCount());
        assertEquals(20, statistics.getCommentCount());
        assertEquals(10, statistics.getFavoriteCount());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
    }

    @Test
    void testGetArticleStatistics_ArticleNotFound() {
        // 准备测试数据
        when(articleMapper.selectById(NON_EXISTENT_ARTICLE_ID)).thenReturn(null);

        // 执行测试
        var result = articleStatisticsService.getArticleStatistics(NON_EXISTENT_ARTICLE_ID);

        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("文章不存在", result.getMessage());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(NON_EXISTENT_ARTICLE_ID);
    }

    @Test
    void testIncrementViewCount_Success() {
        // 准备测试数据
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);
        when(articleMapper.incrementViewCount(TEST_ARTICLE_ID)).thenReturn(1);

        // 执行测试
        var result = articleStatisticsService.incrementViewCount(TEST_ARTICLE_ID);

        // 验证结果
        assertTrue(result.isSuccess());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
        verify(articleMapper, times(1)).incrementViewCount(TEST_ARTICLE_ID);
    }

    @Test
    void testIncrementViewCount_ArticleNotFound() {
        // 准备测试数据
        when(articleMapper.selectById(NON_EXISTENT_ARTICLE_ID)).thenReturn(null);

        // 执行测试
        var result = articleStatisticsService.incrementViewCount(NON_EXISTENT_ARTICLE_ID);

        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("文章不存在", result.getMessage());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(NON_EXISTENT_ARTICLE_ID);
        verify(articleMapper, never()).incrementViewCount(anyLong());
    }

    @Test
    void testIncrementLikeCount_Success() {
        // 准备测试数据
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);
        when(articleMapper.updateLikeCount(TEST_ARTICLE_ID, 1)).thenReturn(1);

        // 执行测试
        var result = articleStatisticsService.incrementLikeCount(TEST_ARTICLE_ID);

        // 验证结果
        assertTrue(result.isSuccess());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
        verify(articleMapper, times(1)).updateLikeCount(TEST_ARTICLE_ID, 1);
    }

    @Test
    void testDecrementLikeCount_Success() {
        // 准备测试数据
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);
        when(articleMapper.updateLikeCount(TEST_ARTICLE_ID, -1)).thenReturn(1);

        // 执行测试
        var result = articleStatisticsService.decrementLikeCount(TEST_ARTICLE_ID);

        // 验证结果
        assertTrue(result.isSuccess());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
        verify(articleMapper, times(1)).updateLikeCount(TEST_ARTICLE_ID, -1);
    }

    @Test
    void testDecrementLikeCount_ZeroLikes() {
        // 准备测试数据 - 点赞数为0
        testArticle.setLikeCount(0);
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);

        // 执行测试
        var result = articleStatisticsService.decrementLikeCount(TEST_ARTICLE_ID);

        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("文章点赞数已为0，无法继续减少", result.getMessage());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
        verify(articleMapper, never()).updateLikeCount(anyLong(), anyInt());
    }

    @Test
    void testIncrementCommentCount_Success() {
        // 准备测试数据
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);
        when(articleMapper.updateCommentCount(TEST_ARTICLE_ID, 1)).thenReturn(1);

        // 执行测试
        var result = articleStatisticsService.incrementCommentCount(TEST_ARTICLE_ID);

        // 验证结果
        assertTrue(result.isSuccess());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
        verify(articleMapper, times(1)).updateCommentCount(TEST_ARTICLE_ID, 1);
    }

    @Test
    void testDecrementCommentCount_Success() {
        // 准备测试数据
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);
        when(articleMapper.updateCommentCount(TEST_ARTICLE_ID, -1)).thenReturn(1);

        // 执行测试
        var result = articleStatisticsService.decrementCommentCount(TEST_ARTICLE_ID);

        // 验证结果
        assertTrue(result.isSuccess());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
        verify(articleMapper, times(1)).updateCommentCount(TEST_ARTICLE_ID, -1);
    }

    @Test
    void testDecrementCommentCount_ZeroComments() {
        // 准备测试数据 - 评论数为0
        testArticle.setCommentCount(0);
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);

        // 执行测试
        var result = articleStatisticsService.decrementCommentCount(TEST_ARTICLE_ID);

        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("文章评论数已为0，无法继续减少", result.getMessage());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
        verify(articleMapper, never()).updateCommentCount(anyLong(), anyInt());
    }

    @Test
    void testIncrementFavoriteCount_Success() {
        // 准备测试数据
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);
        when(articleMapper.updateFavoriteCount(TEST_ARTICLE_ID, 1)).thenReturn(1);

        // 执行测试
        var result = articleStatisticsService.incrementFavoriteCount(TEST_ARTICLE_ID);

        // 验证结果
        assertTrue(result.isSuccess());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
        verify(articleMapper, times(1)).updateFavoriteCount(TEST_ARTICLE_ID, 1);
    }

    @Test
    void testDecrementFavoriteCount_Success() {
        // 准备测试数据
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);
        when(articleMapper.updateFavoriteCount(TEST_ARTICLE_ID, -1)).thenReturn(1);

        // 执行测试
        var result = articleStatisticsService.decrementFavoriteCount(TEST_ARTICLE_ID);

        // 验证结果
        assertTrue(result.isSuccess());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
        verify(articleMapper, times(1)).updateFavoriteCount(TEST_ARTICLE_ID, -1);
    }

    @Test
    void testDecrementFavoriteCount_ZeroFavorites() {
        // 准备测试数据 - 收藏数为0
        testArticle.setFavoriteCount(0);
        when(articleMapper.selectById(TEST_ARTICLE_ID)).thenReturn(testArticle);

        // 执行测试
        var result = articleStatisticsService.decrementFavoriteCount(TEST_ARTICLE_ID);

        // 验证结果
        assertFalse(result.isSuccess());
        assertEquals("文章收藏数已为0，无法继续减少", result.getMessage());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectById(TEST_ARTICLE_ID);
        verify(articleMapper, never()).updateFavoriteCount(anyLong(), anyInt());
    }

    @Test
    void testGetHotArticleStatistics_Success() {
        // 准备测试数据
        Article article1 = new Article();
        article1.setId(1L);
        article1.setViewCount(100);
        article1.setLikeCount(50);
        article1.setCommentCount(20);
        article1.setFavoriteCount(10);

        Article article2 = new Article();
        article2.setId(2L);
        article2.setViewCount(80);
        article2.setLikeCount(40);
        article2.setCommentCount(15);
        article2.setFavoriteCount(8);

        List<Article> hotArticles = Arrays.asList(article1, article2);
        when(articleMapper.selectHotArticles(10)).thenReturn(hotArticles);

        // 执行测试
        var result = articleStatisticsService.getHotArticleStatistics(10);

        // 验证结果
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        
        // 验证第一篇文章统计
        ArticleStatisticsDTO statistics1 = result.getData().get(0);
        assertEquals(1L, statistics1.getArticleId());
        assertEquals(100, statistics1.getViewCount());
        assertEquals(50, statistics1.getLikeCount());
        assertEquals(20, statistics1.getCommentCount());
        assertEquals(10, statistics1.getFavoriteCount());
        
        // 验证第二篇文章统计
        ArticleStatisticsDTO statistics2 = result.getData().get(1);
        assertEquals(2L, statistics2.getArticleId());
        assertEquals(80, statistics2.getViewCount());
        assertEquals(40, statistics2.getLikeCount());
        assertEquals(15, statistics2.getCommentCount());
        assertEquals(8, statistics2.getFavoriteCount());
        
        // 验证方法调用
        verify(articleMapper, times(1)).selectHotArticles(10);
    }
}