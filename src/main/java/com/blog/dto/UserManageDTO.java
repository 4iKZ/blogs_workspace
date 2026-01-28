package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户管理DTO
 */
@Data
@Schema(description = "用户管理DTO")
public class UserManageDTO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    private String username;

    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @Schema(description = "邮箱")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号不能超过20个字符")
    private String phone;

    @Schema(description = "头像URL")
    @Size(max = 500, message = "头像URL不能超过500个字符")
    private String avatar;

    @Schema(description = "用户状态：0-正常，1-禁用")
    private Integer status;

    @Schema(description = "用户角色：user-普通用户，admin-管理员")
    private String role;

    @Schema(description = "注册时间")
    private String registerTime;

    @Schema(description = "最后登录时间")
    private String lastLoginTime;

    @Schema(description = "文章数量")
    private Integer articleCount;

    @Schema(description = "评论数量")
    private Integer commentCount;

    @Schema(description = "收藏数量")
    private Integer favoriteCount;

    @Schema(description = "点赞数量")
    private Integer likeCount;

    @Schema(description = "个人简介")
    @Size(max = 500, message = "个人简介不能超过500个字符")
    private String bio;
}