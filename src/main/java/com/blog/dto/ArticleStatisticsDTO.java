package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章统计DTO
 */
@Data
@Schema(description = "文章统计DTO")
public class ArticleStatisticsDTO {

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "收藏数")
    private Integer favoriteCount;

    @Schema(description = "分享数")
    private Integer shareCount;

    @Schema(description = "最后统计时间")
    private String lastStatisticsTime;
}