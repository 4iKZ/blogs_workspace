package com.blog.interceptor;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import com.blog.utils.JWTUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWT登录拦截器
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtInterceptor.class);

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的Authorization
        String authorization = request.getHeader("Authorization");

        // 检查Authorization头是否存在且以Bearer开头
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("缺少Authorization头或格式错误");
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        // 提取JWT令牌
        String token = authorization.substring(7);

        try {
            // 验证JWT令牌
            if (!jwtUtils.validateToken(token)) {
                log.warn("JWT令牌验证失败");
                writeErrorResponse(response, ResultCode.UNAUTHORIZED);
                return false;
            }

            // 检查令牌是否过期
            if (jwtUtils.isTokenExpired(token)) {
                log.warn("JWT令牌已过期");
                writeErrorResponse(response, ResultCode.UNAUTHORIZED);
                return false;
            }

            // 获取用户ID并设置到请求属性中
            Long userId = jwtUtils.getUserIdFromToken(token);
            String username = jwtUtils.getUsernameFromToken(token);
            
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);

            log.info("用户验证成功: userId={}, username={}", userId, username);
            return true;

        } catch (Exception e) {
            log.error("JWT验证过程发生异常: {}", e.getMessage(), e);
            writeErrorResponse(response, ResultCode.UNAUTHORIZED);
            return false;
        }
    }

    /**
     * 写入错误响应
     * @param response 响应对象
     * @param resultCode 结果码
     */
    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        Result<Void> result = Result.error(resultCode);
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().write(json);
    }
}