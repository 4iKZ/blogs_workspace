package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArticleSearchControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("搜索文章 - 未登录应允许访问")
    void searchArticles_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/search/article")
                .contentType("application/json")
                .content("{\"keyword\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("快速搜索 - 未登录应允许访问")
    void quickSearch_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/quick")
                .param("keyword", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取搜索建议 - 未登录应允许访问")
    void getSearchSuggestions_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/suggestions")
                .param("keyword", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取热门搜索关键词 - 未登录应允许访问")
    void getHotKeywords_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/hot-keywords"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取搜索统计信息 - 未登录应允许访问")
    void getSearchStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/statistics")
                .param("keyword", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("按分类搜索文章 - 未登录应允许访问")
    void searchByCategory_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/category/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("按标签搜索文章 - 未登录应允许访问")
    void searchByTag_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/tag/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("按作者搜索文章 - 未登录应允许访问")
    void searchByAuthor_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/author/1"))
                .andExpect(status().isOk());
    }
}
