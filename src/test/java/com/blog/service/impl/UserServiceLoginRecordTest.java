package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.blog.dto.UserLoginDTO;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.service.CaptchaService;
import com.blog.utils.JWTUtils;
import com.blog.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * bug #5：登录成功后整行 updateById 回写会覆盖并发的密码修改/tokenVersion 递增。
 * 期望：登录完成点只按列更新 last_login_time/last_login_ip/update_time，永不整行回写。
 */
class UserServiceLoginRecordTest {

    @BeforeAll
    static void initLambdaCache() {
        // 手动初始化 User 的 Lambda 列缓存，避免单测中 wrapper 因反射缓存未初始化而失败（同 AdminServiceImplTest）；
        // 打开 mapUnderscoreToCamelCase 使列名与生产一致（snake_case）
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, "test"),
                User.class);
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

    /** 通用 mock：jwt 发 token + refresh token 落 redis。 */
    private static void stubTokenInfra(JWTUtils jwtUtils, RedisUtils redisUtils) {
        when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyInt())).thenReturn("access-token");
        when(jwtUtils.generateRefreshToken(anyLong(), anyString(), anyInt())).thenReturn("refresh-token");
        when(jwtUtils.getRemainingRefreshTime(anyString())).thenReturn(3600L);
        when(jwtUtils.getRefreshJti(anyString())).thenReturn("jti-1");
        when(jwtUtils.getRefreshFamilyId(anyString())).thenReturn("family-1");
        when(jwtUtils.getRefreshGeneration(anyString())).thenReturn(0);
        when(jwtUtils.getRefreshTokenVersion(anyString())).thenReturn(3);
    }

    private static HttpServletRequest mockRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(anyString())).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        return request;
    }

    private static User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("alice");
        user.setPassword("hash-of-password");
        user.setStatus(User.STATUS_ACTIVE);
        user.setTokenVersion(3);
        return user;
    }

    /** 断言：按列更新只写登录三列，且永不整行 updateById。返回 SET 子句供调用方补充断言。 */
    private static String verifyColumnOnlyUpdate(UserMapper userMapper) {
        verify(userMapper, never()).updateById(any(User.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<User>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(userMapper, times(1)).update(isNull(), captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        assertThat(sqlSet).contains("last_login_time").contains("last_login_ip").contains("update_time");
        assertThat(sqlSet).doesNotContain("password");
        return sqlSet;
    }

    @Test
    void login_shouldUpdateLoginColumnsOnly_neverWholeRow() {
        UserServiceImpl service = new UserServiceImpl();
        UserMapper userMapper = mock(UserMapper.class);
        CaptchaService captchaService = mock(CaptchaService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JWTUtils jwtUtils = mock(JWTUtils.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        setField(service, "userMapper", userMapper);
        setField(service, "captchaService", captchaService);
        setField(service, "passwordEncoder", passwordEncoder);
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "request", mockRequest());

        when(captchaService.verifyCaptcha("k", "c")).thenReturn(true);
        when(userMapper.selectByUsername("alice")).thenReturn(activeUser(7L));
        when(passwordEncoder.matches("Password123!", "hash-of-password")).thenReturn(true);
        stubTokenInfra(jwtUtils, redisUtils);

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("Password123!");
        dto.setCaptchaKey("k");
        dto.setCaptcha("c");

        var result = service.login(dto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getLastLoginIp()).isEqualTo("10.0.0.1");
        assertThat(result.getData().getLastLoginTime()).isNotNull();
        verifyColumnOnlyUpdate(userMapper);
    }

    private static void stubGithubTokenResponse(RestTemplate restTemplate, String githubId, String login, String email) {
        when(restTemplate.postForEntity(anyString(),
                any(org.springframework.http.HttpEntity.class), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        "{\"access_token\":\"gho_token\"}", org.springframework.http.HttpStatus.OK));
        when(restTemplate.exchange(eq("https://api.github.com/user"),
                eq(org.springframework.http.HttpMethod.GET),
                any(org.springframework.http.HttpEntity.class), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        "{\"id\":" + githubId + ",\"login\":\"" + login + "\",\"email\":\"" + email + "\"}",
                        org.springframework.http.HttpStatus.OK));
    }

    private static UserServiceImpl githubService(UserMapper userMapper, RedisUtils redisUtils,
                                                 RestTemplate restTemplate, JWTUtils jwtUtils) {
        UserServiceImpl service = new UserServiceImpl();
        setField(service, "userMapper", userMapper);
        setField(service, "redisUtils", redisUtils);
        setField(service, "restTemplate", restTemplate);
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "request", mockRequest());
        setField(service, "transactionManager", mock(org.springframework.transaction.PlatformTransactionManager.class));
        when(redisUtils.get(anyString())).thenReturn("1");
        stubTokenInfra(jwtUtils, redisUtils);
        return service;
    }

    @Test
    void githubLogin_existingUser_shouldUpdateLoginColumnsOnly_neverWholeRow() {
        UserMapper userMapper = mock(UserMapper.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        JWTUtils jwtUtils = mock(JWTUtils.class);
        UserServiceImpl service = githubService(userMapper, redisUtils, restTemplate, jwtUtils);
        stubGithubTokenResponse(restTemplate, "12345", "octocat", "octo@github.com");

        User existing = activeUser(1L);
        existing.setGithubId(12345L);
        when(userMapper.selectByGithubId(12345L)).thenReturn(existing);

        var result = service.githubLogin("code", "state", "state");

        assertThat(result.isSuccess()).isTrue();
        verifyColumnOnlyUpdate(userMapper);
    }

    @Test
    void githubLogin_emailBind_shouldWriteGithubIdAndLoginColumnsOnly() {
        UserMapper userMapper = mock(UserMapper.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        JWTUtils jwtUtils = mock(JWTUtils.class);
        UserServiceImpl service = githubService(userMapper, redisUtils, restTemplate, jwtUtils);
        stubGithubTokenResponse(restTemplate, "999", "octocat", "bind@example.com");

        when(userMapper.selectByGithubId(999L)).thenReturn(null);
        when(userMapper.selectByUsername("octocat")).thenReturn(null);
        User emailUser = activeUser(2L);
        emailUser.setEmail("bind@example.com");
        when(userMapper.selectByEmail("bind@example.com")).thenReturn(emailUser);

        var result = service.githubLogin("code", "state", "state");

        assertThat(result.isSuccess()).isTrue();
        String sqlSet = verifyColumnOnlyUpdate(userMapper);
        assertThat(sqlSet).contains("github_id");
    }

    @Test
    void githubLogin_newUser_shouldUpdateLoginColumnsOnly_neverWholeRow() {
        UserMapper userMapper = mock(UserMapper.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        JWTUtils jwtUtils = mock(JWTUtils.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        UserServiceImpl service = githubService(userMapper, redisUtils, restTemplate, jwtUtils);
        setField(service, "passwordEncoder", passwordEncoder);
        stubGithubTokenResponse(restTemplate, "777", "octocat", "new@example.com");

        when(userMapper.selectByGithubId(777L)).thenReturn(null);
        User taken = activeUser(9L); // 用户名被占用，走 createGithubUser + loginAndReturnDto
        when(userMapper.selectByUsername("octocat")).thenReturn(taken);
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");

        var result = service.githubLogin("code", "state", "state");

        assertThat(result.isSuccess()).isTrue();
        verify(userMapper, never()).updateById(any(User.class));
        verify(userMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }
}
