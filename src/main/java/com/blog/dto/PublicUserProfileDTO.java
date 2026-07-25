package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "用户公开资料")
public class PublicUserProfileDTO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private String website;
    private String position;
    private String company;
    private String role;
    private LocalDateTime createTime;
    private Integer articleCount;
    private Integer commentCount;
    private Integer followerCount;
    private Integer followingCount;
    private Boolean isFollowed;
}
