package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送注册邮箱验证码请求
 */
@Data
@Schema(description = "发送注册邮箱验证码请求")
public class SendRegisterCodeDTO {

    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "图形验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "图形验证码不能为空")
    private String captcha;

    @Schema(description = "图形验证码key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "图形验证码key不能为空")
    private String captchaKey;
}