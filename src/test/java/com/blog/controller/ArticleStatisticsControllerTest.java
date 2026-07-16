package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ArticleStatisticsControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("热门文章统计 - 公开接口应可匿名访问")
    void getHotArticleStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/article/hot"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("置顶文章统计 - 公开接口应可匿名访问")
    void getTopArticleStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/article/top"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("推荐文章统计 - 公开接口应可匿名访问")
    void getRecommendedArticleStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/article/recommended"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("文章统计信息 - 公开接口应可匿名访问")
    void getArticleStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/article/1"))
                .andExpect(status().isOk());
    }
}
