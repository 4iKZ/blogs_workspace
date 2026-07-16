package com.blog.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticationEntryPointTest {

    private final AuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint();

    @Test
    @DisplayName("未认证访问 - 应返回 401 和 JSON 响应")
    void unauthenticatedAccess_shouldReturn401WithJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new AuthenticationException("Unauthorized") {});

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        String content = response.getContentAsString();
        assertThat(content).contains("401");
        assertThat(content).contains("用户未登录");
    }
}
