package com.blog.service;

import com.blog.common.PageResult;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleCreateDTO;
import com.blog.dto.ArticleQueryDTO;
import com.blog.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文章服务测试类
 */
@SpringBootTest
@Transactional
public class ArticleServiceTest {

    @Autowired
    private ArticleService articleService;

    @Test
    public void testCreateArticle() {
        ArticleDTO articleDTO = new ArticleDTO();
        articleDTO.setTitle("测试文章标题");
        articleDTO.setContent("这是测试文章内容");
        articleDTO.setSummary("文章摘要");
        articleDTO.setCategoryId(1L);
        
        articleDTO.setAuthorId(1L);

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
        // 先创建文章
        ArticleDTO articleDTO = new ArticleDTO();
        articleDTO.setTitle("获取文章测试");
        articleDTO.setContent("获取文章内容");
        articleDTO.setSummary("获取文章摘要");
        articleDTO.setCategoryId(1L);
        
        articleDTO.setAuthorId(1L);

        ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
        articleCreateDTO.setTitle("获取文章测试");
        articleCreateDTO.setContent("获取文章内容");
        articleCreateDTO.setSummary("获取文章摘要");
        articleCreateDTO.setCategoryId(1L);

        Result<Long> createResult = articleService.publishArticle(articleCreateDTO, 1L);
        Long articleId = createResult.getData();

        // 测试获取文章
        Result<ArticleDTO> result = articleService.getArticleDetail(articleId);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("获取文章测试", result.getData().getTitle());
        assertEquals("获取文章内容", result.getData().getContent());
    }

    @Test
    public void testGetArticleByIdNotFound() {
        Result<ArticleDTO> result = articleService.getArticleDetail(99999L);
        
        assertFalse(result.isSuccess());
        assertEquals("文章不存在", result.getMessage());
    }

    @Test
    public void testUpdateArticle() {
        // 先创建文章
        ArticleDTO articleDTO = new ArticleDTO();
        articleDTO.setTitle("更新文章测试");
        articleDTO.setContent("原始文章内容");
        articleDTO.setSummary("原始文章摘要");
        articleDTO.setCategoryId(1L);
        
        articleDTO.setAuthorId(1L);

        ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
        articleCreateDTO.setTitle("更新文章测试");
        articleCreateDTO.setContent("原始文章内容");
        articleCreateDTO.setSummary("原始文章摘要");
        articleCreateDTO.setCategoryId(1L);

        Result<Long> createResult = articleService.publishArticle(articleCreateDTO, 1L);
        Long articleId = createResult.getData();

        // 更新文章
        ArticleCreateDTO updateDTO = new ArticleCreateDTO();
        updateDTO.setTitle("更新后的文章标题");
        updateDTO.setContent("更新后的文章内容");
        updateDTO.setSummary("更新后的文章摘要");
        updateDTO.setCategoryId(2L);

        Result<Void> result = articleService.editArticle(articleId, updateDTO, 1L);
        
        assertTrue(result.isSuccess());
    }

    @Test
    public void testDeleteArticle() {
        // 先创建文章
        ArticleDTO articleDTO = new ArticleDTO();
        articleDTO.setTitle("删除文章测试");
        articleDTO.setContent("删除文章内容");
        articleDTO.setSummary("删除文章摘要");
        articleDTO.setCategoryId(1L);
        
        articleDTO.setAuthorId(1L);

        ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
        articleCreateDTO.setTitle("删除文章测试");
        articleCreateDTO.setContent("删除文章内容");
        articleCreateDTO.setSummary("删除文章摘要");
        articleCreateDTO.setCategoryId(1L);

        Result<Long> createResult = articleService.publishArticle(articleCreateDTO, 1L);
        Long articleId = createResult.getData();

        // 删除文章
        Result<Void> result = articleService.deleteArticle(articleId, 1L);
        
        assertTrue(result.isSuccess());

        // 验证文章已被删除
        Result<ArticleDTO> getResult = articleService.getArticleDetail(articleId);
        assertFalse(getResult.isSuccess());
    }

    @Test
    public void testGetArticlesByPage() {
        // 创建多篇文章
        for (int i = 1; i <= 5; i++) {
            ArticleDTO articleDTO = new ArticleDTO();
            articleDTO.setTitle("分页测试文章" + i);
            articleDTO.setContent("分页测试内容" + i);
            articleDTO.setSummary("分页测试摘要" + i);
            articleDTO.setCategoryId(1L);

            articleDTO.setAuthorId(1L);
            ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
            articleCreateDTO.setTitle("分页测试文章" + i);
            articleCreateDTO.setContent("分页测试内容" + i);
            articleCreateDTO.setSummary("分页测试摘要" + i);
            articleCreateDTO.setCategoryId(1L);

            articleService.publishArticle(articleCreateDTO, 1L);
        }

        // 测试分页查询
        ArticleQueryDTO queryDTO = new ArticleQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setSize(3);
        queryDTO.setCategoryId(1L);

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(queryDTO.getPage(), queryDTO.getSize(), null, queryDTO.getCategoryId(), null, null, null, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().getItems().size());
    }

