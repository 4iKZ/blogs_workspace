package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.controller.CaptchaController.CaptchaResponse;
import com.blog.service.CaptchaService;
import com.blog.utils.RedisUtils;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    @Mock
    private com.blog.utils.RedisUtils redisUtils;

    @Mock
    private DefaultKaptcha captchaProducer;

    @InjectMocks
    private CaptchaServiceImpl captchaService;

    @Test
    @DisplayName("生成验证码 - 应返回非空 key")
    void generateCaptcha_shouldReturnNonEmptyKey() {
        when(captchaProducer.createText()).thenReturn("1234");

        Result<String> result = captchaService.generateCaptcha();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData()).isNotEmpty();
        verify(redisUtils).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("验证码验证 - 正确应返回 true")
    void verifyCaptcha_correct_shouldReturnTrue() {
        String captchaKey = UUID.randomUUID().toString();
        when(redisUtils.get("captcha:" + captchaKey)).thenReturn("1234");

        boolean result = captchaService.verifyCaptcha(captchaKey, "1234");

        assertThat(result).isTrue();
        verify(redisUtils).delete("captcha:" + captchaKey);
    }

    @Test
    @DisplayName("验证码验证 - 错误应返回 false")
    void verifyCaptcha_wrong_shouldReturnFalse() {
        String captchaKey = UUID.randomUUID().toString();
        when(redisUtils.get("captcha:" + captchaKey)).thenReturn("1234");

        boolean result = captchaService.verifyCaptcha(captchaKey, "wrong");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("验证码验证 - key 为 null 应返回 false")
    void verifyCaptcha_nullKey_shouldReturnFalse() {
        assertThat(captchaService.verifyCaptcha(null, "1234")).isFalse();
        assertThat(captchaService.verifyCaptcha(null, null)).isFalse();
    }
}
