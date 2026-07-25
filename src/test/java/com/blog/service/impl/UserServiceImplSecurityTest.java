package com.blog.service.impl;

import com.blog.dto.UserLoginDTO;
import com.blog.dto.PublicUserProfileDTO;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.mapper.UserMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.service.CaptchaService;
import com.blog.utils.JWTUtils;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplSecurityTest {

    @Test
    void getPublicUserInfo_shouldReturnDedicatedDtoWithoutSensitiveFields() {
        UserServiceImpl service = new UserServiceImpl();
        UserMapper userMapper = mock(UserMapper.class);
        ArticleMapper articleMapper = mock(ArticleMapper.class);
        CommentMapper commentMapper = mock(CommentMapper.class);
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setEmail("private@example.com");
        user.setPhone("13800000000");
        user.setStatus(1);
        user.setLastLoginIp("127.0.0.1");
        user.setRole(2);
        user.setFollowerCount(3);
        user.setFollowingCount(4);
        when(userMapper.selectById(7L)).thenReturn(user);
        setField(service, "userMapper", userMapper);
        setField(service, "articleMapper", articleMapper);
        setField(service, "commentMapper", commentMapper);

        PublicUserProfileDTO profile = service.getPublicUserInfo(7L).getData();

        assertThat(profile.getId()).isEqualTo(7L);
        assertThat(profile.getUsername()).isEqualTo("alice");
        assertThat(profile.getRole()).isEqualTo("admin");
        assertThat(profile.getFollowerCount()).isEqualTo(3);
        assertThat(PublicUserProfileDTO.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain(
                        "email", "phone", "status", "lastLoginTime",
                        "lastLoginIp", "accessToken", "refreshToken"
                );
    }

    @Test
    void login_invalidCaptcha_shouldRejectBeforeUserLookup() {
        UserServiceImpl service = new UserServiceImpl();
        CaptchaService captchaService = mock(CaptchaService.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(captchaService.verifyCaptcha("captcha-key", "bad-code")).thenReturn(false);
        setField(service, "captchaService", captchaService);
        setField(service, "userMapper", userMapper);

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("Password123!");
        dto.setCaptchaKey("captcha-key");
        dto.setCaptcha("bad-code");

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("验证码");
        verify(userMapper, never()).selectByUsername(anyString());
    }

    @Test
    void refreshToken_disabledUser_shouldReject() {
        UserServiceImpl service = new UserServiceImpl();
        JWTUtils jwtUtils = mock(JWTUtils.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        UserMapper userMapper = mock(UserMapper.class);
        String token = "refresh-token";
        when(jwtUtils.validateRefreshToken(token)).thenReturn(true);
        when(jwtUtils.isRefreshToken(token)).thenReturn(true);
        when(jwtUtils.isRefreshTokenExpired(token)).thenReturn(false);
        when(jwtUtils.getUserIdFromRefreshToken(token)).thenReturn(7L);
        when(jwtUtils.getUsernameFromRefreshToken(token)).thenReturn("alice");
        when(redisUtils.get("auth:refresh:user:7")).thenReturn(token);
        User disabled = new User();
        disabled.setId(7L);
        disabled.setStatus(2);
        when(userMapper.selectById(7L)).thenReturn(disabled);
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "userMapper", userMapper);

        assertThatThrownBy(() -> service.refreshToken(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁用");
        verify(jwtUtils, never()).generateAccessToken(7L, "alice");
    }

    @Test
    void validateToken_disabledUser_shouldReturnFalse() {
        UserServiceImpl service = new UserServiceImpl();
        JWTUtils jwtUtils = mock(JWTUtils.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(jwtUtils.validateToken("access-token")).thenReturn(true);
        when(jwtUtils.isTokenExpired("access-token")).thenReturn(false);
        when(jwtUtils.isAccessToken("access-token")).thenReturn(true);
        when(jwtUtils.getUserIdFromToken("access-token")).thenReturn(7L);
        User disabled = new User();
        disabled.setStatus(2);
        when(userMapper.selectById(7L)).thenReturn(disabled);
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "userMapper", userMapper);

        var result = service.validateToken("Bearer access-token");

        assertThat(result.getData()).isFalse();
    }

    @Test
    void updateUserStatus_disabled_shouldDeleteRefreshToken() {
        UserServiceImpl service = new UserServiceImpl();
        UserMapper userMapper = mock(UserMapper.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        User user = new User();
        user.setId(7L);
        user.setStatus(1);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);
        setField(service, "userMapper", userMapper);
        setField(service, "redisUtils", redisUtils);

        service.updateUserStatus(7L, 2);

        verify(redisUtils).delete("auth:refresh:user:7");
    }

    private static void setField(UserServiceImpl target, String fieldName, Object value) {
        try {
            var field = UserServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
