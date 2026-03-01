package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ErrorDetailDTO;
import com.blog.dto.ImageConvertDTO;
import com.blog.dto.ImageMetadataDTO;
import com.blog.service.ImageProcessingService;
import com.blog.utils.ImageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 图片处理服务实现类
 */
@Slf4j
@Service
public class ImageProcessingServiceImpl implements ImageProcessingService {

    @Override
    public Result<ImageMetadataDTO> extractMetadata(MultipartFile file) {
        try {
            // 验证文件
            Result<Void> validation = validateImageFile(file);
            if (validation.getCode() != 200) {
                return Result.error(validation.getMessage());
            }

            // 提取元信息
            ImageMetadataDTO metadata = ImageProcessor.extractMetadata(file);
            log.info("成功提取图片元信息: {}x{}, {}",
                metadata.getWidth(), metadata.getHeight(), metadata.getFormat());

            return Result.success(metadata);

        } catch (IOException e) {
            log.error("提取图片元信息失败", e);
            ErrorDetailDTO errorDetail = ErrorDetailDTO.systemError(
                "提取图片元信息失败",
                e
            );
            errorDetail.setErrorCode("METADATA_EXTRACTION_FAILED");
            errorDetail.setDetail("无法读取图片文件，请确保文件格式正确且未损坏");
            return Result.error("提取图片元信息失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("不支持的图片格式", e);
            ErrorDetailDTO errorDetail = ErrorDetailDTO.businessError(
                "UNSUPPORTED_FORMAT",
                "不支持的图片格式",
                e.getMessage()
            );
            return Result.error("不支持的图片格式");
        }
    }

    @Override
    public Result<ImageConvertDTO> convertFormat(MultipartFile file, String targetFormat, Float quality) {
        try {
            // 验证文件
            Result<Void> validation = validateImageFile(file);
            if (validation.getCode() != 200) {
                return Result.error(validation.getMessage());
            }

            // 验证目标格式
            if (!ImageProcessor.isFormatSupported(targetFormat)) {
                List<String> supportedFormats = Arrays.stream(ImageProcessor.getSupportedOutputFormats())
                    .map(String::toUpperCase)
                    .collect(Collectors.toList());

                ErrorDetailDTO errorDetail = ErrorDetailDTO.businessError(
                    "UNSUPPORTED_TARGET_FORMAT",
                    String.format("不支持的目标格式: %s", targetFormat.toUpperCase()),
                    String.format("支持的格式: %s", String.join(", ", supportedFormats))
                );
                return Result.error(String.format("不支持的目标格式: %s", targetFormat));
            }

            // 转换格式
            ImageConvertDTO result = ImageProcessor.convertFormat(file, targetFormat, quality);

            log.info("成功转换图片格式: {} -> {}, 原始大小: {} bytes, 转换后: {} bytes, 压缩率: {}%",
                result.getOriginalFormat(),
                result.getTargetFormat(),
                result.getOriginalSize(),
                result.getConvertedSize(),
                result.getCompressionRatio());

            return Result.success(result);

        } catch (IOException e) {
            log.error("转换图片格式失败", e);
            ErrorDetailDTO errorDetail = ErrorDetailDTO.systemError(
                "转换图片格式失败",
                e
            );
            errorDetail.setErrorCode("FORMAT_CONVERSION_FAILED");
            return Result.error("转换图片格式失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("格式转换参数错误", e);
            return Result.error("参数错误: " + e.getMessage());
        }
    }

    @Override
    public Result<List<ImageConvertDTO>> batchConvertFormat(
        List<MultipartFile> files,
        String targetFormat,
        Float quality
    ) {
        List<ImageConvertDTO> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            try {
                Result<ImageConvertDTO> result = convertFormat(file, targetFormat, quality);
                if (result.getCode() == 200 && result.getData() != null) {
                    results.add(result.getData());
                } else {
                    errors.add(String.format("[%d] %s: %s", i + 1, file.getOriginalFilename(), result.getMessage()));
                }
            } catch (Exception e) {
                log.error("批量转换第{}个文件失败: {}", i + 1, file.getOriginalFilename(), e);
                errors.add(String.format("[%d] %s: %s", i + 1, file.getOriginalFilename(), e.getMessage()));
            }
        }

        if (results.isEmpty()) {
            return Result.error("批量转换失败: " + String.join("; ", errors));
        }

        log.info("批量转换完成: 成功 {} 个, 失败 {} 个", results.size(), errors.size());

        // 即使有部分失败，也返回成功的结果
        if (!errors.isEmpty()) {
            log.warn("部分文件转换失败: {}", String.join("; ", errors));
        }

        return Result.success(results);
    }

    @Override
    public Result<List<String>> getSupportedFormats() {
        String[] formats = ImageProcessor.getSupportedOutputFormats();
        List<String> formatList = Arrays.stream(formats)
            .map(String::toUpperCase)
            .collect(Collectors.toList());

        log.debug("返回支持的输出格式: {}", formatList);

        return Result.success(formatList);
    }

    @Override
    public Result<byte[]> compressImage(
        MultipartFile file,
        Integer maxWidth,
        Integer maxHeight,
        Float quality
    ) {
        try {
            // 验证文件
            Result<Void> validation = validateImageFile(file);
            if (validation.getCode() != 200) {
                return Result.error(validation.getMessage());
            }

            // 设置默认值
            int maxW = maxWidth != null ? maxWidth : 2048;
            int maxH = maxHeight != null ? maxHeight : 2048;
            float q = quality != null ? quality : 0.8f;

            // 压缩图片
            byte[] compressed = ImageProcessor.scaleAndCompress(file, maxW, maxH, q);

            log.info("成功压缩图片: 原始 {} bytes -> 压缩后 {} bytes",
                file.getSize(), compressed.length);

            return Result.success(compressed);

        } catch (IOException e) {
            log.error("压缩图片失败", e);
            ErrorDetailDTO errorDetail = ErrorDetailDTO.systemError(
                "压缩图片失败",
                e
            );
            errorDetail.setErrorCode("IMAGE_COMPRESSION_FAILED");
            return Result.error("压缩图片失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Boolean> validateImage(MultipartFile file) {
        try {
            boolean valid = ImageProcessor.isValidImage(file);

            if (!valid) {
                ErrorDetailDTO errorDetail = ErrorDetailDTO.validationError(
                    "file",
                    "无效的图片文件"
                );
                errorDetail.setErrorCode("INVALID_IMAGE_FILE");
                errorDetail.setDetail("请上传有效的图片文件（JPG、PNG、GIF、WEBP等格式）");
                return Result.error("无效的图片文件");
            }

            return Result.success(true);

        } catch (Exception e) {
            log.error("验证图片文件失败", e);
            return Result.error("验证图片文件失败: " + e.getMessage());
        }
    }

    /**
     * 验证图片文件
     */
    private Result<Void> validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            ErrorDetailDTO errorDetail = ErrorDetailDTO.validationError(
                "file",
                "文件不能为空"
            );
            errorDetail.setErrorCode("EMPTY_FILE");
            return Result.error("文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            ErrorDetailDTO errorDetail = ErrorDetailDTO.validationError(
                "file",
                "只允许上传图片文件"
            );
            errorDetail.setErrorCode("INVALID_CONTENT_TYPE");
            errorDetail.setDetail("文件MIME类型: " + contentType);
            return Result.error("只允许上传图片文件");
        }

        // 检查文件大小（限制为100MB）
        long maxSize = 100 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            ErrorDetailDTO errorDetail = ErrorDetailDTO.validationError(
                "file",
                String.format("文件大小不能超过%dMB", maxSize / 1024 / 1024)
            );
            errorDetail.setErrorCode("FILE_TOO_LARGE");
            errorDetail.setAdditionalInfo(new java.util.HashMap<String, Object>() {{
                put("fileSize", file.getSize());
                put("maxSize", maxSize);
            }});
            return Result.error(String.format("文件大小不能超过%dMB", maxSize / 1024 / 1024));
        }

        return Result.success(null);
    }

    /**
     * 生成请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
