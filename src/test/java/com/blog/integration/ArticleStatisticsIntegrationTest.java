package com.blog.integration;

import com.blog.BlogBackendApplication;
import com.blog.dto.ArticleStatisticsDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleStatisticsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文章统计功能集成测试
 */
@SpringBootTest(classes = BlogBackendApplication.class)
@ActiveProfiles("test")
@Transactional
class ArticleStatisticsIntegrationTest {

    @Autowired
    private ArticleStatisticsService articleStatisticsService;
    
    @Autowired
    private ArticleMapper articleMapper;

    private Article testArticle;
    private static final String TEST_TITLE = "集成测试文章";
    private static final String TEST_CONTENT = "这是用于集成测试的文章内容";

    @BeforeEach
    void setUp() {
        // 创建测试文章
        testArticle = new Article();
        testArticle.setTitle(TEST_TITLE);
        testArticle.setContent(TEST_CONTENT);
        testArticle.setSummary("测试摘要");
        testArticle.setViewCount(0);
        testArticle.setLikeCount(0);
        testArticle.setCommentCount(0);
        testArticle.setFavoriteCount(0);
        
        // 保存到数据库
        articleMapper.insert(testArticle);
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        if (testArticle != null && testArticle.getId() != null) {
            articleMapper.deleteById(testArticle.getId());
        }
    }

    @Test
    void testGetArticleStatistics_Integration() {
        // 执行测试
        var result = articleStatisticsService.getArticleStatistics(testArticle.getId());

        // 验证结果
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        
        ArticleStatisticsDTO statistics = result.getData();
        assertEquals(testArticle.getId(), statistics.getArticleId());
        assertEquals(0, statistics.getViewCount());
        assertEquals(0, statistics.getLikeCount());
        assertEquals(0, statistics.getCommentCount());
        assertEquals(0, statistics.getFavoriteCount());
    }

    @Test
    void testIncrementViewCount_Integration() {
        // 执行测试 - 增加浏览量
        var result = articleStatisticsService.incrementViewCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证浏览量是否增加
        var statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(1, statisticsResult.getData().getViewCount());

        // 再次增加浏览量
        result = articleStatisticsService.incrementViewCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证浏览量是否再次增加
        statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(2, statisticsResult.getData().getViewCount());
    }

    @Test
    void testIncrementLikeCount_Integration() {
        // 执行测试 - 增加点赞数
        var result = articleStatisticsService.incrementLikeCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证点赞数是否增加
        var statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(1, statisticsResult.getData().getLikeCount());

        // 再次增加点赞数
        result = articleStatisticsService.incrementLikeCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证点赞数是否再次增加
        statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(2, statisticsResult.getData().getLikeCount());
    }

    @Test
    void testDecrementLikeCount_Integration() {
        // 先增加点赞数
        articleStatisticsService.incrementLikeCount(testArticle.getId());
        articleStatisticsService.incrementLikeCount(testArticle.getId());

        // 执行测试 - 减少点赞数
        var result = articleStatisticsService.decrementLikeCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证点赞数是否减少
        var statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(1, statisticsResult.getData().getLikeCount());

        // 再次减少点赞数
        result = articleStatisticsService.decrementLikeCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证点赞数是否再次减少
        statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(0, statisticsResult.getData().getLikeCount());

        // 尝试再次减少点赞数（应该失败）
        result = articleStatisticsService.decrementLikeCount(testArticle.getId());
        assertFalse(result.isSuccess());
        assertEquals("文章点赞数已为0，无法继续减少", result.getMessage());
    }

    @Test
    void testIncrementCommentCount_Integration() {
        // 执行测试 - 增加评论数
        var result = articleStatisticsService.incrementCommentCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证评论数是否增加
        var statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(1, statisticsResult.getData().getCommentCount());

        // 再次增加评论数
        result = articleStatisticsService.incrementCommentCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证评论数是否再次增加
        statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(2, statisticsResult.getData().getCommentCount());
    }

    @Test
    void testDecrementCommentCount_Integration() {
        // 先增加评论数
        articleStatisticsService.incrementCommentCount(testArticle.getId());
        articleStatisticsService.incrementCommentCount(testArticle.getId());

        // 执行测试 - 减少评论数
        var result = articleStatisticsService.decrementCommentCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证评论数是否减少
        var statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(1, statisticsResult.getData().getCommentCount());

        // 再次减少评论数
        result = articleStatisticsService.decrementCommentCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证评论数是否再次减少
        statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(0, statisticsResult.getData().getCommentCount());

        // 尝试再次减少评论数（应该失败）
        result = articleStatisticsService.decrementCommentCount(testArticle.getId());
        assertFalse(result.isSuccess());
        assertEquals("文章评论数已为0，无法继续减少", result.getMessage());
    }

