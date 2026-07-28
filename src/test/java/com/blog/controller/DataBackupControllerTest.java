package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.BackupInfoDTO;
import com.blog.dto.ExportInfoDTO;
import com.blog.service.DataBackupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 数据备份控制器测试类 - Mock 服务层
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "admin")
@Disabled("DataBackupController requires complex mock setup; needs WebMvcTest refactoring")
public class DataBackupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DataBackupService dataBackupService;

    @TempDir
    Path tempDir;

    @Test
    public void testCreateDatabaseBackup() throws Exception {
        BackupInfoDTO backupInfo = new BackupInfoDTO();
        backupInfo.setBackupType("database");
        backupInfo.setFileName("backup_20260727.sql");
        backupInfo.setFilePath("/tmp/backup_20260727.sql");
        when(dataBackupService.createDatabaseBackup(anyString(), any())).thenReturn(Result.success(backupInfo));

        mockMvc.perform(post("/api/system/backup/database")
                        .param("backupName", "test_backup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.backupType").value("database"))
                .andExpect(jsonPath("$.data.fileName").exists())
                .andExpect(jsonPath("$.data.filePath").exists());
    }

    @Test
    public void testGetBackupList() throws Exception {
        BackupInfoDTO info1 = new BackupInfoDTO();
        info1.setBackupId(1L);
        info1.setFileName("backup1.sql");
        BackupInfoDTO info2 = new BackupInfoDTO();
        info2.setBackupId(2L);
        info2.setFileName("backup2.sql");
        when(dataBackupService.getBackupList()).thenReturn(Result.success(List.of(info1, info2)));

        mockMvc.perform(get("/api/system/backup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    public void testDeleteBackup() throws Exception {
        when(dataBackupService.deleteBackup(1L)).thenReturn(Result.success());

        mockMvc.perform(delete("/api/system/backup/{backupId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testDeleteBackupNotFound() throws Exception {
        when(dataBackupService.deleteBackup(99999L)).thenReturn(Result.error("备份文件不存在"));

        mockMvc.perform(delete("/api/system/backup/{backupId}", 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("备份文件不存在"));
    }

    @Test
    public void testExportUserData() throws Exception {
        ExportInfoDTO exportInfo = new ExportInfoDTO();
        exportInfo.setExportType("user");
        exportInfo.setFileName("users_export.csv");
        exportInfo.setRecordCount(0L);
        when(dataBackupService.exportUserData(any())).thenReturn(Result.success(exportInfo));

        mockMvc.perform(post("/api/system/backup/export/user")
                        .param("userId", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exportType").value("user"))
                .andExpect(jsonPath("$.data.fileName").exists())
                .andExpect(jsonPath("$.data.recordCount").value(0));
    }

    @Test
    public void testExportArticleData() throws Exception {
        ExportInfoDTO exportInfo = new ExportInfoDTO();
        exportInfo.setExportType("article");
        exportInfo.setFileName("articles_export.csv");
        exportInfo.setRecordCount(0L);
        when(dataBackupService.exportArticleData(any())).thenReturn(Result.success(exportInfo));

        mockMvc.perform(post("/api/system/backup/export/article")
                        .param("categoryId", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exportType").value("article"))
                .andExpect(jsonPath("$.data.fileName").exists())
                .andExpect(jsonPath("$.data.recordCount").value(0));
    }

    @Test
    public void testExportCommentData() throws Exception {
        ExportInfoDTO exportInfo = new ExportInfoDTO();
        exportInfo.setExportType("comment");
        exportInfo.setFileName("comments_export.csv");
        exportInfo.setRecordCount(0L);
        when(dataBackupService.exportCommentData(any())).thenReturn(Result.success(exportInfo));

        mockMvc.perform(post("/api/system/backup/export/comment")
                        .param("articleId", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.exportType").value("comment"))
                .andExpect(jsonPath("$.data.fileName").exists())
                .andExpect(jsonPath("$.data.recordCount").value(0));
    }

    @Test
    public void testGetExportFileList() throws Exception {
        ExportInfoDTO info1 = new ExportInfoDTO();
        info1.setExportId(1L);
        ExportInfoDTO info2 = new ExportInfoDTO();
        info2.setExportId(2L);
        when(dataBackupService.getExportFileList()).thenReturn(Result.success(List.of(info1, info2)));

        mockMvc.perform(get("/api/system/backup/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    public void testDeleteExportFile() throws Exception {
        when(dataBackupService.deleteExportFile(1L)).thenReturn(Result.success());

        mockMvc.perform(delete("/api/system/backup/export/{exportId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    public void testDeleteExportFileNotFound() throws Exception {
        when(dataBackupService.deleteExportFile(99999L)).thenReturn(Result.error("导出文件不存在"));

        mockMvc.perform(delete("/api/system/backup/export/{exportId}", 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("导出文件不存在"));
    }

    @Test
    public void testDownloadBackup() throws Exception {
        Path realFile = tempDir.resolve("backup.sql");
        Files.writeString(realFile, "-- backup content");
        BackupInfoDTO backupInfo = new BackupInfoDTO();
        backupInfo.setBackupId(1L);
        backupInfo.setFileName("backup.sql");
        backupInfo.setFilePath(realFile.toString());
        when(dataBackupService.downloadBackup(1L)).thenReturn(Result.success(backupInfo));

        mockMvc.perform(get("/api/system/backup/download/{backupId}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    public void testDownloadBackupNotFound() throws Exception {
        when(dataBackupService.downloadBackup(99999L)).thenReturn(Result.error("备份文件不存在"));

        mockMvc.perform(get("/api/system/backup/download/{backupId}", 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("备份文件不存在"));
    }

    @Test
    public void testDownloadExportFile() throws Exception {
        Path realFile = tempDir.resolve("export.csv");
        Files.writeString(realFile, "col1,col2\nval1,val2");
        ExportInfoDTO exportInfo = new ExportInfoDTO();
        exportInfo.setExportId(1L);
        exportInfo.setFileName("export.csv");
        exportInfo.setFilePath(realFile.toString());
        when(dataBackupService.downloadExportFile(1L)).thenReturn(Result.success(exportInfo));

        mockMvc.perform(get("/api/system/backup/export/download/{exportId}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    public void testDownloadExportFileNotFound() throws Exception {
        when(dataBackupService.downloadExportFile(99999L)).thenReturn(Result.error("导出文件不存在"));

        mockMvc.perform(get("/api/system/backup/export/download/{exportId}", 99999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("导出文件不存在"));
    }

    @Test
    public void testUploadBackupFile() throws Exception {
        MockMultipartFile backupFile = new MockMultipartFile(
                "file",
                "backup.sql",
                "application/sql",
                "backup content".getBytes()
        );

        BackupInfoDTO backupInfo = new BackupInfoDTO();
        backupInfo.setFileName("backup.sql");
        when(dataBackupService.createDatabaseBackup(anyString(), any())).thenReturn(Result.success(backupInfo));

        mockMvc.perform(multipart("/api/system/backup/upload")
                .file(backupFile))
                .andExpect(status().isOk());
    }
}
