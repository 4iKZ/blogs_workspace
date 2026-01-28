package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 评论统计DTO
 */
@Data
@Schema(description = "评论统计DTO")
public class CommentStatisticsDTO {

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "总评论数")
    private Integer totalCount;

    @Schema(description = "待审核评论数")
    private Integer pendingCount;

    @Schema(description = "已通过评论数")
    private Integer approvedCount;

    @Schema(description = "已拒绝评论数")
    private Integer rejectedCount;

    @Schema(description = "今日新增评论数")
    private Integer todayCount;

    @Schema(description = "最后评论时间")
    private String lastCommentTime;
}