package com.blog.service.impl;

import com.blog.common.ResultCode;
import com.blog.dto.UserLoginDTO;
import com.blog.dto.UserRegisterDTO;
import com.blog.dto.PublicUserProfileDTO;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.mapper.UserMapper;
import com.blog.security.password.PasswordResetCodeSecurity;
import com.blog.service.ArticleQueryService;
import com.blog.service.CaptchaService;
import com.blog.service.CommentService;
import com.blog.service.AuthSessionRevocationService;
import com.blog.utils.JWTUtils;
import com.blog.utils.RedisDistributedLock;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

class UserServiceImplSecurityTest {

    @Test
    void getPublicUserInfo_shouldReturnDedicatedDtoWithoutSensitiveFields() {
        UserServiceImpl service = new UserServiceImpl();
        UserMapper userMapper = mock(UserMapper.class);
        ArticleQueryService articleQueryService = mock(ArticleQueryService.class);
        CommentService commentService = mock(CommentService.class);
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
        setField(service, "articleQueryService", articleQueryService);
        setField(service, "commentService", commentService);

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
        RedisDistributedLock lock = mock(RedisDistributedLock.class);
        AuthSessionRevocationService revocation = mock(AuthSessionRevocationService.class);
        UserMapper userMapper = mock(UserMapper.class);
        String token = "refresh-token";
        when(jwtUtils.validateRefreshToken(token)).thenReturn(true);
        when(jwtUtils.isRefreshToken(token)).thenReturn(true);
        when(jwtUtils.isRefreshTokenExpired(token)).thenReturn(false);
        when(jwtUtils.getUserIdFromRefreshToken(token)).thenReturn(7L);
        when(jwtUtils.getUsernameFromRefreshToken(token)).thenReturn("alice");
        when(jwtUtils.getRefreshFamilyId(token)).thenReturn("family-1");
        when(jwtUtils.getRefreshGeneration(token)).thenReturn(0);
        User disabled = new User();
        disabled.setId(7L);
        disabled.setStatus(2);
        when(userMapper.selectById(7L)).thenReturn(disabled);
        when(lock.tryLockWithWatchdog(eq("auth:refresh-family:family-1"), anyLong(), eq(TimeUnit.SECONDS),
                anyLong(), eq(TimeUnit.SECONDS))).thenReturn("lock-value");
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "userMapper", userMapper);
        setField(service, "redisDistributedLock", lock);
        setField(service, "authSessionRevocationService", revocation);

