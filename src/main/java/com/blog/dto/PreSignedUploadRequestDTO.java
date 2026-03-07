package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Data
@Schema(description = "预签名上传请求DTO")
public class PreSignedUploadRequestDTO {

    @Schema(description = "文件名称", example = "photo.jpg")
    @NotBlank(message = "文件名称不能为空")
    private String fileName;

    @Schema(description = "文件MIME类型", example = "image/jpeg")
    @NotBlank(message = "文件类型不能为空")
    private String contentType;

    @Schema(description = "文件大小（字节）", example = "2048576")
    @Positive(message = "文件大小必须为正数")
    private Long fileSize;
}
