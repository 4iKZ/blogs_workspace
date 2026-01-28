package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件上传配置DTO
 */
@Data
@Schema(description = "文件上传配置DTO")
public class FileUploadConfigDTO {

    @Schema(description = "上传文件最大大小（MB）")
    private Integer maxFileSize;

    @Schema(description = "允许上传的图片类型，逗号分隔")
    private String allowedImageTypes;

    @Schema(description = "允许上传的文件类型，逗号分隔")
    private String allowedFileTypes;

    @Schema(description = "图片上传路径")
    private String imageUploadPath;

    @Schema(description = "文件上传路径")
    private String fileUploadPath;

    @Schema(description = "是否启用本地存储：0-否，1-是")
    private Integer enableLocalStorage;

    @Schema(description = "是否启用OSS存储：0-否，1-是")
    private Integer enableOssStorage;

    @Schema(description = "OSS访问密钥")
    private String ossAccessKey;

    @Schema(description = "OSS密钥")
    private String ossSecretKey;

    @Schema(description = "OSS存储桶名称")
    private String ossBucketName;

    @Schema(description = "OSS访问域名")
    private String ossEndpoint;
}