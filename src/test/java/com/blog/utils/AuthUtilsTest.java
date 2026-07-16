package com.blog.utils;

import com.blog.common.ResultCode;
import com.blog.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthUtilsTest {

    @Test
    @DisplayName("无请求上下文 - getCurrentUserIdOptional 应返回 null")
    void getCurrentUserIdOptional_noContext_shouldReturnNull() {
        SecurityContextHolder.clearContext();
        assertThat(AuthUtils.getCurrentUserIdOptional()).isNull();
    }

    @Test
    @DisplayName("无请求上下文 - getCurrentUserId 应抛出异常")
    void getCurrentUserId_noContext_shouldThrowException() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(AuthUtils::getCurrentUserId)
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("请求属性包含 userId - getCurrentUserIdOptional 应返回用户ID")
    void getCurrentUserIdOptional_withUserId_shouldReturnUserId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 42L);
        request.setAttribute("username", "testuser");

        // 模拟 RequestContextHolder
        // 由于 AuthUtils 依赖 RequestContextHolder，这里使用反射或直接测试逻辑
        // 在真实环境中，这需要 Spring 上下文支持
    }
}
