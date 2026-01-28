package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
@Schema(description = "用户实体")
public class User extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    @TableField("username")
    private String username;

    /**
     * 邮箱地址
     */
    @Schema(description = "邮箱地址")
    @TableField("email")
    private String email;

    /**
     * 密码（BCrypt加密）
     */
    @Schema(description = "密码（BCrypt加密）")
    @TableField("password")
    @JsonIgnore
    private String password;

    /**
     * 昵称
     */
    @Schema(description = "昵称")
    @TableField("nickname")
    private String nickname;

    /**
     * 头像URL
     */
    @Schema(description = "头像URL")
    @TableField("avatar")
    private String avatar;

    /**
     * 电话号码
     */
    @Schema(description = "电话号码")
    @TableField
    private String phone;

    /**
     * 个人简介
     */
    @Schema(description = "个人简介")
    @TableField
    private String bio;

    /**
     * 个人网站
     */
    @Schema(description = "个人网站")
    @TableField
    private String website;

    /**
     * 职位
     */
    @Schema(description = "职位")
    @TableField
    private String position;

    /**
     * 公司/单位/学校
     */
    @Schema(description = "公司/单位/学校")
    @TableField
    private String company;

    /**
     * 粉丝数
     */
    @Schema(description = "粉丝数")
    @TableField
    private Integer followerCount;

    /**
     * 关注数
     */
    @Schema(description = "关注数")
    @TableField
    private Integer followingCount;

    /**
     * 状态：1-正常，2-禁用，3-删除
     */
    @Schema(description = "状态：1-正常，2-禁用，3-删除")
    @TableField
    private Integer status;

    /**
     * 角色：1-普通用户，2-管理员，3-超级管理员
     */
    @Schema(description = "角色：1-普通用户，2-管理员，3-超级管理员")
    @TableField
    private Integer role;

    /**
     * 最后登录时间
     */
    @Schema(description = "最后登录时间")
    @TableField
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @Schema(description = "最后登录IP")
    @TableField
    private String lastLoginIp;

}