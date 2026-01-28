package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 访客来源DTO
 */
@Data
@Schema(description = "访客来源DTO")
public class VisitorSourceDTO {

    @Schema(description = "来源类型：direct-直接访问，search-搜索引擎，social-社交媒体，referral-外部链接")
    private String sourceType;

    @Schema(description = "来源名称")
    private String sourceName;

    @Schema(description = "访问次数")
    private Long visitCount;

    @Schema(description = "独立访客数")
    private Long uniqueVisitor;

    @Schema(description = "占比（%）")
    private Double percentage;
}