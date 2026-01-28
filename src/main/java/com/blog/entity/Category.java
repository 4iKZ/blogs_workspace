package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("categories")
@Schema(description = "分类实体")
public class Category extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    @Schema(description = "分类名称")
    @TableField("name")
    private String name;

    /**
     * 分类描述
     */
    @Schema(description = "分类描述")
    @TableField("description")
    private String description;

    /**
     * 父分类ID，0表示顶级分类
     */
    @Schema(description = "父分类ID，0表示顶级分类")
    @TableField("parent_id")
    private Long parentId;

    /**
     * 排序序号
     */
    @Schema(description = "排序序号")
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 文章数量
     */
    @Schema(description = "文章数量")
    @TableField("article_count")
    private Integer articleCount;

    /**
     * 状态：1-正常，2-禁用
     */
    @Schema(description = "状态：1-正常，2-禁用")
    @TableField("status")
    private Integer status;
    
    // 排除逻辑删除字段，因为数据库表中没有该字段
    @TableField(exist = false)
    private Integer deleted;
    
    // 手动添加缺失的getter方法以确保编译通过
    public String getName() {
        return name;
    }
    
    public void setArticleCount(Integer articleCount) {
        this.articleCount = articleCount;
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
    
    public Integer getArticleCount() {
        return articleCount;
    }
}