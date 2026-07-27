package com.blog.controller;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.exception.BusinessException;
import com.blog.dto.UserDTO;
import com.blog.dto.UserLoginDTO;
import com.blog.dto.UserRegisterDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.SendResetCodeDTO;
import com.blog.dto.SendRegisterCodeDTO;
import com.blog.dto.ResetPasswordByCodeDTO;
import com.blog.dto.TokenRefreshResponseDTO;
import com.blog.dto.PublicUserProfileDTO;
import com.blog.service.TOSService;
import com.blog.service.UserService;
import com.blog.service.RefreshTokenCookieService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private RefreshTokenCookieService refreshTokenCookieService;

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
    public Result<UserDTO> login(
            @Valid @RequestBody UserLoginDTO loginDTO,
            HttpServletResponse response) {
        return moveRefreshTokenToCookie(userService.login(loginDTO), response);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = refreshTokenCookieService.readRefreshToken(request).orElse(null);
        Object currentUserId = request.getAttribute("userId");
        Long userId = currentUserId == null ? null : Long.valueOf(currentUserId.toString());
        Result<Void> result = userService.logout(userId, refreshToken, authorization);
        refreshTokenCookieService.clearRefreshToken(response);
        return result;
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "轮换刷新令牌")
    public Result<Map<String, String>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = refreshTokenCookieService.readRefreshToken(request)
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "缺少刷新令牌"));
        Result<TokenRefreshResponseDTO> refreshResult = userService.refreshToken(refreshToken);

        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("token", refreshResult.getData().getToken());
        refreshTokenCookieService.setRefreshToken(response, refreshResult.getData().getRefreshToken());
        return Result.success(tokenData);
    }

    @GetMapping("/token/validate")
    @Operation(summary = "校验访问令牌有效性")
    public Result<Boolean> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return userService.validateToken(authorization);
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息")
    public Result<UserDTO> getUserInfo() {
        Long userId = getCurrentUserId();
        return userService.getUserInfo(userId);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "获取指定用户公开信息")
    public Result<PublicUserProfileDTO> getPublicUserInfo(
            @Parameter(description = "用户ID") @PathVariable Long userId
    ) {
        return userService.getPublicUserInfo(userId);
    }

    @GetMapping("/public/{userId}")
    @Operation(summary = "匿名获取指定用户公开信息")
    public Result<PublicUserProfileDTO> getAnonymousPublicUserInfo(
            @Parameter(description = "用户ID") @PathVariable Long userId
    ) {
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
    public Result<Void> changePassword(
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = getCurrentUserId();
        return userService.changePassword(userId, changePasswordDTO, authorization);
    }

    @PostMapping("/password/reset/send")
    @Operation(summary = "发送邮箱重置验证码")
    public Result<Void> sendResetCode(@Valid @RequestBody SendResetCodeDTO sendResetCodeDTO) {
        return userService.sendResetCode(sendResetCodeDTO);
    }

    @PostMapping("/password/reset")
    @Operation(summary = "验证码重置密码")
    public Result<Void> resetPasswordByCode(@Valid @RequestBody ResetPasswordByCodeDTO resetPasswordByCodeDTO) {
        return userService.resetPasswordByCode(resetPasswordByCodeDTO);
    }

    // ==================== 注册邮箱验证接口 ====================

    @PostMapping("/register/verify/send")
    @Operation(summary = "发送注册邮箱验证码（需图形验证码）")
    public Result<Void> sendRegisterVerifyCode(@Valid @RequestBody SendRegisterCodeDTO sendRegisterCodeDTO) {
        return userService.sendRegisterVerifyCode(sendRegisterCodeDTO);
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
            if (!List.of("image/jpeg", "image/png", "image/gif").contains(contentType)) {
                return Result.error("头像仅支持 JPEG、PNG 或 GIF 图片");
            }

            // 验证文件大小（2MB）
            long maxSize = 2 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return Result.error("头像文件大小不能超过2MB");
            }
            if (file.isEmpty() || ImageIO.read(file.getInputStream()) == null) {
                return Result.error("上传内容不是有效图片");
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

    @GetMapping("/auth/github/callback")
    @Operation(summary = "GitHub OAuth 回调接口")
    public Result<UserDTO> githubCallback(
            @Parameter(description = "授权码") @RequestParam String code,
            @Parameter(description = "状态参数（防CSRF）") @RequestParam(required = false) String state,
            HttpServletResponse response) {
        return moveRefreshTokenToCookie(userService.githubLogin(code, state), response);
    }

    @PostMapping("/auth/github/state")
    @Operation(summary = "生成 GitHub OAuth state 并存储")
    public Result<String> generateGithubState() {
        return userService.generateGithubState();
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

    private Result<UserDTO> moveRefreshTokenToCookie(
            Result<UserDTO> result, HttpServletResponse response) {
        if (result.getData() != null && org.springframework.util.StringUtils.hasText(result.getData().getRefreshToken())) {
            refreshTokenCookieService.setRefreshToken(response, result.getData().getRefreshToken());
            result.getData().setRefreshToken(null);
        }
        return result;
    }
}
