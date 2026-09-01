package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.ArticleCreateDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * 回归测试：@Transactional 方法内 catch 吞异常必须标记事务回滚，
 * 否则审核任务失败时草稿文章仍会被提交落库。
 */
@SpringBootTest
class ArticlePublishRollbackTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private ArticleModerationSubmissionService moderationSubmissionService;

    @Test
    @DisplayName("发布文章 - 审核提交失败时文章不得落库（事务回滚）")
    void publishArticle_moderationFailure_shouldRollbackInsert() {
        Long authorId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='admin'", Long.class);
        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories WHERE name='技术分享'", Long.class);
        String title = "rollback-test-" + System.nanoTime();

        doThrow(new RuntimeException("mock submitNew failure"))
                .when(moderationSubmissionService).submitNew(any());

        ArticleCreateDTO dto = new ArticleCreateDTO();
        dto.setTitle(title);
        dto.setContent("rollback test content");
        dto.setSummary("rollback test summary");
        dto.setCategoryId(categoryId);

        Result<Long> result = articleService.publishArticle(dto, authorId);

        assertThat(result.isSuccess()).isFalse();
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM articles WHERE title = ?", Integer.class, title);
        assertThat(count).isZero();
    }
}
