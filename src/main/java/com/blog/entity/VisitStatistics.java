package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 访问统计实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visit_statistics")
@Schema(description = "访问统计实体")
public class VisitStatistics extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 统计ID
     */
    @Schema(description = "统计ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 日期（格式：yyyy-MM-dd）
     */
    @Schema(description = "日期（格式：yyyy-MM-dd）")
    @TableField("visit_date")
    private String visitDate;

    /**
     * 页面访问量（PV）
     */
    @Schema(description = "页面访问量（PV）")
    @TableField("page_views")
    private Long pageViews;

    /**
     * 独立访客数（UV）
     */
    @Schema(description = "独立访客数（UV）")
    @TableField("unique_visitors")
    private Long uniqueVisitors;

    /**
     * 新访客数
     */
    @Schema(description = "新访客数")
    @TableField("new_visitors")
    private Long newVisitors;

    /**
     * 跳出率（百分比）
     */
    @Schema(description = "跳出率（百分比）")
    @TableField("bounce_rate")
    private Double bounceRate;

    /**
     * 平均访问时长（秒）
     */
    @Schema(description = "平均访问时长（秒）")
    @TableField("avg_duration")
    private Long avgDuration;

    /**
     * 逻辑删除字段
     */
    private Integer deleted;
}