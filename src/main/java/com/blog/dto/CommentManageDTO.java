package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 评论管理DTO
 */
@Data
@Schema(description = "评论管理DTO")
public class CommentManageDTO {

    @Schema(description = "评论ID")
    private Long commentId;

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "文章标题")
    private String articleTitle;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论状态：0-待审核，1-已通过，2-已拒绝")
    private Integer status;

    @Schema(description = "评论人ID")
    private Long commenterId;

    @Schema(description = "评论人昵称")
    private String commenterName;

    @Schema(description = "评论人头像URL")
    private String commenterAvatar;

    @Schema(description = "评论人邮箱")
    private String commenterEmail;

    @Schema(description = "父评论ID（顶级评论为0）")
    private Long parentId;

    @Schema(description = "回复目标用户ID")
    private Long replyToUserId;

    @Schema(description = "回复目标用户昵称")
    private String replyToUserName;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "回复数")
    private Integer replyCount;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "用户代理")
    private String userAgent;

    @Schema(description = "评论时间")
    private String commentTime;

    @Schema(description = "审核时间")
    private String auditTime;

    @Schema(description = "审核人ID")
    private Long auditUserId;

    @Schema(description = "审核人昵称")
    private String auditUserName;
}