        assertThatThrownBy(() -> service.refreshToken(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("禁用");
        verify(jwtUtils, never()).generateAccessToken(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("alice"), anyInt());
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
    void refreshToken_replayShouldBeRejectedAfterAtomicConsumption() {
        UserServiceImpl service = new UserServiceImpl();
        JWTUtils jwtUtils = mock(JWTUtils.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        RedisDistributedLock lock = mock(RedisDistributedLock.class);
        AuthSessionRevocationService revocation = mock(AuthSessionRevocationService.class);
        UserMapper userMapper = mock(UserMapper.class);
        String token = "refresh-token";
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setStatus(User.STATUS_ACTIVE);
        user.setTokenVersion(4);
        when(jwtUtils.validateRefreshToken(token)).thenReturn(true);
        when(jwtUtils.isRefreshToken(token)).thenReturn(true);
        when(jwtUtils.isRefreshTokenExpired(token)).thenReturn(false);
        when(jwtUtils.getUserIdFromRefreshToken(token)).thenReturn(7L);
        when(jwtUtils.getUsernameFromRefreshToken(token)).thenReturn("alice");
        when(jwtUtils.getRefreshJti(token)).thenReturn("old-jti");
        when(jwtUtils.getRefreshFamilyId(token)).thenReturn("family-1");
        when(jwtUtils.getRefreshGeneration(token)).thenReturn(0);
        when(jwtUtils.getRefreshTokenVersion(token)).thenReturn(4);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(jwtUtils.generateAccessToken(7L, "alice", 4)).thenReturn("new-access");
        when(jwtUtils.generateRefreshToken(7L, "alice", 4, "family-1", 1)).thenReturn("new-refresh");
        when(jwtUtils.getRemainingRefreshTime("new-refresh")).thenReturn(600L);
        when(jwtUtils.getRefreshJti("new-refresh")).thenReturn("new-jti");
        when(redisUtils.rotateRefreshTokenFamily(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(1, 0);
        when(lock.tryLockWithWatchdog(eq("auth:refresh-family:family-1"), anyLong(), eq(TimeUnit.SECONDS),
                anyLong(), eq(TimeUnit.SECONDS))).thenReturn("lock-value");
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "userMapper", userMapper);
        setField(service, "redisDistributedLock", lock);
        setField(service, "authSessionRevocationService", revocation);

        assertThat(service.refreshToken(token).getData().getToken()).isEqualTo("new-access");
        assertThatThrownBy(() -> service.refreshToken(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重放");
        verify(revocation).revokeFamily(7L, "family-1");
    }

    @Test
    void logout_withRefreshToken_shouldRevokeEntireUserRefreshFamilyUnderSharedLock() {
        UserServiceImpl service = new UserServiceImpl();
        JWTUtils jwtUtils = mock(JWTUtils.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        RedisDistributedLock lock = mock(RedisDistributedLock.class);
        AuthSessionRevocationService revocation = mock(AuthSessionRevocationService.class);
        when(jwtUtils.validateRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtils.getUserIdFromRefreshToken("refresh-token")).thenReturn(7L);
        when(jwtUtils.getRefreshFamilyId("refresh-token")).thenReturn("family-1");
        when(lock.tryLockWithWatchdog(eq("auth:refresh-family:family-1"), anyLong(), eq(TimeUnit.SECONDS),
                anyLong(), eq(TimeUnit.SECONDS))).thenReturn("lock-value");
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "redisDistributedLock", lock);
        setField(service, "authSessionRevocationService", revocation);

        service.logout(null, "refresh-token", null);

        verify(revocation).revokeFamily(7L, "family-1");
        verify(lock).unlock("auth:refresh-family:family-1", "lock-value");
    }

    @Test
    void logout_whenSynchronousFamilyRevocationFails_shouldReturnFailureInsteadOfSuccess() {
        UserServiceImpl service = new UserServiceImpl();
        JWTUtils jwtUtils = mock(JWTUtils.class);
        RedisDistributedLock lock = mock(RedisDistributedLock.class);
        AuthSessionRevocationService revocation = mock(AuthSessionRevocationService.class);
        when(jwtUtils.validateRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtils.getUserIdFromRefreshToken("refresh-token")).thenReturn(7L);
        when(jwtUtils.getRefreshFamilyId("refresh-token")).thenReturn("family-1");
        when(lock.tryLockWithWatchdog(eq("auth:refresh-family:family-1"), anyLong(), eq(TimeUnit.SECONDS),
                anyLong(), eq(TimeUnit.SECONDS))).thenReturn("lock-value");
        org.mockito.Mockito.doThrow(new IllegalStateException("认证会话撤销失败"))
                .when(revocation).revokeFamily(7L, "family-1");
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisDistributedLock", lock);
        setField(service, "authSessionRevocationService", revocation);

        assertThatThrownBy(() -> service.logout(null, "refresh-token", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("撤销失败");
        verify(lock).unlock("auth:refresh-family:family-1", "lock-value");
    }

    @Test
    void refreshReplay_whenFamilyRevocationFails_shouldNotClaimReplayWasSafelyHandled() {
        UserServiceImpl service = configuredReplayServiceWithRevocationFailure();

        assertThatThrownBy(() -> service.refreshToken("victim-old-refresh"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("撤销失败");
    }

    @Test
    void refreshReplay_shouldRevokeFamilyAndRejectAlreadyRotatedDescendant() {
        UserServiceImpl service = new UserServiceImpl();
        JWTUtils jwtUtils = mock(JWTUtils.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        UserMapper userMapper = mock(UserMapper.class);
        RedisDistributedLock lock = mock(RedisDistributedLock.class);
        AuthSessionRevocationService revocation = mock(AuthSessionRevocationService.class);
        String victimToken = "victim-old-refresh";
        String attackerToken = "attacker-rotated-refresh";
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setStatus(User.STATUS_ACTIVE);
        user.setTokenVersion(4);
        when(jwtUtils.validateRefreshToken(victimToken)).thenReturn(true);
        when(jwtUtils.validateRefreshToken(attackerToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(victimToken)).thenReturn(true);
        when(jwtUtils.isRefreshToken(attackerToken)).thenReturn(true);
        when(jwtUtils.isRefreshTokenExpired(victimToken)).thenReturn(false);
        when(jwtUtils.isRefreshTokenExpired(attackerToken)).thenReturn(false);
        when(jwtUtils.getUserIdFromRefreshToken(victimToken)).thenReturn(7L);
        when(jwtUtils.getUserIdFromRefreshToken(attackerToken)).thenReturn(7L);
        when(jwtUtils.getUsernameFromRefreshToken(victimToken)).thenReturn("alice");
        when(jwtUtils.getUsernameFromRefreshToken(attackerToken)).thenReturn("alice");
        when(jwtUtils.getRefreshJti(victimToken)).thenReturn("old-jti");
        when(jwtUtils.getRefreshJti(attackerToken)).thenReturn("new-jti");
        when(jwtUtils.getRefreshFamilyId(victimToken)).thenReturn("family-1");
        when(jwtUtils.getRefreshFamilyId(attackerToken)).thenReturn("family-1");
        when(jwtUtils.getRefreshGeneration(victimToken)).thenReturn(0);
        when(jwtUtils.getRefreshGeneration(attackerToken)).thenReturn(1);
        when(jwtUtils.getRefreshTokenVersion(victimToken)).thenReturn(4);
        when(jwtUtils.getRefreshTokenVersion(attackerToken)).thenReturn(4);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(jwtUtils.generateAccessToken(7L, "alice", 4)).thenReturn("new-access");
        when(jwtUtils.generateRefreshToken(7L, "alice", 4, "family-1", 1))
                .thenReturn(attackerToken);
        when(jwtUtils.generateRefreshToken(7L, "alice", 4, "family-1", 2))
                .thenReturn("descendant-refresh");
        when(jwtUtils.getRemainingRefreshTime(attackerToken)).thenReturn(600L);
        when(jwtUtils.getRemainingRefreshTime("descendant-refresh")).thenReturn(600L);
        when(jwtUtils.getRefreshJti("descendant-refresh")).thenReturn("descendant-jti");
        when(redisUtils.rotateRefreshTokenFamily(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(1, 0, -1);
        when(lock.tryLockWithWatchdog(eq("auth:refresh-family:family-1"), anyLong(), eq(TimeUnit.SECONDS),
                anyLong(), eq(TimeUnit.SECONDS))).thenReturn("lock-value");
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "userMapper", userMapper);
        setField(service, "redisDistributedLock", lock);
        setField(service, "authSessionRevocationService", revocation);

        assertThat(service.refreshToken(victimToken).getData().getRefreshToken())
                .isEqualTo(attackerToken);
        assertThatThrownBy(() -> service.refreshToken(victimToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重放");
        assertThatThrownBy(() -> service.refreshToken(attackerToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重放");
        verify(revocation, org.mockito.Mockito.times(2)).revokeFamily(7L, "family-1");
    }

    @Test
    void githubLogin_disabledExistingUser_shouldThrowUserDisabled() {
        UserServiceImpl service = new UserServiceImpl();
        UserMapper userMapper = mock(UserMapper.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        JWTUtils jwtUtils = mock(JWTUtils.class);
        setField(service, "userMapper", userMapper);
        setField(service, "redisUtils", redisUtils);
        setField(service, "restTemplate", restTemplate);
        setField(service, "jwtUtils", jwtUtils);
        when(redisUtils.get(org.mockito.ArgumentMatchers.anyString())).thenReturn("1");
        when(restTemplate.postForEntity(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(org.springframework.http.HttpEntity.class),
                org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        "{\"access_token\":\"gho_token\"}", org.springframework.http.HttpStatus.OK));
        when(restTemplate.exchange(org.mockito.ArgumentMatchers.eq("https://api.github.com/user"),
                org.mockito.ArgumentMatchers.eq(org.springframework.http.HttpMethod.GET),
                org.mockito.ArgumentMatchers.any(org.springframework.http.HttpEntity.class),
                org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        "{\"id\":12345,\"login\":\"octocat\",\"email\":\"octo@github.com\"}",
                        org.springframework.http.HttpStatus.OK));
        User disabled = new User();
        disabled.setId(1L);
        disabled.setUsername("octocat");
        disabled.setStatus(User.STATUS_DISABLED);
        when(userMapper.selectByGithubId(12345L)).thenReturn(disabled);

        assertThatThrownBy(() -> service.githubLogin("code", "state", "state"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(com.blog.common.ResultCode.USER_DISABLED.getCode());
        verify(jwtUtils, never()).generateAccessToken(anyLong(), anyString(), anyInt());
        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void githubLogin_inactiveExistingUser_shouldThrowNotActivated() {
        UserServiceImpl service = new UserServiceImpl();
        UserMapper userMapper = mock(UserMapper.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        setField(service, "userMapper", userMapper);
        setField(service, "redisUtils", redisUtils);
        setField(service, "restTemplate", restTemplate);
        when(redisUtils.get(org.mockito.ArgumentMatchers.anyString())).thenReturn("1");
        when(restTemplate.postForEntity(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(org.springframework.http.HttpEntity.class),
                org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        "{\"access_token\":\"gho_token\"}", org.springframework.http.HttpStatus.OK));
        when(restTemplate.exchange(org.mockito.ArgumentMatchers.eq("https://api.github.com/user"),
                org.mockito.ArgumentMatchers.eq(org.springframework.http.HttpMethod.GET),
                org.mockito.ArgumentMatchers.any(org.springframework.http.HttpEntity.class),
                org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        "{\"id\":12345,\"login\":\"octocat\",\"email\":\"octo@github.com\"}",
                        org.springframework.http.HttpStatus.OK));
        User inactive = new User();
        inactive.setId(1L);
        inactive.setUsername("octocat");
        inactive.setStatus(0);
        when(userMapper.selectByGithubId(12345L)).thenReturn(inactive);

        assertThatThrownBy(() -> service.githubLogin("code", "state", "state"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号未激活");
    }

    @Test
    void githubLogin_emailMatchedDisabledUser_shouldRejectBeforeBindingGithubId() {
        UserServiceImpl service = new UserServiceImpl();
        UserMapper userMapper = mock(UserMapper.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        setField(service, "userMapper", userMapper);
        setField(service, "redisUtils", redisUtils);
        setField(service, "restTemplate", restTemplate);
        when(redisUtils.get(org.mockito.ArgumentMatchers.anyString())).thenReturn("1");
        when(restTemplate.postForEntity(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(org.springframework.http.HttpEntity.class),
                org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        "{\"access_token\":\"gho_token\"}", org.springframework.http.HttpStatus.OK));
        when(restTemplate.exchange(org.mockito.ArgumentMatchers.eq("https://api.github.com/user"),
                org.mockito.ArgumentMatchers.eq(org.springframework.http.HttpMethod.GET),
                org.mockito.ArgumentMatchers.any(org.springframework.http.HttpEntity.class),
                org.mockito.ArgumentMatchers.eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(
                        "{\"id\":12345,\"login\":\"octocat\",\"email\":\"octo@github.com\"}",
                        org.springframework.http.HttpStatus.OK));
        when(userMapper.selectByGithubId(12345L)).thenReturn(null);
        when(userMapper.selectByUsername("octocat")).thenReturn(null);
        User emailUser = new User();
        emailUser.setId(5L);
        emailUser.setUsername("existing");
        emailUser.setStatus(User.STATUS_DISABLED);
        when(userMapper.selectByEmail("octo@github.com")).thenReturn(emailUser);

        assertThatThrownBy(() -> service.githubLogin("code", "state", "state"))
                .isInstanceOf(BusinessException.class);
        assertThat(emailUser.getGithubId()).isNull();
        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void githubLogin_missingOrMismatchedStateCookie_shouldRejectBeforeRedis() {
        UserServiceImpl service = new UserServiceImpl();
        RedisUtils redisUtils = mock(RedisUtils.class);
        RestTemplate restTemplate = mock(RestTemplate.class);
        setField(service, "redisUtils", redisUtils);
        setField(service, "restTemplate", restTemplate);

        assertThatThrownBy(() -> service.githubLogin("code", "state", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OAuth 状态验证失败");
        assertThatThrownBy(() -> service.githubLogin("code", "state", "other"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OAuth 状态验证失败");
        verify(redisUtils, never()).get(anyString());
        verify(restTemplate, never()).postForEntity(anyString(), org.mockito.ArgumentMatchers.any(), any());
    }

    @Test
    void login_whenFailureCountReached_shouldRejectBeforeUserLookup() {
        UserServiceImpl service = new UserServiceImpl();
        CaptchaService captchaService = mock(CaptchaService.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(captchaService.verifyCaptcha("k", "c")).thenReturn(true);
        when(redisUtils.getString("login:fail:alice")).thenReturn("5");
        setField(service, "captchaService", captchaService);
        setField(service, "redisUtils", redisUtils);
        setField(service, "userMapper", userMapper);

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("Password123!");
        dto.setCaptchaKey("k");
        dto.setCaptcha("c");

        assertThatThrownBy(() -> service.login(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录失败次数过多");
        verify(userMapper, never()).selectByUsername(anyString());
    }

    @Test
    void login_wrongPassword_shouldIncrementFailureCounter() {
        UserServiceImpl service = new UserServiceImpl();
        CaptchaService captchaService = mock(CaptchaService.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(captchaService.verifyCaptcha("k", "c")).thenReturn(true);
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setPassword("hash");
        user.setStatus(User.STATUS_ACTIVE);
        when(userMapper.selectByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(false);
        setField(service, "captchaService", captchaService);
        setField(service, "redisUtils", redisUtils);
        setField(service, "userMapper", userMapper);
        setField(service, "passwordEncoder", passwordEncoder);

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername("alice");
        dto.setPassword("Password123!");
        dto.setCaptchaKey("k");
        dto.setCaptcha("c");

        assertThatThrownBy(() -> service.login(dto)).isInstanceOf(BusinessException.class);
        verify(redisUtils).incrementWithinLimit("login:fail:alice", 5, 900);
    }

    @Test
    void register_duplicateEmailKey_shouldMapToEmailExist() {
        UserServiceImpl service = registerRaceService(
                new DuplicateKeyException("Duplicate entry 'x' for key 'users.uk_email'"));

        assertThatThrownBy(() -> service.register(raceRegisterDto()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.EMAIL_EXIST.getCode());
    }

    @Test
    void register_duplicateUsernameKey_shouldMapToUsernameExist() {
        UserServiceImpl service = registerRaceService(
                new DuplicateKeyException("Duplicate entry 'race' for key 'users.uk_username'"));

        assertThatThrownBy(() -> service.register(raceRegisterDto()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.USERNAME_EXIST.getCode());
    }

    private static UserServiceImpl registerRaceService(DuplicateKeyException ex) {
        UserServiceImpl service = new UserServiceImpl();
        UserMapper userMapper = mock(UserMapper.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PasswordResetCodeSecurity security = mock(PasswordResetCodeSecurity.class);
        when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
        when(security.digest(anyString(), anyString())).thenReturn("digest");
        when(userMapper.selectByUsername(anyString())).thenReturn(null);
        when(userMapper.selectByEmail(anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userMapper.insert(any(User.class))).thenThrow(ex);
        setField(service, "userMapper", userMapper);
        setField(service, "redisUtils", redisUtils);
        setField(service, "passwordEncoder", passwordEncoder);
        setField(service, "passwordResetCodeSecurity", security);
        return service;
    }

    private static UserRegisterDTO raceRegisterDto() {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername("race");
        dto.setEmail("race@example.com");
        dto.setPassword("Password123!");
        dto.setConfirmPassword("Password123!");
        dto.setEmailCode("123456");
        return dto;
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

    private static UserServiceImpl configuredReplayServiceWithRevocationFailure() {
        UserServiceImpl service = new UserServiceImpl();
        JWTUtils jwtUtils = mock(JWTUtils.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        UserMapper userMapper = mock(UserMapper.class);
        RedisDistributedLock lock = mock(RedisDistributedLock.class);
        AuthSessionRevocationService revocation = mock(AuthSessionRevocationService.class);
        String token = "victim-old-refresh";
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setStatus(User.STATUS_ACTIVE);
        user.setTokenVersion(4);
        when(jwtUtils.validateRefreshToken(token)).thenReturn(true);
        when(jwtUtils.isRefreshToken(token)).thenReturn(true);
        when(jwtUtils.isRefreshTokenExpired(token)).thenReturn(false);
        when(jwtUtils.getUserIdFromRefreshToken(token)).thenReturn(7L);
        when(jwtUtils.getUsernameFromRefreshToken(token)).thenReturn("alice");
        when(jwtUtils.getRefreshJti(token)).thenReturn("old-jti");
        when(jwtUtils.getRefreshFamilyId(token)).thenReturn("family-1");
        when(jwtUtils.getRefreshGeneration(token)).thenReturn(0);
        when(jwtUtils.getRefreshTokenVersion(token)).thenReturn(4);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(jwtUtils.generateAccessToken(7L, "alice", 4)).thenReturn("new-access");
        when(jwtUtils.generateRefreshToken(7L, "alice", 4, "family-1", 1))
                .thenReturn("new-refresh");
        when(jwtUtils.getRemainingRefreshTime("new-refresh")).thenReturn(600L);
        when(jwtUtils.getRefreshJti("new-refresh")).thenReturn("new-jti");
        when(redisUtils.rotateRefreshTokenFamily(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyString(), anyString(), anyLong()))
                .thenReturn(0);
        when(lock.tryLockWithWatchdog(eq("auth:refresh-family:family-1"), anyLong(), eq(TimeUnit.SECONDS),
                anyLong(), eq(TimeUnit.SECONDS))).thenReturn("lock-value");
        org.mockito.Mockito.doThrow(new IllegalStateException("认证会话撤销失败"))
                .when(revocation).revokeFamily(7L, "family-1");
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "userMapper", userMapper);
        setField(service, "redisDistributedLock", lock);
        setField(service, "authSessionRevocationService", revocation);
        return service;
    }
}
