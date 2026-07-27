package com.blog.security;

import com.blog.security.password.PasswordResetCodeSecurity;
import com.blog.dto.SendResetCodeDTO;
import com.blog.mapper.UserMapper;
import com.blog.service.CaptchaService;
import com.blog.service.impl.UserServiceImpl;
import com.blog.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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

    private SendResetCodeDTO resetRequest(String email, String key, String captcha) {
        SendResetCodeDTO dto = new SendResetCodeDTO();
        dto.setEmail(email);
        dto.setCaptchaKey(key);
        dto.setCaptcha(captcha);
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
