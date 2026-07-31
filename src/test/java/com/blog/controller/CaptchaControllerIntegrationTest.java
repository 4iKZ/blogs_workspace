package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaptchaControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("生成验证码 - 未登录应允许访问")
    void generateCaptcha_shouldBePublic() throws Exception {
        mockMvc.perform(post("/api/captcha/generate"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取验证码图片 - 未登录应允许访问")
    void getCaptchaImage_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/captcha"))
                .andExpect(status().isOk());
    }
}
