package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知DTO
 */
@Data
@Schema(description = "消息通知DTO")
public class NotificationDTO {

    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "接收通知的用户ID")
    private Long userId;

    @Schema(description = "触发通知的用户ID")
    private Long senderId;

    @Schema(description = "触发通知的用户昵称")
    private String senderNickname;

    @Schema(description = "触发通知的用户头像")
    private String senderAvatar;

    @Schema(description = "通知类型：1-文章点赞，2-文章评论，3-评论点赞，4-评论回复")
    private Integer type;

    @Schema(description = "通知类型名称")
    private String typeName;

    @Schema(description = "目标ID（文章ID或评论ID）")
    private Long targetId;

    @Schema(description = "目标类型：1-文章，2-评论")
    private Integer targetType;

    @Schema(description = "目标标题（文章标题或评论内容摘要）")
    private String targetTitle;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "是否已读：0-未读，1-已读")
    private Integer isRead;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
