package com.blog.controller;

import com.blog.test.AbstractControllerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataBackupControllerIntegrationTest extends AbstractControllerTest {

    @Test
    @DisplayName("创建数据库备份 - 未登录应返回 401")
    void createDatabaseBackup_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/system/backup/database")
                .param("backupName", "test-backup"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取备份列表 - 未登录应返回 401")
    void getBackupList_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/system/backup/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("删除备份 - 未登录应返回 401")
    void deleteBackup_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/system/backup/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("导出用户数据 - 未登录应返回 401")
    void exportUserData_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/system/backup/export/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("导出文章数据 - 未登录应返回 401")
    void exportArticleData_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/system/backup/export/article"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("导出评论数据 - 未登录应返回 401")
    void exportCommentData_shouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/system/backup/export/comment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取导出文件列表 - 未登录应返回 401")
    void getExportFileList_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/system/backup/export/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("删除导出文件 - 未登录应返回 401")
    void deleteExportFile_shouldRequireAuth() throws Exception {
        mockMvc.perform(delete("/api/system/backup/export/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("下载备份文件 - 未登录应返回 401")
    void downloadBackup_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/system/backup/download/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("下载导出文件 - 未登录应返回 401")
    void downloadExportFile_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/system/backup/export/download/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("创建数据库备份 - 管理员登录后可访问")
    void createDatabaseBackup_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/system/backup/database")
                .param("backupName", "test-backup"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取备份列表 - 管理员登录后可访问")
    void getBackupList_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/system/backup/list"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("删除备份 - 管理员登录后可访问")
    void deleteBackup_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(delete("/api/system/backup/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("导出用户数据 - 管理员登录后可访问")
    void exportUserData_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/system/backup/export/user"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("导出文章数据 - 管理员登录后可访问")
    void exportArticleData_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/system/backup/export/article"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("导出评论数据 - 管理员登录后可访问")
    void exportCommentData_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(post("/api/system/backup/export/comment"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("获取导出文件列表 - 管理员登录后可访问")
    void getExportFileList_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/system/backup/export/list"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("删除导出文件 - 管理员登录后可访问")
    void deleteExportFile_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(delete("/api/system/backup/export/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("下载备份文件 - 管理员登录后可访问")
    void downloadBackup_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/system/backup/download/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("下载导出文件 - 管理员登录后可访问")
    void downloadExportFile_shouldBeAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/api/system/backup/export/download/1"))
                .andExpect(status().isNotFound());
    }
}
