package com.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visit_statistics")
@Schema(description = "访问统计实体（每日聚合）")
public class VisitStatistics extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "统计ID")
    private Long id;

    @TableField("`date`")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "统计日期")
    private LocalDate date;

    @TableField("total_visits")
    @Schema(description = "总访问次数")
    private Integer totalVisits;

    @TableField("unique_visitors")
    @Schema(description = "独立访客数（IP去重）")
    private Integer uniqueVisitors;

    @TableField("page_views")
    @Schema(description = "页面浏览量")
    private Integer pageViews;

    @TableField("new_users")
    @Schema(description = "新注册用户数")
    private Integer newUsers;

    @TableField("new_articles")
    @Schema(description = "新发布文章数")
    private Integer newArticles;

    @TableField("new_comments")
    @Schema(description = "新评论数")
    private Integer newComments;
}
