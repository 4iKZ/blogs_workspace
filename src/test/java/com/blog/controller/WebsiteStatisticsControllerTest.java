package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.WebsiteStatisticsDTO;
import com.blog.service.WebsiteStatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebsiteStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebsiteStatisticsService websiteStatisticsService;

    @Test
    @DisplayName("网站总体统计 - 公开接口应可匿名访问")
    void getWebsiteStatistics_shouldBePublic() throws Exception {
        when(websiteStatisticsService.getWebsiteStatistics()).thenReturn(Result.success(new WebsiteStatisticsDTO()));
        mockMvc.perform(get("/api/statistics/website/overview"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("今日统计 - 公开接口应可匿名访问")
    void getTodayStatistics_shouldBePublic() throws Exception {
        when(websiteStatisticsService.getTodayStatistics()).thenReturn(Result.success(new WebsiteStatisticsDTO()));
        mockMvc.perform(get("/api/statistics/website/today"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("本周统计 - 公开接口应可匿名访问")
    void getWeekStatistics_shouldBePublic() throws Exception {
        when(websiteStatisticsService.getWeekStatistics()).thenReturn(Result.success(new WebsiteStatisticsDTO()));
        mockMvc.perform(get("/api/statistics/website/week"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("本月统计 - 公开接口应可匿名访问")
    void getMonthStatistics_shouldBePublic() throws Exception {
        when(websiteStatisticsService.getMonthStatistics()).thenReturn(Result.success(new WebsiteStatisticsDTO()));
        mockMvc.perform(get("/api/statistics/website/month"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("热门页面 - 公开接口应可匿名访问")
    void getTopPages_shouldBePublic() throws Exception {
        when(websiteStatisticsService.getTopPages(1, 10)).thenReturn(Result.success(new com.blog.dto.PageDTO<>()));
        mockMvc.perform(get("/api/statistics/website/top-pages"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("访问来源 - 公开接口应可匿名访问")
    void getTrafficSources_shouldBePublic() throws Exception {
        when(websiteStatisticsService.getTrafficSources()).thenReturn(Result.success(java.util.Collections.emptyList()));
        mockMvc.perform(get("/api/statistics/website/traffic-sources"))
                .andExpect(status().isOk());
    }
}
