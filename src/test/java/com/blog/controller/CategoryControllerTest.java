package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CategoryControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("分类列表 - 公开接口应可匿名访问")
    void categoryList_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/category/list"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("分类详情 - 公开接口应可匿名访问")
    void categoryById_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/category/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("添加分类 - 管理员接口")
    void addCategory_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Category\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("编辑分类 - 管理员接口")
    void updateCategory_shouldBePublic() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/category/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Category\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("删除分类 - 管理员接口")
    void deleteCategory_shouldBePublic() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/category/99999"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("分类文章数量 - 公开接口应可匿名访问")
    void categoryArticleCount_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/category/1/count"))
                .andExpect(status().isOk());
    }
}
