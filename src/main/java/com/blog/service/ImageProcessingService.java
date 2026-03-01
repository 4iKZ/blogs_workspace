package com.blog.service;

import com.blog.dto.ImageConvertDTO;
import com.blog.dto.ImageMetadataDTO;
import com.blog.common.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图片处理服务接口
 */
public interface ImageProcessingService {

    /**
     * 提取图片元信息
     */
    Result<ImageMetadataDTO> extractMetadata(MultipartFile file);

    /**
     * 转换图片格式
     */
    Result<ImageConvertDTO> convertFormat(MultipartFile file, String targetFormat, Float quality);

    /**
     * 批量转换图片格式
     */
    Result<List<ImageConvertDTO>> batchConvertFormat(
        List<MultipartFile> files,
        String targetFormat,
        Float quality
    );

    /**
     * 获取支持的输出格式列表
     */
    Result<List<String>> getSupportedFormats();

    /**
     * 压缩图片
     */
    Result<byte[]> compressImage(
        MultipartFile file,
        Integer maxWidth,
        Integer maxHeight,
        Float quality
    );

    /**
     * 验证图片文件
     */
    Result<Boolean> validateImage(MultipartFile file);
}
