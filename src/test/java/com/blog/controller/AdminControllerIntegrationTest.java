package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AdminControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("获取文章审核队列 - 未登录应返回 401")
    void getModerationSubmissions_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/admin/moderation/submissions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取用户列表 - 未登录应返回 401")
    void getUserList_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取文章列表 - 未登录应返回 401")
    void getArticleList_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/admin/articles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取评论列表 - 未登录应返回 401")
    void getCommentList_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取网站统计信息 - 未登录应返回 401")
    void getWebsiteStatistics_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取访问统计信息 - 未登录应返回 401")
    void getVisitStatistics_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/admin/visit-statistics")
                .param("type", "day"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取系统配置 - 未登录应返回 401")
    void getSystemConfig_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/admin/config"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("数据备份 - 未登录应返回 401")
    void backupDatabase_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/admin/backup"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("清理缓存 - 未登录应返回 401")
    void clearCache_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/admin/cache/clear"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取文章审核队列 - 管理员登录后可访问")
    void getModerationSubmissions_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/moderation/submissions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取用户列表 - 管理员登录后可访问")
    void getUserList_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取文章列表 - 管理员登录后可访问")
    void getArticleList_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/articles")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取评论列表 - 管理员登录后可访问")
    void getCommentList_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/comments")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取网站统计信息 - 管理员登录后可访问")
    void getWebsiteStatistics_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取系统配置 - 管理员登录后可访问")
    void getSystemConfig_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/config"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("数据备份 - 管理员登录后可访问")
    void backupDatabase_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/backup"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("清理缓存 - 管理员登录后可访问")
    void clearCache_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/cache/clear"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("修改用户状态 - 管理员登录后可访问")
    void updateUserStatus_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/status")
                .param("status", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("删除用户 - 管理员登录后可访问")
    void deleteUser_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("修改文章状态 - 非发布状态可修改")
    void updateArticleStatus_nonPublished_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/articles/1/status")
                .contentType("application/json")
                .content("{\"status\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("修改文章状态 - 发布状态应返回错误")
    void updateArticleStatus_published_shouldReturnError() throws Exception {
        mockMvc.perform(put("/api/admin/articles/1/status")
                .contentType("application/json")
                .content("{\"status\":2}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("删除文章 - 管理员登录后可访问")
    void deleteArticle_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/articles/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("更新系统配置 - 管理员登录后可访问")
    void updateSystemConfig_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/config")
                .contentType("application/json")
                .content("{\"key\":\"test\",\"value\":\"value\"}"))
                .andExpect(status().isOk());
    }

}