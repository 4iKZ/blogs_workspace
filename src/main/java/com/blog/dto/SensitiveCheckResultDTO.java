package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 敏感词检测结果DTO
 */
@Data
@Schema(description = "敏感词检测结果")
public class SensitiveCheckResultDTO {

    @Schema(description = "是否通过（无敏感词）")
    private boolean passed;

    @Schema(description = "命中的敏感词列表")
    private List<String> hitWords;

    public SensitiveCheckResultDTO() {
    }

    public SensitiveCheckResultDTO(boolean passed, List<String> hitWords) {
        this.passed = passed;
        this.hitWords = hitWords;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public List<String> getHitWords() {
        return hitWords;
    }

    public void setHitWords(List<String> hitWords) {
        this.hitWords = hitWords;
    }

    /**
     * 创建通过结果
     */
    public static SensitiveCheckResultDTO pass() {
        return new SensitiveCheckResultDTO(true, List.of());
    }

    /**
     * 创建未通过结果
     */
    public static SensitiveCheckResultDTO fail(List<String> hitWords) {
        return new SensitiveCheckResultDTO(false, hitWords);
    }
}
