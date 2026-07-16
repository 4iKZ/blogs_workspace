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
class UserLikeControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("点赞文章 - 需要认证")
    void likeArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/user/like/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("取消点赞 - 需要认证")
    void unlikeArticle_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/user/like/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("点赞列表 - 需要认证")
    void getUserLikes_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/like/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("检查点赞状态 - 需要认证")
    void isArticleLiked_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/like/1/check"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("点赞数量 - 需要认证")
    void getUserLikeCount_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/like/count"))
                .andExpect(status().isUnauthorized());
    }
}
