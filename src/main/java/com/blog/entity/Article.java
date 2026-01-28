package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("articles")
@Schema(description = "文章实体")
public class Article extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    @Schema(description = "文章ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章标题
     */
    @Schema(description = "文章标题")
    @TableField("title")
    private String title;

    /**
     * 文章摘要
     */
    @Schema(description = "文章摘要")
    @TableField("summary")
    private String summary;

    /**
     * 文章内容（Markdown格式）
     */
    @Schema(description = "文章内容（Markdown格式）")
    @TableField("content")
    private String content;

    /**
     * 文章内容（HTML格式）
     */
    @Schema(description = "文章内容（HTML格式）")
    @TableField(exist = false)
    private String contentHtml;

    /**
     * 文章封面图片URL
     */
    @Schema(description = "文章封面图片URL")
    @TableField("cover_image")
    private String coverImage;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    @TableField("category_id")
    private Long categoryId;

    /**
     * 话题ID
     */
    @Schema(description = "话题ID")
    @TableField("topic_id")
    private Long topicId;

    /**
     * 作者ID
     */
    @Schema(description = "作者ID")
    @TableField("author_id")
    private Long authorId;

    /**
     * 状态：1-草稿，2-已发布，3-已下线
     */
    @Schema(description = "状态：1-草稿，2-已发布，3-已下线")
    @TableField("status")
    private Integer status;

    /**
     * 是否置顶：1-否，2-是
     */
    @Schema(description = "是否置顶：1-否，2-是")
    @TableField("is_top")
    private Integer isTop;

    /**
     * 是否推荐：1-否，2-是
     */
    @Schema(description = "是否推荐：1-否，2-是")
    @TableField("is_recommend")
    private Integer isRecommended;

    /**
     * 是否允许评论：0-不允许，1-允许
     */
    @Schema(description = "是否允许评论：0-不允许，1-允许")
    @TableField(exist = false)
    private Integer allowComment;

    /**
     * 浏览次数
     */
    @Schema(description = "浏览次数")
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 点赞次数
     */
    @Schema(description = "点赞次数")
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 评论次数
     */
    @Schema(description = "评论次数")
    @TableField("comment_count")
    private Integer commentCount;

    /**
     * 收藏次数
     */
    @Schema(description = "收藏次数")
    @TableField("favorite_count")
    private Integer favoriteCount;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间")
    @TableField("publish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTime;

    /**
     * 逻辑删除字段
     */
    @TableField(exist = false)
    private Integer deleted;
    
    // 手动添加缺失的setter/getter方法以确保编译通过
    public void setAllowComment(Integer allowComment) {
        this.allowComment = allowComment;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
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

    public Long getAuthorId() {
        return authorId;
    }

    public void setCommentCount(Integer commentCount) {
        this.commentCount = commentCount;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSummary() {
        return summary;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Integer getStatus() {
        return status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Integer getIsTop() {
        return isTop;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public Integer getCommentCount() {
        return commentCount;
    }

    public Integer getIsRecommended() {
        return isRecommended;
    }
}