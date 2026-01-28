package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 消息通知实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notifications")
@Schema(description = "消息通知实体")
public class Notification extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    @Schema(description = "通知ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 接收通知的用户ID
     */
    @Schema(description = "接收通知的用户ID")
    @TableField("user_id")
    private Long userId;

    /**
     * 触发通知的用户ID
     */
    @Schema(description = "触发通知的用户ID")
    @TableField("sender_id")
    private Long senderId;

    /**
     * 通知类型：1-文章点赞，2-文章评论，3-评论点赞，4-评论回复
     */
    @Schema(description = "通知类型：1-文章点赞，2-文章评论，3-评论点赞，4-评论回复")
    @TableField("type")
    private Integer type;

    /**
     * 目标ID（文章ID或评论ID）
     */
    @Schema(description = "目标ID（文章ID或评论ID）")
    @TableField("target_id")
    private Long targetId;

    /**
     * 目标类型：1-文章，2-评论
     */
    @Schema(description = "目标类型：1-文章，2-评论")
    @TableField("target_type")
    private Integer targetType;

    /**
     * 通知内容
     */
    @Schema(description = "通知内容")
    @TableField("content")
    private String content;

    /**
     * 是否已读：0-未读，1-已读
     */
    @Schema(description = "是否已读：0-未读，1-已读")
    @TableField("is_read")
    private Integer isRead;

    // 通知类型常量
    public static final int TYPE_ARTICLE_LIKE = 1;
    public static final int TYPE_ARTICLE_COMMENT = 2;
    public static final int TYPE_COMMENT_LIKE = 3;
    public static final int TYPE_COMMENT_REPLY = 4;
    public static final int TYPE_USER_FOLLOW = 5;

    // 目标类型常量
    public static final int TARGET_TYPE_ARTICLE = 1;
    public static final int TARGET_TYPE_COMMENT = 2;
    public static final int TARGET_TYPE_USER = 3;

    // 已读状态常量
    public static final int READ_STATUS_UNREAD = 0;
    public static final int READ_STATUS_READ = 1;
}
