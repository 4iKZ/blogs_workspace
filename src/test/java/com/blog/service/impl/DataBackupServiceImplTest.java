package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.BackupInfoDTO;
import com.blog.dto.ExportInfoDTO;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.DataBackupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataBackupServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private DataBackupServiceImpl dataBackupService;

    private Path backupRoot;
    private Path exportRoot;

    @BeforeEach
    void setUp() throws Exception {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        backupRoot = Files.createTempDirectory("backup");
        exportRoot = Files.createTempDirectory("export");

        java.lang.reflect.Field backupRootField = DataBackupServiceImpl.class.getDeclaredField("backupRoot");
        java.lang.reflect.Field exportRootField = DataBackupServiceImpl.class.getDeclaredField("exportRoot");
        backupRootField.setAccessible(true);
        exportRootField.setAccessible(true);
        backupRootField.set(dataBackupService, backupRoot);
        exportRootField.set(dataBackupService, exportRoot);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    // ==================== 数据库备份 ====================

    @Test
    @DisplayName("创建数据库备份 - 名称不应为空")
    void createDatabaseBackup_nameShouldNotBeBlank() {
        var result = dataBackupService.createDatabaseBackup("", "desc");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("备份名称不能为空");
    }

    @Test
    @DisplayName("创建数据库备份 - 发生异常应返回错误")
    void createDatabaseBackup_exception_shouldReturnError() {
        doThrow(new RuntimeException("db error")).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        var result = dataBackupService.createDatabaseBackup("backup1", "desc");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("创建数据库备份失败");
    }

    @Test
    @DisplayName("创建数据库备份 - 成功创建文件")
    void createDatabaseBackup_success_shouldCreateFile() throws Exception {
        DataSource mockDs = mock(DataSource.class);
        Connection mockConn = mock(Connection.class);
        DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);
        ResultSet mockRs = mock(ResultSet.class);

        when(mockDs.getConnection()).thenReturn(mockConn);
        when(jdbcTemplate.getDataSource()).thenReturn(mockDs);
        when(mockConn.getCatalog()).thenReturn("blog");
        when(mockConn.getMetaData()).thenReturn(mockMeta);
        when(mockMeta.getTables(eq("blog"), isNull(), eq("%"), any(String[].class))).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(false);

        var result = dataBackupService.createDatabaseBackup("backup_success", "desc");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getFileName()).contains("backup_success_");
        assertThat(result.getData().getFileName()).endsWith(".sql");
        assertThat(result.getData().getFileSize()).isGreaterThan(0);
    }

    @Test
    @DisplayName("获取备份列表 - 应返回列表")
    void getBackupList_shouldReturnList() {
        var result = dataBackupService.getBackupList();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("获取备份列表 - 文件列表异常应返回错误")
    void getBackupList_fileListingException_shouldReturnError() {
        try (MockedStatic<Files> mocked = Mockito.mockStatic(Files.class)) {
            mocked.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            mocked.when(() -> Files.list(any(Path.class))).thenThrow(new IOException("list failed"));

            var result = dataBackupService.getBackupList();

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("获取备份列表失败");
        }
    }

    @Test
    @DisplayName("获取备份列表 - 空目录应返回空列表")
    void getBackupList_emptyDirectory_shouldReturnEmptyList() {
        var result = dataBackupService.getBackupList();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("删除备份 - ID不应为空")
    void deleteBackup_nullId_shouldReturnError() {
        var result = dataBackupService.deleteBackup(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("备份ID不能为空");
    }

    @Test
    @DisplayName("删除备份 - 文件不存在应返回错误")
    void deleteBackup_fileNotFound_shouldReturnError() {
        var result = dataBackupService.deleteBackup(999L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("备份文件不存在");
    }

    @Test
    @DisplayName("删除备份 - 成功删除")
    void deleteBackup_success_shouldDelete() throws Exception {
        long backupId = 1L;
        BackupInfoDTO info = new BackupInfoDTO();
        info.setBackupId(backupId);
        info.setFileName("test.sql");
        info.setFilePath(backupRoot.resolve("test.sql").toString());
        info.setStatus("success");
        writeMetadata(backupRoot, backupId, info);
        Files.writeString(backupRoot.resolve("test.sql"), "-- test backup");

        var result = dataBackupService.deleteBackup(backupId);

        assertThat(result.isSuccess()).isTrue();
        assertThat(Files.exists(backupRoot.resolve(backupId + ".meta.json"))).isFalse();
        assertThat(Files.exists(backupRoot.resolve("test.sql"))).isFalse();
    }

    @Test
    @DisplayName("下载备份 - ID不应为空")
    void downloadBackup_nullId_shouldReturnError() {
        var result = dataBackupService.downloadBackup(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("备份ID不能为空");
    }

    @Test
    @DisplayName("下载备份 - 文件不存在应返回错误")
    void downloadBackup_fileNotFound_shouldReturnError() {
        var result = dataBackupService.downloadBackup(999L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("备份文件不存在");
    }

    @Test
    @DisplayName("下载备份 - 成功返回信息")
    void downloadBackup_success_shouldReturnInfo() throws Exception {
        long backupId = 2L;
        BackupInfoDTO info = new BackupInfoDTO();
        info.setBackupId(backupId);
        info.setFileName("test.sql");
        info.setFilePath(backupRoot.resolve("test.sql").toString());
        info.setStatus("success");
        writeMetadata(backupRoot, backupId, info);
        Files.writeString(backupRoot.resolve("test.sql"), "-- test backup");

        var result = dataBackupService.downloadBackup(backupId);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getBackupId()).isEqualTo(backupId);
    }

    // ==================== 数据导出 ====================

    @Test
    @DisplayName("导出用户数据 - 发生异常应返回错误")
    void exportUserData_exception_shouldReturnError() {
        doThrow(new RuntimeException("db error")).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        var result = dataBackupService.exportUserData(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("导出用户数据失败");
    }

    @Test
    @DisplayName("导出用户数据 - 用户ID为空应导出全部")
    void exportUserData_nullUserId_shouldExportAll() {
        when(jdbcTemplate.queryForList(eq("SELECT * FROM users"))).thenReturn(Collections.emptyList());

        var result = dataBackupService.exportUserData(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getExportType()).isEqualTo("user");
    }

    @Test
    @DisplayName("导出用户数据 - 成功导出文件")
    void exportUserData_success_shouldCreateFile() {
        when(jdbcTemplate.queryForList(eq("SELECT * FROM users WHERE id = ?"), eq(1L))).thenReturn(Collections.emptyList());

        var result = dataBackupService.exportUserData(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getExportType()).isEqualTo("user");
        assertThat(result.getData().getRecordCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("导出文章数据 - 发生异常应返回错误")
    void exportArticleData_exception_shouldReturnError() {
        when(jdbcTemplate.queryForList(anyString())).thenThrow(new RuntimeException("db error"));

        var result = dataBackupService.exportArticleData(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("导出文章数据失败");
    }

    @Test
    @DisplayName("导出文章数据 - 成功导出文件")
    void exportArticleData_success_shouldCreateFile() {
        when(jdbcTemplate.queryForList(eq("SELECT * FROM articles"))).thenReturn(Collections.emptyList());

        var result = dataBackupService.exportArticleData(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getExportType()).isEqualTo("article");
        assertThat(result.getData().getRecordCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("导出评论数据 - 发生异常应返回错误")
    void exportCommentData_exception_shouldReturnError() {
        doThrow(new RuntimeException("db error")).when(jdbcTemplate).queryForList(anyString(), any(Object[].class));

        var result = dataBackupService.exportCommentData(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("导出评论数据失败");
    }

    @Test
    @DisplayName("导出评论数据 - 文章ID为空应导出全部")
    void exportCommentData_nullArticleId_shouldExportAll() {
        when(jdbcTemplate.queryForList(eq("SELECT * FROM comments"))).thenReturn(Collections.emptyList());

        var result = dataBackupService.exportCommentData(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getExportType()).isEqualTo("comment");
    }

    @Test
    @DisplayName("导出评论数据 - 成功导出文件")
    void exportCommentData_success_shouldCreateFile() {
        when(jdbcTemplate.queryForList(eq("SELECT * FROM comments WHERE article_id = ?"), eq(1L))).thenReturn(Collections.emptyList());

        var result = dataBackupService.exportCommentData(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getExportType()).isEqualTo("comment");
        assertThat(result.getData().getRecordCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("获取导出文件列表 - 应返回列表")
    void getExportFileList_shouldReturnList() {
        var result = dataBackupService.getExportFileList();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("获取导出文件列表 - 文件列表异常应返回错误")
    void getExportFileList_fileListingException_shouldReturnError() {
        try (MockedStatic<Files> mocked = Mockito.mockStatic(Files.class)) {
            mocked.when(() -> Files.exists(any(Path.class))).thenReturn(true);
            mocked.when(() -> Files.list(any(Path.class))).thenThrow(new IOException("list failed"));

            var result = dataBackupService.getExportFileList();

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("获取导出列表失败");
        }
    }

    @Test
    @DisplayName("获取导出文件列表 - 空目录应返回空列表")
    void getExportFileList_emptyDirectory_shouldReturnEmptyList() {
        var result = dataBackupService.getExportFileList();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("删除导出文件 - ID不应为空")
    void deleteExportFile_nullId_shouldReturnError() {
        var result = dataBackupService.deleteExportFile(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("导出文件ID不能为空");
    }

    @Test
    @DisplayName("删除导出文件 - 文件不存在应返回错误")
    void deleteExportFile_fileNotFound_shouldReturnError() {
        var result = dataBackupService.deleteExportFile(999L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("导出文件不存在");
    }

    @Test
    @DisplayName("删除导出文件 - 成功删除")
    void deleteExportFile_success_shouldDelete() throws Exception {
        long exportId = 10L;
        ExportInfoDTO info = new ExportInfoDTO();
        info.setExportId(exportId);
        info.setFileName("test.json");
        info.setFilePath(exportRoot.resolve("test.json").toString());
        info.setExportType("user");
        info.setStatus("success");
        writeMetadata(exportRoot, exportId, info);
        Files.writeString(exportRoot.resolve("test.json"), "[]");

        var result = dataBackupService.deleteExportFile(exportId);

        assertThat(result.isSuccess()).isTrue();
        assertThat(Files.exists(exportRoot.resolve(exportId + ".meta.json"))).isFalse();
        assertThat(Files.exists(exportRoot.resolve("test.json"))).isFalse();
    }

    @Test
    @DisplayName("下载导出文件 - ID不应为空")
    void downloadExportFile_nullId_shouldReturnError() {
        var result = dataBackupService.downloadExportFile(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("导出ID不能为空");
    }

    @Test
    @DisplayName("下载导出文件 - 文件不存在应返回错误")
    void downloadExportFile_fileNotFound_shouldReturnError() {
        var result = dataBackupService.downloadExportFile(999L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("导出文件不存在");
    }

    @Test
    @DisplayName("下载导出文件 - 成功返回信息")
    void downloadExportFile_success_shouldReturnInfo() throws Exception {
        long exportId = 20L;
        ExportInfoDTO info = new ExportInfoDTO();
        info.setExportId(exportId);
        info.setFileName("test.json");
        info.setFilePath(exportRoot.resolve("test.json").toString());
        info.setExportType("user");
        info.setStatus("success");
        writeMetadata(exportRoot, exportId, info);
        Files.writeString(exportRoot.resolve("test.json"), "[]");

        var result = dataBackupService.downloadExportFile(exportId);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getExportId()).isEqualTo(exportId);
    }

    // ==================== 工具方法 ====================

    private <T> void writeMetadata(Path dir, long id, T data) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        Path metaFile = dir.resolve(id + ".meta.json");
        mapper.writeValue(metaFile.toFile(), data);
    }
}
