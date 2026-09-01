package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebsiteStatisticsControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("获取访问趋势数据 - 未登录应允许访问")
    void getVisitTrend_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/trend")
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取热门页面排行 - 未登录应允许访问")
    void getTopPages_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/top-pages")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取访问来源统计 - 未登录应允许访问")
    void getTrafficSources_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/traffic-sources"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("记录页面访问 - 未登录应允许访问")
    void recordPageView_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/statistics/website/record")
                .param("pageUrl", "/test")
                .header("User-Agent", "test")
                .header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("清理过期统计数据 - 未登录应返回 401")
    void cleanExpiredStatistics_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/statistics/website/clean")
                .param("daysToKeep", "90"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("清理过期统计数据 - 管理员登录后可访问")
    void cleanExpiredStatistics_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(delete("/api/statistics/website/clean")
                .param("daysToKeep", "90"))
                .andExpect(status().isOk());
    }
}
