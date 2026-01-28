package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文章管理DTO
 */
@Data
@Schema(description = "文章管理DTO")
public class ArticleManageDTO {

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章封面图片URL")
    private String coverImage;

    @Schema(description = "文章内容")
    private String content;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "作者头像URL")
    private String authorAvatar;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "标签列表（逗号分隔）")
    private String tags;

    @Schema(description = "文章状态：0-草稿，1-已发布，2-已下线")
    private Integer status;

    @Schema(description = "是否置顶：0-否，1-是")
    private Integer isTop;

    @Schema(description = "是否推荐：0-否，1-是")
    private Integer isRecommended;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "收藏数")
    private Integer favoriteCount;

    @Schema(description = "发布时间")
    private String publishTime;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;

    @Schema(description = "最后评论时间")
    private String lastCommentTime;
}