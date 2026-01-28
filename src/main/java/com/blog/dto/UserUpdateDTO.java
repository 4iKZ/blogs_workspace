package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户更新信息DTO
 *
 * 注意：所有字段都是可选的，null 表示不更新该字段
 * 空字符串表示要清空该字段
 */
@Data
@Schema(description = "用户更新信息")
public class UserUpdateDTO {

    @Schema(description = "昵称", nullable = true)
    @Size(max = 50, message = "昵称长度不能超过50位")
    private String nickname;

    @Schema(description = "邮箱", nullable = true)
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "手机号", nullable = true)
    private String phone;

    @Schema(description = "头像", nullable = true)
    private String avatar;

    @Schema(description = "个人简介", nullable = true)
    @Size(max = 500, message = "个人简介长度不能超过500位")
    private String bio;

    @Schema(description = "个人网站", nullable = true)
    @Size(max = 200, message = "个人网站长度不能超过200位")
    private String website;

    @Schema(description = "职位", nullable = true)
    @Size(max = 100, message = "职位长度不能超过100位")
    private String position;

    @Schema(description = "公司/单位/学校", nullable = true)
    @Size(max = 100, message = "公司/单位/学校长度不能超过100位")
    private String company;
}