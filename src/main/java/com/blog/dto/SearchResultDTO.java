package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 搜索结果DTO
 */
@Data
@Schema(description = "搜索结果DTO")
public class SearchResultDTO {

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章内容（包含高亮标签）")
    private String content;

    @Schema(description = "文章封面图片URL")
    private String coverImage;

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

    @Schema(description = "标签列表")
    private String[] tags;

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

    @Schema(description = "相关性评分")
    private Double relevanceScore;

    @Schema(description = "匹配字段：title-标题，content-内容，tag-标签，category-分类")
    private String matchedField;
}