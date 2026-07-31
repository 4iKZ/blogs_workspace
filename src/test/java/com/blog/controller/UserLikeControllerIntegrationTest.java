package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserLikeControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("点赞文章 - 未登录应返回 401")
    void likeArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/user/like/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("取消点赞文章 - 未登录应返回 401")
    void unlikeArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/user/like/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取点赞列表 - 未登录应返回 401")
    void getUserLikes_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/like/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("检查文章是否已点赞 - 未登录应返回 401")
    void isArticleLiked_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/like/1/check"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取点赞数量 - 未登录应返回 401")
    void getUserLikeCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/like/count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("点赞文章 - 登录后应放行到控制器")
    @WithMockUser
    void likeArticle_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/user/like/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取点赞列表 - 登录后应放行到控制器")
    @WithMockUser
    void getUserLikes_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/like/list")
                .param("page", "1")
                .param("size", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }
}
