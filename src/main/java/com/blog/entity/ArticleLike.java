package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@TableName("article_like")
@Schema(description = "文章点赞实体")
public class ArticleLike {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "点赞ID")
    private Long id;
    
    @Schema(description = "文章ID")
    private Long articleId;
    
    @Setter
    @Schema(description = "用户ID")
    private Long userId;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "逻辑删除标志")
    private Integer deleted;
    
    // 手动添加缺失的getter/setter方法以确保编译通过
    public Long getArticleId() {
        return articleId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}