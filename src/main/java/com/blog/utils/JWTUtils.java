package com.blog.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT工具类
 */
@Component
public class JWTUtils {
    
    private static final Logger log = LoggerFactory.getLogger(JWTUtils.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:900}")
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800}")
    private Long refreshExpiration;

    /**
     * 生成访问令牌
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT令牌
     */
    public String generateAccessToken(Long userId, String username) {
        return generateToken(userId, username, expiration);
    }

    /**
     * 生成刷新令牌
     * @param userId 用户ID
     * @param username 用户名
     * @return JWT刷新令牌
     */
    public String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, refreshExpiration);
    }

    /**
     * 生成JWT令牌
     * @param userId 用户ID
     * @param username 用户名
     * @param expirationTime 过期时间（秒）
     * @return JWT令牌
     */
    private String generateToken(Long userId, String username, Long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime * 1000);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("username", username)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 JWT令牌中获取用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            // 首先尝试从 claims 中获取 userId
            Object userIdObj = claims.get("userId");
            if (userIdObj != null) {
                if (userIdObj instanceof Integer) {
                    return ((Integer) userIdObj).longValue();
                } else if (userIdObj instanceof Long) {
                    return (Long) userIdObj;
                } else {
                    return Long.parseLong(userIdObj.toString());
                }
            }
            // 如果 userId 不存在，从 subject 中获取
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            log.error("JWT令牌中的用户ID格式错误: {}", e.getMessage());
            throw new RuntimeException("用户ID格式错误");
        }
    }

    /**
     * 从JWT令牌中获取用户名
     * @param token JWT令牌
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 验证JWT令牌是否有效
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证JWT令牌是否过期
     * @param token JWT令牌
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT令牌过期验证失败: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 刷新JWT令牌
     * @param token 原JWT令牌
     * @return 新的JWT令牌
     */
    public String refreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            String username = claims.get("username", String.class);
            return generateAccessToken(userId, username);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT令牌刷新失败: {}", e.getMessage());
            throw new RuntimeException("令牌刷新失败");
        }
    }

    /**
     * 解析JWT令牌
     * @param token JWT令牌
     * @return 声明信息
     */
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取JWT令牌的剩余有效时间（秒）
     * @param token JWT令牌
     * @return 剩余有效时间（秒）
     */
    public Long getRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            return Math.max(0, (expiration.getTime() - now.getTime()) / 1000);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("获取JWT令牌剩余时间失败: {}", e.getMessage());
            return 0L;
        }
    }
}