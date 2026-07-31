package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemConfigControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("获取网站配置 - 未登录应允许访问")
    void getWebsiteConfig_shouldBePublic() throws Exception {
        mockMvc.perform(get("/api/system/config/website"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("获取系统配置 - 未登录应返回 401")
    void getSystemConfig_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/system/config/test-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取所有系统配置 - 未登录应返回 401")
    void getAllSystemConfigs_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/system/config/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("根据配置类型获取配置 - 未登录应返回 401")
    void getSystemConfigsByType_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/system/config/type/site"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("更新系统配置 - 未登录应返回 401")
    void updateSystemConfig_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/system/config")
                .contentType("application/json")
                .content("{\"configKey\":\"test\",\"configValue\":\"value\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("批量更新系统配置 - 未登录应返回 401")
    void batchUpdateSystemConfigs_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/system/config/batch")
                .contentType("application/json")
                .content("[{\"configKey\":\"test\",\"configValue\":\"value\"}]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("更新网站配置 - 未登录应返回 401")
    void updateWebsiteConfig_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/system/config/website")
                .contentType("application/json")
                .content("{\"siteName\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取邮件配置 - 未登录应返回 401")
    void getEmailConfig_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/system/config/email"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("更新邮件配置 - 未登录应返回 401")
    void updateEmailConfig_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/system/config/email")
                .contentType("application/json")
                .content("{\"smtpHost\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取文件上传配置 - 未登录应返回 401")
    void getFileUploadConfig_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/system/config/file-upload"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("更新文件上传配置 - 未登录应返回 401")
    void updateFileUploadConfig_shouldRequireAuth() throws Exception {
        mockMvc.perform(put("/api/system/config/file-upload")
                .contentType("application/json")
                .content("{\"maxSize\":10485760}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取系统配置 - 管理员登录后可访问")
    void getSystemConfig_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/system/config/test-key"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取所有系统配置 - 管理员登录后可访问")
    void getAllSystemConfigs_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/system/config/all"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("根据配置类型获取配置 - 管理员登录后可访问")
    void getSystemConfigsByType_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/system/config/type/site"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("更新系统配置 - 管理员登录后可访问")
    void updateSystemConfig_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(put("/api/system/config")
                .contentType("application/json")
                .content("{\"configKey\":\"test\",\"configValue\":\"value\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("批量更新系统配置 - 管理员登录后可访问")
    void batchUpdateSystemConfigs_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(put("/api/system/config/batch")
                .contentType("application/json")
                .content("[{\"configKey\":\"test\",\"configValue\":\"value\"}]"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("更新网站配置 - 管理员登录后可访问")
    void updateWebsiteConfig_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(put("/api/system/config/website")
                .contentType("application/json")
                .content("{\"siteName\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取邮件配置 - 管理员登录后可访问")
    void getEmailConfig_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/system/config/email"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("更新邮件配置 - 管理员登录后可访问")
    void updateEmailConfig_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(put("/api/system/config/email")
                .contentType("application/json")
                .content("{\"smtpHost\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取文件上传配置 - 管理员登录后可访问")
    void getFileUploadConfig_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/system/config/file-upload"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("更新文件上传配置 - 管理员登录后可访问")
    void updateFileUploadConfig_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(put("/api/system/config/file-upload")
                .contentType("application/json")
                .content("{\"maxSize\":10485760}"))
                .andExpect(status().isOk());
    }
}
