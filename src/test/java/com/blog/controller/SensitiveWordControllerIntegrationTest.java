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

class SensitiveWordControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("获取敏感词列表 - 未登录应返回 401")
    void getWordList_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/admin/sensitive-words"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("新增敏感词 - 未登录应返回 401")
    void addWord_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words")
                .contentType("application/json")
                .content("{\"word\":\"test\",\"category\":\"default\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("编辑敏感词 - 未登录应返回 401")
    void updateWord_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/admin/sensitive-words/1")
                .contentType("application/json")
                .content("{\"word\":\"test\",\"category\":\"default\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("删除敏感词 - 未登录应返回 401")
    void deleteWord_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/admin/sensitive-words/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("批量删除敏感词 - 未登录应返回 401")
    void batchDeleteWords_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/admin/sensitive-words/batch")
                .contentType("application/json")
                .content("[1,2,3]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("批量导入敏感词 - 未登录应返回 401")
    void batchImport_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words/batch-import")
                .contentType("application/json")
                .content("{\"words\":[\"test\"],\"category\":\"default\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("重载敏感词缓存 - 未登录应返回 401")
    void reloadCache_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words/reload-cache"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("检测文本是否包含敏感词 - 未登录应返回 401")
    void checkContent_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words/check")
                .contentType("application/json")
                .content("{\"content\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取敏感词列表 - 管理员登录后可访问")
    void getWordList_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/sensitive-words")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("新增敏感词 - 管理员登录后可访问")
    void addWord_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words")
                .contentType("application/json")
                .content("{\"word\":\"test\",\"category\":\"default\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("编辑敏感词 - 管理员登录后可访问")
    void updateWord_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(put("/api/admin/sensitive-words/1")
                .contentType("application/json")
                .content("{\"word\":\"test\",\"category\":\"default\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("删除敏感词 - 管理员登录后可访问")
    void deleteWord_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/sensitive-words/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("批量删除敏感词 - 管理员登录后可访问")
    void batchDeleteWords_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(delete("/api/admin/sensitive-words/batch")
                .contentType("application/json")
                .content("[1,2,3]"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("批量导入敏感词 - 管理员登录后可访问")
    void batchImport_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words/batch-import")
                .contentType("application/json")
                .content("{\"words\":[\"test\"],\"category\":\"default\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("重载敏感词缓存 - 管理员登录后可访问")
    void reloadCache_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words/reload-cache"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("检测文本是否包含敏感词 - 管理员登录后可访问")
    void checkContent_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/sensitive-words/check")
                .contentType("application/json")
                .content("{\"content\":\"test\"}"))
                .andExpect(status().isOk());
    }
}
