package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录DTO
 */
@Data
@Schema(description = "用户登录信息")
public class UserLoginDTO {

    @Schema(description = "用户名/邮箱/手机号")
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "验证码")
    private String captcha;

    @Schema(description = "验证码key")
    private String captchaKey;

    @Schema(description = "记住我")
    private Boolean rememberMe = false;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCaptcha() {
        return captcha;
    }

    public String getCaptchaKey() {
        return captchaKey;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }
}