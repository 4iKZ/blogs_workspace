package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.FileInfoDTO;
import com.blog.dto.FileUploadDTO;
import com.blog.service.FileUploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 文件上传管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件上传管理接口")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/upload/image")
    @Operation(summary = "上传图片")
    public Result<String> uploadImage(@Parameter(description = "图片文件") @RequestParam("file") MultipartFile file) {
        return fileUploadService.uploadImage(file);
    }

    @PostMapping("/upload/file")
    @Operation(summary = "上传文件")
    public Result<FileInfoDTO> uploadFile(@Parameter(description = "文件") @RequestParam("file") MultipartFile file) {
        return fileUploadService.uploadFile(file);
    }

    @PostMapping("/upload/batch")
    @Operation(summary = "批量上传文件")
    public Result<List<FileInfoDTO>> batchUploadFiles(@Parameter(description = "文件列表") @RequestParam("files") List<MultipartFile> files) {
        return fileUploadService.batchUploadFiles(files);
    }

    @GetMapping("/list")
    @Operation(summary = "获取文件列表")
    public Result<List<FileInfoDTO>> getFileList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "文件类型") @RequestParam(required = false) String fileType) {
        return fileUploadService.getFileList(page, size, fileType);
    }

    @GetMapping("/{fileId}")
    @Operation(summary = "获取文件详情")
    public Result<FileInfoDTO> getFileInfo(@Parameter(description = "文件ID") @PathVariable Long fileId) {
        return fileUploadService.getFileById(fileId);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "删除文件")
    public Result<Void> deleteFile(@Parameter(description = "文件ID") @PathVariable Long fileId) {
        return fileUploadService.deleteFile(fileId);
    }

    @GetMapping("/check/md5/{md5}")
    @Operation(summary = "检查MD5文件是否存在")
    public Result<FileInfoDTO> checkFileByMd5(@Parameter(description = "文件MD5值") @PathVariable String md5) {
        return fileUploadService.checkFileExists(md5);
    }
}
