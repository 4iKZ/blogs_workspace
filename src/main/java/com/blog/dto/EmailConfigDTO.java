package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 邮件配置DTO
 */
@Data
@Schema(description = "邮件配置DTO")
public class EmailConfigDTO {

    @Schema(description = "SMTP服务器地址")
    private String smtpHost;

    @Schema(description = "SMTP服务器端口")
    private Integer smtpPort;

    @Schema(description = "SMTP用户名")
    private String smtpUsername;

    @Schema(description = "SMTP密码")
    private String smtpPassword;

    @Schema(description = "是否启用SSL：0-否，1-是")
    private Integer enableSsl;

    @Schema(description = "发件人邮箱")
    private String fromEmail;

    @Schema(description = "发件人名称")
    private String fromName;

    @Schema(description = "是否启用邮件功能：0-否，1-是")
    private Integer emailEnabled;
}