    @Test
    void testIncrementFavoriteCount_Integration() {
        // 执行测试 - 增加收藏数
        var result = articleStatisticsService.incrementFavoriteCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证收藏数是否增加
        var statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(1, statisticsResult.getData().getFavoriteCount());

        // 再次增加收藏数
        result = articleStatisticsService.incrementFavoriteCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证收藏数是否再次增加
        statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(2, statisticsResult.getData().getFavoriteCount());
    }

    @Test
    void testDecrementFavoriteCount_Integration() {
        // 先增加收藏数
        articleStatisticsService.incrementFavoriteCount(testArticle.getId());
        articleStatisticsService.incrementFavoriteCount(testArticle.getId());

        // 执行测试 - 减少收藏数
        var result = articleStatisticsService.decrementFavoriteCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证收藏数是否减少
        var statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(1, statisticsResult.getData().getFavoriteCount());

        // 再次减少收藏数
        result = articleStatisticsService.decrementFavoriteCount(testArticle.getId());
        assertTrue(result.isSuccess());

        // 验证收藏数是否再次减少
        statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        assertEquals(0, statisticsResult.getData().getFavoriteCount());

        // 尝试再次减少收藏数（应该失败）
        result = articleStatisticsService.decrementFavoriteCount(testArticle.getId());
        assertFalse(result.isSuccess());
        assertEquals("文章收藏数已为0，无法继续减少", result.getMessage());
    }

    @Test
    void testMultipleStatisticsOperations_Integration() {
        // 执行多个统计操作
        articleStatisticsService.incrementViewCount(testArticle.getId());
        articleStatisticsService.incrementViewCount(testArticle.getId());
        articleStatisticsService.incrementViewCount(testArticle.getId());
        
        articleStatisticsService.incrementLikeCount(testArticle.getId());
        articleStatisticsService.incrementLikeCount(testArticle.getId());
        
        articleStatisticsService.incrementCommentCount(testArticle.getId());
        
        articleStatisticsService.incrementFavoriteCount(testArticle.getId());
        articleStatisticsService.incrementFavoriteCount(testArticle.getId());
        articleStatisticsService.incrementFavoriteCount(testArticle.getId());

        // 验证所有统计数据
        var statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        
        ArticleStatisticsDTO statistics = statisticsResult.getData();
        assertEquals(3, statistics.getViewCount());
        assertEquals(2, statistics.getLikeCount());
        assertEquals(1, statistics.getCommentCount());
        assertEquals(3, statistics.getFavoriteCount());

        // 执行减少操作
        articleStatisticsService.decrementLikeCount(testArticle.getId());
        articleStatisticsService.decrementCommentCount(testArticle.getId());
        articleStatisticsService.decrementFavoriteCount(testArticle.getId());
        articleStatisticsService.decrementFavoriteCount(testArticle.getId());

        // 验证减少后的统计数据
        statisticsResult = articleStatisticsService.getArticleStatistics(testArticle.getId());
        assertTrue(statisticsResult.isSuccess());
        
        statistics = statisticsResult.getData();
        assertEquals(3, statistics.getViewCount()); // 浏览量不变
        assertEquals(1, statistics.getLikeCount()); // 点赞数减1
        assertEquals(0, statistics.getCommentCount()); // 评论数减1
        assertEquals(1, statistics.getFavoriteCount()); // 收藏数减2
    }

    @Test
    void testGetHotArticleStatistics_Integration() {
        // 创建多篇文章，设置不同的统计数据
        for (int i = 0; i < 5; i++) {
            Article article = new Article();
            article.setTitle("热门文章" + i);
            article.setContent("内容" + i);
            article.setSummary("摘要" + i);
            article.setViewCount(100 - i * 10);
            article.setLikeCount(50 - i * 5);
            article.setCommentCount(20 - i * 2);
            article.setFavoriteCount(10 - i);
            articleMapper.insert(article);
            
            // 增加浏览量，使文章变"热"
            articleStatisticsService.incrementViewCount(article.getId());
        }

        // 执行测试 - 获取热门文章统计
        var result = articleStatisticsService.getHotArticleStatistics(5);
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        
        List<ArticleStatisticsDTO> hotArticles = result.getData();
        // 至少应该有我们创建的5篇文章
        assertTrue(hotArticles.size() >= 5);
    }
}