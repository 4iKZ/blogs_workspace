package com.blog.service;

import com.blog.dto.BackupInfoDTO;
import com.blog.dto.ExportInfoDTO;
import com.blog.common.Result;
import com.blog.service.impl.DataBackupServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据备份服务测试类
 * <p>
 * 使用独立的 H2 内存数据库 + 临时目录，无需启动 Spring 容器。
 * 注意：因 H2 不支持 SHOW CREATE TABLE（MySQL 语法），
 * createDatabaseBackup 和 restoreDatabase 仅验证基本的错误处理路径。
 * 数据导出、文件管理等功能可完整测试。
 */
class DataBackupServiceTest {

    private DataBackupServiceImpl backupService;
    private JdbcTemplate jdbcTemplate;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        // 创建独立的 H2 内存数据库，包含测试表
        DataSource dataSource = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("backup_test_" + System.nanoTime())
                .build();

        jdbcTemplate = new JdbcTemplate(dataSource);

        // 创建测试表并插入数据
        jdbcTemplate.execute("CREATE TABLE users (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL, " +
                "email VARCHAR(100) NOT NULL, " +
                "password VARCHAR(255) NOT NULL, " +
                "nickname VARCHAR(50), " +
                "status INT DEFAULT 1, " +
                "role INT DEFAULT 1, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
        jdbcTemplate.execute("CREATE TABLE articles (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "title VARCHAR(200) NOT NULL, " +
                "content CLOB NOT NULL, " +
                "category_id BIGINT NOT NULL, " +
                "author_id BIGINT NOT NULL, " +
                "status INT DEFAULT 2, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
        jdbcTemplate.execute("CREATE TABLE comments (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "article_id BIGINT NOT NULL, " +
                "user_id BIGINT NOT NULL, " +
                "content VARCHAR(500) NOT NULL, " +
                "status INT DEFAULT 2, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

        // 插入测试数据
        jdbcTemplate.update("INSERT INTO users (username, email, password, nickname) VALUES (?, ?, ?, ?)",
                "admin", "admin@test.com", "hashed_pwd", "管理员");
        jdbcTemplate.update("INSERT INTO users (username, email, password, nickname) VALUES (?, ?, ?, ?)",
                "user1", "user1@test.com", "hashed_pwd", "测试用户");
        jdbcTemplate.update("INSERT INTO articles (title, content, category_id, author_id) VALUES (?, ?, ?, ?)",
                "测试文章", "这是一篇测试文章内容", 1, 1);
        jdbcTemplate.update("INSERT INTO comments (article_id, user_id, content) VALUES (?, ?, ?)",
                1, 2, "这是一条测试评论");

        // 构建 Service 并注入依赖
        backupService = new DataBackupServiceImpl();
        injectField(backupService, "jdbcTemplate", jdbcTemplate);

        // 使用临时目录覆盖 backupRoot 和 exportRoot
        Path backupRoot = tempDir.resolve("backup");
        Path exportRoot = tempDir.resolve("export");
        Files.createDirectories(backupRoot);
        Files.createDirectories(exportRoot);
        injectField(backupService, "backupRoot", backupRoot);
        injectField(backupService, "exportRoot", exportRoot);
    }

    // ==================== 参数校验测试 ====================

    @Test
    @DisplayName("创建备份 - 名称为空应返回错误")
    void testCreateBackup_emptyName() {
        Result<BackupInfoDTO> result = backupService.createDatabaseBackup("", "desc");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("备份名称不能为空"));
    }

    @Test
    @DisplayName("创建备份 - 名称为null应返回错误")
    void testCreateBackup_nullName() {
        Result<BackupInfoDTO> result = backupService.createDatabaseBackup(null, null);
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("删除备份 - ID为null应返回错误")
    void testDeleteBackup_nullId() {
        Result<Void> result = backupService.deleteBackup(null);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("备份ID不能为空"));
    }

    @Test
    @DisplayName("恢复数据库 - ID为null应返回错误")
    void testRestoreDatabase_nullId() {
        Result<Void> result = backupService.restoreDatabase(null);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("备份ID不能为空"));
    }

    // ==================== 不存在的资源测试 ====================

    @Test
    @DisplayName("删除不存在的备份应返回错误")
    void testDeleteBackup_notFound() {
        Result<Void> result = backupService.deleteBackup(99999L);
        assertFalse(result.isSuccess());
        assertEquals("备份文件不存在", result.getMessage());
    }

    @Test
    @DisplayName("恢复不存在的备份应返回错误")
    void testRestoreDatabase_notFound() {
        Result<Void> result = backupService.restoreDatabase(99999L);
        assertFalse(result.isSuccess());
        assertEquals("备份文件不存在", result.getMessage());
    }

    @Test
    @DisplayName("下载不存在的备份应返回错误")
    void testDownloadBackup_notFound() {
        Result<BackupInfoDTO> result = backupService.downloadBackup(99999L);
        assertFalse(result.isSuccess());
        assertEquals("备份文件不存在", result.getMessage());
    }

