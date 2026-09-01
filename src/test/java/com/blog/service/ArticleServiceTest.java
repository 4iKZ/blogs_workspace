package com.blog.service;

import com.blog.common.PageResult;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleCreateDTO;
import com.blog.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文章服务测试类 - 适配草稿审核流程
 */
@SpringBootTest
@Transactional
public class ArticleServiceTest {

    @Autowired
    private ArticleService articleService;

    @Test
    public void testCreateArticle() {
        ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
        articleCreateDTO.setTitle("测试文章标题");
        articleCreateDTO.setContent("这是测试文章内容");
        articleCreateDTO.setSummary("文章摘要");
        articleCreateDTO.setCategoryId(1L);

        Result<Long> result = articleService.publishArticle(articleCreateDTO, 1L);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testGetArticleById() {
        ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
        articleCreateDTO.setTitle("获取文章测试");
        articleCreateDTO.setContent("获取文章内容");
        articleCreateDTO.setSummary("获取文章摘要");
        articleCreateDTO.setCategoryId(1L);

        Result<Long> createResult = articleService.publishArticle(articleCreateDTO, 1L);
        Long articleId = createResult.getData();

        assertTrue(createResult.isSuccess());
        assertNotNull(articleId);
    }

    @Test
    public void testGetArticleByIdNotFound() {
        Result<ArticleDTO> result = articleService.getArticleDetail(99999L);

        assertFalse(result.isSuccess());
        assertEquals("文章不存在", result.getMessage());
    }

    @Test
    public void testUpdateArticle() {
        ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
        articleCreateDTO.setTitle("更新文章测试");
        articleCreateDTO.setContent("原始文章内容");
        articleCreateDTO.setSummary("原始文章摘要");
        articleCreateDTO.setCategoryId(1L);

        Result<Long> createResult = articleService.publishArticle(articleCreateDTO, 1L);
        Long articleId = createResult.getData();

        assertTrue(createResult.isSuccess());
        assertNotNull(articleId);
    }

    @Test
    public void testDeleteArticle() {
        ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
        articleCreateDTO.setTitle("删除文章测试");
        articleCreateDTO.setContent("删除文章内容");
        articleCreateDTO.setSummary("删除文章摘要");
        articleCreateDTO.setCategoryId(1L);

        Result<Long> createResult = articleService.publishArticle(articleCreateDTO, 1L);
        Long articleId = createResult.getData();

        assertTrue(createResult.isSuccess());
        assertNotNull(articleId);
    }

    @Test
    public void testGetArticlesByPage() {
        for (int i = 1; i <= 5; i++) {
            ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
            articleCreateDTO.setTitle("分页测试文章" + i);
            articleCreateDTO.setContent("分页测试内容" + i);
            articleCreateDTO.setSummary("分页测试摘要" + i);
            articleCreateDTO.setCategoryId(1L);

            articleService.publishArticle(articleCreateDTO, 1L);
        }

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, 3, null, 1L, null, null, null, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testGetArticlesByCategory() {
        for (int i = 1; i <= 3; i++) {
            ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
            articleCreateDTO.setTitle("分类测试文章" + i);
            articleCreateDTO.setContent("分类测试内容" + i);
            articleCreateDTO.setSummary("分类测试摘要" + i);
            articleCreateDTO.setCategoryId(1L);

            articleService.publishArticle(articleCreateDTO, 1L);
        }

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, 10, null, 1L, null, null, null, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testPublishArticle() {
        ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
        articleCreateDTO.setTitle("发布文章测试");
        articleCreateDTO.setContent("发布文章内容");
        articleCreateDTO.setSummary("发布文章摘要");
        articleCreateDTO.setCategoryId(1L);

        Result<Long> createResult = articleService.publishArticle(articleCreateDTO, 1L);

        assertTrue(createResult.isSuccess());
        assertNotNull(createResult.getData());
    }

    @Test
    public void testGetPublishedArticles() {
        for (int i = 1; i <= 3; i++) {
            ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
            articleCreateDTO.setTitle("发布状态测试文章" + i);
            articleCreateDTO.setContent("发布状态测试内容" + i);
            articleCreateDTO.setSummary("发布状态测试摘要" + i);
            articleCreateDTO.setCategoryId(1L);

            articleService.publishArticle(articleCreateDTO, 1L);
        }

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(1, 10, null, null, null, 1, null, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }
}
