package com.blog.service;

import com.blog.dto.BackupInfoDTO;
import com.blog.dto.ExportInfoDTO;
import com.blog.common.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据备份服务测试类
 */
@SpringBootTest
@Transactional
public class DataBackupServiceTest {

    @Autowired
    private DataBackupService dataBackupService;

    @Test
    public void testCreateDatabaseBackup() {
        // 创建数据库备份
        Result<BackupInfoDTO> result = dataBackupService.createDatabaseBackup("test_backup", "测试备份");
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("database", result.getData().getBackupType());
        assertNotNull(result.getData().getFileName());
        assertNotNull(result.getData().getFilePath());
        assertTrue(result.getData().getFileSize() > 0);
    }

    @Test
    public void testGetBackupList() {
        // 先创建备份
        dataBackupService.createDatabaseBackup("backup1", "备份1");
        dataBackupService.createDatabaseBackup("backup2", "备份2");

        // 获取备份列表
        Result<List<BackupInfoDTO>> result = dataBackupService.getBackupList();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().size() >= 2);
    }

    @Test
    public void testDeleteBackup() {
        // 先创建备份
        Result<BackupInfoDTO> createResult = dataBackupService.createDatabaseBackup("delete_test", "删除测试备份");
        Long backupId = createResult.getData().getBackupId();

        // 删除备份
        Result<Void> result = dataBackupService.deleteBackup(backupId);
        
        assertTrue(result.isSuccess());

        // 验证备份已被删除
        Result<List<BackupInfoDTO>> listResult = dataBackupService.getBackupList();
        boolean backupExists = listResult.getData().stream()
                .anyMatch(backup -> backup.getBackupId().equals(backupId));
        assertFalse(backupExists);
    }

    @Test
    public void testDeleteBackupNotFound() {
        // 删除不存在的备份
        Result<Void> result = dataBackupService.deleteBackup(99999L);
        
        assertFalse(result.isSuccess());
        assertEquals("备份文件不存在", result.getMessage());
    }

    @Test
    public void testRestoreDatabase() {
        // 先创建备份
        Result<BackupInfoDTO> createResult = dataBackupService.createDatabaseBackup("restore_test", "恢复测试备份");
        Long backupId = createResult.getData().getBackupId();

        // 恢复数据库
        Result<Void> result = dataBackupService.restoreDatabase(backupId);
        
        assertTrue(result.isSuccess());
    }

    @Test
    public void testRestoreDatabaseNotFound() {
        // 恢复不存在的备份
        Result<Void> result = dataBackupService.restoreDatabase(99999L);
        
        assertFalse(result.isSuccess());
        assertEquals("备份文件不存在", result.getMessage());
    }

    @Test
    public void testExportUserData() {
        // 导出用户数据（null表示导出所有用户）
        Result<ExportInfoDTO> result = dataBackupService.exportUserData(null);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("user", result.getData().getExportType());
        assertNotNull(result.getData().getFileName());
        assertNotNull(result.getData().getFilePath());
        assertTrue(result.getData().getRecordCount() >= 0);
    }

    @Test
    public void testExportArticleData() {
        // 导出文章数据（null表示导出所有文章）
        Result<ExportInfoDTO> result = dataBackupService.exportArticleData(null);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("article", result.getData().getExportType());
        assertNotNull(result.getData().getFileName());
        assertNotNull(result.getData().getFilePath());
        assertTrue(result.getData().getRecordCount() >= 0);
    }

    @Test
    public void testExportCommentData() {
        // 导出评论数据（null表示导出所有评论）
        Result<ExportInfoDTO> result = dataBackupService.exportCommentData(null);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("comment", result.getData().getExportType());
        assertNotNull(result.getData().getFileName());
        assertNotNull(result.getData().getFilePath());
        assertTrue(result.getData().getRecordCount() >= 0);
    }

    @Test
    public void testGetExportFileList() {
        // 先导出数据
        dataBackupService.exportUserData(null);
        dataBackupService.exportArticleData(null);

        // 获取导出文件列表
        Result<List<ExportInfoDTO>> result = dataBackupService.getExportFileList();
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().size() >= 2);
    }

    @Test
    public void testDeleteExportFile() {
        // 先导出数据
        Result<ExportInfoDTO> exportResult = dataBackupService.exportUserData(null);
        Long exportId = exportResult.getData().getExportId();

        // 删除导出文件
        Result<Void> result = dataBackupService.deleteExportFile(exportId);
        
        assertTrue(result.isSuccess());

        // 验证导出文件已被删除
        Result<List<ExportInfoDTO>> listResult = dataBackupService.getExportFileList();
        boolean exportExists = listResult.getData().stream()
                .anyMatch(export -> export.getExportId().equals(exportId));
        assertFalse(exportExists);
    }

    @Test
    public void testDeleteExportFileNotFound() {
        // 删除不存在的导出文件
        Result<Void> result = dataBackupService.deleteExportFile(99999L);
        
        assertFalse(result.isSuccess());
        assertEquals("导出文件不存在", result.getMessage());
    }

    @Test
    public void testDownloadBackup() {
        // 先创建备份
        Result<BackupInfoDTO> createResult = dataBackupService.createDatabaseBackup("download_test", "下载测试备份");
        Long backupId = createResult.getData().getBackupId();

        // 下载备份文件
        Result<BackupInfoDTO> result = dataBackupService.downloadBackup(backupId);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getFileName());
        assertNotNull(result.getData().getFilePath());
    }

    @Test
    public void testDownloadBackupNotFound() {
        // 下载不存在的备份
        Result<BackupInfoDTO> result = dataBackupService.downloadBackup(99999L);
        
        assertFalse(result.isSuccess());
        assertEquals("备份文件不存在", result.getMessage());
    }

    @Test
    public void testDownloadExportFile() {
        // 先导出数据
        Result<ExportInfoDTO> exportResult = dataBackupService.exportUserData(null);
        Long exportId = exportResult.getData().getExportId();

        // 下载导出文件
        Result<ExportInfoDTO> result = dataBackupService.downloadExportFile(exportId);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getFileName());
        assertNotNull(result.getData().getFilePath());
    }

    @Test
    public void testDownloadExportFileNotFound() {
        // 下载不存在的导出文件
        Result<ExportInfoDTO> result = dataBackupService.downloadExportFile(99999L);
        
        assertFalse(result.isSuccess());
        assertEquals("导出文件不存在", result.getMessage());
    }
}