package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 文章浏览记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article_views")
@Schema(description = "文章浏览记录实体")
public class ArticleView extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 浏览记录ID
     */
    @Schema(description = "浏览记录ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章ID
     */
    @Schema(description = "文章ID")
    @TableField("article_id")
    private Long articleId;

    /**
     * 用户ID（未登录用户为0）
     */
    @Schema(description = "用户ID（未登录用户为0）")
    @TableField("user_id")
    private Long userId;

    /**
     * IP地址
     */
    @Schema(description = "IP地址")
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 用户代理
     */
    @Schema(description = "用户代理")
    @TableField("user_agent")
    private String userAgent;

    /**
     * 逻辑删除字段
     */
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Integer deleted;
}