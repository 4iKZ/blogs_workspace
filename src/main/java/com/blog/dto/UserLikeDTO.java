package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户点赞DTO
 */
@Data
@Schema(description = "用户点赞DTO")
public class UserLikeDTO {

    @Schema(description = "点赞ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "文章ID")
    @NotBlank(message = "文章ID不能为空")
    private Long articleId;

    @Schema(description = "点赞时间")
    private String createdAt;

    @Schema(description = "文章信息")
    private ArticleDTO article;
}