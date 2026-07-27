package com.blog.security;

import com.blog.service.impl.CustomUserDetailsServiceImpl;
import com.blog.utils.JWTUtils;
import com.blog.utils.RedisUtils;
import com.blog.mapper.UserMapper;
import com.blog.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JWTUtils jwtUtils;

    @Mock
    private CustomUserDetailsServiceImpl userDetailsService;

    @Mock
    private RedisUtils redisUtils;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("有效 JWT Token - 应设置 SecurityContext")
    void validJwtToken_shouldSetSecurityContext() throws Exception {
        SecurityContextHolder.clearContext();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.isTokenExpired("valid-token")).thenReturn(false);
        when(jwtUtils.isAccessToken("valid-token")).thenReturn(true);
        when(jwtUtils.getJti("valid-token")).thenReturn("valid-jti");
        when(redisUtils.exists("auth:blacklist:access:valid-jti")).thenReturn(false);
        when(jwtUtils.getUsernameFromToken("valid-token")).thenReturn("testuser");

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.isEnabled()).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        when(jwtUtils.getUserIdFromToken("valid-token")).thenReturn(1L);
        when(jwtUtils.getTokenVersion("valid-token")).thenReturn(2);
        User user = new User();
        user.setId(1L);
        user.setTokenVersion(2);
        when(userMapper.selectById(1L)).thenReturn(user);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assert (SecurityContextHolder.getContext().getAuthentication() != null);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("过期 Token - 不应设置 SecurityContext")
    void expiredToken_shouldNotSetSecurityContext() throws Exception {
        SecurityContextHolder.clearContext();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(jwtUtils.validateToken("expired-token")).thenReturn(true);
        when(jwtUtils.isTokenExpired("expired-token")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assert (SecurityContextHolder.getContext().getAuthentication() == null);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("黑名单 Token - 不应设置 SecurityContext")
    void blacklistedToken_shouldNotSetSecurityContext() throws Exception {
        SecurityContextHolder.clearContext();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer blacklisted-token");
        when(jwtUtils.validateToken("blacklisted-token")).thenReturn(true);
        when(jwtUtils.isTokenExpired("blacklisted-token")).thenReturn(false);
        when(jwtUtils.isAccessToken("blacklisted-token")).thenReturn(true);
        when(jwtUtils.getJti("blacklisted-token")).thenReturn("blacklisted-jti");
        when(redisUtils.exists("auth:blacklist:access:blacklisted-jti")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assert (SecurityContextHolder.getContext().getAuthentication() == null);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("无效 Token 格式 - 不应设置 SecurityContext")
    void invalidTokenFormat_shouldNotSetSecurityContext() throws Exception {
        SecurityContextHolder.clearContext();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("InvalidToken");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assert (SecurityContextHolder.getContext().getAuthentication() == null);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("验证异常 - 不应阻止请求继续")
    void validationException_shouldNotBlockRequest() throws Exception {
        SecurityContextHolder.clearContext();

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtils.validateToken(anyString())).thenThrow(new RuntimeException("Validation error"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        SecurityContextHolder.clearContext();
    }
}
