package com.blog.service.impl;

import com.blog.dto.BackupInfoDTO;
import com.blog.dto.ExportInfoDTO;
import com.blog.common.Result;
import com.blog.mapper.UserMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.service.DataBackupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 数据备份与恢复服务实现类
 * <p>
 * 使用纯 JDBC 方式导出数据库结构和数据为 SQL 文件，
 * 使用 Jackson 导出业务数据为 JSON 文件。
 * 所有元数据通过 JSON 文件持久化到文件系统，无需额外数据库表。
 */
@Service
public class DataBackupServiceImpl implements DataBackupService {

    private static final Logger log = LoggerFactory.getLogger(DataBackupServiceImpl.class);
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String META_SUFFIX = ".meta.json";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    private final ObjectMapper objectMapper;
    private final AtomicLong idGenerator = new AtomicLong(System.currentTimeMillis());

    /** 备份根目录 */
    private Path backupRoot;
    /** 导出根目录 */
    private Path exportRoot;

    public DataBackupServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @PostConstruct
    public void init() {
        // 使用工作目录下的 data/backup 和 data/export
        String userDir = System.getProperty("user.dir");
        this.backupRoot = Paths.get(userDir, "data", "backup");
        this.exportRoot = Paths.get(userDir, "data", "export");
        try {
            Files.createDirectories(backupRoot);
            Files.createDirectories(exportRoot);
            log.info("备份目录初始化完成: backup={}, export={}", backupRoot, exportRoot);
        } catch (IOException e) {
            log.error("创建备份目录失败", e);
        }
    }

    // ==================== 数据库备份 ====================

    @Override
    public Result<BackupInfoDTO> createDatabaseBackup(String backupName, String description) {
        log.info("创建数据库备份: backupName={}", backupName);

        if (backupName == null || backupName.trim().isEmpty()) {
            return Result.error("备份名称不能为空");
        }

        long backupId = idGenerator.incrementAndGet();
        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String fileName = backupName.trim().replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5-]", "_") + "_" + timestamp + ".sql";
        Path sqlFile = backupRoot.resolve(fileName);

