package com.blog.security;

import com.blog.service.impl.CustomUserDetailsServiceImpl;
import com.blog.utils.JWTUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 验证JWT令牌，设置Spring Security的认证信息
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private CustomUserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            // 获取JWT令牌
            String jwt = getJwtFromRequest(request);
            
            // 验证令牌
            if (StringUtils.hasText(jwt) && jwtUtils.validateToken(jwt) && !jwtUtils.isTokenExpired(jwt)) {
                // 从令牌中获取用户名
                String username = jwtUtils.getUsernameFromToken(jwt);
                
                // 从数据库获取用户详情
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
                
                // 设置认证详情
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 设置认证信息到SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // 设置用户ID到请求属性中，方便后续使用
                Long userId = jwtUtils.getUserIdFromToken(jwt);
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
            }
        } catch (Exception ex) {
            // Log but don't fail - let downstream handle auth or lack thereof
            logger.warn("JWT validation failed: " + ex.getMessage());
        }
        
        // 继续执行过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中获取JWT令牌
     * @param request HTTP请求
     * @return JWT令牌
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
