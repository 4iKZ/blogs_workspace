package com.blog.config;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityConfigTest extends AbstractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    @DisplayName("CORS 凭据模式仅允许配置的精确来源")
    void cors_shouldUseExactOriginsWithoutWildcard() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/info");
        var configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("http://localhost:5173")
                .doesNotContain("*");
    }

    @Test
    @DisplayName("CSP 限制内容来源，同时允许受信任的应用样式")
    void csp_shouldUseStrictXssPolicyWithTrustedApplicationStyles() throws Exception {
        mockMvc.perform(get("/api/article/list"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("default-src 'self'"),
                        org.hamcrest.Matchers.containsString("script-src 'self'"),
                        org.hamcrest.Matchers.containsString("object-src 'none'"),
                        org.hamcrest.Matchers.containsString("base-uri 'self'"),
                        org.hamcrest.Matchers.containsString("frame-ancestors 'none'"),
                        org.hamcrest.Matchers.containsString("style-src 'self' 'unsafe-inline' https://fonts.googleapis.com"),
                        org.hamcrest.Matchers.containsString("font-src 'self' https://fonts.gstatic.com"),
                        org.hamcrest.Matchers.containsString("img-src 'self' data: https://syhaox.tos-cn-beijing.volces.com"),
                        org.hamcrest.Matchers.containsString("connect-src 'self' https://syhaox.tos-cn-beijing.volces.com"),
                        org.hamcrest.Matchers.containsString("worker-src 'self' blob:"),
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("style-src *"))
                )));
    }

    @Test
    @DisplayName("公开端点 - 注册接口应允许匿名访问")
    void publicEndpoint_register_shouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"password123\",\"confirmPassword\":\"password123\",\"email\":\"test@test.com\",\"emailCode\":\"123456\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    @DisplayName("公开端点 - 登录接口应允许匿名访问")
    void publicEndpoint_login_shouldBeAccessible() throws Exception {
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"password123\"}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
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
    @DisplayName("公开端点 - 用户公开资料应允许匿名访问")
    void publicEndpoint_publicUserProfile_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/api/user/public/999999"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
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

    @Test
    @DisplayName("旧版无验证码重置密码接口不得匿名访问")
    void legacyPasswordReset_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/user/reset-password")
                        .param("email", "victim@example.com")
                        .param("newPassword", "newPassword123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("普通用户不得访问旧版用户管理接口")
    void legacyUserAdmin_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/user/admin/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("普通用户不得读取系统邮件配置")
    void systemConfig_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/system/config/email"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("普通用户不得访问系统备份")
    void systemBackup_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/system/backup/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("普通用户不得创建分类")
    void categoryCreate_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "user")
    @DisplayName("普通用户不得清理网站统计")
    void statisticsClean_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/statistics/website/clean"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("头像上传必须先登录")
    void avatarUpload_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(multipart("/api/user/avatar/upload"))
                .andExpect(status().isUnauthorized());
    }
}
