package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 评论创建DTO
 */
@Data
@Schema(description = "评论创建DTO")
public class CommentCreateDTO {

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "父评论ID，0或null表示顶级评论（回复评论时使用）")
    private Long parentId;

    @Schema(description = "回复的目标评论ID（用于展示回复关系）")
    private Long replyToCommentId;

    @Schema(description = "评论内容")
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000个字符")
    private String content;

    @Schema(description = "评论者昵称（未登录时使用）")
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @Schema(description = "评论者邮箱（未登录时使用）")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;

    @Schema(description = "评论者网站（未登录时使用）")
    @Size(max = 200, message = "网站地址不能超过200个字符")
    private String website;

    @Schema(description = "用户ID（系统自动设置）")
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
