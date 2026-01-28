package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 分类创建/编辑DTO
 */
@Data
@Schema(description = "分类创建/编辑DTO")
public class CategoryCreateDTO {

    @Schema(description = "分类名称")
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过50个字符")
    private String name;

    @Schema(description = "分类描述")
    @Size(max = 500, message = "分类描述不能超过500个字符")
    private String description;

    @Schema(description = "排序值")
    private Integer sortOrder;

    public String getName() {
        return name;
    }
}