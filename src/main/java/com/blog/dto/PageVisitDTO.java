package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 页面访问DTO
 */
@Data
@Schema(description = "页面访问DTO")
public class PageVisitDTO {

    @Schema(description = "页面URL")
    private String pageUrl;

    @Schema(description = "页面标题")
    private String pageTitle;

    @Schema(description = "访问次数")
    private Long visitCount;

    @Schema(description = "独立访客数")
    private Long uniqueVisitor;

    @Schema(description = "平均停留时间（秒）")
    private Long avgStayTime;

    @Schema(description = "跳出率（%）")
    private Double bounceRate;
}