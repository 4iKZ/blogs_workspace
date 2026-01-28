package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册DTO
 */
@Data
@Schema(description = "用户注册信息")
public class UserRegisterDTO {

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20位之间")
    private String username;

    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    private String password;

    @Schema(description = "确认密码")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Schema(description = "邮箱")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称长度不能超过50位")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "公司/单位/学校")
    private String company;

    @Schema(description = "验证码")
    @NotBlank(message = "验证码不能为空")
    private String captcha;

    @Schema(description = "验证码key")
    @NotBlank(message = "验证码key不能为空")
    private String captchaKey;

    public String getUsername() {
        return username;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public void setCaptchaKey(String captchaKey) {
        this.captchaKey = captchaKey;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getBio() {
        return bio;
    }

    public String getPosition() {
        return position;
    }

    public String getCompany() {
        return company;
    }

    public String getCaptcha() {
        return captcha;
    }

    public String getCaptchaKey() {
        return captchaKey;
    }
}