    @Test
    public void testGetArticlesByCategory() {
        // 创建不同分类的文章
        for (int i = 1; i <= 3; i++) {
            ArticleDTO articleDTO = new ArticleDTO();
            articleDTO.setTitle("分类测试文章" + i);
            articleDTO.setContent("分类测试内容" + i);
            articleDTO.setSummary("分类测试摘要" + i);
            articleDTO.setCategoryId(1L);

            articleDTO.setAuthorId(1L);
            ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
            articleCreateDTO.setTitle("分类测试文章" + i);
            articleCreateDTO.setContent("分类测试内容" + i);
            articleCreateDTO.setSummary("分类测试摘要" + i);
            articleCreateDTO.setCategoryId(1L);

            articleService.publishArticle(articleCreateDTO, 1L);
        }

        // 测试按分类查询
        ArticleQueryDTO queryDTO = new ArticleQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setSize(10);
        queryDTO.setCategoryId(1L);

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(queryDTO.getPage(), queryDTO.getSize(), null, queryDTO.getCategoryId(), null, null, null, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(3, result.getData().getItems().size());
    }



    @Test
    public void testPublishArticle() {
        // 先创建草稿文章
        ArticleDTO articleDTO = new ArticleDTO();
        articleDTO.setTitle("发布文章测试");
        articleDTO.setContent("发布文章内容");
        articleDTO.setSummary("发布文章摘要");
        articleDTO.setCategoryId(1L);
        
        articleDTO.setAuthorId(1L);
        articleDTO.setStatus(0); // 0 = draft

        ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
        articleCreateDTO.setTitle("发布文章测试");
        articleCreateDTO.setContent("发布文章内容");
        articleCreateDTO.setSummary("发布文章摘要");
        articleCreateDTO.setCategoryId(1L);
        articleCreateDTO.setStatus(0); // draft

        Result<Long> createResult = articleService.publishArticle(articleCreateDTO, 1L);
        Long articleId = createResult.getData();

        // 发布文章
        ArticleCreateDTO publishDTO = new ArticleCreateDTO();
        publishDTO.setTitle("发布文章测试");
        publishDTO.setContent("发布文章内容");
        publishDTO.setSummary("发布文章摘要");
        publishDTO.setCategoryId(1L);
        publishDTO.setStatus(1); // published

        Result<Void> result = articleService.editArticle(articleId, publishDTO, 1L);
        
        assertTrue(result.isSuccess());
        // 需要重新查询文章来验证状态
        Result<ArticleDTO> getResult = articleService.getArticleDetail(articleId);
        assertEquals(1, getResult.getData().getStatus()); // 1 = published
    }

    @Test
    public void testGetPublishedArticles() {
        // 创建已发布和草稿文章
        for (int i = 1; i <= 3; i++) {
            ArticleDTO articleDTO = new ArticleDTO();
            articleDTO.setTitle("发布状态测试文章" + i);
            articleDTO.setContent("发布状态测试内容" + i);
            articleDTO.setSummary("发布状态测试摘要" + i);
            articleDTO.setCategoryId(1L);

            articleDTO.setAuthorId(1L);
            articleDTO.setStatus(i % 2 == 0 ? 1 : 0); // 1 = published, 0 = draft
            ArticleCreateDTO articleCreateDTO = new ArticleCreateDTO();
            articleCreateDTO.setTitle("发布状态测试文章" + i);
            articleCreateDTO.setContent("发布状态测试内容" + i);
            articleCreateDTO.setSummary("发布状态测试摘要" + i);
            articleCreateDTO.setCategoryId(1L);
            articleCreateDTO.setStatus(i % 2 == 0 ? 1 : 0); // published or draft

            articleService.publishArticle(articleCreateDTO, 1L);
        }

        // 测试获取已发布文章
        ArticleQueryDTO queryDTO = new ArticleQueryDTO();
        queryDTO.setPage(1);
        queryDTO.setSize(10);
        queryDTO.setStatus("published"); // published status

        Result<PageResult<ArticleDTO>> result = articleService.getArticleList(queryDTO.getPage(), queryDTO.getSize(), null, null, null, 1, null, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().getItems().size()); // 只有1篇已发布文章
    }
}