package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 内容请求DTO
 * 用于敏感词检测等需要接收文本内容的API
 */
@Data
@Schema(description = "内容请求DTO")
public class ContentRequest {
    
    @NotBlank(message = "内容不能为空")
    @Schema(description = "文本内容")
    private String content;
    
    public ContentRequest() {
    }
    
    public ContentRequest(String content) {
        this.content = content;
    }
}
