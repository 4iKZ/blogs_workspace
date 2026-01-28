package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论通知实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment_notifications")
@Schema(description = "评论通知实体")
public class CommentNotification extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知ID
     */
    @Schema(description = "通知ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 评论ID
     */
    @Schema(description = "评论ID")
    @TableField("comment_id")
    private Long commentId;

    /**
     * 接收者ID
     */
    @Schema(description = "接收者ID")
    @TableField("receiver_id")
    private Long receiverId;

    /**
     * 通知类型：1-评论通知，2-回复通知，3-点赞通知
     */
    @Schema(description = "通知类型：1-评论通知，2-回复通知，3-点赞通知")
    @TableField("type")
    private Integer type;

    /**
     * 阅读状态：0-未读，1-已读
     */
    @Schema(description = "阅读状态：0-未读，1-已读")
    @TableField("read_status")
    private Integer readStatus;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public CommentNotification() {
    }

    public CommentNotification(Long commentId, Long receiverId, Integer type) {
        this.commentId = commentId;
        this.receiverId = receiverId;
        this.type = type;
        this.readStatus = 0; // 0-未读
    }
}
