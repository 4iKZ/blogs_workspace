package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户点赞实体类
 */
@TableName("user_likes")
@Schema(description = "用户点赞实体")
public class UserLike implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 点赞ID
     */
    @Schema(description = "点赞ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

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
     * 创建时间
     */
    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取文章ID（兼容方法，返回targetId）
     * @return 文章ID
     */
    public Long getArticleId() {
        return targetId;
    }

    /**
     * 设置文章ID（兼容方法，自动设置targetType为1）
     * 注意：使用此方法会覆盖targetType为1（文章类型）
     * @param articleId 文章ID
     */
    public void setArticleId(Long articleId) {
        this.targetId = articleId;
        this.targetType = 1; // 1表示文章
    }

    /**
     * 检查是否为文章类型的点赞
     * @return 是否为文章类型
     */
    public boolean isArticleLike() {
        return targetType != null && targetType == 1;
    }

    /**
     * 检查是否为评论类型的点赞
     * @return 是否为评论类型
     */
    public boolean isCommentLike() {
        return targetType != null && targetType == 2;
    }
}