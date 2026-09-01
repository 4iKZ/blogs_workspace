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

class CategoryControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("获取分类列表 - 未登录应允许访问")
    void getCategoryList_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/category/list"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取分类详情 - 未登录应允许访问")
    void getCategoryById_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/category/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取分类下的文章数量 - 未登录应允许访问")
    void getCategoryArticleCount_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/category/1/count"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("添加分类 - 未登录应返回 401")
    void addCategory_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/category")
                .contentType("application/json")
                .content("{\"name\":\"test\",\"description\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("编辑分类 - 未登录应返回 401")
    void updateCategory_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/category/1")
                .contentType("application/json")
                .content("{\"name\":\"test\",\"description\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("删除分类 - 未登录应返回 401")
    void deleteCategory_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/category/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("添加分类 - 管理员登录后可访问")
    void addCategory_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/category")
                .contentType("application/json")
                .content("{\"name\":\"test\",\"description\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("编辑分类 - 管理员登录后可访问")
    void updateCategory_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(put("/api/category/1")
                .contentType("application/json")
                .content("{\"name\":\"test\",\"description\":\"test\"}"))
                .andExpect(status().isOk());
    }
}
