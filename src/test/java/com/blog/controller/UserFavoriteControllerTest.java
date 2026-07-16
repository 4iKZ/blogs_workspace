package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserFavoriteControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("收藏文章 - 需要认证")
    void favoriteArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/user/favorite/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("取消收藏 - 需要认证")
    void unfavoriteArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/user/favorite/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("收藏列表 - 需要认证")
    void getUserFavorites_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/favorite/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("检查收藏状态 - 需要认证")
    void isArticleFavorited_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/favorite/1/check"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("收藏数量 - 需要认证")
    void getUserFavoriteCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/favorite/count"))
                .andExpect(status().isUnauthorized());
    }
}
