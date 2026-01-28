package com.blog.dto;

import lombok.Data;
import java.util.List;

/**
 * 访问统计DTO
 */
@Data
public class VisitStatisticsDTO {
    /**
     * 总访问量
     */
    private Long totalVisits;

    /**
     * 总独立访客数
     */
    private Long totalUniqueVisitors;

    /**
     * 平均访问时长（秒）
     */
    private Double averageVisitDuration;

    /**
     * 跳出率（百分比）
     */
    private Double bounceRate;

    /**
     * 访问趋势数据
     */
    private List<VisitTrendDTO> visitTrends;
}