package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserFavoriteControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("收藏文章 - 未登录应返回 401")
    void favoriteArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/user/favorite/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("取消收藏文章 - 未登录应返回 401")
    void unfavoriteArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/user/favorite/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取收藏列表 - 未登录应返回 401")
    void getUserFavorites_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/favorite/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("检查文章是否已收藏 - 未登录应返回 401")
    void isArticleFavorited_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/favorite/1/check"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取收藏数量 - 未登录应返回 401")
    void getUserFavoriteCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/favorite/count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("收藏文章 - 登录后应放行到控制器")
    @WithMockUser
    void favoriteArticle_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/user/favorite/1")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取收藏列表 - 登录后应放行到控制器")
    @WithMockUser
    void getUserFavorites_shouldReachControllerWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/user/favorite/list")
                .param("page", "1")
                .param("size", "10")
                .requestAttr("userId", 1L))
                .andExpect(status().isOk());
    }
}
