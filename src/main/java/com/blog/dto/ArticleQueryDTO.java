package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "文章查询条件")
public class ArticleQueryDTO {
    
    @Schema(description = "页码")
    private int page = 1;
    
    @Schema(description = "每页大小")
    private int size = 10;
    
    @Schema(description = "文章标题关键字")
    private String keyword;
    
    @Schema(description = "分类ID")
    private Long categoryId;
    
    @Schema(description = "标签")
    private String tag;
    
    @Schema(description = "状态：draft/published")
    private String status;
    
    @Schema(description = "作者ID")
    private Long authorId;
    
    @Schema(description = "开始日期")
    private String startDate;
    
    @Schema(description = "结束日期")
    private String endDate;
}