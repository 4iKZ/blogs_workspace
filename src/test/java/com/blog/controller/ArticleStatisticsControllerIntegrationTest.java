package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 已删除的统计修改端点（like/comment/favorite 增减）应拒绝匿名访问：
 * 这些路径落入 /api/statistics/** 的 admin 规则，未登录应返回 401。
 */
class ArticleStatisticsControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("点赞数修改端点（已删除）- 未登录应返回 401")
    void incrementLikeCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/statistics/article/like/1/increment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("评论数修改端点（已删除）- 未登录应返回 401")
    void incrementCommentCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/statistics/article/comment/1/increment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("收藏数修改端点（已删除）- 未登录应返回 401")
    void incrementFavoriteCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/statistics/article/favorite/1/increment"))
                .andExpect(status().isUnauthorized());
    }
}
