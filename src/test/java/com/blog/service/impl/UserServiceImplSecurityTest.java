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
import com.blog.utils.RedisDistributedLock;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
        RedisDistributedLock lock = mock(RedisDistributedLock.class);
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
        when(lock.tryLockWithWatchdog(eq("auth:refresh-family:7"), anyLong(), eq(TimeUnit.SECONDS),
                anyLong(), eq(TimeUnit.SECONDS))).thenReturn("lock-value");
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "userMapper", userMapper);
        setField(service, "redisDistributedLock", lock);

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
        when(jwtUtils.getRefreshTokenVersion(token)).thenReturn(4);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(redisUtils.consumeString("auth:refresh:jti:old-jti", "7:4"))
                .thenReturn(true, false);
        when(jwtUtils.generateAccessToken(7L, "alice", 4)).thenReturn("new-access");
        when(jwtUtils.generateRefreshToken(7L, "alice", 4)).thenReturn("new-refresh");
        when(jwtUtils.getRemainingRefreshTime("new-refresh")).thenReturn(600L);
        when(jwtUtils.getRefreshJti("new-refresh")).thenReturn("new-jti");
        when(jwtUtils.getRefreshTokenVersion("new-refresh")).thenReturn(4);
        when(lock.tryLockWithWatchdog(eq("auth:refresh-family:7"), anyLong(), eq(TimeUnit.SECONDS),
                anyLong(), eq(TimeUnit.SECONDS))).thenReturn("lock-value");
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "userMapper", userMapper);
        setField(service, "redisDistributedLock", lock);

        assertThat(service.refreshToken(token).getData().getToken()).isEqualTo("new-access");
        assertThatThrownBy(() -> service.refreshToken(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("失效");
    }

    @Test
    void logout_withRefreshToken_shouldRevokeEntireUserRefreshFamilyUnderSharedLock() {
        UserServiceImpl service = new UserServiceImpl();
        JWTUtils jwtUtils = mock(JWTUtils.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        RedisDistributedLock lock = mock(RedisDistributedLock.class);
        when(jwtUtils.validateRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtils.getUserIdFromRefreshToken("refresh-token")).thenReturn(7L);
        when(lock.tryLockWithWatchdog(eq("auth:refresh-family:7"), anyLong(), eq(TimeUnit.SECONDS),
                anyLong(), eq(TimeUnit.SECONDS))).thenReturn("lock-value");
        when(redisUtils.stringSetMembers("auth:refresh:user-jtis:7"))
                .thenReturn(Set.of("old-jti", "rotated-jti"));
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "redisDistributedLock", lock);

        service.logout(null, "refresh-token", null);

        verify(redisUtils).deleteString("auth:refresh:jti:old-jti");
        verify(redisUtils).deleteString("auth:refresh:jti:rotated-jti");
        verify(redisUtils).deleteString("auth:refresh:user-jtis:7");
        verify(lock).unlock("auth:refresh-family:7", "lock-value");
    }

    @Test
    void concurrentRefreshThenLogoutLeavesNoRotatedRefreshSession() throws Exception {
        UserServiceImpl service = new UserServiceImpl();
        JWTUtils jwtUtils = mock(JWTUtils.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        UserMapper userMapper = mock(UserMapper.class);
        SerializingTestLock lock = new SerializingTestLock();
        CountDownLatch refreshStoreEntered = new CountDownLatch(1);
        CountDownLatch allowRefreshStore = new CountDownLatch(1);
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
        when(jwtUtils.getRefreshTokenVersion(token)).thenReturn(4);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(redisUtils.consumeString("auth:refresh:jti:old-jti", "7:4")).thenReturn(true);
        when(jwtUtils.generateAccessToken(7L, "alice", 4)).thenReturn("new-access");
        when(jwtUtils.generateRefreshToken(7L, "alice", 4)).thenReturn("new-refresh");
        when(jwtUtils.getRemainingRefreshTime("new-refresh")).thenReturn(600L);
        when(jwtUtils.getRefreshJti("new-refresh")).thenReturn("new-jti");
        when(jwtUtils.getRefreshTokenVersion("new-refresh")).thenReturn(4);
        doAnswer(invocation -> {
            refreshStoreEntered.countDown();
            assertThat(allowRefreshStore.await(5, TimeUnit.SECONDS)).isTrue();
            return true;
        }).when(redisUtils).setString(
                "auth:refresh:jti:new-jti", "7:4", 600L, TimeUnit.SECONDS);
        when(redisUtils.stringSetMembers("auth:refresh:user-jtis:7"))
                .thenReturn(Set.of("new-jti"));
        setField(service, "jwtUtils", jwtUtils);
        setField(service, "redisUtils", redisUtils);
        setField(service, "userMapper", userMapper);
        setField(service, "redisDistributedLock", lock);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var refresh = executor.submit(() -> service.refreshToken(token));
            assertThat(refreshStoreEntered.await(5, TimeUnit.SECONDS)).isTrue();
            var logout = executor.submit(() -> service.logout(null, token, null));
            assertThat(lock.secondCallerWaiting.await(5, TimeUnit.SECONDS)).isTrue();
            allowRefreshStore.countDown();

            assertThat(refresh.get(5, TimeUnit.SECONDS).isSuccess()).isTrue();
            assertThat(logout.get(5, TimeUnit.SECONDS).isSuccess()).isTrue();
        } finally {
            allowRefreshStore.countDown();
            executor.shutdownNow();
            lock.destroy();
        }

        var order = inOrder(redisUtils);
        order.verify(redisUtils).setString(
                "auth:refresh:jti:new-jti", "7:4", 600L, TimeUnit.SECONDS);
        order.verify(redisUtils).deleteString("auth:refresh:jti:new-jti");
        order.verify(redisUtils).deleteString("auth:refresh:user-jtis:7");
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

        verify(redisUtils).deleteString("auth:refresh:user-jtis:7");
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

    private static final class SerializingTestLock extends RedisDistributedLock {
        private final ReentrantLock lock = new ReentrantLock();
        private final CountDownLatch secondCallerWaiting = new CountDownLatch(1);

        @Override
        public String tryLockWithWatchdog(String lockKey, long expireTime, TimeUnit expireUnit,
                                          long waitTime, TimeUnit waitUnit) {
            if (lock.isLocked()) {
                secondCallerWaiting.countDown();
            }
            lock.lock();
            return Thread.currentThread().getName();
        }

        @Override
        public boolean unlock(String lockKey, String lockValue) {
            lock.unlock();
            return true;
        }
    }
}
