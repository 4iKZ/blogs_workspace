package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论DTO
 */
@Data
@Schema(description = "评论DTO")
public class CommentDTO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "父评论ID，0表示顶级评论")
    private Long parentId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论者用户ID")
    private Long userId;

    @Schema(description = "评论者昵称")
    private String nickname;

    @Schema(description = "评论者邮箱")
    private String email;

    @Schema(description = "评论者网站")
    private String website;

    @Schema(description = "评论者头像")
    private String avatar;

    @Schema(description = "评论状态：0-待审核，1-已通过，2-已拒绝")
    private Integer status;

    @Schema(description = "点赞数量")
    private Integer likeCount;

    @Schema(description = "是否点赞")
    private Boolean liked;

    @Schema(description = "回复数量")
    private Integer replyCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "子评论列表")
    private List<CommentDTO> children;
    
    @Schema(description = "回复的目标评论ID")
    private Long replyToCommentId;

    @Schema(description = "回复目标用户ID")
    private Long replyToUserId;

    @Schema(description = "回复目标昵称")
    private String replyToNickname;
    
    public Long getParentId() {
        return parentId;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setChildren(List<CommentDTO> children) {
        this.children = children;
    }
}
