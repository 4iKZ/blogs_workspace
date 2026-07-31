package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArticleStatisticsControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("获取热门文章统计 - 未登录应允许访问")
    void getHotArticleStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/article/hot")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取置顶文章统计 - 未登录应允许访问")
    void getTopArticleStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/article/top")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取推荐文章统计 - 未登录应允许访问")
    void getRecommendedArticleStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/article/recommended")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取文章统计信息 - 未登录应允许访问")
    void getArticleStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/article/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("增加文章浏览量 - 未登录应允许访问")
    void incrementViewCount_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/statistics/article/view/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("增加文章点赞数 - 未登录应返回 401")
    void incrementLikeCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/statistics/article/like/1/increment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("减少文章点赞数 - 未登录应返回 401")
    void decrementLikeCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/statistics/article/like/1/decrement"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("增加文章评论数 - 未登录应返回 401")
    void incrementCommentCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/statistics/article/comment/1/increment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("减少文章评论数 - 未登录应返回 401")
    void decrementCommentCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/statistics/article/comment/1/decrement"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("增加文章收藏数 - 未登录应返回 401")
    void incrementFavoriteCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/statistics/article/favorite/1/increment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("减少文章收藏数 - 未登录应返回 401")
    void decrementFavoriteCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/statistics/article/favorite/1/decrement"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("增加文章点赞数 - 管理员登录后可访问")
    void incrementLikeCount_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/statistics/article/like/1/increment"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("减少文章点赞数 - 管理员登录后可访问")
    void decrementLikeCount_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/statistics/article/like/1/decrement"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("增加文章评论数 - 管理员登录后可访问")
    void incrementCommentCount_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/statistics/article/comment/1/increment"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("减少文章评论数 - 管理员登录后可访问")
    void decrementCommentCount_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/statistics/article/comment/1/decrement"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("增加文章收藏数 - 管理员登录后可访问")
    void incrementFavoriteCount_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/statistics/article/favorite/1/increment"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("减少文章收藏数 - 管理员登录后可访问")
    void decrementFavoriteCount_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/statistics/article/favorite/1/decrement"))
                .andExpect(status().isOk());
    }
}
