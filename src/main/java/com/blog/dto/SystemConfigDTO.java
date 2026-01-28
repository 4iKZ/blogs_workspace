package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 系统配置DTO
 */
@Data
@Schema(description = "系统配置DTO")
public class SystemConfigDTO {

    @Schema(description = "配置ID")
    private Long configId;

    @Schema(description = "配置键")
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    @Schema(description = "配置值")
    @NotBlank(message = "配置值不能为空")
    private String configValue;

    @Schema(description = "配置描述")
    private String description;

    @Schema(description = "配置类型：system-系统配置，website-网站配置，email-邮件配置，file-文件配置")
    private String configType;

    @Schema(description = "是否可编辑：0-否，1-是")
    private Integer isEditable;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;
}