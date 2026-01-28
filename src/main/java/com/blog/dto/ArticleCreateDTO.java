package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 文章发布/编辑DTO
 */
@Data
@Schema(description = "文章发布/编辑DTO")
public class ArticleCreateDTO {

    @Schema(description = "文章标题")
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题不能超过200个字符")
    private String title;

    @Schema(description = "文章内容")
    @NotBlank(message = "文章内容不能为空")
    @Size(max = 50000, message = "文章内容不能超过50000个字符")
    private String content;

    @Schema(description = "文章摘要")
    @Size(max = 500, message = "文章摘要不能超过500个字符")
    private String summary;

    @Schema(description = "封面图片URL")
    @Size(max = 500, message = "封面图片URL不能超过500个字符")
    private String coverImage;

    @Schema(description = "文章状态：0-草稿，1-已发布")
    private Integer status;

    @Schema(description = "是否允许评论：0-不允许，1-允许")
    private Integer allowComment;

    @Schema(description = "分类ID")
    @NotNull(message = "文章分类不能为空")
    private Long categoryId;
    
    @Schema(description = "话题ID")
    private Long topicId;
    
    // 手动添加getter/setter方法以确保编译通过
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getCoverImage() {
        return coverImage;
    }
    
    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Integer getAllowComment() {
        return allowComment;
    }
    
    public void setAllowComment(Integer allowComment) {
        this.allowComment = allowComment;
    }
    
    public Long getCategoryId() {
        return categoryId;
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
}