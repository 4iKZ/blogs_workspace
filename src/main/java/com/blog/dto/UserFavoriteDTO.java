package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户收藏DTO
 */
@Data
@Schema(description = "用户收藏DTO")
public class UserFavoriteDTO {

    @Schema(description = "收藏ID")
    private Long favoriteId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "文章ID")
    @NotBlank(message = "文章ID不能为空")
    private Long articleId;

    @Schema(description = "收藏时间")
    private String createdAt;

    @Schema(description = "文章信息")
    private ArticleDTO article;
}