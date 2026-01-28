package com.blog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章信息DTO
 */
@Data
@Schema(description = "文章信息DTO")
public class ArticleDTO {

    @Schema(description = "文章ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章内容")
    private String content;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "封面图片URL")
    private String coverImage;

    @Schema(description = "文章状态：0-草稿，1-已发布")
    private Integer status;

    @Schema(description = "是否允许评论：0-不允许，1-允许")
    private Integer allowComment;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "收藏数")
    private Integer favoriteCount;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "作者昵称")
    private String authorNickname;

    @Schema(description = "作者头像")
    private String authorAvatar;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "分类信息")
    private CategoryDTO category;

    @Schema(description = "是否已点赞")
    private Boolean liked;

    @Schema(description = "是否已收藏")
    private Boolean favorited;

    @Schema(description = "热度分数（排行榜用）")
    private Double hotScore;

    // 手动添加缺失的setter方法以确保编译通过
    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public void setCategory(CategoryDTO category) {
        this.category = category;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setLiked(Boolean liked) {
        this.liked = liked;
    }

    public void setFavorited(Boolean favorited) {
        this.favorited = favorited;
    }

    public void setAuthorNickname(String authorNickname) {
        this.authorNickname = authorNickname;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public Double getHotScore() {
        return hotScore;
    }

    public void setHotScore(Double hotScore) {
        this.hotScore = hotScore;
    }
}