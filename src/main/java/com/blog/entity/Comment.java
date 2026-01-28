package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comments")
@Schema(description = "评论实体")
public class Comment extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @Schema(description = "评论ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章ID
     */
    @Schema(description = "文章ID")
    @TableField("article_id")
    private Long articleId;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @TableField("user_id")
    private Long userId;

    /**
     * 父评论ID，0表示顶级评论
     */
    @Schema(description = "父评论ID，0表示顶级评论")
    @TableField("parent_id")
    private Long parentId;

    /**
     * 回复的目标评论ID（用于展示“X 回复 Y”）
     */
    @Schema(description = "回复的目标评论ID")
    @TableField("reply_to_comment_id")
    private Long replyToCommentId;

    /**
     * 评论内容
     */
    @Schema(description = "评论内容")
    @TableField("content")
    private String content;

    /**
     * 点赞数量
     */
    @Schema(description = "点赞数量")
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 状态：1-待审核，2-已通过，3-已拒绝，4-已删除
     */
    @Schema(description = "状态：1-待审核，2-已通过，3-已拒绝，4-已删除")
    @TableField("status")
    private Integer status;

    /**
     * 逻辑删除字段
     */
    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;

    /**
     * 用户昵称（非数据库字段）
     */
    @Schema(description = "用户昵称")
    @TableField(exist = false)
    private String nickname;

    /**
     * 用户头像（非数据库字段）
     */
    @Schema(description = "用户头像")
    @TableField(exist = false)
    private String avatar;

    // 构造函数中设置deleted字段的默认值
    public Comment() {
        this.deleted = 0; // 未删除状态
    }

    public Long getArticleId() {
        return articleId;
    }
    
    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public Long getId() {
        return id;
    }
    
    public Integer getLikeCount() {
        return likeCount;
    }
}
