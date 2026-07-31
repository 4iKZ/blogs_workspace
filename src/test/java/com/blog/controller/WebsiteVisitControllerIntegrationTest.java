package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebsiteVisitControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("记录页面访问 - 未登录应返回 401")
    void recordPageVisit_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/statistics/website/visit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("记录页面访问 - 管理员登录后可访问")
    void recordPageVisit_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/statistics/website/visit"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取网站访问统计 - 未登录应允许访问")
    void getWebsiteVisitStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/statistics")
                .param("type", "day")
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取热门页面统计 - 未登录应允许访问")
    void getHotPageStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/hot-pages")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取访客来源统计 - 未登录应允许访问")
    void getVisitorSourceStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/visitor-sources")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }
}
