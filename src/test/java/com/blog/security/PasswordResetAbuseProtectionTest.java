package com.blog.security;

import com.blog.security.password.PasswordResetCodeSecurity;
import com.blog.dto.SendResetCodeDTO;
import com.blog.dto.ResetPasswordByCodeDTO;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.service.CaptchaService;
import com.blog.service.impl.UserServiceImpl;
import com.blog.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.util.concurrent.Executor;
import org.springframework.mail.javamail.JavaMailSender;

class PasswordResetAbuseProtectionTest {

    @Test
    void generatedCodeHasSixDigitsAndOnlyItsHmacDigestIsStored() {
        PasswordResetCodeSecurity security =
                new PasswordResetCodeSecurity("password-reset-hmac-key-at-least-thirty-two-bytes");

        String code = security.generateCode();
        String digest = security.digest("alice@example.com", code);

        assertThat(code).matches("\\d{6}");
        assertThat(digest).matches("[0-9a-f]{64}");
        assertThat(digest).doesNotContain(code);
        assertThat(security.matches("alice@example.com", code, digest)).isTrue();
        assertThat(security.matches("alice@example.com", "000000", digest)).isEqualTo("000000".equals(code));
    }

    @Test
    void emailNormalizationProducesStableDigest() {
        PasswordResetCodeSecurity security =
                new PasswordResetCodeSecurity("password-reset-hmac-key-at-least-thirty-two-bytes");

        assertThat(security.digest(" Alice@Example.COM ", "123456"))
                .isEqualTo(security.digest("alice@example.com", "123456"));
    }

