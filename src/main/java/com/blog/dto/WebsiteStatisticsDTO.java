package com.blog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 网站访问统计DTO
 */
@Data
@Schema(description = "网站访问统计信息")
public class WebsiteStatisticsDTO {

    @Schema(description = "总访问量(PV)")
    private Long totalPageViews;

    @Schema(description = "总访客数(UV)")
    private Long totalUniqueVisitors;

    @Schema(description = "今日访问量")
    private Long todayPageViews;

    @Schema(description = "今日访客数")
    private Long todayUniqueVisitors;

    @Schema(description = "昨日访问量")
    private Long yesterdayPageViews;

    @Schema(description = "昨日访客数")
    private Long yesterdayUniqueVisitors;

    @Schema(description = "平均访问时长(分钟)")
    private Double averageVisitDuration;

    @Schema(description = "跳出率(%)")
    private Double bounceRate;

    @Schema(description = "统计日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime statisticsDate;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}