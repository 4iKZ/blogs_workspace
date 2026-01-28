package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 文件上传DTO
 */
@Data
@Schema(description = "文件上传DTO")
public class FileUploadDTO {

    @Schema(description = "文件名称")
    @NotBlank(message = "文件名称不能为空")
    @Size(max = 255, message = "文件名称不能超过255个字符")
    private String fileName;

    @Schema(description = "文件类型")
    @Size(max = 100, message = "文件类型不能超过100个字符")
    private String fileType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件MD5值")
    @Size(max = 32, message = "文件MD5值不能超过32个字符")
    private String fileMd5;

    @Schema(description = "文件URL")
    @Size(max = 500, message = "文件URL不能超过500个字符")
    private String fileUrl;

    @Schema(description = "文件存储路径")
    @Size(max = 500, message = "文件存储路径不能超过500个字符")
    private String filePath;
}