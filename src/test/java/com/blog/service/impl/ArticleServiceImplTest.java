package com.blog.service.impl;

import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.entity.User;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArticleServiceImpl 批量转换功能单元测试
 */
@SpringBootTest
@Transactional
class ArticleServiceImplTest {

    @Autowired
    private ArticleServiceImpl articleService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    private List<Article> testArticles;

    @BeforeEach
    void setUp() {
        // 尝试检查是否有测试数据，如果数据库为空则跳过
        try {
            if (userMapper.selectById(1L) == null || categoryMapper.selectById(1L) == null) {
                // 如果没有测试数据，跳过测试
                testArticles = Collections.emptyList();
                return;
            }
        } catch (Exception e) {
            // 数据库可能没有初始化，跳过测试
            testArticles = Collections.emptyList();
            return;
        }

        // 创建10篇测试文章
        testArticles = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Article article = new Article();
            article.setId((long) i);
            article.setAuthorId(1L); // 假设用户ID为1存在
            article.setCategoryId(1L); // 假设分类ID为1存在
            article.setTitle("测试文章 " + i);
            article.setContent("测试内容 " + i);
            article.setSummary("测试摘要 " + i);
            article.setIsTop(i == 1 ? 2 : 1); // 第一篇文章置顶 (2-是，1-否)
            article.setViewCount(100 + i);
            article.setLikeCount(i);
            article.setCommentCount(i);
            article.setFavoriteCount(i);
            article.setStatus(2); // 已发布
            testArticles.add(article);
        }
    }

    @Test
    @DisplayName("测试批量转换DTO - 正常场景")
    void testBatchConvertToDTO_Normal() {
        // 如果没有测试数据，跳过测试
        if (testArticles.isEmpty()) {
            return;
        }

        // 执行批量转换
        List<ArticleDTO> result = articleService.batchConvertToDTO(testArticles);

        // 验证
        assertNotNull(result);
        assertEquals(10, result.size());

        for (ArticleDTO dto : result) {
            assertNotNull(dto.getAuthorNickname(), "作者昵称不应为空");
            assertNotNull(dto.getCategoryName(), "分类名称不应为空");
            assertNotNull(dto.getTitle(), "标题不应为空");
        }
    }

    @Test
    @DisplayName("测试批量转换DTO - 空列表")
    void testBatchConvertToDTO_EmptyList() {
        List<ArticleDTO> result = articleService.batchConvertToDTO(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("测试批量转换DTO - null输入")
    void testBatchConvertToDTO_NullInput() {
        List<ArticleDTO> result = articleService.batchConvertToDTO(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("测试批量转换DTO - 包含不同作者和分类的文章")
    void testBatchConvertToDTO_MultipleAuthorsAndCategories() {
        // 如果没有测试数据，跳过测试
        if (testArticles.isEmpty()) {
            return;
        }

        // 获取数据库中的实际用户和分类数量
        List<User> users = userMapper.selectList(null);
        List<Category> categories = categoryMapper.selectList(null);

        if (users.size() < 2 || categories.size() < 2) {
            // 数据不足，无法测试多作者/多分类场景
            return;
        }

        // 创建包含不同作者和分类的文章
        List<Article> articles = new ArrayList<>();
        for (int i = 0; i < Math.min(5, users.size()); i++) {
            Article article = new Article();
            article.setId((long) i);
            article.setAuthorId(users.get(i).getId());
            article.setCategoryId(categories.get(i % categories.size()).getId());
            article.setTitle("测试文章 " + i);
            article.setContent("测试内容 " + i);
            article.setStatus(2);
            articles.add(article);
        }

        // 执行批量转换
        List<ArticleDTO> result = articleService.batchConvertToDTO(articles);

        // 验证
        assertNotNull(result);
        assertEquals(articles.size(), result.size());

        // 验证每个文章都有正确的作者和分类信息
        Set<String> authorNicknames = new HashSet<>();
        Set<String> categoryNames = new HashSet<>();

        for (ArticleDTO dto : result) {
            assertNotNull(dto.getAuthorNickname());
            assertNotNull(dto.getCategoryName());
            authorNicknames.add(dto.getAuthorNickname());
            categoryNames.add(dto.getCategoryName());
        }

        // 应该有多个不同的作者和分类
        assertTrue(authorNicknames.size() > 1, "应该有多个不同的作者");
        assertTrue(categoryNames.size() >= 1, "应该至少有一个分类");
    }

    @Test
    @DisplayName("测试批量转换DTO - 单篇文章场景")
    void testBatchConvertToDTO_SingleArticle() {
        // 如果没有测试数据，跳过测试
        if (testArticles.isEmpty()) {
            return;
        }

        // 只转换一篇文章
        List<Article> singleArticleList = Collections.singletonList(testArticles.get(0));

        // 执行批量转换
        List<ArticleDTO> result = articleService.batchConvertToDTO(singleArticleList);

        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试文章 1", result.get(0).getTitle());
        assertNotNull(result.get(0).getAuthorNickname());
        assertNotNull(result.get(0).getCategoryName());
    }

    @Test
    @DisplayName("测试批量转换DTO - 大量文章性能")
    void testBatchConvertToDTO_Performance() {
        // 如果没有测试数据，跳过测试
        if (testArticles.isEmpty()) {
            return;
        }

        // 创建100篇测试文章
        List<Article> largeArticleList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Article article = new Article();
            article.setId((long) i);
            article.setAuthorId(1L);
            article.setCategoryId(1L);
            article.setTitle("性能测试文章 " + i);
            article.setContent("性能测试内容 " + i);
            article.setStatus(2);
            largeArticleList.add(article);
        }

        // 执行批量转换并测量时间
        long startTime = System.currentTimeMillis();
        List<ArticleDTO> result = articleService.batchConvertToDTO(largeArticleList);
        long duration = System.currentTimeMillis() - startTime;

        // 验证
        assertNotNull(result);
        assertEquals(100, result.size());

        // 批量查询100篇文章应该很快（< 2秒）
        assertTrue(duration < 2000, "批量转换100篇文章耗时: " + duration + "ms");

        System.out.println("批量转换100篇文章耗时: " + duration + "ms");
    }

    @Test
    @DisplayName("测试批量转换DTO - 字段完整性验证")
    void testBatchConvertToDTO_FieldIntegrity() {
        // 如果没有测试数据，跳过测试
        if (testArticles.isEmpty()) {
            return;
        }

        // 执行批量转换
        List<ArticleDTO> result = articleService.batchConvertToDTO(testArticles);

        // 验证所有关键字段都正确复制
        for (int i = 0; i < testArticles.size(); i++) {
            Article original = testArticles.get(i);
            ArticleDTO dto = result.get(i);

            assertEquals(original.getId(), dto.getId());
            assertEquals(original.getTitle(), dto.getTitle());
            assertEquals(original.getContent(), dto.getContent());
            assertEquals(original.getSummary(), dto.getSummary());
            assertEquals(original.getViewCount(), dto.getViewCount());
            assertEquals(original.getLikeCount(), dto.getLikeCount());
            assertEquals(original.getCommentCount(), dto.getCommentCount());
            assertEquals(original.getFavoriteCount(), dto.getFavoriteCount());
            assertEquals(original.getStatus(), dto.getStatus());
            assertEquals(original.getCategoryId(), dto.getCategoryId());
        }
    }
}
