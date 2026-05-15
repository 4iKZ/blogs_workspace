package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 文章审核记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article_moderation_log")
@Schema(description = "文章审核记录实体")
public class ArticleModerationLog extends BaseEntity {

    /**
     * 记录ID
     */
    @Schema(description = "记录ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章ID
     */
    @Schema(description = "文章ID")
    @TableField("article_id")
    private Long articleId;

    /**
     * 审核时文章标题
     */
    @Schema(description = "审核时文章标题")
    @TableField("title")
    private String title;

    /**
     * 审核时文章内容摘要
     */
    @Schema(description = "审核时文章内容摘要")
    @TableField("content")
    private String content;

    /**
     * 是否通过：0-未通过，1-通过
     */
    @Schema(description = "是否通过：0-未通过，1-通过")
    @TableField("passed")
    private Integer passed;

    /**
     * 违规类型
     */
    @Schema(description = "违规类型")
    @TableField("violation_type")
    private String violationType;

    /**
     * 违规原因（JSON数组格式存储）
     */
    @Schema(description = "违规原因")
    @TableField("reasons")
    private String reasons;

    /**
     * 置信度
     */
    @Schema(description = "置信度")
    @TableField("confidence")
    private Double confidence;

    /**
     * 审核时间
     */
    @Schema(description = "审核时间")
    @TableField("check_time")
    private LocalDateTime checkTime;

    // 审核状态常量
    public static final int PASSED = 1;
    public static final int NOT_PASSED = 0;
}
