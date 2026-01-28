package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * 搜索请求DTO
 */
@Data
@Schema(description = "搜索请求DTO")
public class SearchRequestDTO {

    @Schema(description = "搜索关键词")
    @Size(max = 50, message = "搜索关键词不能超过50个字符")
    private String keyword;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签ID列表")
    private Long[] tagIds;

    @Schema(description = "作者ID")
    private Long authorId;

    @Schema(description = "搜索范围：title-标题，content-内容，all-全部")
    private String searchScope = "all";

    @Schema(description = "排序方式：time-时间，relevance-相关性，view-浏览量")
    private String sortBy = "time";

    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Schema(description = "每页数量")
    private Integer pageSize = 10;

    @Schema(description = "开始日期，格式为YYYY-MM-DD")
    private String startDate;

    @Schema(description = "结束日期，格式为YYYY-MM-DD")
    private String endDate;
}