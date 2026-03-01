package com.blog.controller;

import com.blog.common.Result;
import com.blog.dto.ImageConvertDTO;
import com.blog.dto.ImageMetadataDTO;
import com.blog.service.ImageProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图片处理控制器
 * 提供图片格式转换、元信息提取、压缩等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/image")
@Tag(name = "图片处理接口")
public class ImageController {

    @Autowired
    private ImageProcessingService imageProcessingService;

    @PostMapping("/metadata")
    @Operation(summary = "提取图片元信息")
    public Result<ImageMetadataDTO> extractMetadata(
        @Parameter(description = "图片文件")
        @RequestParam("file") MultipartFile file
    ) {
        log.info("提取图片元信息: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
        return imageProcessingService.extractMetadata(file);
    }

    @PostMapping("/convert")
    @Operation(summary = "转换图片格式",
        description = "支持转换为 JPG、PNG、WEBP、BMP 等格式")
    public Result<ImageConvertDTO> convertFormat(
        @Parameter(description = "图片文件")
        @RequestParam("file") MultipartFile file,
        @Parameter(description = "目标格式 (jpg/png/webp/bmp)")
        @RequestParam("format") String format,
        @Parameter(description = "压缩质量 (0.1-1.0，仅JPEG有效)")
        @RequestParam(value = "quality", required = false) Float quality
    ) {
        log.info("转换图片格式: {} -> {}, 质量: {}",
            file.getOriginalFilename(), format, quality);

        Result<ImageConvertDTO> result = imageProcessingService.convertFormat(file, format, quality);

        if (result.getCode() == 200 && result.getData() != null) {
            log.info("格式转换成功: {} -> {} (原始: {} bytes, 转换后: {} bytes, 压缩率: {}%)",
                file.getOriginalFilename(),
                format,
                result.getData().getOriginalSize(),
                result.getData().getConvertedSize(),
                result.getData().getCompressionRatio());
        }

        return result;
    }

    @PostMapping("/convert/download")
    @Operation(summary = "转换图片格式并下载",
        description = "转换后直接返回图片文件用于下载")
    public ResponseEntity<byte[]> convertAndDownload(
        @Parameter(description = "图片文件")
        @RequestParam("file") MultipartFile file,
        @Parameter(description = "目标格式 (jpg/png/webp/bmp)")
        @RequestParam("format") String format,
        @Parameter(description = "压缩质量 (0.1-1.0)")
        @RequestParam(value = "quality", required = false, defaultValue = "0.8") Float quality
    ) {
        log.info("转换并下载图片: {} -> {}", file.getOriginalFilename(), format);

        Result<ImageConvertDTO> result = imageProcessingService.convertFormat(file, format, quality);

        if (result.getCode() != 200 || result.getData() == null) {
            return ResponseEntity.badRequest().build();
        }

        ImageConvertDTO convertDTO = result.getData();

        // 构建文件名
        String originalName = file.getOriginalFilename();
        String baseName = originalName != null && originalName.contains(".")
            ? originalName.substring(0, originalName.lastIndexOf("."))
            : "image";
        String newFileName = baseName + "." + format;

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(convertDTO.getMimeType()));
        headers.setContentLength(convertDTO.getConvertedSize());
        headers.setContentDispositionFormData("attachment", newFileName);

        return new ResponseEntity<>(convertDTO.getImageData(), headers, HttpStatus.OK);
    }

    @PostMapping("/batch-convert")
    @Operation(summary = "批量转换图片格式")
    public Result<List<ImageConvertDTO>> batchConvertFormat(
        @Parameter(description = "图片文件列表")
        @RequestParam("files") List<MultipartFile> files,
        @Parameter(description = "目标格式 (jpg/png/webp/bmp)")
        @RequestParam("format") String format,
        @Parameter(description = "压缩质量 (0.1-1.0)")
        @RequestParam(value = "quality", required = false) Float quality
    ) {
        log.info("批量转换图片格式: {} 个文件 -> {}", files.size(), format);
        return imageProcessingService.batchConvertFormat(files, format, quality);
    }

    @GetMapping("/formats")
    @Operation(summary = "获取支持的输出格式列表")
    public Result<List<String>> getSupportedFormats() {
        return imageProcessingService.getSupportedFormats();
    }

    @PostMapping("/compress")
    @Operation(summary = "压缩图片",
        description = "按指定尺寸和质量压缩图片")
    public ResponseEntity<byte[]> compressImage(
        @Parameter(description = "图片文件")
        @RequestParam("file") MultipartFile file,
        @Parameter(description = "最大宽度（像素）")
        @RequestParam(value = "maxWidth", required = false, defaultValue = "2048") Integer maxWidth,
        @Parameter(description = "最大高度（像素）")
        @RequestParam(value = "maxHeight", required = false, defaultValue = "2048") Integer maxHeight,
        @Parameter(description = "压缩质量 (0.1-1.0)")
        @RequestParam(value = "quality", required = false, defaultValue = "0.8") Float quality
    ) {
        log.info("压缩图片: {}, 最大尺寸: {}x{}, 质量: {}",
            file.getOriginalFilename(), maxWidth, maxHeight, quality);

        Result<byte[]> result = imageProcessingService.compressImage(file, maxWidth, maxHeight, quality);

        if (result.getCode() != 200 || result.getData() == null) {
            return ResponseEntity.badRequest().build();
        }

        byte[] compressedData = result.getData();

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(compressedData.length);

        String fileName = file.getOriginalFilename();
        String newFileName = fileName != null ? "compressed_" + fileName : "compressed_image.jpg";
        headers.setContentDispositionFormData("attachment", newFileName);

        return new ResponseEntity<>(compressedData, headers, HttpStatus.OK);
    }

    @PostMapping("/validate")
    @Operation(summary = "验证图片文件")
    public Result<Boolean> validateImage(
        @Parameter(description = "图片文件")
        @RequestParam("file") MultipartFile file
    ) {
        log.info("验证图片文件: {}", file.getOriginalFilename());
        return imageProcessingService.validateImage(file);
    }
}
