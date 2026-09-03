package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.dto.*;
import com.blog.entity.*;
import com.blog.exception.BusinessException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserMapper;
import com.blog.security.password.PasswordResetCodeSecurity;
import com.blog.service.AuthSessionRevocationService;
import com.blog.service.CaptchaService;
import com.blog.service.EmailTemplateService;
import com.blog.service.NotificationService;
import com.blog.utils.JWTUtils;
import com.blog.utils.AuthUtils;
import com.blog.utils.PasswordPolicyUtils;
import com.blog.utils.RedisDistributedLock;
import com.blog.utils.RedisUtils;
import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.SendResetCodeDTO;
import com.blog.dto.ResetPasswordByCodeDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.concurrent.Executor;
import static org.mockito.Mockito.doThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplCoverageTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordResetCodeSecurity passwordResetCodeSecurity;
    @Mock
    private JWTUtils jwtUtils;
    @Mock
    private HttpServletRequest request;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private com.blog.service.NotificationService notificationService;
    @Mock
    private UserFollowMapper userFollowMapper;
    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private RedisDistributedLock redisDistributedLock;
    @Mock
    private RedisUtils redisUtils;
    @Mock
    private AuthSessionRevocationService authSessionRevocationService;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private EmailTemplateService emailTemplateService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private Executor notificationTaskExecutor;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
        lenient().when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyInt())).thenReturn("access-token");
        lenient().when(jwtUtils.generateRefreshToken(anyLong(), anyString(), anyInt())).thenReturn("refresh-token");
        lenient().when(jwtUtils.getRefreshJti(any())).thenReturn("jti");
        lenient().when(jwtUtils.getRefreshFamilyId(any())).thenReturn("family");
        lenient().when(jwtUtils.getRefreshTokenVersion(any())).thenReturn(0);
        lenient().when(jwtUtils.getRemainingRefreshTime(any())).thenReturn(3600L);
        lenient().when(jwtUtils.getRemainingTime(any())).thenReturn(3600L);
        lenient().when(jwtUtils.getJti(any())).thenReturn("jti");
        lenient().when(redisUtils.setString(any(), any(), anyLong(), any())).thenReturn(true);
        lenient().when(redisUtils.set(any(), any(), anyLong(), any())).thenReturn(true);
        lenient().when(redisUtils.rotateRefreshTokenFamily(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyLong())).thenReturn(1);
        lenient().when(passwordEncoder.matches(any(), any())).thenReturn(true);
        lenient().when(redisDistributedLock.tryLock(any(), anyLong(), any())).thenReturn("lock");
        lenient().when(redisDistributedLock.tryLockWithWatchdog(any(), anyLong(), any(), anyLong(), any())).thenReturn("lock");
        lenient().when(passwordResetCodeSecurity.generateCode()).thenReturn("123456");
        lenient().when(passwordResetCodeSecurity.digest(any(), any())).thenReturn("digest");
        lenient().when(passwordEncoder.encode(any())).thenReturn("encoded");
        lenient().when(passwordEncoder.matches(any(), any())).thenReturn(true);
        lenient().when(redisUtils.getExpire(any(), any())).thenReturn(0L);
        lenient().when(redisUtils.incrementWithinLimit(any(), anyLong(), anyLong())).thenReturn(true);
    }

    private void setUserId(Long userId) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("userId")).thenReturn(userId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    private void setAdmin(boolean isAdmin) {
        if (isAdmin) {
            org.springframework.security.core.authority.SimpleGrantedAuthority authority =
                    new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_admin");
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin", "password", List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }

    @Nested
    @DisplayName("用户注册")
    class Register {

        @Test
        @DisplayName("邮箱验证码错误")
        void invalidEmailCode() {
            when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(0);

            assertThrows(BusinessException.class, () -> userService.register(createRegisterDTO("user", "email", "nick")));
        }

        @Test
        @DisplayName("用户名已存在")
        void usernameExists() {
            when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
            when(userMapper.selectByUsername(any())).thenReturn(new User());

            assertThrows(BusinessException.class, () -> userService.register(createRegisterDTO("user", "email", "nick")));
        }

        @Test
        @DisplayName("邮箱已存在")
        void emailExists() {
            lenient().when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
            lenient().when(userMapper.selectByUsername(any())).thenReturn(null);
            lenient().when(userMapper.selectByEmail(any())).thenReturn(new User());

            assertThrows(BusinessException.class, () -> userService.register(createRegisterDTO("user", "email", "nick")));
        }

        @Test
        @DisplayName("密码与确认密码不一致")
        void passwordMismatch() {
            UserRegisterDTO dto = createRegisterDTO("user", "email", "nick");
            dto.setPassword("Password123!");
            dto.setConfirmPassword("Password456!");

            assertThrows(BusinessException.class, () -> userService.register(dto));
        }

        @Test
        @DisplayName("密码策略不满足")
        void passwordPolicyViolation() {
            UserRegisterDTO dto = createRegisterDTO("user", "email", "nick");
            dto.setPassword("short");
            dto.setConfirmPassword("short");

            assertThrows(BusinessException.class, () -> userService.register(dto));
        }

        @Test
        @DisplayName("插入用户失败")
        void insertFails() {
            when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
            when(userMapper.selectByUsername(any())).thenReturn(null);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(userMapper.insert(any())).thenReturn(0);

            assertThrows(BusinessException.class, () -> userService.register(createRegisterDTO("user", "email", "nick")));
        }

        @Test
        @DisplayName("非Spring事务环境同步降级")
        void syncFallback() {
            when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
            when(userMapper.selectByUsername(any())).thenReturn(null);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(userMapper.insert(any())).thenReturn(1);

            Result<String> result = userService.register(createRegisterDTO("user", "email", "nick"));
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("注册成功后清理Redis验证码")
        void redisCleanupAfterSuccess() {
            when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
            when(userMapper.selectByUsername(any())).thenReturn(null);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(userMapper.insert(any())).thenReturn(1);

            Result<String> result = userService.register(createRegisterDTO("user", "email", "nick"));
            assertThat(result.isSuccess()).isTrue();
            verify(redisUtils).delete(eq("register:code:email"));
            verify(redisUtils).delete(eq("register:code:limit:email"));
        }

        @Test
        @DisplayName("注册后异步欢迎邮件任务正常发送")
        void welcomeEmailTaskSendsEmail() throws Exception {
            org.springframework.test.util.ReflectionTestUtils.setField(userService, "mailFrom", "no-reply@lumina.cn");
            when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
            when(userMapper.selectByUsername(any())).thenReturn(null);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(userMapper.insert(any())).thenReturn(1);
            when(emailTemplateService.getWelcomeEmailHtml(any())).thenReturn("<html>welcome</html>");
            jakarta.mail.internet.MimeMessage mimeMessage = mock(jakarta.mail.internet.MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            userService.register(createRegisterDTO("user", "email", "nick"));

            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            verify(notificationTaskExecutor).execute(captor.capture());
            captor.getValue().run();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("欢迎邮件发送失败不影响注册结果")
        void welcomeEmailFailureSwallowed() throws Exception {
            org.springframework.test.util.ReflectionTestUtils.setField(userService, "mailFrom", "no-reply@lumina.cn");
            when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
            when(userMapper.selectByUsername(any())).thenReturn(null);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(userMapper.insert(any())).thenReturn(1);
            when(emailTemplateService.getWelcomeEmailHtml(any())).thenReturn("<html>welcome</html>");
            jakarta.mail.internet.MimeMessage mimeMessage = mock(jakarta.mail.internet.MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("smtp down")).when(mailSender).send(mimeMessage);

            Result<String> result = userService.register(createRegisterDTO("user", "email", "nick"));

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            verify(notificationTaskExecutor).execute(captor.capture());
            captor.getValue().run();
        }

        @Test
        @DisplayName("验证码尝试次数超限应锁定并拒绝")
        void tooManyAttempts_shouldLock() {
            when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(-1);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.register(createRegisterDTO("user", "email", "nick")));
            assertThat(ex.getMessage()).contains("尝试次数过多");
            verify(userMapper, never()).insert(any());
        }

        @Test
        @DisplayName("验证码消费时校验码应使用摘要而非明文")
        void shouldVerifyDigestOfCode() {
            when(redisUtils.consumePasswordResetCode(anyString(), anyString(), anyString(), anyString())).thenReturn(1);
            when(passwordResetCodeSecurity.digest("email", "123456")).thenReturn("expected-digest");
            when(userMapper.selectByUsername(any())).thenReturn(null);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(userMapper.insert(any())).thenReturn(1);

            userService.register(createRegisterDTO("user", "email", "nick"));

            verify(redisUtils).consumePasswordResetCode(
                    eq("register:code:email"),
                    eq("register:code:attempts:email"),
                    eq("register:code:lock:email"),
                    eq("expected-digest"));
        }
    }

    @Nested
    @DisplayName("用户登录")
    class Login {

        @Test
        @DisplayName("验证码错误")
        void captchaFailed() {
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(false);

            assertThrows(BusinessException.class, () -> login("user", "pass"));
        }

        @Test
        @DisplayName("用户不存在")
        void userNotFound() {
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(userMapper.selectByUsername(any())).thenReturn(null);
            when(userMapper.selectByEmail(any())).thenReturn(null);

            assertThrows(BusinessException.class, () -> login("user", "pass"));
        }

        @Test
        @DisplayName("用户未激活")
        void userNotActive() {
            User user = new User();
            user.setId(1L);
            user.setUsername("user");
            user.setPassword("encoded");
            user.setStatus(0);
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(userMapper.selectByUsername(any())).thenReturn(user);

            assertThrows(BusinessException.class, () -> login("user", "pass"));
        }

        @Test
        @DisplayName("用户被禁用")
        void userDisabled() {
            User user = new User();
            user.setId(1L);
            user.setUsername("user");
            user.setPassword("encoded");
            user.setStatus(2);
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(userMapper.selectByUsername(any())).thenReturn(user);

            assertThrows(BusinessException.class, () -> login("user", "pass"));
        }

        @Test
        @DisplayName("密码错误")
        void wrongPassword() {
            User user = new User();
            user.setId(1L);
            user.setUsername("user");
            user.setPassword("encoded");
            user.setStatus(1);
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(userMapper.selectByUsername(any())).thenReturn(user);
            when(passwordEncoder.matches(any(), any())).thenReturn(false);

            assertThrows(BusinessException.class, () -> login("user", "pass"));
        }

        @Test
        @DisplayName("登录成功")
        void loginSuccess() {
            User user = new User();
            user.setId(1L);
            user.setUsername("user");
            user.setPassword("encoded");
            user.setStatus(1);
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(userMapper.selectByUsername(any())).thenReturn(user);
            lenient().when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyInt())).thenReturn("access");
            lenient().when(jwtUtils.generateRefreshToken(anyLong(), anyString(), anyInt())).thenReturn("refresh");

            Result<UserDTO> result = login("user", "pass");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getUsername()).isEqualTo("user");
        }

        @Test
        @DisplayName("手机号格式登录成功")
        void phoneLoginSuccess() {
            lenient().when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            lenient().when(userMapper.selectByUsername(any())).thenReturn(null);
            lenient().when(userMapper.selectByEmail(any())).thenReturn(null);
            User user = new User();
            user.setId(1L);
            user.setUsername("user");
            user.setPassword("encoded");
            user.setStatus(1);
            lenient().when(userMapper.selectOne(any())).thenReturn(user);

            Result<UserDTO> result = login("13800138000", "pass");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getUsername()).isEqualTo("user");
        }

        @Test
        @DisplayName("登录成功更新最后登录时间和IP")
        void loginUpdatesLastLoginInfo() {
            lenient().when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            User user = new User();
            user.setId(1L);
            user.setUsername("user");
            user.setPassword("encoded");
            user.setStatus(1);
            when(userMapper.selectByUsername(any())).thenReturn(user);
            lenient().when(request.getHeader(anyString())).thenReturn("127.0.0.1");

            Result<UserDTO> result = login("user", "pass");
            assertThat(result.isSuccess()).isTrue();
            assertThat(user.getLastLoginTime()).isNotNull();
            assertThat(user.getLastLoginIp()).isEqualTo("127.0.0.1");
            verify(userMapper).updateById(user);
        }
    }

    @Nested
    @DisplayName("登出")
    class Logout {

        @Test
        @DisplayName("正常登出")
        void logoutSuccess() {
            when(jwtUtils.validateRefreshToken(any())).thenReturn(false);

            Result<Void> result = userService.logout(1L, "refresh");
            assertThat(result.isSuccess()).isTrue();
            verify(authSessionRevocationService).revokeUserSessions(1L);
        }

        @Test
        @DisplayName("accessToken 黑名单")
        void accessTokenBlacklist() {
            lenient().when(jwtUtils.validateToken(any())).thenReturn(true);
            lenient().when(jwtUtils.getRemainingTime(any())).thenReturn(60L);

            Result<Void> result = userService.logout(1L, null, "Bearer access-token");
            assertThat(result.isSuccess()).isTrue();
            verify(redisUtils).set(contains("auth:blacklist:"), eq("1"), eq(60L), any());
        }

        @Test
        @DisplayName("获取分布式锁失败")
        void lockNullThrows() {
            when(redisDistributedLock.tryLockWithWatchdog(any(), anyLong(), any(), anyLong(), any())).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.logout(1L, "refresh"));
        }

        @Test
        @DisplayName("撤销认证会话失败")
        void revokeThrows() {
            when(jwtUtils.validateRefreshToken(any())).thenReturn(true);
            when(jwtUtils.getUserIdFromRefreshToken(any())).thenReturn(1L);
            when(jwtUtils.getRefreshFamilyId(any())).thenReturn(null);
            when(redisDistributedLock.tryLockWithWatchdog(any(), anyLong(), any(), anyLong(), any())).thenReturn("lock");
            doThrow(new RuntimeException("revoke failed")).when(authSessionRevocationService).revokeUserSessions(anyLong());

            assertThrows(BusinessException.class, () -> userService.logout(1L, "refresh"));
        }

        @Test
        @DisplayName("仅提供accessToken登出")
        void onlyAccessToken() {
            when(jwtUtils.validateRefreshToken(any())).thenReturn(false);
            lenient().when(jwtUtils.validateToken(any())).thenReturn(true);
            lenient().when(jwtUtils.getRemainingTime(any())).thenReturn(60L);

            Result<Void> result = userService.logout(1L, null, "Bearer access-token");
            assertThat(result.isSuccess()).isTrue();
            verify(authSessionRevocationService).revokeUserSessions(1L);
            verify(redisUtils).set(contains("auth:blacklist:"), eq("1"), eq(60L), any());
        }
    }

    @Nested
    @DisplayName("刷新令牌")
    class RefreshToken {

        @Test
        @DisplayName("空令牌")
        void emptyToken() {
            assertThrows(BusinessException.class, () -> userService.refreshToken(null));
        }

        @Test
        @DisplayName("无效令牌")
        void invalidToken() {
            when(jwtUtils.validateRefreshToken(any())).thenReturn(false);

            assertThrows(BusinessException.class, () -> userService.refreshToken("token"));
        }

        @Test
        @DisplayName("令牌已过期")
        void expiredToken() {
            when(jwtUtils.validateRefreshToken(any())).thenReturn(true);
            when(jwtUtils.isRefreshTokenExpired(any())).thenReturn(true);

            assertThrows(BusinessException.class, () -> userService.refreshToken("token"));
        }

        @Test
        @DisplayName("用户不存在")
        void userNotFound() {
            when(jwtUtils.validateRefreshToken(any())).thenReturn(true);
            when(jwtUtils.isRefreshTokenExpired(any())).thenReturn(false);
            when(jwtUtils.getUserIdFromRefreshToken(any())).thenReturn(1L);
            when(jwtUtils.getUsernameFromRefreshToken(any())).thenReturn("user");
            when(jwtUtils.getRefreshFamilyId(any())).thenReturn("family");
            when(jwtUtils.getRefreshGeneration(any())).thenReturn(0);
            when(userMapper.selectById(1L)).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.refreshToken("token"));
        }

        @Test
        @DisplayName("令牌类型错误")
        void isRefreshTokenFalse() {
            when(jwtUtils.validateRefreshToken(any())).thenReturn(true);
            when(jwtUtils.isRefreshToken(any())).thenReturn(false);

            assertThrows(BusinessException.class, () -> userService.refreshToken("token"));
        }

        @Test
        @DisplayName("用户状态为空")
        void userStatusNull() {
            when(jwtUtils.validateRefreshToken(any())).thenReturn(true);
            when(jwtUtils.isRefreshToken(any())).thenReturn(true);
            when(jwtUtils.isRefreshTokenExpired(any())).thenReturn(false);
            when(jwtUtils.getUserIdFromRefreshToken(any())).thenReturn(1L);
            when(jwtUtils.getUsernameFromRefreshToken(any())).thenReturn("user");
            when(jwtUtils.getRefreshFamilyId(any())).thenReturn("family");
            when(jwtUtils.getRefreshGeneration(any())).thenReturn(0);
            when(jwtUtils.getRefreshTokenVersion(any())).thenReturn(0);
            User user = new User();
            user.setId(1L);
            user.setStatus(null);
            when(userMapper.selectById(1L)).thenReturn(user);

            assertThrows(BusinessException.class, () -> userService.refreshToken("token"));
        }

        @Test
        @DisplayName("刷新令牌版本不匹配")
        void tokenVersionMismatch() {
            when(jwtUtils.validateRefreshToken(any())).thenReturn(true);
            when(jwtUtils.isRefreshToken(any())).thenReturn(true);
            when(jwtUtils.isRefreshTokenExpired(any())).thenReturn(false);
            when(jwtUtils.getUserIdFromRefreshToken(any())).thenReturn(1L);
            when(jwtUtils.getUsernameFromRefreshToken(any())).thenReturn("user");
            when(jwtUtils.getRefreshFamilyId(any())).thenReturn("family");
            when(jwtUtils.getRefreshGeneration(any())).thenReturn(0);
            when(jwtUtils.getRefreshTokenVersion(any())).thenReturn(1);
            User user = new User();
            user.setId(1L);
            user.setStatus(1);
            user.setTokenVersion(0);
            when(userMapper.selectById(1L)).thenReturn(user);

            assertThrows(BusinessException.class, () -> userService.refreshToken("token"));
        }

        @Test
        @DisplayName("令牌族轮换异常且撤销成功")
        void rotateNotOneAndRevokeSucceeds() {
            when(jwtUtils.validateRefreshToken(any())).thenReturn(true);
            when(jwtUtils.isRefreshToken(any())).thenReturn(true);
            when(jwtUtils.isRefreshTokenExpired(any())).thenReturn(false);
            when(jwtUtils.getUserIdFromRefreshToken(any())).thenReturn(1L);
            when(jwtUtils.getUsernameFromRefreshToken(any())).thenReturn("user");
            when(jwtUtils.getRefreshFamilyId(any())).thenReturn("family");
            when(jwtUtils.getRefreshGeneration(any())).thenReturn(0);
            when(jwtUtils.getRefreshTokenVersion(any())).thenReturn(0);
            when(jwtUtils.getRefreshJti(any())).thenReturn("jti");
            lenient().when(jwtUtils.generateAccessToken(anyLong(), anyString(), anyInt())).thenReturn("access");
            lenient().when(jwtUtils.generateRefreshToken(anyLong(), anyString(), anyInt(), any(), anyInt())).thenReturn("refresh");
            when(jwtUtils.getRefreshJti("refresh")).thenReturn("newJti");
            when(jwtUtils.getRemainingRefreshTime(any())).thenReturn(3600L);
            User user = new User();
            user.setId(1L);
            user.setStatus(1);
            user.setTokenVersion(0);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(redisUtils.rotateRefreshTokenFamily(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), anyLong())).thenReturn(2);
            doNothing().when(authSessionRevocationService).revokeFamily(anyLong(), anyString());

            assertThrows(BusinessException.class, () -> userService.refreshToken("token"));
        }
    }

    @Nested
    @DisplayName("更新用户信息")
    class UpdateUserInfo {

        @Test
        @DisplayName("用户不存在")
        void userNotFound() {
            when(userMapper.selectById(anyLong())).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.updateUserInfo(1L, new UserUpdateDTO()));
        }

        @Test
        @DisplayName("邮箱已被占用")
        void emailExists() {
            User user = new User();
            user.setId(1L);
            user.setEmail("old@example.com");
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.selectByEmail(any())).thenReturn(new User());

            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setEmail("new@example.com");

            assertThrows(BusinessException.class, () -> userService.updateUserInfo(1L, dto));
        }

        @Test
        @DisplayName("空 nickname 设为 null")
        void emptyNicknameSetToNull() {
            User user = new User();
            user.setId(1L);
            user.setNickname("old");
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateById(any())).thenReturn(1);

            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("");

            userService.updateUserInfo(1L, dto);
            assertThat(user.getNickname()).isNull();
        }

        @Test
        @DisplayName("空 email 不更新")
        void emptyEmailNotUpdated() {
            User user = new User();
            user.setId(1L);
            user.setEmail("old@example.com");
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateById(any())).thenReturn(1);

            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setEmail("");

            userService.updateUserInfo(1L, dto);
            assertThat(user.getEmail()).isEqualTo("old@example.com");
        }

        @Test
        @DisplayName("null 字段保持原值")
        void nullFieldsPreserved() {
            User user = new User();
            user.setId(1L);
            user.setNickname("nick");
            user.setPhone("phone");
            user.setAvatar("avatar");
            user.setBio("bio");
            user.setWebsite("website");
            user.setPosition("position");
            user.setCompany("company");
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateById(any())).thenReturn(1);

            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname(null);
            dto.setPhone(null);
            dto.setAvatar(null);
            dto.setBio(null);
            dto.setWebsite(null);
            dto.setPosition(null);
            dto.setCompany(null);

            userService.updateUserInfo(1L, dto);
            assertThat(user.getNickname()).isEqualTo("nick");
            assertThat(user.getPhone()).isEqualTo("phone");
            assertThat(user.getAvatar()).isEqualTo("avatar");
            assertThat(user.getBio()).isEqualTo("bio");
            assertThat(user.getWebsite()).isEqualTo("website");
            assertThat(user.getPosition()).isEqualTo("position");
            assertThat(user.getCompany()).isEqualTo("company");
        }

        @Test
        @DisplayName("更新用户信息失败")
        void updateUserInfoFails() {
            User user = new User();
            user.setId(1L);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.updateById(any())).thenReturn(0);

            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("new");

            assertThrows(BusinessException.class, () -> userService.updateUserInfo(1L, dto));
        }

        @Test
        @DisplayName("非空字段应 trim 后更新")
        void nonBlankFieldsAreTrimmed() {
            User user = new User();
            user.setId(1L);
            user.setEmail("old@example.com");
            when(userMapper.selectById(1L)).thenReturn(user);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(userMapper.updateById(any())).thenReturn(1);

            UserUpdateDTO dto = new UserUpdateDTO();
            dto.setNickname("  nick  ");
            dto.setEmail(" new@example.com ");
            dto.setPhone(" 13800138000 ");
            dto.setAvatar(" http://avatar ");
            dto.setBio(" hello ");
            dto.setWebsite(" http://site ");
            dto.setPosition(" dev ");
            dto.setCompany(" acme ");

            userService.updateUserInfo(1L, dto);

            assertThat(user.getNickname()).isEqualTo("nick");
            assertThat(user.getEmail()).isEqualTo("new@example.com");
            assertThat(user.getPhone()).isEqualTo("13800138000");
            assertThat(user.getAvatar()).isEqualTo("http://avatar");
            assertThat(user.getBio()).isEqualTo("hello");
            assertThat(user.getWebsite()).isEqualTo("http://site");
            assertThat(user.getPosition()).isEqualTo("dev");
            assertThat(user.getCompany()).isEqualTo("acme");
        }
    }

    @Nested
    @DisplayName("关注用户")
    class Follow {

        @Test
        @DisplayName("自己关注自己")
        void followSelf() {
            assertThrows(BusinessException.class, () -> userService.follow(1L, 1L));
        }

        @Test
        @DisplayName("被关注者不存在")
        void followingUserNotFound() {
            when(userMapper.selectById(anyLong())).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.follow(1L, 2L));
        }

        @Test
        @DisplayName("已关注")
        void alreadyFollowing() {
            User following = new User();
            following.setId(2L);
            when(userMapper.selectById(2L)).thenReturn(following);
            UserFollow exist = new UserFollow();
            exist.setId(1L);
            exist.setDeleted(0);
            when(userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(anyLong(), anyLong())).thenReturn(exist);

            assertThrows(BusinessException.class, () -> userService.follow(1L, 2L));
        }

        @Test
        @DisplayName("恢复关注关系")
        void restoreFollow() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                User following = new User();
                following.setId(2L);
                when(userMapper.selectById(2L)).thenReturn(following);
                UserFollow exist = new UserFollow();
                exist.setId(1L);
                exist.setDeleted(1);
                when(userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(anyLong(), anyLong())).thenReturn(exist);
                when(userFollowMapper.restoreFollow(1L)).thenReturn(1);

                Result<Void> result = userService.follow(1L, 2L);
                assertThat(result.isSuccess()).isTrue();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("关注成功")
        void followSuccess() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                User following = new User();
                following.setId(2L);
                when(userMapper.selectById(2L)).thenReturn(following);
                when(userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(anyLong(), anyLong())).thenReturn(null);
                when(userFollowMapper.insert(any())).thenReturn(1);

                Result<Void> result = userService.follow(1L, 2L);
                assertThat(result.isSuccess()).isTrue();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("关注后 afterCommit 更新计数成功")
        void afterCommitUpdatesCounters() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                User following = new User();
                following.setId(2L);
                when(userMapper.selectById(2L)).thenReturn(following);
                when(userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(anyLong(), anyLong())).thenReturn(null);
                when(userFollowMapper.insert(any())).thenReturn(1);
                when(userMapper.incrementFollowerCount(2L)).thenReturn(1);
                when(userMapper.incrementFollowingCount(1L)).thenReturn(1);

                userService.follow(1L, 2L);
                for (TransactionSynchronization ts : TransactionSynchronizationManager.getSynchronizations()) {
                    ts.afterCommit();
                }

                verify(userMapper).incrementFollowerCount(2L);
                verify(userMapper).incrementFollowingCount(1L);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("关注后 afterCommit 计数失败重试成功")
        void afterCommitCounterRetriesOnFailure() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                User following = new User();
                following.setId(2L);
                when(userMapper.selectById(2L)).thenReturn(following);
                when(userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(anyLong(), anyLong())).thenReturn(null);
                when(userFollowMapper.insert(any())).thenReturn(1);
                when(userMapper.incrementFollowerCount(2L))
                        .thenThrow(new RuntimeException("db down"))
                        .thenReturn(1);
                when(userMapper.incrementFollowingCount(1L)).thenReturn(1);

                userService.follow(1L, 2L);
                for (TransactionSynchronization ts : TransactionSynchronizationManager.getSynchronizations()) {
                    ts.afterCommit();
                }

                verify(userMapper, times(2)).incrementFollowerCount(2L);
                verify(userMapper).incrementFollowingCount(1L);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("关注后 afterCommit 计数一直失败重试到上限")
        void afterCommitCounterRetriesExhausted() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                User following = new User();
                following.setId(2L);
                when(userMapper.selectById(2L)).thenReturn(following);
                when(userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(anyLong(), anyLong())).thenReturn(null);
                when(userFollowMapper.insert(any())).thenReturn(1);
                when(userMapper.incrementFollowerCount(2L)).thenThrow(new RuntimeException("db down"));
                when(userMapper.incrementFollowingCount(1L)).thenReturn(1);

                userService.follow(1L, 2L);
                for (TransactionSynchronization ts : TransactionSynchronizationManager.getSynchronizations()) {
                    ts.afterCommit();
                }

                verify(userMapper, times(5)).incrementFollowerCount(2L);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("取消关注后 afterCommit 递减计数失败重试成功")
        void unfollowAfterCommitCounterRetries() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                UserFollow follow = new UserFollow();
                follow.setId(1L);
                follow.setFollowerId(1L);
                follow.setFollowingId(2L);
                when(userFollowMapper.selectOne(any())).thenReturn(follow);
                when(userFollowMapper.deleteById(1L)).thenReturn(1);
                when(userMapper.decrementFollowerCount(2L))
                        .thenThrow(new RuntimeException("db down"))
                        .thenReturn(1);
                when(userMapper.decrementFollowingCount(1L)).thenReturn(1);

                userService.unfollow(1L, 2L);
                for (TransactionSynchronization ts : TransactionSynchronizationManager.getSynchronizations()) {
                    ts.afterCommit();
                }

                verify(userMapper, times(2)).decrementFollowerCount(2L);
                verify(userMapper).decrementFollowingCount(1L);
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("获取分布式锁失败")
        void tryLockNull() {
            when(redisDistributedLock.tryLock(any(), anyLong(), any())).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.follow(1L, 2L));
        }

        @Test
        @DisplayName("发送通知失败不影响关注结果")
        void notificationThrows() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                User following = new User();
                following.setId(2L);
                when(userMapper.selectById(2L)).thenReturn(following);
                when(userFollowMapper.selectByFollowerAndFollowingIncludingDeleted(anyLong(), anyLong())).thenReturn(null);
                when(userFollowMapper.insert(any())).thenReturn(1);
                doThrow(new RuntimeException("notify failed")).when(notificationService).createNotification(anyLong(), anyLong(), anyInt(), anyLong(), anyInt(), anyString());

                Result<Void> result = userService.follow(1L, 2L);
                assertThat(result.isSuccess()).isTrue();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }

    @Nested
    @DisplayName("取消关注")
    class Unfollow {

        @Test
        @DisplayName("未关注")
        void notFollowing() {
            when(userFollowMapper.selectOne(any())).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.unfollow(1L, 2L));
        }

        @Test
        @DisplayName("取消关注成功")
        void unfollowSuccess() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                UserFollow follow = new UserFollow();
                follow.setId(1L);
                when(userFollowMapper.selectOne(any())).thenReturn(follow);
                when(userFollowMapper.deleteById(1L)).thenReturn(1);

                Result<Void> result = userService.unfollow(1L, 2L);
                assertThat(result.isSuccess()).isTrue();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }

        @Test
        @DisplayName("取消关注后递减计数")
        void afterCommitDecrementCounters() {
            TransactionSynchronizationManager.initSynchronization();
            try {
                UserFollow follow = new UserFollow();
                follow.setId(1L);
                follow.setFollowerId(1L);
                follow.setFollowingId(2L);
                when(userFollowMapper.selectOne(any())).thenReturn(follow);
                when(userFollowMapper.deleteById(1L)).thenReturn(1);

                Result<Void> result = userService.unfollow(1L, 2L);
                assertThat(result.isSuccess()).isTrue();
            } finally {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }

    @Nested
    @DisplayName("公开用户信息")
    class GetPublicUserInfo {

        @Test
        @DisplayName("用户不存在")
        void userNotFound() {
            when(userMapper.selectById(anyLong())).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.getPublicUserInfo(1L));
        }

        @Test
        @DisplayName("未登录 - 未关注")
        void notLoggedIn() {
            User user = new User();
            user.setId(1L);
            user.setUsername("user");
            user.setNickname("User");
            user.setRole(1);
            user.setCreateTime(LocalDateTime.now());
            user.setFollowerCount(0);
            user.setFollowingCount(0);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(articleMapper.selectCount(any())).thenReturn(0L);
            when(commentMapper.selectCount(any())).thenReturn(0L);

            Result<PublicUserProfileDTO> result = userService.getPublicUserInfo(1L);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getIsFollowed()).isFalse();
        }

        @Test
        @DisplayName("查看自己的公开信息")
        void viewOwnProfile() {
            User user = new User();
            user.setId(1L);
            user.setUsername("user");
            user.setNickname("User");
            user.setRole(1);
            user.setCreateTime(LocalDateTime.now());
            user.setFollowerCount(0);
            user.setFollowingCount(0);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(articleMapper.selectCount(any())).thenReturn(0L);
            when(commentMapper.selectCount(any())).thenReturn(0L);
            setUserId(1L);

            Result<PublicUserProfileDTO> result = userService.getPublicUserInfo(1L);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getIsFollowed()).isFalse();
        }

        @Test
        @DisplayName("已关注该用户")
        void alreadyFollows() {
            User user = new User();
            user.setId(2L);
            user.setUsername("user2");
            user.setNickname("User2");
            user.setRole(1);
            user.setCreateTime(LocalDateTime.now());
            user.setFollowerCount(0);
            user.setFollowingCount(0);
            when(userMapper.selectById(2L)).thenReturn(user);
            when(articleMapper.selectCount(any())).thenReturn(0L);
            when(commentMapper.selectCount(any())).thenReturn(0L);
            when(userFollowMapper.selectCount(any())).thenReturn(1L);
            setUserId(1L);

            Result<PublicUserProfileDTO> result = userService.getPublicUserInfo(2L);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getIsFollowed()).isTrue();
        }
    }

    @Nested
    @DisplayName("作者排行榜")
    class GetTopAuthors {

        @Test
        @DisplayName("空列表")
        void emptyList() {
            when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            Result<List<PublicUserProfileDTO>> result = userService.getTopAuthors(10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("返回的公开资料不应包含邮箱和手机号")
        void shouldNotExposeEmailOrPhone() throws Exception {
            User author = new User();
            author.setId(1L);
            author.setUsername("alice");
            author.setNickname("alice");
            author.setEmail("alice@example.com");
            author.setPhone("13800000000");
            when(userMapper.selectList(any())).thenReturn(List.of(author));

            Result<List<PublicUserProfileDTO>> result = userService.getTopAuthors(10);

            assertThat(result.isSuccess()).isTrue();
            PublicUserProfileDTO dto = result.getData().get(0);
            assertThat(dto.getUsername()).isEqualTo("alice");
            assertThat(dto.getId()).isEqualTo(1L);
            // PublicUserProfileDTO 不应声明 email/phone 敏感字段
            for (java.lang.reflect.Field field : PublicUserProfileDTO.class.getDeclaredFields()) {
                assertThat(field.getName()).isNotIn("email", "phone", "lastLoginIp");
            }
        }

        @Test
        @DisplayName("limit 超上限时应被截断到最大值")
        void limit_oversized_shouldBeCapped() {
            when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            userService.getTopAuthors(10000);

            verify(userMapper).selectList(argThat(wrapper ->
                    wrapper.getSqlSegment().contains("LIMIT 50")));
        }
    }

    @Nested
    @DisplayName("用户列表")
    class GetUserList {

        @Test
        @DisplayName("空分页")
        void emptyPage() {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
            when(userMapper.selectPage(any(), any())).thenReturn(page);
            when(page.getRecords()).thenReturn(Collections.emptyList());

            Result<PageResult<UserDTO>> result = userService.getUserList(1, 10, null);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).isEmpty();
        }
    }

    @Nested
    @DisplayName("发送注册验证码")
    class SendRegisterVerifyCode {

        @Test
        @DisplayName("邮箱已被注册")
        void emailExists() {
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(userMapper.selectByEmail(any())).thenReturn(new User());

            assertThrows(BusinessException.class, () -> userService.sendRegisterVerifyCode(sendRegisterCodeDTO("email")));
        }

        @Test
        @DisplayName("发送过于频繁")
        void tooFrequent() {
            lenient().when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            lenient().when(userMapper.selectByEmail(any())).thenReturn(null);
            lenient().when(redisUtils.getExpire(any(), any())).thenReturn(10L);

            assertThrows(BusinessException.class, () -> userService.sendRegisterVerifyCode(sendRegisterCodeDTO("email")));
        }

        @Test
        @DisplayName("图形验证码错误")
        void captchaFailed() {
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(false);

            assertThrows(BusinessException.class, () -> userService.sendRegisterVerifyCode(sendRegisterCodeDTO("email")));
        }

        @Test
        @DisplayName("验证码缓存写入失败")
        void cacheWriteFails() throws Exception {
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(redisUtils.getExpire(any(), any())).thenReturn(0L);
            when(redisUtils.set(any(), any(), anyLong(), any())).thenReturn(false);

            assertThrows(BusinessException.class, () -> userService.sendRegisterVerifyCode(sendRegisterCodeDTO("email")));
        }

        @Test
        @DisplayName("发送成功")
        void sendSuccess() throws Exception {
            org.springframework.test.util.ReflectionTestUtils.setField(userService, "mailFrom", "no-reply@lumina.cn");
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(redisUtils.getExpire(any(), any())).thenReturn(0L);
            when(redisUtils.set(any(), any(), anyLong(), any())).thenReturn(true);
            when(emailTemplateService.getRegisterVerifyCodeEmailHtml(any(), anyLong()))
                    .thenReturn("<html>code</html>");
            jakarta.mail.internet.MimeMessage mimeMessage = mock(jakarta.mail.internet.MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            Result<Void> result = userService.sendRegisterVerifyCode(sendRegisterCodeDTO("email@example.com"));

            assertThat(result.isSuccess()).isTrue();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("邮件发送失败应删除验证码并报错")
        void sendFailure_shouldCleanup() throws Exception {
            org.springframework.test.util.ReflectionTestUtils.setField(userService, "mailFrom", "no-reply@lumina.cn");
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(redisUtils.getExpire(any(), any())).thenReturn(0L);
            when(redisUtils.set(any(), any(), anyLong(), any())).thenReturn(true);
            jakarta.mail.internet.MimeMessage mimeMessage = mock(jakarta.mail.internet.MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("smtp error")).when(mailSender).send(mimeMessage);

            assertThrows(BusinessException.class, () -> userService.sendRegisterVerifyCode(sendRegisterCodeDTO("email@example.com")));
            verify(redisUtils, times(2)).delete(anyString());
        }
    }

    @Nested
    @DisplayName("获取用户信息")
    class GetUserInfo {

        @Test
        @DisplayName("用户不存在")
        void userNotFound() {
            when(userMapper.selectById(anyLong())).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.getUserInfo(1L));
        }
    }

    @Nested
    @DisplayName("修改密码")
    class ChangePassword {

        @Test
        @DisplayName("新密码不符合策略")
        void newPasswordPolicyViolation() {
            when(userMapper.selectById(anyLong())).thenReturn(new User());
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            ChangePasswordDTO dto = new ChangePasswordDTO();
            dto.setOldPassword("old");
            dto.setNewPassword("short");

            assertThrows(BusinessException.class, () -> userService.changePassword(1L, dto, "auth"));
        }

        @Test
        @DisplayName("更新密码失败")
        void updatePasswordFails() {
            User user = new User();
            user.setId(1L);
            user.setPassword("encoded");
            when(userMapper.selectById(1L)).thenReturn(user);
            when(passwordEncoder.matches(any(), any())).thenReturn(true);
            when(userMapper.updatePasswordAndIncrementTokenVersion(anyLong(), any(), any())).thenReturn(0);

            ChangePasswordDTO dto = new ChangePasswordDTO();
            dto.setOldPassword("old");
            dto.setNewPassword("Password123!");

            assertThrows(BusinessException.class, () -> userService.changePassword(1L, dto, "auth"));
        }
    }

    @Nested
    @DisplayName("发送密码重置验证码")
    class SendResetCode {

        @Test
        @DisplayName("图形验证码错误")
        void captchaFailed() {
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(false);

            SendResetCodeDTO dto = new SendResetCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCaptchaKey("key");
            dto.setCaptcha("code");

            assertThrows(BusinessException.class, () -> userService.sendResetCode(dto));
        }

        @Test
        @DisplayName("频率限制")
        void rateLimited() {
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(redisUtils.incrementWithinLimit(any(), anyLong(), anyLong())).thenReturn(false);

            SendResetCodeDTO dto = new SendResetCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCaptchaKey("key");
            dto.setCaptcha("code");

            assertThrows(BusinessException.class, () -> userService.sendResetCode(dto));
        }

        @Test
        @DisplayName("Redis缓存失败降级")
        void redisCacheFailureFallback() {
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(redisUtils.incrementWithinLimit(any(), anyLong(), anyLong())).thenReturn(true);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(redisUtils.setString(any(), any(), anyLong(), any())).thenThrow(new RuntimeException("redis down"));

            SendResetCodeDTO dto = new SendResetCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCaptchaKey("key");
            dto.setCaptcha("code");

            Result<Void> result = userService.sendResetCode(dto);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("用户不存在仍返回成功")
        void userNotFoundStillSuccess() {
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(redisUtils.incrementWithinLimit(any(), anyLong(), anyLong())).thenReturn(true);
            when(userMapper.selectByEmail(any())).thenReturn(null);
            when(redisUtils.setString(any(), any(), anyLong(), any())).thenReturn(true);

            SendResetCodeDTO dto = new SendResetCodeDTO();
            dto.setEmail("notexist@example.com");
            dto.setCaptchaKey("key");
            dto.setCaptcha("code");

            Result<Void> result = userService.sendResetCode(dto);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("异步邮件任务失败降级")
        void asyncEmailTaskFailureFallback() {
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(redisUtils.incrementWithinLimit(any(), anyLong(), anyLong())).thenReturn(true);
            User user = new User();
            user.setEmail("test@example.com");
            when(userMapper.selectByEmail(any())).thenReturn(user);
            when(redisUtils.setString(any(), any(), anyLong(), any())).thenReturn(true);
            doThrow(new RuntimeException("executor down")).when(notificationTaskExecutor).execute(any());

            SendResetCodeDTO dto = new SendResetCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCaptchaKey("key");
            dto.setCaptcha("code");

            Result<Void> result = userService.sendResetCode(dto);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("异步邮件任务正常执行发送密码重置邮件")
        void asyncEmailTaskSendsEmail() throws Exception {
            org.springframework.test.util.ReflectionTestUtils.setField(userService, "mailFrom", "no-reply@lumina.cn");
            when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
            when(redisUtils.incrementWithinLimit(any(), anyLong(), anyLong())).thenReturn(true);
            User user = new User();
            user.setEmail("test@example.com");
            when(userMapper.selectByEmail(any())).thenReturn(user);
            when(redisUtils.setString(any(), any(), anyLong(), any())).thenReturn(true);
            when(emailTemplateService.getResetPasswordEmailHtml(any(), anyLong())).thenReturn("<html>reset</html>");
            jakarta.mail.internet.MimeMessage mimeMessage = mock(jakarta.mail.internet.MimeMessage.class);
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            SendResetCodeDTO dto = new SendResetCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCaptchaKey("key");
            dto.setCaptcha("code");

            userService.sendResetCode(dto);

            ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
            verify(notificationTaskExecutor).execute(captor.capture());
            captor.getValue().run();
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    @DisplayName("验证码重置密码")
    class ResetPasswordByCode {

        @Test
        @DisplayName("新密码不符合策略")
        void newPasswordPolicyViolation() {
            when(userMapper.selectByEmail(any())).thenReturn(new User());

            ResetPasswordByCodeDTO dto = new ResetPasswordByCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCode("123456");
            dto.setNewPassword("short");

            assertThrows(BusinessException.class, () -> userService.resetPasswordByCode(dto));
        }

        @Test
        @DisplayName("用户不存在")
        void userNotFound() {
            when(userMapper.selectByEmail(any())).thenReturn(null);

            ResetPasswordByCodeDTO dto = new ResetPasswordByCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCode("123456");
            dto.setNewPassword("Password123!");

            assertThrows(BusinessException.class, () -> userService.resetPasswordByCode(dto));
        }

        @Test
        @DisplayName("验证码消费失败")
        void claimCodeFails() {
            User user = new User();
            user.setId(1L);
            when(userMapper.selectByEmail(any())).thenReturn(user);
            when(redisUtils.claimPasswordResetCode(any(), any(), any(), any(), any(), any(), anyLong())).thenReturn(0);

            ResetPasswordByCodeDTO dto = new ResetPasswordByCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCode("123456");
            dto.setNewPassword("Password123!");

            assertThrows(BusinessException.class, () -> userService.resetPasswordByCode(dto));
        }

        @Test
        @DisplayName("更新密码失败")
        void updatePasswordFails() {
            User user = new User();
            user.setId(1L);
            when(userMapper.selectByEmail(any())).thenReturn(user);
            when(redisUtils.claimPasswordResetCode(any(), any(), any(), any(), any(), any(), anyLong())).thenReturn(1);
            when(userMapper.updatePasswordAndIncrementTokenVersion(anyLong(), any(), any())).thenReturn(0);

            ResetPasswordByCodeDTO dto = new ResetPasswordByCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCode("123456");
            dto.setNewPassword("Password123!");

            assertThrows(BusinessException.class, () -> userService.resetPasswordByCode(dto));
        }

        @Test
        @DisplayName("更新异常触发释放claim")
        void runtimeExceptionTriggersReleaseClaim() {
            User user = new User();
            user.setId(1L);
            when(userMapper.selectByEmail(any())).thenReturn(user);
            when(redisUtils.claimPasswordResetCode(any(), any(), any(), any(), any(), any(), anyLong())).thenReturn(1);
            when(userMapper.updatePasswordAndIncrementTokenVersion(anyLong(), any(), any())).thenThrow(new RuntimeException("db error"));

            ResetPasswordByCodeDTO dto = new ResetPasswordByCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCode("123456");
            dto.setNewPassword("Password123!");

            assertThrows(RuntimeException.class, () -> userService.resetPasswordByCode(dto));
            verify(redisUtils).releasePasswordResetClaim(any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("非Spring事务环境降级")
        void nonSpringTransactionEnvironment() {
            User user = new User();
            user.setId(1L);
            when(userMapper.selectByEmail(any())).thenReturn(user);
            when(redisUtils.claimPasswordResetCode(any(), any(), any(), any(), any(), any(), anyLong())).thenReturn(1);
            when(userMapper.updatePasswordAndIncrementTokenVersion(anyLong(), any(), any())).thenReturn(1);
            doNothing().when(authSessionRevocationService).afterCommitWithRetry(anyString(), any(Runnable.class));
            when(redisUtils.finalizePasswordResetClaim(any(), any())).thenReturn(true);

            ResetPasswordByCodeDTO dto = new ResetPasswordByCodeDTO();
            dto.setEmail("test@example.com");
            dto.setCode("123456");
            dto.setNewPassword("Password123!");

            Result<Void> result = userService.resetPasswordByCode(dto);
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("更新用户状态")
    class UpdateUserStatus {

        @Test
        @DisplayName("用户不存在")
        void userNotFound() {
            when(userMapper.selectById(anyLong())).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.updateUserStatus(1L, 1));
        }

        @Test
        @DisplayName("更新状态并撤销失败")
        void updateStatusAndRevokeFails() {
            User user = new User();
            user.setId(1L);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(authSessionRevocationService.updateStatusAndRevoke(anyLong(), anyInt())).thenReturn(false);

            assertThrows(BusinessException.class, () -> userService.updateUserStatus(1L, 1));
        }
    }

    @Nested
    @DisplayName("删除用户")
    class DeleteUser {

        @Test
        @DisplayName("用户不存在")
        void userNotFound() {
            when(userMapper.selectById(anyLong())).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.deleteUser(1L));
        }

        @Test
        @DisplayName("版本递增并撤销失败")
        void incrementVersionAndRevokeFails() {
            User user = new User();
            user.setId(1L);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(authSessionRevocationService.incrementVersionAndRevoke(anyLong())).thenReturn(false);

            assertThrows(BusinessException.class, () -> userService.deleteUser(1L));
        }

        @Test
        @DisplayName("删除用户失败")
        void deleteByIdFails() {
            User user = new User();
            user.setId(1L);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(authSessionRevocationService.incrementVersionAndRevoke(anyLong())).thenReturn(true);
            when(userMapper.deleteById(anyLong())).thenReturn(0);

            assertThrows(BusinessException.class, () -> userService.deleteUser(1L));
        }
    }

    @Nested
    @DisplayName("GitHub OAuth 登录")
    class GithubLogin {

        private void stubTokenResponse() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(
                            "{\"access_token\":\"gho_token\",\"token_type\":\"bearer\"}", HttpStatus.OK));
        }

        private void stubUserInfo(String json) {
            when(restTemplate.exchange(eq("https://api.github.com/user"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(json, HttpStatus.OK));
        }

        private User user(long id, String username) {
            User u = new User();
            u.setId(id);
            u.setUsername(username);
            u.setNickname(username);
            u.setStatus(1);
            u.setRole(1);
            u.setPassword("hashed");
            u.setEmail(username + "@github.com");
            return u;
        }

        @Test
        @DisplayName("state 缺失应拒绝")
        void missingState_shouldThrow() {
            assertThrows(BusinessException.class, () -> userService.githubLogin("code", null));
            assertThrows(BusinessException.class, () -> userService.githubLogin("code", ""));
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("state 无效应拒绝")
        void invalidState_shouldThrow() {
            when(redisUtils.get(any())).thenReturn(null);

            assertThrows(BusinessException.class, () -> userService.githubLogin("code", "badstate"));
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("GitHub 返回 error 时应抛出授权失败")
        void tokenError_shouldThrow() {
            when(redisUtils.get(any())).thenReturn("1");
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(
                            "{\"error\":\"bad_verification_code\",\"error_description\":\"invalid\"}", HttpStatus.OK));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.githubLogin("code", "state"));
            assertThat(ex.getMessage()).contains("GitHub 授权失败");
        }

        @Test
        @DisplayName("老用户通过 GitHub id 登录成功")
        void existingGithubUser_shouldReturnDTO() {
            when(redisUtils.get(any())).thenReturn("1");
            stubTokenResponse();
            stubUserInfo("{\"id\":12345,\"login\":\"octocat\",\"email\":\"octo@github.com\"}");
            User existing = user(1L, "octocat");
            when(userMapper.selectByGithubId(12345L)).thenReturn(existing);

            Result<UserDTO> result = userService.githubLogin("code", "state");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getId()).isEqualTo(1L);
            assertThat(result.getData().getAccessToken()).isEqualTo("access-token");
            verify(userMapper).updateById(existing);
        }

        @Test
        @DisplayName("GitHub 用户名已存在时追加 _gh 后缀")
        void usernameTaken_shouldAppendGhSuffix() {
            when(redisUtils.get(any())).thenReturn("1");
            stubTokenResponse();
            stubUserInfo("{\"id\":12345,\"login\":\"octocat\",\"email\":\"octo@github.com\"}");
            when(userMapper.selectByGithubId(12345L)).thenReturn(null);
            when(userMapper.selectByUsername("octocat")).thenReturn(new User());

            Result<UserDTO> result = userService.githubLogin("code", "state");

            assertThat(result.isSuccess()).isTrue();
            org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
            verify(userMapper).insert(captor.capture());
            assertThat(captor.getValue().getUsername()).isEqualTo("octocat_gh");
        }

        @Test
        @DisplayName("邮箱已存在时应绑定 GitHub id")
        void emailExists_shouldBindGithubId() {
            when(redisUtils.get(any())).thenReturn("1");
            stubTokenResponse();
            stubUserInfo("{\"id\":12345,\"login\":\"octocat\",\"email\":\"octo@github.com\"}");
            when(userMapper.selectByGithubId(12345L)).thenReturn(null);
            when(userMapper.selectByUsername("octocat")).thenReturn(null);
            User emailUser = user(5L, "existing");
            when(userMapper.selectByEmail("octo@github.com")).thenReturn(emailUser);

            Result<UserDTO> result = userService.githubLogin("code", "state");

            assertThat(result.isSuccess()).isTrue();
            assertThat(emailUser.getGithubId()).isEqualTo(12345L);
            verify(userMapper).updateById(emailUser);
        }

        @Test
        @DisplayName("邮箱已存在且无头像时绑定 GitHub 头像")
        void emailExists_withoutAvatar_shouldBindGithubAvatar() {
            when(redisUtils.get(any())).thenReturn("1");
            stubTokenResponse();
            stubUserInfo("{\"id\":12345,\"login\":\"octocat\",\"email\":\"octo@github.com\",\"avatar_url\":\"https://avatars/1.png\"}");
            when(userMapper.selectByGithubId(12345L)).thenReturn(null);
            when(userMapper.selectByUsername("octocat")).thenReturn(null);
            User emailUser = user(5L, "existing");
            emailUser.setAvatar(null);
            when(userMapper.selectByEmail("octo@github.com")).thenReturn(emailUser);

            Result<UserDTO> result = userService.githubLogin("code", "state");

            assertThat(result.isSuccess()).isTrue();
            assertThat(emailUser.getGithubId()).isEqualTo(12345L);
            assertThat(emailUser.getAvatar()).isEqualTo("https://avatars/1.png");
        }

        @Test
        @DisplayName("全新用户应创建并登录")
        void newUser_shouldCreateAndLogin() {
            when(redisUtils.get(any())).thenReturn("1");
            stubTokenResponse();
            stubUserInfo("{\"id\":12345,\"login\":\"octocat\",\"email\":\"octo@github.com\"}");
            when(userMapper.selectByGithubId(12345L)).thenReturn(null);
            when(userMapper.selectByUsername("octocat")).thenReturn(null);
            when(userMapper.selectByEmail("octo@github.com")).thenReturn(null);

            Result<UserDTO> result = userService.githubLogin("code", "state");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getUsername()).isEqualTo("octocat");
            verify(userMapper).insert(any(User.class));
        }

        @Test
        @DisplayName("email 缺失时应从 /user/emails 获取已验证邮箱")
        void emailMissing_shouldFetchFromEmailsApi() {
            when(redisUtils.get(any())).thenReturn("1");
            stubTokenResponse();
            stubUserInfo("{\"id\":12345,\"login\":\"octocat\"}");
            when(restTemplate.exchange(eq("https://api.github.com/user/emails"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(
                            "[{\"email\":\"verified@github.com\",\"verified\":true,\"primary\":true}]", HttpStatus.OK));
            when(userMapper.selectByGithubId(12345L)).thenReturn(null);
            when(userMapper.selectByUsername("octocat")).thenReturn(null);
            when(userMapper.selectByEmail("verified@github.com")).thenReturn(null);

            Result<UserDTO> result = userService.githubLogin("code", "state");

            assertThat(result.isSuccess()).isTrue();
            org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
            verify(userMapper).insert(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("verified@github.com");
        }

        @Test
        @DisplayName("email 缺失且 emails 接口失败时应使用占位邮箱")
        void emailMissing_emailsApiFails_shouldUsePlaceholder() {
            when(redisUtils.get(any())).thenReturn("1");
            stubTokenResponse();
            stubUserInfo("{\"id\":12345,\"login\":\"octocat\"}");
            when(restTemplate.exchange(eq("https://api.github.com/user/emails"), eq(HttpMethod.GET),
                    any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RuntimeException("network error"));
            when(userMapper.selectByGithubId(12345L)).thenReturn(null);
            when(userMapper.selectByUsername("octocat")).thenReturn(null);
            when(userMapper.selectByEmail("octocat@github.placeholder")).thenReturn(null);

            Result<UserDTO> result = userService.githubLogin("code", "state");

            assertThat(result.isSuccess()).isTrue();
            org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
            verify(userMapper).insert(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("octocat@github.placeholder");
        }

        @Test
        @DisplayName("外部调用异常应包装为 BusinessException")
        void externalException_shouldWrap() {
            when(redisUtils.get(any())).thenReturn("1");
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RuntimeException("connect timeout"));

            assertThrows(BusinessException.class, () -> userService.githubLogin("code", "state"));
        }

        @Test
        @DisplayName("生成 OAuth state 应写入 Redis")
        void generateState_shouldStoreAndReturn() {
            Result<String> result = userService.generateGithubState();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotBlank();
            verify(redisUtils).set(any(), eq("1"), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("作者排行榜补充")
    class GetTopAuthorsAdditional {

        @Test
        @DisplayName("登录用户关注状态应为 true")
        void loggedInUserWithFollow_shouldSetIsFollowed() {
            User author = new User();
            author.setId(1L);
            author.setUsername("alice");
            author.setNickname("alice");
            when(userMapper.selectList(any())).thenReturn(List.of(author));
            setUserId(2L);
            UserFollow follow = UserFollow.builder().followerId(2L).followingId(1L).build();
            when(userFollowMapper.selectList(any())).thenReturn(List.of(follow));

            Result<List<PublicUserProfileDTO>> result = userService.getTopAuthors(10);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getIsFollowed()).isTrue();
        }

        @Test
        @DisplayName("当前用户 ID 获取失败时默认未关注")
        void authFailure_shouldDefaultToNotFollowed() {
            User author = new User();
            author.setId(1L);
            author.setUsername("alice");
            author.setNickname("alice");
            when(userMapper.selectList(any())).thenReturn(List.of(author));

            Result<List<PublicUserProfileDTO>> result = userService.getTopAuthors(10);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().get(0).getIsFollowed()).isFalse();
        }

        @Test
        @DisplayName("getFollowings 双方互相关注时 isMutual 为 true")
        void getFollowingsMutual() {
            // 三次 selectList 调用：关注关系查询、viewer 是否关注列表用户、列表用户是否关注 viewer
            UserFollow follow = UserFollow.builder().followerId(1L).followingId(2L).build();
            UserFollow reverse = UserFollow.builder().followerId(2L).followingId(1L).build();
            when(userFollowMapper.selectList(any())).thenReturn(
                    List.of(follow),
                    List.of(follow),
                    List.of(reverse)
            );
            User target = new User();
            target.setId(2L);
            target.setUsername("bob");
            when(userMapper.selectBatchIds(any())).thenReturn(List.of(target));

            Result<List<PublicUserProfileDTO>> result = userService.getFollowings(1L, 1, 10);
            assertThat(result.getData()).hasSize(1);
            PublicUserProfileDTO dto = result.getData().get(0);
            assertThat(dto.getIsFollowed()).isTrue();
            assertThat(dto.getIsMutual()).isTrue();
        }
    }

    @Nested
    @DisplayName("关注列表")
    class FollowLists {

        @Test
        @DisplayName("getFollowings 空列表")
        void getFollowingsEmpty() {
            when(userFollowMapper.selectList(any())).thenReturn(Collections.emptyList());

            Result<List<PublicUserProfileDTO>> result = userService.getFollowings(1L, 1, 10);
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("getFollowings 非空列表")
        void getFollowingsNonEmpty() {
            UserFollow follow = UserFollow.builder().followerId(1L).followingId(2L).build();
            when(userFollowMapper.selectList(any())).thenReturn(List.of(follow));
            User target = new User();
            target.setId(2L);
            target.setUsername("bob");
            when(userMapper.selectBatchIds(any())).thenReturn(List.of(target));

            Result<List<PublicUserProfileDTO>> result = userService.getFollowings(1L, 1, 10);
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getId()).isEqualTo(2L);
            // attachFollowState 应注入 isFollowed（viewer 关注了该用户）
            assertThat(result.getData().get(0).getIsFollowed()).isTrue();
        }

        @Test
        @DisplayName("getFollowings 分页参数非法时使用默认值")
        void getFollowingsInvalidPage() {
            when(userFollowMapper.selectList(any())).thenReturn(Collections.emptyList());

            Result<List<PublicUserProfileDTO>> result = userService.getFollowings(1L, null, null);
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("getFollowers 空列表")
        void getFollowersEmpty() {
            when(userFollowMapper.selectList(any())).thenReturn(Collections.emptyList());

            Result<List<PublicUserProfileDTO>> result = userService.getFollowers(1L, 1, 10);
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("getFollowers 非空列表")
        void getFollowersNonEmpty() {
            UserFollow follow = UserFollow.builder().followerId(3L).followingId(1L).build();
            when(userFollowMapper.selectList(any())).thenReturn(List.of(follow));
            User follower = new User();
            follower.setId(3L);
            follower.setUsername("carol");
            when(userMapper.selectBatchIds(any())).thenReturn(List.of(follower));

            Result<List<PublicUserProfileDTO>> result = userService.getFollowers(1L, 1, 10);
            assertThat(result.getData()).hasSize(1);
            assertThat(result.getData().get(0).getId()).isEqualTo(3L);
            // 粉丝场景下 viewer 未关注该用户，isFollowed 应为 false
            assertThat(result.getData().get(0).getIsFollowed()).isFalse();
        }
    }

    @Nested
    @DisplayName("其它补充方法")
    class MiscCoverage {

        @Test
        @DisplayName("isFollowing 已关注")
        void isFollowingTrue() {
            when(userFollowMapper.selectCount(any())).thenReturn(1L);
            assertThat(userService.isFollowing(1L, 2L).getData()).isTrue();
        }

        @Test
        @DisplayName("isFollowing 未关注")
        void isFollowingFalse() {
            when(userFollowMapper.selectCount(any())).thenReturn(0L);
            assertThat(userService.isFollowing(1L, 2L).getData()).isFalse();
        }

        @Test
        @DisplayName("getUserByEmail")
        void getUserByEmail() {
            User u = new User();
            u.setId(1L);
            u.setUsername("x");
            when(userMapper.selectByEmail("a@b.com")).thenReturn(u);
            assertThat(userService.getUserByEmail("a@b.com")).isSameAs(u);
        }

        @Test
        @DisplayName("logout 单参重载")
        void logoutSingleArg() {
            when(redisDistributedLock.tryLockWithWatchdog(any(), anyLong(), any(), anyLong(), any()))
                    .thenReturn("lock");

            Result<Void> result = userService.logout(1L);
            assertThat(result.isSuccess()).isTrue();
            verify(authSessionRevocationService).revokeUserSessions(1L);
        }
    }

    @Nested
    @DisplayName("令牌校验")
    class ValidateToken {

        @Test
        @DisplayName("无 Authorization 头返回 false")
        void emptyAuthHeader() {
            assertThat(userService.validateToken(null).getData()).isFalse();
            assertThat(userService.validateToken("").getData()).isFalse();
            assertThat(userService.validateToken("Basic abc").getData()).isFalse();
        }

        @Test
        @DisplayName("令牌无效返回 false")
        void invalidToken() {
            when(jwtUtils.validateToken("token")).thenReturn(false);
            assertThat(userService.validateToken("Bearer token").getData()).isFalse();
        }

        @Test
        @DisplayName("令牌过期返回 false")
        void expiredToken() {
            when(jwtUtils.validateToken("token")).thenReturn(true);
            when(jwtUtils.isTokenExpired("token")).thenReturn(true);
            assertThat(userService.validateToken("Bearer token").getData()).isFalse();
        }

        @Test
        @DisplayName("非访问令牌返回 false")
        void notAccessToken() {
            when(jwtUtils.validateToken("token")).thenReturn(true);
            when(jwtUtils.isTokenExpired("token")).thenReturn(false);
            when(jwtUtils.isAccessToken("token")).thenReturn(false);
            assertThat(userService.validateToken("Bearer token").getData()).isFalse();
        }

        @Test
        @DisplayName("用户有效返回 true")
        void validUser() {
            when(jwtUtils.validateToken("token")).thenReturn(true);
            when(jwtUtils.isTokenExpired("token")).thenReturn(false);
            when(jwtUtils.isAccessToken("token")).thenReturn(true);
            when(jwtUtils.getUserIdFromToken("token")).thenReturn(1L);
            User user = new User();
            user.setId(1L);
            user.setStatus(User.STATUS_ACTIVE);
            user.setTokenVersion(0);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(jwtUtils.getTokenVersion("token")).thenReturn(0);

            assertThat(userService.validateToken("Bearer token").getData()).isTrue();
        }

        @Test
        @DisplayName("用户状态无效返回 false")
        void disabledUser() {
            when(jwtUtils.validateToken("token")).thenReturn(true);
            when(jwtUtils.isTokenExpired("token")).thenReturn(false);
            when(jwtUtils.isAccessToken("token")).thenReturn(true);
            when(jwtUtils.getUserIdFromToken("token")).thenReturn(1L);
            User user = new User();
            user.setId(1L);
            user.setStatus(User.STATUS_DISABLED);
            when(userMapper.selectById(1L)).thenReturn(user);

            assertThat(userService.validateToken("Bearer token").getData()).isFalse();
        }

        @Test
        @DisplayName("令牌版本不一致返回 false")
        void tokenVersionMismatch() {
            when(jwtUtils.validateToken("token")).thenReturn(true);
            when(jwtUtils.isTokenExpired("token")).thenReturn(false);
            when(jwtUtils.isAccessToken("token")).thenReturn(true);
            when(jwtUtils.getUserIdFromToken("token")).thenReturn(1L);
            User user = new User();
            user.setId(1L);
            user.setStatus(User.STATUS_ACTIVE);
            user.setTokenVersion(5);
            when(userMapper.selectById(1L)).thenReturn(user);
            when(jwtUtils.getTokenVersion("token")).thenReturn(3);

            assertThat(userService.validateToken("Bearer token").getData()).isFalse();
        }
    }

    @Nested
    @DisplayName("用户列表补充")
    class GetUserListAdditional {

        @Test
        @DisplayName("带关键字搜索")
        void withKeyword() {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
            when(userMapper.selectPage(any(), any())).thenReturn(page);
            when(page.getRecords()).thenReturn(Collections.emptyList());
            when(page.getTotal()).thenReturn(0L);

            Result<PageResult<UserDTO>> result = userService.getUserList(1, 10, "alice");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).isEmpty();
        }

        @Test
        @DisplayName("有记录时转换DTO")
        void withRecords() {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page = mock(com.baomidou.mybatisplus.extension.plugins.pagination.Page.class);
            when(userMapper.selectPage(any(), any())).thenReturn(page);
            User user = new User();
            user.setId(1L);
            user.setUsername("alice");
            user.setRole(2);
            when(page.getRecords()).thenReturn(List.of(user));
            when(page.getTotal()).thenReturn(1L);

            Result<PageResult<UserDTO>> result = userService.getUserList(1, 10, null);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().getItems()).hasSize(1);
            assertThat(result.getData().getItems().get(0).getRole()).isEqualTo("admin");
        }
    }

    private UserRegisterDTO createRegisterDTO(String username, String email, String nickname) {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(username);
        dto.setPassword("Password123!");
        dto.setConfirmPassword("Password123!");
        dto.setEmail(email);
        dto.setNickname(nickname);
        dto.setEmailCode("123456");
        return dto;
    }

    private UserLoginDTO loginDTO(String username, String password) {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setCaptchaKey("key");
        dto.setCaptcha("code");
        return dto;
    }

    private Result<UserDTO> login(String username, String password) {
        return userService.login(loginDTO(username, password));
    }

    private SendRegisterCodeDTO sendRegisterCodeDTO(String email) {
        SendRegisterCodeDTO dto = new SendRegisterCodeDTO();
        dto.setEmail(email);
        dto.setCaptchaKey("key");
        dto.setCaptcha("code");
        return dto;
    }
}
