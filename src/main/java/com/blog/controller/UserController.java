package com.blog.controller;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.exception.BusinessException;
import com.blog.dto.UserDTO;
import com.blog.dto.UserLoginDTO;
import com.blog.dto.UserRegisterDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.dto.ChangePasswordDTO;
import com.blog.service.TOSService;
import com.blog.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理接口")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TOSService tosService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        Result<String> result = userService.register(registerDTO);
        try {
            log.info("Register Response JSON: {}", objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.warn("Failed to log register response", e);
        }
        return result;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<UserDTO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        return userService.login(loginDTO);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout() {
        // 从请求属性中获取用户ID
        Long userId = getCurrentUserId();
        return userService.logout(userId);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "刷新JWT令牌")
    public Result<String> refreshToken(
            @Parameter(description = "刷新令牌") @RequestParam String refreshToken) {
        return userService.refreshToken(refreshToken);
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息")
    public Result<UserDTO> getUserInfo() {
        Long userId = getCurrentUserId();
        return userService.getUserInfo(userId);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取指定用户公开信息")
    public Result<UserDTO> getPublicUserInfo(@Parameter(description = "用户ID") @PathVariable Long userId) {
        return userService.getPublicUserInfo(userId);
    }

    @PutMapping("/info")
    @Operation(summary = "更新用户信息")
    public Result<Void> updateUserInfo(@Valid @RequestBody UserUpdateDTO updateDTO) {
        Long userId = getCurrentUserId();
        return userService.updateUserInfo(userId, updateDTO);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        Long userId = getCurrentUserId();
        return userService.changePassword(userId, changePasswordDTO);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码")
    public Result<Void> resetPassword(
            @Parameter(description = "邮箱地址") @RequestParam String email,
            @Parameter(description = "新密码") @RequestParam String newPassword) {
        return userService.resetPassword(email, newPassword);
    }

    // 管理员接口
    @GetMapping("/admin/list")
    @Operation(summary = "获取用户列表（管理员）")
    public Result<List<UserDTO>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {
        return userService.getUserList(page, size, keyword);
    }

    @PutMapping("/admin/status/{userId}")
    @Operation(summary = "更新用户状态（管理员）")
    public Result<Void> updateUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "状态：1-启用，0-禁用") @RequestParam Integer status) {
        return userService.updateUserStatus(userId, status);
    }

    @DeleteMapping("/admin/{userId}")
    @Operation(summary = "删除用户（管理员）")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        return userService.deleteUser(userId);
    }

    @PostMapping("/follow/{followingId}")
    @Operation(summary = "关注用户")
    public Result<Void> followUser(@Parameter(description = "被关注者ID") @PathVariable Long followingId) {
        Long followerId = getCurrentUserId();
        return userService.follow(followerId, followingId);
    }

    @DeleteMapping("/unfollow/{followingId}")
    @Operation(summary = "取消关注用户")
    public Result<Void> unfollowUser(@Parameter(description = "被关注者ID") @PathVariable Long followingId) {
        Long followerId = getCurrentUserId();
        return userService.unfollow(followerId, followingId);
    }

    @GetMapping("/is-following/{followingId}")
    @Operation(summary = "检查是否关注")
    public Result<Boolean> isFollowing(@Parameter(description = "被关注者ID") @PathVariable Long followingId) {
        Long followerId = getCurrentUserId();
        return userService.isFollowing(followerId, followingId);
    }

    @GetMapping("/top-authors")
    @Operation(summary = "获取作者排行榜")
    public Result<List<UserDTO>> getTopAuthors(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        return userService.getTopAuthors(limit);
    }

    @GetMapping("/followings")
    @Operation(summary = "获取当前用户关注列表")
    public Result<List<UserDTO>> getFollowings(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        return userService.getFollowings(userId, page, size);
    }

    @GetMapping("/followers")
    @Operation(summary = "获取当前用户粉丝列表")
    public Result<List<UserDTO>> getFollowers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        return userService.getFollowers(userId, page, size);
    }

    @PostMapping("/avatar/upload")
    @Operation(summary = "上传用户头像")
    public Result<String> uploadAvatar(@Parameter(description = "头像文件") @RequestParam("file") MultipartFile file) {
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件");
            }

            // 验证文件大小（2MB）
            long maxSize = 2 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return Result.error("头像文件大小不能超过2MB");
            }

            // 上传到TOS，使用avatar文件夹
            String avatarUrl = tosService.uploadFile(file, "avatar");

            log.info("头像上传成功: {}", avatarUrl);

            return Result.success(avatarUrl);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    private Long getCurrentUserId() {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户未登录");
        }
        return Long.valueOf(userId.toString());
    }
}
