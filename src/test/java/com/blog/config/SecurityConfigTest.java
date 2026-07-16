package com.blog.config;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityConfigTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("公开端点 - 注册接口应允许匿名访问")
    void publicEndpoint_register_shouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"pass\",\"confirmPassword\":\"pass\",\"email\":\"test@test.com\",\"emailCode\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("公开端点 - 登录接口应允许匿名访问")
    void publicEndpoint_login_shouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"pass\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("公开端点 - 验证码接口应允许匿名访问")
    void publicEndpoint_captcha_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/captcha"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("公开端点 - 公开文章列表应允许匿名访问")
    void publicEndpoint_articleList_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/article/list"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("公开端点 - 分类列表应允许匿名访问")
    void publicEndpoint_categoryList_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/category/list"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("管理员端点 - 未认证应返回 401")
    void adminEndpoint_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("认证端点 - 用户信息接口需要认证")
    void authenticatedEndpoint_userInfo_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/user/info"))
                .andExpect(status().isUnauthorized());
    }
}
