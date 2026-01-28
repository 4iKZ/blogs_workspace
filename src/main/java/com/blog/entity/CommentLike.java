package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论点赞实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment_likes")
@Schema(description = "评论点赞实体")
public class CommentLike extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 点赞ID
     */
    @Schema(description = "点赞ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 评论ID
     */
    @Schema(description = "评论ID")
    @TableField("comment_id")
    private Long commentId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    public CommentLike() {
    }

    public CommentLike(Long commentId, Long userId) {
        this.commentId = commentId;
        this.userId = userId;
    }
}