    @Test
    @DisplayName("删除不存在的导出文件应返回错误")
    void testDeleteExportFile_notFound() {
        Result<Void> result = backupService.deleteExportFile(99999L);
        assertFalse(result.isSuccess());
        assertEquals("导出文件不存在", result.getMessage());
    }

    @Test
    @DisplayName("下载不存在的导出文件应返回错误")
    void testDownloadExportFile_notFound() {
        Result<ExportInfoDTO> result = backupService.downloadExportFile(99999L);
        assertFalse(result.isSuccess());
        assertEquals("导出文件不存在", result.getMessage());
    }

    // ==================== 数据导出测试 ====================

    @Test
    @DisplayName("导出所有用户数据")
    void testExportUserData_all() {
        Result<ExportInfoDTO> result = backupService.exportUserData(null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("user", result.getData().getExportType());
        assertEquals(2L, result.getData().getRecordCount(), "应导出 2 个用户");
        assertTrue(result.getData().getFileSize() > 0);
        assertTrue(result.getData().getFileName().contains("user_data"));
        assertTrue(Files.exists(Path.of(result.getData().getFilePath())));
    }

    @Test
    @DisplayName("按指定用户ID导出数据")
    void testExportUserData_specific() {
        Result<ExportInfoDTO> result = backupService.exportUserData(1L);

        assertTrue(result.isSuccess());
        assertEquals(1L, result.getData().getRecordCount(), "应只导出 1 个用户");
    }

    @Test
    @DisplayName("导出所有文章数据")
    void testExportArticleData_all() {
        Result<ExportInfoDTO> result = backupService.exportArticleData(null);

        assertTrue(result.isSuccess());
        assertEquals("article", result.getData().getExportType());
        assertEquals(1L, result.getData().getRecordCount());
        assertTrue(Files.exists(Path.of(result.getData().getFilePath())));
    }

    @Test
    @DisplayName("导出所有评论数据")
    void testExportCommentData_all() {
        Result<ExportInfoDTO> result = backupService.exportCommentData(null);

        assertTrue(result.isSuccess());
        assertEquals("comment", result.getData().getExportType());
        assertEquals(1L, result.getData().getRecordCount());
        assertTrue(Files.exists(Path.of(result.getData().getFilePath())));
    }

    // ==================== 导出文件管理测试 ====================

    @Test
    @DisplayName("获取导出文件列表")
    void testGetExportFileList() {
        // 先导出一些数据
        backupService.exportUserData(null);
        backupService.exportArticleData(null);

        Result<List<ExportInfoDTO>> result = backupService.getExportFileList();

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
    }

    @Test
    @DisplayName("删除导出文件")
    void testDeleteExportFile() {
        Result<ExportInfoDTO> exportResult = backupService.exportUserData(null);
        Long exportId = exportResult.getData().getExportId();
        String filePath = exportResult.getData().getFilePath();

        // 确认文件存在
        assertTrue(Files.exists(Path.of(filePath)));

        // 删除
        Result<Void> deleteResult = backupService.deleteExportFile(exportId);
        assertTrue(deleteResult.isSuccess());

        // 确认数据文件和元数据都已删除
        assertFalse(Files.exists(Path.of(filePath)));

        // 列表中不应再有该记录
        Result<List<ExportInfoDTO>> listResult = backupService.getExportFileList();
        boolean exists = listResult.getData().stream()
                .anyMatch(e -> e.getExportId().equals(exportId));
        assertFalse(exists);
    }

    @Test
    @DisplayName("下载导出文件 - 应返回文件信息")
    void testDownloadExportFile() {
        Result<ExportInfoDTO> exportResult = backupService.exportUserData(null);
        Long exportId = exportResult.getData().getExportId();

        Result<ExportInfoDTO> downloadResult = backupService.downloadExportFile(exportId);

        assertTrue(downloadResult.isSuccess());
        assertNotNull(downloadResult.getData());
        assertEquals(exportId, downloadResult.getData().getExportId());
        assertTrue(Files.exists(Path.of(downloadResult.getData().getFilePath())));
    }

    // ==================== 空数据导出测试 ====================

    @Test
    @DisplayName("导出不存在的分类文章应返回0条记录")
    void testExportArticleData_noMatch() {
        Result<ExportInfoDTO> result = backupService.exportArticleData(99999L);

        assertTrue(result.isSuccess());
        assertEquals(0L, result.getData().getRecordCount());
    }

    @Test
    @DisplayName("导出不存在的文章评论应返回0条记录")
    void testExportCommentData_noMatch() {
        Result<ExportInfoDTO> result = backupService.exportCommentData(99999L);

        assertTrue(result.isSuccess());
        assertEquals(0L, result.getData().getRecordCount());
    }

    // ==================== 空备份列表测试 ====================

    @Test
    @DisplayName("初始备份列表应为空")
    void testGetBackupList_empty() {
        Result<List<BackupInfoDTO>> result = backupService.getBackupList();
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    @DisplayName("初始导出列表应为空")
    void testGetExportFileList_empty() {
        Result<List<ExportInfoDTO>> result = backupService.getExportFileList();
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    // ==================== 工具方法 ====================

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}