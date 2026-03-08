package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.BackupInfoDTO;
import com.blog.dto.ExportInfoDTO;
import com.blog.service.DataBackupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 数据备份与恢复控制器
 */
@RestController
@RequestMapping("/api/system/backup")
@Tag(name = "数据备份与恢复管理")
public class DataBackupController {

    @Autowired
    private DataBackupService dataBackupService;

    @PostMapping("/database")
    @Operation(summary = "创建数据库备份")
    public Result<BackupInfoDTO> createDatabaseBackup(
            @Parameter(description = "备份名称") @RequestParam String backupName,
            @Parameter(description = "备份描述") @RequestParam(required = false) String description) {
        return dataBackupService.createDatabaseBackup(backupName, description);
    }

    @GetMapping("/list")
    @Operation(summary = "获取备份列表")
    public Result<List<BackupInfoDTO>> getBackupList() {
        return dataBackupService.getBackupList();
    }

    @DeleteMapping("/{backupId}")
    @Operation(summary = "删除备份")
    public Result<Void> deleteBackup(
            @Parameter(description = "备份ID") @PathVariable Long backupId) {
        return dataBackupService.deleteBackup(backupId);
    }

    @PostMapping("/restore/{backupId}")
    @Operation(summary = "恢复数据库")
    public Result<Void> restoreDatabase(
            @Parameter(description = "备份ID") @PathVariable Long backupId) {
        return dataBackupService.restoreDatabase(backupId);
    }

    @PostMapping("/export/user")
    @Operation(summary = "导出用户数据")
    public Result<ExportInfoDTO> exportUserData(
            @Parameter(description = "用户ID，可选") @RequestParam(required = false) Long userId) {
        return dataBackupService.exportUserData(userId);
    }

    @PostMapping("/export/article")
    @Operation(summary = "导出文章数据")
    public Result<ExportInfoDTO> exportArticleData(
            @Parameter(description = "分类ID，可选") @RequestParam(required = false) Long categoryId) {
        return dataBackupService.exportArticleData(categoryId);
    }

    @PostMapping("/export/comment")
    @Operation(summary = "导出评论数据")
    public Result<ExportInfoDTO> exportCommentData(
            @Parameter(description = "文章ID，可选") @RequestParam(required = false) Long articleId) {
        return dataBackupService.exportCommentData(articleId);
    }

    @GetMapping("/export/list")
    @Operation(summary = "获取导出文件列表")
    public Result<List<ExportInfoDTO>> getExportFileList() {
        return dataBackupService.getExportFileList();
    }

    @DeleteMapping("/export/{exportId}")
    @Operation(summary = "删除导出文件")
    public Result<Void> deleteExportFile(
            @Parameter(description = "导出ID") @PathVariable Long exportId) {
        return dataBackupService.deleteExportFile(exportId);
    }

    @GetMapping("/download/{backupId}")
    @Operation(summary = "下载备份文件")
    public ResponseEntity<Resource> downloadBackup(
            @Parameter(description = "备份ID") @PathVariable Long backupId) {
        Result<BackupInfoDTO> result = dataBackupService.downloadBackup(backupId);
        if (!result.isSuccess() || result.getData() == null) {
            return ResponseEntity.notFound().build();
        }
        BackupInfoDTO info = result.getData();
        return buildFileResponse(info.getFilePath(), info.getFileName());
    }

    @GetMapping("/export/download/{exportId}")
    @Operation(summary = "下载导出文件")
    public ResponseEntity<Resource> downloadExportFile(
            @Parameter(description = "导出ID") @PathVariable Long exportId) {
        Result<ExportInfoDTO> result = dataBackupService.downloadExportFile(exportId);
        if (!result.isSuccess() || result.getData() == null) {
            return ResponseEntity.notFound().build();
        }
        ExportInfoDTO info = result.getData();
        return buildFileResponse(info.getFilePath(), info.getFileName());
    }

    /**
     * 构建文件下载响应
     */
    private ResponseEntity<Resource> buildFileResponse(String filePath, String fileName) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(file);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .contentLength(file.length())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
