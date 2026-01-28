package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息DTO
 */
@Data
@Schema(description = "用户信息")
public class UserDTO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "个人网站")
    private String website;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "公司/单位/学校")
    private String company;

    @Schema(description = "状态（0-禁用，1-启用）")
    private Integer status;

    @Schema(description = "角色（user-普通用户，admin-管理员）")
    private String role;

    @Schema(description = "注册时间")
    private LocalDateTime createTime;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "文章数量")
    private Integer articleCount;

    @Schema(description = "评论数量")
    private Integer commentCount;

    @Schema(description = "粉丝数")
    private Integer followerCount;

    @Schema(description = "关注数")
    private Integer followingCount;

    @Schema(description = "是否已关注")
    private Boolean isFollowed;

    @Schema(description = "JWT访问令牌")
    private String accessToken;

    @Schema(description = "JWT刷新令牌")
    private String refreshToken;

    // 添加缺失的setter方法
    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}