package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 回归测试：公开搜索接口不得返回草稿/未发布文章。
 * 修复前搜索条件为 `title LIKE ? OR content LIKE ? AND status=2`，
 * 标题命中关键词的草稿文章会绕过 status 过滤被泄露。
 */
class ArticleSearchStatusFilterTest extends AbstractControllerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("搜索 - 标题含关键词的草稿文章不得出现在公开搜索结果中")
    void search_shouldExcludeDraftArticleEvenIfTitleMatches() throws Exception {
        String token = String.valueOf(System.nanoTime());
        String keyword = "leakkw" + token;
        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='admin'", Long.class);
        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories WHERE name='技术分享'", Long.class);

        // 已发布文章：内容含关键词（应命中）
        Long publishedId = insertArticle("published-" + token, keyword + " published body", 2, adminId, categoryId);
        // 草稿文章：标题含关键词（修复前的泄露路径），内容不含（不应命中）
        Long draftId = insertArticle(keyword + " draft title", "draft body", 1, adminId, categoryId);

        String body = mockMvc.perform(get("/api/article/search")
                        .param("keyword", keyword)
                        .param("page", "1")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(body);
        assertThat(root.path("code").asInt()).isEqualTo(200);

        List<Long> resultIds = new ArrayList<>();
        for (JsonNode item : root.path("data").path("items")) {
            resultIds.add(item.path("id").asLong());
        }

        assertThat(resultIds).contains(publishedId);
        assertThat(resultIds).doesNotContain(draftId);
    }

    private Long insertArticle(String title, String content, int status, Long authorId, Long categoryId) {
        jdbcTemplate.update(
                "INSERT INTO articles (title, content, summary, category_id, author_id, status, view_count, " +
                        "like_count, comment_count, favorite_count, is_top, is_recommend, publish_time) " +
                        "VALUES (?,?,?,?,?,?,0,0,0,0,0,0,NOW())",
                title, content, "summary", categoryId, authorId, status);
        return jdbcTemplate.queryForObject("SELECT id FROM articles WHERE title = ?", Long.class, title);
    }
}
