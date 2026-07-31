package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.BackupInfoDTO;
import com.blog.dto.ExportInfoDTO;
import com.blog.service.DataBackupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DataBackupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataBackupService dataBackupService;

    @Test
    @DisplayName("createDatabaseBackup - 成功场景")
    @WithMockUser(roles = "admin")
    void createDatabaseBackup_success() throws Exception {
        when(dataBackupService.createDatabaseBackup(any(), any()))
                .thenReturn(Result.success(new BackupInfoDTO()));

        mockMvc.perform(post("/api/system/backup/database")
                .param("backupName", "test-backup")
                .param("description", "test description"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getBackupList - 成功场景")
    @WithMockUser(roles = "admin")
    void getBackupList_success() throws Exception {
        when(dataBackupService.getBackupList())
                .thenReturn(Result.success(List.of(new BackupInfoDTO())));

        mockMvc.perform(get("/api/system/backup/list"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deleteBackup - 成功场景")
    @WithMockUser(roles = "admin")
    void deleteBackup_success() throws Exception {
        when(dataBackupService.deleteBackup(any()))
                .thenReturn(Result.success(null));

        mockMvc.perform(delete("/api/system/backup/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("exportUserData - 成功场景")
    @WithMockUser(roles = "admin")
    void exportUserData_success() throws Exception {
        when(dataBackupService.exportUserData(any()))
                .thenReturn(Result.success(new ExportInfoDTO()));

        mockMvc.perform(post("/api/system/backup/export/user")
                .param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("exportArticleData - 成功场景")
    @WithMockUser(roles = "admin")
    void exportArticleData_success() throws Exception {
        when(dataBackupService.exportArticleData(any()))
                .thenReturn(Result.success(new ExportInfoDTO()));

        mockMvc.perform(post("/api/system/backup/export/article")
                .param("categoryId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("exportCommentData - 成功场景")
    @WithMockUser(roles = "admin")
    void exportCommentData_success() throws Exception {
        when(dataBackupService.exportCommentData(any()))
                .thenReturn(Result.success(new ExportInfoDTO()));

        mockMvc.perform(post("/api/system/backup/export/comment")
                .param("articleId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getExportFileList - 成功场景")
    @WithMockUser(roles = "admin")
    void getExportFileList_success() throws Exception {
        when(dataBackupService.getExportFileList())
                .thenReturn(Result.success(List.of(new ExportInfoDTO())));

        mockMvc.perform(get("/api/system/backup/export/list"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("deleteExportFile - 成功场景")
    @WithMockUser(roles = "admin")
    void deleteExportFile_success() throws Exception {
        when(dataBackupService.deleteExportFile(any()))
                .thenReturn(Result.success(null));

        mockMvc.perform(delete("/api/system/backup/export/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("downloadBackup - 文件不存在应返回 404")
    @WithMockUser(roles = "admin")
    void downloadBackup_fileNotFound_shouldReturn404() throws Exception {
        when(dataBackupService.downloadBackup(any()))
                .thenReturn(Result.success(null));

        mockMvc.perform(get("/api/system/backup/download/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("downloadBackup - 文件存在应返回 200")
    @WithMockUser(roles = "admin")
    void downloadBackup_fileExists_shouldReturn200() throws Exception {
        File tempFile = File.createTempFile("backup", ".sql");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("backup data");
        }

        BackupInfoDTO backupInfo = new BackupInfoDTO();
        backupInfo.setFilePath(tempFile.getAbsolutePath());
        backupInfo.setFileName("backup_1.sql");

        org.mockito.Mockito.doReturn(Result.success(backupInfo))
                .when(dataBackupService).downloadBackup(any());

        mockMvc.perform(get("/api/system/backup/download/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("downloadExportFile - 文件不存在应返回 404")
    @WithMockUser(roles = "admin")
    void downloadExportFile_fileNotFound_shouldReturn404() throws Exception {
        when(dataBackupService.downloadExportFile(any()))
                .thenReturn(Result.success(null));

        mockMvc.perform(get("/api/system/backup/export/download/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("downloadExportFile - 文件存在应返回 200")
    @WithMockUser(roles = "admin")
    void downloadExportFile_fileExists_shouldReturn200() throws Exception {
        File tempFile = File.createTempFile("export", ".csv");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("export data");
        }

        ExportInfoDTO exportInfo = new ExportInfoDTO();
        exportInfo.setFilePath(tempFile.getAbsolutePath());
        exportInfo.setFileName("export_1.csv");

        org.mockito.Mockito.doReturn(Result.success(exportInfo))
                .when(dataBackupService).downloadExportFile(any());

        mockMvc.perform(get("/api/system/backup/export/download/1"))
                .andExpect(status().isOk());
    }
}
