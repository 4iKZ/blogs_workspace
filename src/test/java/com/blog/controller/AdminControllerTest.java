package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("管理员接口 - 未认证应返回 401")
    void adminEndpoint_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员接口 - 获取用户列表应返回 401")
    void getUserList_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员接口 - 获取文章列表应返回 401")
    void getArticleList_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/articles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员接口 - 获取评论列表应返回 401")
    void getCommentList_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员接口 - 获取统计信息应返回 401")
    void getStatistics_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员接口 - 删除用户应返回 401")
    void deleteUser_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员接口 - 数据备份应返回 401")
    void backupDatabase_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/admin/backup"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("管理员接口 - 清理缓存应返回 401")
    void clearCache_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/admin/cache/clear"))
                .andExpect(status().isUnauthorized());
    }
}
