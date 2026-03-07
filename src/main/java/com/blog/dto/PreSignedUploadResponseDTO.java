package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预签名上传响应DTO")
public class PreSignedUploadResponseDTO {

    @Schema(description = "预签名PUT URL，前端直接用此URL上传文件到TOS")
    private String signedUrl;

    @Schema(description = "上传成功后的公开访问URL（已带lumina样式参数）")
    private String publicUrl;

    @Schema(description = "TOS对象Key")
    private String objectKey;

    @Schema(description = "签名URL有效期（秒）")
    private long expiresIn;
}
