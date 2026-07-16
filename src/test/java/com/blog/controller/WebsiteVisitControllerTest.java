package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class WebsiteVisitControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("记录页面访问 - 应可匿名访问")
    void recordVisit_shouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/statistics/website/visit")
                        .param("pageUrl", "/test-page")
                        .header("User-Agent", "Mozilla/5.0")
                        .header("X-Forwarded-For", "127.0.0.1"))
                .andExpect(status().isOk());
    }
}
