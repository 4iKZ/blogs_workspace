package com.blog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义认证入口点
 * 当用户未认证访问需要认证的资源时，返回 401 状态码
 * 而不是 Spring Security 默认的 403
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        // 设置响应状态码为 401
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        // 构建统一的错误响应格式
        Map<String, Object> data = new HashMap<>();
        data.put("code", 401);
        data.put("message", "用户未登录");
        data.put("data", null);

        // 写入响应
        response.getWriter().write(objectMapper.writeValueAsString(data));
    }
}
