package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("visit_statistics")
@Schema(description = "网站访问统计")
public class WebsiteStatistics {
    
    @Schema(description = "统计ID")
    private Long id;
    
    @Schema(description = "统计日期")
    private LocalDate date;
    
    @Schema(description = "总访问量")
    private Integer totalVisits;
    
    @Schema(description = "独立访客数")
    private Integer uniqueVisitors;
    
    @Schema(description = "页面浏览量")
    private Integer pageViews;
    
    @Schema(description = "新用户数")
    private Integer newUsers;
    
    @Schema(description = "新文章数")
    private Integer newArticles;
    
    @Schema(description = "新评论数")
    private Integer newComments;
    
    @Schema(description = "今日访问量")
    private Long todayPageViews;
    
    @Schema(description = "今日独立访客数")
    private Long todayUniqueVisitors;
    
    @Schema(description = "昨日访问量")
    private Long yesterdayPageViews;
    
    @Schema(description = "昨日独立访客数")
    private Long yesterdayUniqueVisitors;
    
    @Schema(description = "平均访问时长")
    private Double averageVisitDuration;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}