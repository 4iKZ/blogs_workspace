package com.blog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访问趋势DTO
 */
@Data
@Schema(description = "访问趋势信息")
public class VisitTrendDTO {

    @Schema(description = "日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "访问量(PV)")
    private Long pageViews;

    @Schema(description = "访客数(UV)")
    private Long uniqueVisitors;

    @Schema(description = "新访客数")
    private Long newVisitors;

    @Schema(description = "老访客数")
    private Long returningVisitors;

    @Schema(description = "平均访问时长(分钟)")
    private Double averageVisitDuration;

    @Schema(description = "跳出率(%)")
    private Double bounceRate;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}