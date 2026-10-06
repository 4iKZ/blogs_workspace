package com.blog.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSON 数据导出的 H2 集成测试（P2-9）。
 * <p>
 * 通过真实 H2 内存库验证三条导出链路是流式读取 + 流式写出：产出文件为合法 JSON 数组、
 * recordCount 来自累计行数、空表输出 {@code []}。
 */
class DataBackupJsonExportTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private DataBackupServiceImpl service;
    private JdbcTemplate jdbcTemplate;
    private Path exportRoot;

    @BeforeEach
    void setUp() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:mem:data_backup_export_test;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        ds.setUsername("sa");
        ds.setPassword("");
        jdbcTemplate = new JdbcTemplate(ds);

        jdbcTemplate.execute("DROP TABLE IF EXISTS users");
        jdbcTemplate.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, username VARCHAR(50))");
        jdbcTemplate.execute("DROP TABLE IF EXISTS articles");
        jdbcTemplate.execute("CREATE TABLE articles (id BIGINT PRIMARY KEY, category_id BIGINT, title VARCHAR(100))");
        jdbcTemplate.execute("DROP TABLE IF EXISTS comments");
        jdbcTemplate.execute("CREATE TABLE comments (id BIGINT PRIMARY KEY, article_id BIGINT, content VARCHAR(200))");

        exportRoot = Files.createTempDirectory("export-h2");
        service = new DataBackupServiceImpl();
        setField(service, "jdbcTemplate", jdbcTemplate);
        setField(service, "exportRoot", exportRoot);
    }

    @Test
    @DisplayName("导出用户数据 - 全量导出为合法 JSON 数组且计数正确")
    void exportUserData_all_shouldStreamRowsToJsonArray() throws Exception {
        jdbcTemplate.update("INSERT INTO users (id, username) VALUES (?, ?)", 1L, "Alice");
        jdbcTemplate.update("INSERT INTO users (id, username) VALUES (?, ?)", 2L, "Bob");

        var result = service.exportUserData(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getExportType()).isEqualTo("user");
        assertThat(result.getData().getStatus()).isEqualTo("success");
        assertThat(result.getData().getFileSize()).isGreaterThan(0);
        assertThat(result.getData().getFilePath()).endsWith(".json");
        assertThat(result.getData().getRecordCount()).isEqualTo(2);

        JsonNode node = readJson(result.getData().getFileName());
        assertThat(node.isArray()).isTrue();
        assertThat(node).hasSize(2);
        assertThat(node.get(0).get("username").asText()).isEqualTo("Alice");
        assertThat(node.get(1).get("id").asLong()).isEqualTo(2L);
    }

    @Test
    @DisplayName("导出用户数据 - 按ID过滤只导出匹配行")
    void exportUserData_byId_shouldFilterRows() throws Exception {
        jdbcTemplate.update("INSERT INTO users (id, username) VALUES (?, ?)", 1L, "Alice");
        jdbcTemplate.update("INSERT INTO users (id, username) VALUES (?, ?)", 2L, "Bob");

        var result = service.exportUserData(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getRecordCount()).isEqualTo(1);
        JsonNode node = readJson(result.getData().getFileName());
        assertThat(node).hasSize(1);
        assertThat(node.get(0).get("username").asText()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("导出用户数据 - 空表输出 [] 且计数为 0")
    void exportUserData_emptyTable_shouldWriteEmptyArray() throws Exception {
        var result = service.exportUserData(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getRecordCount()).isEqualTo(0);
        assertThat(Files.readString(exportRoot.resolve(result.getData().getFileName()))).isEqualTo("[]");
    }

    @Test
    @DisplayName("导出文章数据 - 按分类过滤")
    void exportArticleData_byCategory_shouldFilterRows() throws Exception {
        jdbcTemplate.update("INSERT INTO articles (id, category_id, title) VALUES (?, ?, ?)", 1L, 10L, "A");
        jdbcTemplate.update("INSERT INTO articles (id, category_id, title) VALUES (?, ?, ?)", 2L, 20L, "B");

        var result = service.exportArticleData(10L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getExportType()).isEqualTo("article");
        assertThat(result.getData().getRecordCount()).isEqualTo(1);
        JsonNode node = readJson(result.getData().getFileName());
        assertThat(node).hasSize(1);
        assertThat(node.get(0).get("title").asText()).isEqualTo("A");
    }

    @Test
    @DisplayName("导出评论数据 - 全量导出计数正确")
    void exportCommentData_all_shouldStreamRowsToJsonArray() throws Exception {
        jdbcTemplate.update("INSERT INTO comments (id, article_id, content) VALUES (?, ?, ?)", 1L, 10L, "c1");
        jdbcTemplate.update("INSERT INTO comments (id, article_id, content) VALUES (?, ?, ?)", 2L, 10L, "c2");
        jdbcTemplate.update("INSERT INTO comments (id, article_id, content) VALUES (?, ?, ?)", 3L, 20L, "c3");

        var result = service.exportCommentData(null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getExportType()).isEqualTo("comment");
        assertThat(result.getData().getRecordCount()).isEqualTo(3);
        JsonNode node = readJson(result.getData().getFileName());
        assertThat(node).hasSize(3);
        assertThat(node.get(2).get("content").asText()).isEqualTo("c3");
    }

    @Test
    @DisplayName("导出评论数据 - 空表输出 []")
    void exportCommentData_emptyTable_shouldWriteEmptyArray() throws Exception {
        var result = service.exportCommentData(999L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getRecordCount()).isEqualTo(0);
        assertThat(Files.readString(exportRoot.resolve(result.getData().getFileName()))).isEqualTo("[]");
    }

    private JsonNode readJson(String fileName) throws Exception {
        return mapper.readTree(Files.readString(exportRoot.resolve(fileName)));
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = DataBackupServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
