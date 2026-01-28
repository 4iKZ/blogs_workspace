package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 分类信息DTO
 */
@Data
@Schema(description = "分类信息DTO")
public class CategoryDTO {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "分类描述")
    private String description;

    @Schema(description = "排序值")
    private Integer sortOrder;

    @Schema(description = "关联文章数量")
    private Long articleCount;
}