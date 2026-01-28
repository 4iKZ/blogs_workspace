package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 搜索统计DTO
 */
@Data
@Schema(description = "搜索统计DTO")
public class SearchStatisticsDTO {

    @Schema(description = "总搜索结果数")
    private Long totalResults;

    @Schema(description = "搜索耗时（毫秒）")
    private Long searchTime;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "相关分类统计")
    private CategoryCountDTO[] categoryCounts;

    @Schema(description = "相关标签统计")
    private TagCountDTO[] tagCounts;

    @Schema(description = "时间分布统计")
    private TimeCountDTO[] timeCounts;

    /**
     * 分类统计DTO
     */
    @Data
    public static class CategoryCountDTO {
        @Schema(description = "分类ID")
        private Long categoryId;
        
        @Schema(description = "分类名称")
        private String categoryName;
        
        @Schema(description = "匹配数量")
        private Long count;
    }

    /**
     * 标签统计DTO
     */
    @Data
    public static class TagCountDTO {
        @Schema(description = "标签ID")
        private Long tagId;
        
        @Schema(description = "标签名称")
        private String tagName;
        
        @Schema(description = "匹配数量")
        private Long count;
    }

    /**
     * 时间统计DTO
     */
    @Data
    public static class TimeCountDTO {
        @Schema(description = "时间段")
        private String timeRange;
        
        @Schema(description = "匹配数量")
        private Long count;
    }
}