        try {
            // 通过 JDBC 导出所有表结构和数据
            exportDatabaseToSql(sqlFile);

            long fileSize = Files.size(sqlFile);

            BackupInfoDTO info = new BackupInfoDTO();
            info.setBackupId(backupId);
            info.setFileName(fileName);
            info.setFilePath(sqlFile.toAbsolutePath().toString());
            info.setFileSize(fileSize);
            info.setBackupType("database");
            info.setDescription(description);
            info.setCreateTime(LocalDateTime.now());
            info.setStatus("success");

            // 保存元数据
            saveMetadata(backupRoot, backupId, info);

            log.info("数据库备份成功: file={}, size={}", fileName, fileSize);
            return Result.success(info);
        } catch (Exception e) {
            log.error("创建数据库备份失败", e);
            // 清理失败文件
            try {
                Files.deleteIfExists(sqlFile);
            } catch (IOException ignored) {
            }
            return Result.error("创建数据库备份失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<BackupInfoDTO>> getBackupList() {
        log.info("获取备份列表");
        try {
            List<BackupInfoDTO> list = loadAllMetadata(backupRoot, BackupInfoDTO.class);
            // 按创建时间倒序
            list.sort((a, b) -> {
                if (a.getCreateTime() == null || b.getCreateTime() == null)
                    return 0;
                return b.getCreateTime().compareTo(a.getCreateTime());
            });
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取备份列表失败", e);
            return Result.error("获取备份列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteBackup(Long backupId) {
        log.info("删除备份: backupId={}", backupId);
        if (backupId == null)
            return Result.error("备份ID不能为空");

        try {
            BackupInfoDTO info = loadMetadata(backupRoot, backupId, BackupInfoDTO.class);
            if (info == null)
                return Result.error("备份文件不存在");

            // 删除 SQL 文件
            if (info.getFilePath() != null) {
                Files.deleteIfExists(Path.of(info.getFilePath()));
            }
            // 删除元数据文件
            deleteMetadataFile(backupRoot, backupId);

            log.info("备份删除成功: backupId={}", backupId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除备份失败", e);
            return Result.error("删除备份失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> restoreDatabase(Long backupId) {
        log.info("恢复数据库: backupId={}", backupId);
        if (backupId == null)
            return Result.error("备份ID不能为空");

        try {
            BackupInfoDTO info = loadMetadata(backupRoot, backupId, BackupInfoDTO.class);
            if (info == null)
                return Result.error("备份文件不存在");

            Path sqlFile = Path.of(info.getFilePath());
            if (!Files.exists(sqlFile))
                return Result.error("备份文件已丢失");

            restoreDatabaseFromSql(sqlFile);

            log.info("数据库恢复成功: backupId={}", backupId);
            return Result.success();
        } catch (Exception e) {
            log.error("恢复数据库失败", e);
            return Result.error("恢复数据库失败: " + e.getMessage());
        }
    }

    @Override
    public Result<BackupInfoDTO> downloadBackup(Long backupId) {
        log.info("下载备份: backupId={}", backupId);
        if (backupId == null)
            return Result.error("备份ID不能为空");

        try {
            BackupInfoDTO info = loadMetadata(backupRoot, backupId, BackupInfoDTO.class);
            if (info == null)
                return Result.error("备份文件不存在");
            if (!Files.exists(Path.of(info.getFilePath())))
                return Result.error("备份文件已丢失");
            return Result.success(info);
        } catch (Exception e) {
            log.error("下载备份失败", e);
            return Result.error("下载备份失败: " + e.getMessage());
        }
    }

    // ==================== 数据导出 ====================

    @Override
    public Result<ExportInfoDTO> exportUserData(Long userId) {
        log.info("导出用户数据: userId={}", userId);
        try {
            List<Map<String, Object>> data;
            if (userId != null) {
                data = jdbcTemplate.queryForList("SELECT * FROM users WHERE id = ?", userId);
            } else {
                data = jdbcTemplate.queryForList("SELECT * FROM users");
            }
            return buildExportResult(data, "user", "user_data");
        } catch (Exception e) {
            log.error("导出用户数据失败", e);
            return Result.error("导出用户数据失败: " + e.getMessage());
        }
    }

    @Override
    public Result<ExportInfoDTO> exportArticleData(Long categoryId) {
        log.info("导出文章数据: categoryId={}", categoryId);
        try {
            List<Map<String, Object>> data;
            if (categoryId != null) {
                data = jdbcTemplate.queryForList("SELECT * FROM articles WHERE category_id = ?", categoryId);
            } else {
                data = jdbcTemplate.queryForList("SELECT * FROM articles");
            }
            return buildExportResult(data, "article", "article_data");
        } catch (Exception e) {
            log.error("导出文章数据失败", e);
            return Result.error("导出文章数据失败: " + e.getMessage());
        }
    }

    @Override
    public Result<ExportInfoDTO> exportCommentData(Long articleId) {
        log.info("导出评论数据: articleId={}", articleId);
        try {
            List<Map<String, Object>> data;
            if (articleId != null) {
                data = jdbcTemplate.queryForList("SELECT * FROM comments WHERE article_id = ?", articleId);
            } else {
                data = jdbcTemplate.queryForList("SELECT * FROM comments");
            }
            return buildExportResult(data, "comment", "comment_data");
        } catch (Exception e) {
            log.error("导出评论数据失败", e);
            return Result.error("导出评论数据失败: " + e.getMessage());
        }
    }

    @Override
    public Result<List<ExportInfoDTO>> getExportFileList() {
        log.info("获取导出文件列表");
        try {
            List<ExportInfoDTO> list = loadAllMetadata(exportRoot, ExportInfoDTO.class);
            list.sort((a, b) -> {
                if (a.getCreateTime() == null || b.getCreateTime() == null)
                    return 0;
                return b.getCreateTime().compareTo(a.getCreateTime());
            });
            return Result.success(list);
        } catch (Exception e) {
            log.error("获取导出列表失败", e);
            return Result.error("获取导出列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> deleteExportFile(Long exportId) {
        log.info("删除导出文件: exportId={}", exportId);
        if (exportId == null)
            return Result.error("导出文件ID不能为空");

        try {
            ExportInfoDTO info = loadMetadata(exportRoot, exportId, ExportInfoDTO.class);
            if (info == null)
                return Result.error("导出文件不存在");

            if (info.getFilePath() != null) {
                Files.deleteIfExists(Path.of(info.getFilePath()));
            }
            deleteMetadataFile(exportRoot, exportId);

            log.info("导出文件删除成功: exportId={}", exportId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除导出文件失败", e);
            return Result.error("删除导出文件失败: " + e.getMessage());
        }
    }

    @Override
    public Result<ExportInfoDTO> downloadExportFile(Long exportId) {
        log.info("下载导出文件: exportId={}", exportId);
        if (exportId == null)
            return Result.error("导出ID不能为空");

        try {
            ExportInfoDTO info = loadMetadata(exportRoot, exportId, ExportInfoDTO.class);
            if (info == null)
                return Result.error("导出文件不存在");
            if (!Files.exists(Path.of(info.getFilePath())))
                return Result.error("导出文件已丢失");
            return Result.success(info);
        } catch (Exception e) {
            log.error("下载导出文件失败", e);
            return Result.error("下载导出文件失败: " + e.getMessage());
        }
    }

    // ==================== 核心：SQL 导出 ====================

    private void exportDatabaseToSql(Path outputFile) throws Exception {
        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
                BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {

            writer.write("-- Lumina Blog Database Backup\n");
            writer.write("-- Generated at: " + LocalDateTime.now() + "\n");
            writer.write("-- ============================================\n\n");
            writer.write("SET FOREIGN_KEY_CHECKS = 0;\n");
            writer.write("SET NAMES utf8mb4;\n\n");

            DatabaseMetaData metaData = conn.getMetaData();
            // 获取当前数据库名
            String catalog = conn.getCatalog();

            // 获取所有表名
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[] { "TABLE" })) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }

            log.info("开始导出 {} 张表", tables.size());

            for (String table : tables) {
                writer.write("-- -------------------------------------------\n");
                writer.write("-- Table: " + table + "\n");
                writer.write("-- -------------------------------------------\n\n");

                // 导出建表语句
                exportCreateTable(conn, table, writer);

                // 导出数据
                exportTableData(conn, table, writer);
            }

            writer.write("\nSET FOREIGN_KEY_CHECKS = 1;\n");
            writer.flush();
            log.info("SQL 导出完成: {}", outputFile.getFileName());
        }
    }

    private void exportCreateTable(Connection conn, String table, BufferedWriter writer) throws Exception {
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
            if (rs.next()) {
                String createSql = rs.getString(2);
                writer.write("DROP TABLE IF EXISTS `" + table + "`;\n");
                writer.write(createSql + ";\n\n");
            }
        }
    }

    private void exportTableData(Connection conn, String table, BufferedWriter writer) throws Exception {
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM `" + table + "`")) {

            ResultSetMetaData rsMeta = rs.getMetaData();
            int colCount = rsMeta.getColumnCount();

            // 构建列名
            StringBuilder colNames = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                if (i > 1)
                    colNames.append(", ");
                colNames.append("`").append(rsMeta.getColumnName(i)).append("`");
            }

            int rowCount = 0;
            while (rs.next()) {
                StringBuilder values = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1)
                        values.append(", ");
                    Object val = rs.getObject(i);
                    if (val == null) {
                        values.append("NULL");
                    } else if (val instanceof Number) {
                        values.append(val);
                    } else if (val instanceof byte[]) {
                        values.append("X'").append(bytesToHex((byte[]) val)).append("'");
                    } else {
                        values.append("'").append(escapeSQL(val.toString())).append("'");
                    }
                }
                writer.write("INSERT INTO `" + table + "` (" + colNames + ") VALUES (" + values + ");\n");
                rowCount++;
            }

            if (rowCount > 0) {
                writer.write("\n");
                log.debug("表 {} 导出 {} 行数据", table, rowCount);
            }
        }
    }

    // ==================== 核心：SQL 恢复 ====================

    private void restoreDatabaseFromSql(Path sqlFile) throws Exception {
        String content = Files.readString(sqlFile, StandardCharsets.UTF_8);

        // 按分号分割 SQL 语句，跳过注释和空行
        String[] statements = content.split(";\\s*\n");

        int executed = 0;
        for (String rawStmt : statements) {
            String stmt = rawStmt.trim();
            // 跳过注释和空语句
            if (stmt.isEmpty() || stmt.startsWith("--"))
                continue;

            // 去除多行语句中的注释行
            String cleanStmt = Arrays.stream(stmt.split("\n"))
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"))
                    .trim();

            if (cleanStmt.isEmpty())
                continue;

            try {
                jdbcTemplate.execute(cleanStmt);
                executed++;
            } catch (Exception e) {
                log.warn("执行 SQL 语句失败（已跳过）: {}", cleanStmt.substring(0, Math.min(100, cleanStmt.length())), e);
            }
        }
        log.info("SQL 恢复完成，共执行 {} 条语句", executed);
    }

    // ==================== 导出结果构建 ====================

    private Result<ExportInfoDTO> buildExportResult(List<Map<String, Object>> data, String exportType, String prefix)
            throws Exception {
        long exportId = idGenerator.incrementAndGet();
        String timestamp = LocalDateTime.now().format(FILE_DATE_FMT);
        String fileName = prefix + "_" + timestamp + ".json";
        Path jsonFile = exportRoot.resolve(fileName);

        // 写入 JSON
        objectMapper.writeValue(jsonFile.toFile(), data);
        long fileSize = Files.size(jsonFile);

        ExportInfoDTO info = new ExportInfoDTO();
        info.setExportId(exportId);
        info.setFileName(fileName);
        info.setFilePath(jsonFile.toAbsolutePath().toString());
        info.setFileSize(fileSize);
        info.setExportType(exportType);
        info.setRecordCount((long) data.size());
        info.setCreateTime(LocalDateTime.now());
        info.setStatus("success");

        // 保存元数据
        saveExportMetadata(exportId, info);

        log.info("数据导出成功: type={}, records={}, file={}", exportType, data.size(), fileName);
        return Result.success(info);
    }

    // ==================== 元数据持久化 ====================

    private <T> void saveMetadata(Path dir, long id, T data) throws IOException {
        Path metaFile = dir.resolve(id + META_SUFFIX);
        objectMapper.writeValue(metaFile.toFile(), data);
    }

    private void saveExportMetadata(long id, ExportInfoDTO data) throws IOException {
        saveMetadata(exportRoot, id, data);
    }

    private <T> T loadMetadata(Path dir, long id, Class<T> clazz) throws IOException {
        Path metaFile = dir.resolve(id + META_SUFFIX);
        if (!Files.exists(metaFile))
            return null;
        return objectMapper.readValue(metaFile.toFile(), clazz);
    }

    private <T> List<T> loadAllMetadata(Path dir, Class<T> clazz) throws IOException {
        List<T> result = new ArrayList<>();
        if (!Files.exists(dir))
            return result;

        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> metaFiles = stream
                    .filter(p -> p.getFileName().toString().endsWith(META_SUFFIX))
                    .collect(Collectors.toList());
            for (Path metaFile : metaFiles) {
                try {
                    result.add(objectMapper.readValue(metaFile.toFile(), clazz));
                } catch (Exception e) {
                    log.warn("读取元数据文件失败: {}", metaFile, e);
                }
            }
        }
        return result;
    }

    private void deleteMetadataFile(Path dir, long id) throws IOException {
        Files.deleteIfExists(dir.resolve(id + META_SUFFIX));
    }

    // ==================== 工具方法 ====================

    private static String escapeSQL(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\0", "");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}