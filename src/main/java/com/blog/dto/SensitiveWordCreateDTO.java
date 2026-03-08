package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 敏感词创建/编辑DTO
 */
@Data
@Schema(description = "敏感词创建/编辑DTO")
public class SensitiveWordCreateDTO {

    @Schema(description = "敏感词内容")
    @NotBlank(message = "敏感词内容不能为空")
    @Size(max = 50, message = "敏感词不能超过50个字符")
    private String word;

    @Schema(description = "分类")
    @Size(max = 20, message = "分类不能超过20个字符")
    private String category = "default";

    @Schema(description = "级别：1-警告，2-禁止")
    private Integer level = 1;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }
}
