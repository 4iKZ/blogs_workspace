package com.blog.security;

import com.blog.service.impl.CustomUserDetailsServiceImpl;
import com.blog.utils.JWTUtils;
import com.blog.utils.RedisUtils;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_BLACKLIST_KEY_PREFIX = "auth:blacklist:access:";

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private CustomUserDetailsServiceImpl userDetailsService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)
                    && jwtUtils.validateToken(jwt)
                    && !jwtUtils.isTokenExpired(jwt)
                    && jwtUtils.isAccessToken(jwt)
                    && !redisUtils.exists(ACCESS_TOKEN_BLACKLIST_KEY_PREFIX + jwtUtils.getJti(jwt))) {

                String username = jwtUtils.getUsernameFromToken(jwt);
                Long userId = jwtUtils.getUserIdFromToken(jwt);
                User user = userMapper.selectById(userId);
                int currentVersion = user == null || user.getTokenVersion() == null ? 0 : user.getTokenVersion();
                if (user == null || jwtUtils.getTokenVersion(jwt) != currentVersion) {
                    filterChain.doFilter(request, response);
                    return;
                }
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    request.setAttribute("userId", userId);
                    request.setAttribute("username", username);
                }
            }
        } catch (Exception ex) {
            logger.warn("JWT validation failed: " + ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
