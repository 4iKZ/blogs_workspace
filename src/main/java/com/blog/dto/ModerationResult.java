package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * AI内容审核结果DTO
 */
@Data
@Schema(description = "AI内容审核结果")
public class ModerationResult {

    @Schema(description = "是否通过审核")
    private boolean passed;

    @Schema(description = "违规类型: none/violence/hate/porn/politics/fake/ad/other")
    private String violationType;

    @Schema(description = "具体违规原因列表")
    private List<String> reasons;

    @Schema(description = "审核置信度 (0-1)")
    private double confidence;

    @Schema(description = "审核建议")
    private String suggestion;

    public ModerationResult() {
    }

    public ModerationResult(boolean passed, String violationType, List<String> reasons, double confidence, String suggestion) {
        this.passed = passed;
        this.violationType = violationType;
        this.reasons = reasons;
        this.confidence = confidence;
        this.suggestion = suggestion;
    }

    /**
     * 创建通过结果
     */
    public static ModerationResult pass() {
        return new ModerationResult(true, "none", List.of(), 1.0, null);
    }

    /**
     * 创建未通过结果
     */
    public static ModerationResult fail(String violationType, List<String> reasons, double confidence, String suggestion) {
        return new ModerationResult(false, violationType, reasons, confidence, suggestion);
    }
}
