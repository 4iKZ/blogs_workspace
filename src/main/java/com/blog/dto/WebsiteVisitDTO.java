package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 网站访问统计DTO
 */
@Data
@Schema(description = "网站访问统计DTO")
public class WebsiteVisitDTO {

    @Schema(description = "日期")
    private String date;

    @Schema(description = "页面浏览量PV")
    private Long pageView;

    @Schema(description = "独立访客数UV")
    private Long uniqueVisitor;

    @Schema(description = "访问次数")
    private Long visitCount;

    @Schema(description = "平均访问时长（秒）")
    private Long avgVisitTime;

    @Schema(description = "跳出率（%）")
    private Double bounceRate;

    @Schema(description = "新访客数")
    private Long newVisitor;

    @Schema(description = "老访客数")
    private Long oldVisitor;
}