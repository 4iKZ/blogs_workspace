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
class CaptchaControllerTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("生成验证码 - 应公开可访问")
    void generateCaptcha_shouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/captcha/generate"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取验证码图片 - 应公开可访问")
    void getCaptcha_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/captcha"))
                .andExpect(status().isOk());
    }
}
