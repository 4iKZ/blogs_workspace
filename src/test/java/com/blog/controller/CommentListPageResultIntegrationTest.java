package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文章评论列表分页契约集成测试（H2）：
 * 返回结构为 PageResult（items/total/page/size），且 total 只统计顶层已审核评论。
 * 使用测试内自建文章，避免受 data-h2.sql 种子评论影响；数据随测试事务回滚。
 */
class CommentListPageResultIntegrationTest extends AbstractControllerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String marker = "fd024-" + System.nanoTime();

    private Long seedArticleId() {
        Long categoryId = jdbcTemplate.queryForObject(
                "SELECT id FROM categories WHERE name = '技术分享'", Long.class);
        Long authorId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = 'admin'", Long.class);
        jdbcTemplate.update(
                "INSERT INTO articles (title, content, summary, category_id, author_id, status, view_count, "
                        + "like_count, comment_count, favorite_count, is_top, is_recommend, publish_time) "
                        + "VALUES (?, 'c', NULL, ?, ?, 2, 0, 0, 0, 0, 0, 0, NOW())",
                marker + "-article", categoryId, authorId);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM articles WHERE title = ?", Long.class, marker + "-article");
    }

    private Long adminId() {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);
    }

    private void insertComment(Long articleId, Long userId, Long parentId, int status, String content,
            Timestamp createTime) {
        jdbcTemplate.update(
                "INSERT INTO comments (article_id, user_id, parent_id, reply_to_comment_id, content, like_count, "
                        + "status, deleted, create_time, update_time) VALUES (?, ?, ?, ?, ?, 0, ?, 0, ?, ?)",
                articleId, userId, parentId, parentId == 0 ? null : parentId, content, status, createTime, createTime);
    }

    @Test
    @DisplayName("评论列表 - 返回分页结构且 total 仅统计顶层已审核评论")
    void commentList_shouldReturnPageResultWithTopLevelTotal() throws Exception {
        Long articleId = seedArticleId();
        Long userId = adminId();
        Timestamp base = new Timestamp(System.currentTimeMillis() - 600_000L);

        // 2 条顶层已审核 + 1 条顶层待审核（不计入 total）+ 2 条回复（挂在 top1 上，不计入 total）
        insertComment(articleId, userId, 0L, 2, marker + "-top-1", base);
        insertComment(articleId, userId, 0L, 2, marker + "-top-2", new Timestamp(base.getTime() + 10_000L));
        insertComment(articleId, userId, 0L, 1, marker + "-top-pending", new Timestamp(base.getTime() + 20_000L));
        Long top1Id = jdbcTemplate.queryForObject(
                "SELECT id FROM comments WHERE content = ?", Long.class, marker + "-top-1");
        insertComment(articleId, userId, top1Id, 2, marker + "-reply-1", new Timestamp(base.getTime() + 1_000L));
        insertComment(articleId, userId, top1Id, 2, marker + "-reply-2", new Timestamp(base.getTime() + 2_000L));

        mockMvc.perform(get("/api/comment/list")
                        .param("articleId", String.valueOf(articleId))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].children.length()").value(2));
    }

    @Test
    @DisplayName("评论列表 - 无评论文章应返回空分页结构")
    void commentList_emptyArticle_shouldReturnEmptyPageResult() throws Exception {
        Long articleId = seedArticleId();

        mockMvc.perform(get("/api/comment/list")
                        .param("articleId", String.valueOf(articleId))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }
}
