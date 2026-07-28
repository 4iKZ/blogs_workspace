package com.blog.interceptor;

import com.blog.common.ResultCode;
import com.blog.utils.JWTUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtInterceptorTest {

    @Mock
    private JWTUtils jwtUtils;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private JwtInterceptor interceptor;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"code\":401,\"message\":\"未登录\",\"success\":false}");
    }

    @Test
    @DisplayName("缺少 Authorization 头 - 应返回 false")
    void missingAuthHeader_shouldReturnFalse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(response.getContentAsString()).contains("未登录");
    }

    @Test
    @DisplayName("无效 Token 格式 - 应返回 false")
    void invalidTokenFormat_shouldReturnFalse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "InvalidToken");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("有效 Token - 应设置请求属性并返回 true")
    void validToken_shouldSetAttributesAndReturnTrue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.isTokenExpired("valid-token")).thenReturn(false);
        when(jwtUtils.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("testuser");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(request.getAttribute("userId")).isEqualTo(1L);
        assertThat(request.getAttribute("username")).isEqualTo("testuser");
    }

    @Test
    @DisplayName("过期 Token - 应返回 false")
    void expiredToken_shouldReturnFalse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("expired-token")).thenReturn(true);
        when(jwtUtils.isTokenExpired("expired-token")).thenReturn(true);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Token 验证失败 - 应返回 false")
    void invalidToken_shouldReturnFalse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("invalid-token")).thenReturn(false);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
    }
}
