package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class WebsiteStatisticsControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("网站总体统计 - 公开接口应可匿名访问")
    void getWebsiteStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("今日统计 - 公开接口应可匿名访问")
    void getTodayStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/today"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("本周统计 - 公开接口应可匿名访问")
    void getWeekStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/week"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("本月统计 - 公开接口应可匿名访问")
    void getMonthStatistics_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/month"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("热门页面 - 公开接口应可匿名访问")
    void getTopPages_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/top-pages"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("访问来源 - 公开接口应可匿名访问")
    void getTrafficSources_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/statistics/website/traffic-sources"))
                .andExpect(status().isOk());
    }
}
