package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章摘要DTO（用于列表展示，不包含完整内容）
 */
@Data
@Schema(description = "文章摘要信息")
public class ArticleSummaryDTO {

    @Schema(description = "文章ID")
    @JsonProperty("id")
    private Long id;

    @Schema(description = "文章标题")
    @JsonProperty("title")
    private String title;

    @Schema(description = "文章摘要")
    @JsonProperty("summary")
    private String summary;

    @Schema(description = "封面图片")
    @JsonProperty("coverImage")
    private String coverImage;

    @Schema(description = "文章状态：0-草稿，1-待审核，2-已发布")
    @JsonProperty("status")
    private Integer status;

    @Schema(description = "浏览次数")
    @JsonProperty("viewCount")
    private Integer viewCount;

    @Schema(description = "点赞数")
    @JsonProperty("likeCount")
    private Integer likeCount;

    @Schema(description = "评论数")
    @JsonProperty("commentCount")
    private Integer commentCount;

    @Schema(description = "收藏数")
    @JsonProperty("favoriteCount")
    private Integer favoriteCount;

    @Schema(description = "作者ID")
    @JsonProperty("authorId")
    private Long authorId;

    @Schema(description = "作者昵称")
    @JsonProperty("authorNickname")
    private String authorNickname;

    @Schema(description = "作者头像")
    @JsonProperty("authorAvatar")
    private String authorAvatar;

    @Schema(description = "分类ID")
    @JsonProperty("categoryId")
    private Long categoryId;

    @Schema(description = "分类名称")
    @JsonProperty("categoryName")
    private String categoryName;

    @Schema(description = "是否置顶")
    @JsonProperty("isTop")
    private Integer isTop;

    @Schema(description = "是否推荐")
    @JsonProperty("isRecommended")
    private Integer isRecommended;

    @Schema(description = "是否已点赞（当前用户）")
    @JsonProperty("liked")
    private Boolean liked;

    @Schema(description = "是否已收藏（当前用户）")
    @JsonProperty("favorited")
    private Boolean favorited;

    @Schema(description = "发布时间")
    @JsonProperty("publishTime")
    private LocalDateTime publishTime;

    @Schema(description = "创建时间")
    @JsonProperty("createTime")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonProperty("updateTime")
    private LocalDateTime updateTime;
}