    @Test
    void captchaIsCheckedBeforeAccountLookup() {
        UserServiceImpl service = new UserServiceImpl();
        CaptchaService captchaService = mock(CaptchaService.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(captchaService.verifyCaptcha("captcha-key", "bad")).thenReturn(false);
        setField(service, "captchaService", captchaService);
        setField(service, "userMapper", userMapper);
        SendResetCodeDTO dto = resetRequest("alice@example.com", "captcha-key", "bad");

        assertThatThrownBy(() -> service.sendResetCode(dto)).hasMessageContaining("图形验证码");
        verifyNoInteractions(userMapper);
    }

    @Test
    void unknownEmailUsesSameSuccessAndChargesAllThreeLimits() {
        UserServiceImpl service = new UserServiceImpl();
        CaptchaService captchaService = mock(CaptchaService.class);
        UserMapper userMapper = mock(UserMapper.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        PasswordResetCodeSecurity security =
                new PasswordResetCodeSecurity("password-reset-hmac-key-at-least-thirty-two-bytes");
        when(captchaService.verifyCaptcha("captcha-key", "good")).thenReturn(true);
        when(redisUtils.incrementWithinLimit(anyString(), anyLong(), anyLong())).thenReturn(true);
        when(userMapper.selectByEmail("missing@example.com")).thenReturn(null);
        when(request.getHeader(anyString())).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.0.2.7");
        setField(service, "captchaService", captchaService);
        setField(service, "userMapper", userMapper);
        setField(service, "redisUtils", redisUtils);
        setField(service, "request", request);
        setField(service, "passwordResetCodeSecurity", security);

        assertThat(service.sendResetCode(
                resetRequest("missing@example.com", "captcha-key", "good")).isSuccess()).isTrue();
        verify(redisUtils).incrementWithinLimit(
                "password:reset:limit:minute:missing@example.com", 1, 60);
        verify(redisUtils).incrementWithinLimit(
                "password:reset:limit:email-hour:missing@example.com", 5, 3600);
        verify(redisUtils).incrementWithinLimit(
                "password:reset:limit:ip-hour:192.0.2.7", 20, 3600);
    }

    @Test
    void knownAndUnknownEmailPerformTheSameSynchronousSecuritySequenceAndNeverWaitForSmtp() {
        for (boolean known : new boolean[]{true, false}) {
            UserServiceImpl service = new UserServiceImpl();
            CaptchaService captcha = mock(CaptchaService.class);
            UserMapper mapper = mock(UserMapper.class);
            RedisUtils redis = mock(RedisUtils.class);
            HttpServletRequest request = mock(HttpServletRequest.class);
            JavaMailSender mail = mock(JavaMailSender.class);
            PasswordResetCodeSecurity codes = mock(PasswordResetCodeSecurity.class);
            RecordingExecutor executor = new RecordingExecutor();
            when(captcha.verifyCaptcha("key", "good")).thenReturn(true);
            when(redis.incrementWithinLimit(anyString(), anyLong(), anyLong())).thenReturn(true);
            when(codes.generateCode()).thenReturn("123456");
            when(codes.digest("same@example.com", "123456")).thenReturn("digest");
            when(redis.setString("password:reset:code:same@example.com", "digest", 10,
                    java.util.concurrent.TimeUnit.MINUTES)).thenReturn(true);
            when(mapper.selectByEmail("same@example.com")).thenReturn(known ? new User() : null);
            when(request.getHeader(anyString())).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.0.2.9");
            setField(service, "captchaService", captcha);
            setField(service, "userMapper", mapper);
            setField(service, "redisUtils", redis);
            setField(service, "request", request);
            setField(service, "passwordResetCodeSecurity", codes);
            setField(service, "mailSender", mail);
            setField(service, "notificationTaskExecutor", executor);

            assertThat(service.sendResetCode(resetRequest("same@example.com", "key", "good")).isSuccess()).isTrue();
            verify(codes).generateCode();
            verify(codes).digest("same@example.com", "123456");
            verify(redis).setString("password:reset:code:same@example.com", "digest", 10,
                    java.util.concurrent.TimeUnit.MINUTES);
            verifyNoInteractions(mail);
            assertThat(executor.submitted).isEqualTo(known ? 1 : 0);
        }
    }

    @Test
    void weakPasswordDoesNotClaimOrConsumeResetCode() {
        UserServiceImpl service = new UserServiceImpl();
        RedisUtils redis = mock(RedisUtils.class);
        setField(service, "redisUtils", redis);
        ResetPasswordByCodeDTO dto = new ResetPasswordByCodeDTO();
        dto.setEmail("alice@example.com");
        dto.setCode("123456");
        dto.setNewPassword("weak");

        assertThatThrownBy(() -> service.resetPasswordByCode(dto)).hasMessageContaining("密码");
        verifyNoInteractions(redis);
    }

    @Test
    void successfulResetFinalizesClaimAndRevokesRefreshFamily() {
        UserServiceImpl service = new UserServiceImpl();
        RedisUtils redis = mock(RedisUtils.class);
        UserMapper mapper = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        PasswordResetCodeSecurity codes = mock(PasswordResetCodeSecurity.class);
        User user = new User();
        user.setId(7L);
        user.setTokenVersion(2);
        when(mapper.selectByEmail("alice@example.com")).thenReturn(user);
        when(codes.digest("alice@example.com", "123456")).thenReturn("digest");
        when(redis.claimPasswordResetCode(
                eq("password:reset:code:alice@example.com"),
                eq("password:reset:attempts:alice@example.com"),
                eq("password:reset:lock:alice@example.com"),
                eq("password:reset:claim:alice@example.com"),
                eq("digest"), anyString(), eq(120L))).thenReturn(1);
        when(encoder.encode("StrongPassword123!")).thenReturn("encoded");
        when(mapper.updateById(user)).thenReturn(1);
        setField(service, "redisUtils", redis);
        setField(service, "userMapper", mapper);
        setField(service, "passwordEncoder", encoder);
        setField(service, "passwordResetCodeSecurity", codes);

        assertThat(service.resetPasswordByCode(resetPasswordRequest()).isSuccess()).isTrue();

        verify(redis).finalizePasswordResetClaim(
                eq("password:reset:claim:alice@example.com"), anyString());
        verify(redis).deleteString("auth:refresh:user-jtis:7");
        assertThat(user.getTokenVersion()).isEqualTo(3);
    }

    @Test
    void failedDatabaseUpdateReleasesClaimSoCodeCanBeRetried() {
        UserServiceImpl service = new UserServiceImpl();
        RedisUtils redis = mock(RedisUtils.class);
        UserMapper mapper = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        PasswordResetCodeSecurity codes = mock(PasswordResetCodeSecurity.class);
        User user = new User();
        user.setId(7L);
        when(mapper.selectByEmail("alice@example.com")).thenReturn(user);
        when(codes.digest("alice@example.com", "123456")).thenReturn("digest");
        when(redis.claimPasswordResetCode(
                anyString(), anyString(), anyString(), anyString(), eq("digest"), anyString(), eq(120L)))
                .thenReturn(1);
        when(encoder.encode("StrongPassword123!")).thenReturn("encoded");
        when(mapper.updateById(user)).thenReturn(0);
        setField(service, "redisUtils", redis);
        setField(service, "userMapper", mapper);
        setField(service, "passwordEncoder", encoder);
        setField(service, "passwordResetCodeSecurity", codes);

        assertThatThrownBy(() -> service.resetPasswordByCode(resetPasswordRequest()))
                .hasMessageContaining("密码重置失败");

        verify(redis).releasePasswordResetClaim(
                eq("password:reset:code:alice@example.com"),
                eq("password:reset:claim:alice@example.com"),
                anyString(),
                eq(600L));
        verify(redis, never()).finalizePasswordResetClaim(anyString(), anyString());
    }

    private static final class RecordingExecutor implements Executor {
        private int submitted;

        @Override
        public void execute(Runnable command) {
            submitted++;
        }
    }

    private SendResetCodeDTO resetRequest(String email, String key, String captcha) {
        SendResetCodeDTO dto = new SendResetCodeDTO();
        dto.setEmail(email);
        dto.setCaptchaKey(key);
        dto.setCaptcha(captcha);
        return dto;
    }

    private ResetPasswordByCodeDTO resetPasswordRequest() {
        ResetPasswordByCodeDTO dto = new ResetPasswordByCodeDTO();
        dto.setEmail("alice@example.com");
        dto.setCode("123456");
        dto.setNewPassword("StrongPassword123!");
        return dto;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            var field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
