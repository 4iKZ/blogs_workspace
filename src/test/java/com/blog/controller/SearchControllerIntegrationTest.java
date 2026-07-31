package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("关键词搜索文章 - 未登录应允许访问")
    void searchByKeyword_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/legacy/keyword")
                .param("keyword", "test")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("按分类搜索文章 - 未登录应允许访问")
    void searchByCategory_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/legacy/category/1")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("按标签搜索文章 - 未登录应允许访问")
    void searchByTag_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/legacy/tag/1")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("按作者搜索文章 - 未登录应允许访问")
    void searchByAuthor_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/legacy/author/1")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("高级搜索 - 未登录应允许访问")
    void advancedSearch_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/legacy/advanced")
                .param("keyword", "test")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("搜索建议 - 未登录应允许访问")
    void getSearchSuggestions_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/legacy/suggestion")
                .param("keyword", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("热门搜索词 - 未登录应允许访问")
    void getHotKeywordsLegacy_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/search/legacy/hot-keywords")
                .param("source", "legacy"))
                .andExpect(status().isOk());
    }
}
