package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 刷新令牌响应
 */
@Data
@Schema(description = "刷新令牌响应")
public class TokenRefreshResponseDTO {

    @Schema(description = "新的访问令牌")
    private String token;

    @Schema(description = "新的刷新令牌")
    private String refreshToken;

    public TokenRefreshResponseDTO() {
    }

    public TokenRefreshResponseDTO(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }
}
